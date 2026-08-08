import { createHash } from 'node:crypto';
import { spawnSync } from 'node:child_process';
import {
  cpSync,
  existsSync,
  mkdirSync,
  readFileSync,
  readdirSync,
  rmSync,
  statSync,
  writeFileSync,
} from 'node:fs';
import { dirname, join, relative, resolve, sep } from 'node:path';
import { fileURLToPath } from 'node:url';
import { optimize } from 'svgo';

const benchmarkRoot = dirname(fileURLToPath(import.meta.url));
const repositoryRoot = resolve(benchmarkRoot, '..');
const workRoot = join(benchmarkRoot, '.work');

function fail(message) {
  console.error(`Error: ${message}`);
  process.exit(1);
}

function parsePositiveInteger(value, flag) {
  const parsed = Number.parseInt(value, 10);
  if (!Number.isSafeInteger(parsed) || parsed < 1) fail(`${flag} must be a positive integer.`);
  return parsed;
}

function resolveUserPath(value) {
  return resolve(repositoryRoot, value);
}

function parseOptions(args) {
  const options = {
    corpus: join(repositoryRoot, 'vgo', 'src', 'test', 'resources'),
    customCorpus: false,
    runs: 10,
    warmup: 3,
    batchSize: 100,
    vgoBin: join(repositoryRoot, 'vgo-cli', 'build', 'libs', 'vgo'),
    hyperfine: 'hyperfine',
  };

  for (let index = 0; index < args.length; index += 1) {
    const flag = args[index];
    const value = args[index + 1];
    if (flag === '--help' || flag === '-h') {
      console.log(`Usage: npm run bench -- [options]

Options:
  --corpus PATH       recursively benchmark SVGs in PATH
  --runs N            measured Hyperfine runs (default: 10)
  --warmup N          Hyperfine warmup runs (default: 3)
  --batch-size N      files processed per CLI invocation (default: 100)
  --vgo-bin PATH      use an existing vgo executable
  --hyperfine PATH    use a specific Hyperfine executable`);
      process.exit(0);
    }
    if (!value || value.startsWith('--')) fail(`Missing value after ${flag}.`);
    switch (flag) {
      case '--corpus':
        options.corpus = resolveUserPath(value);
        options.customCorpus = true;
        break;
      case '--runs':
        options.runs = parsePositiveInteger(value, flag);
        break;
      case '--warmup':
        options.warmup = parsePositiveInteger(value, flag);
        break;
      case '--batch-size':
        options.batchSize = parsePositiveInteger(value, flag);
        break;
      case '--vgo-bin':
        options.vgoBin = resolveUserPath(value);
        break;
      case '--hyperfine':
        options.hyperfine = resolveUserPath(value);
        break;
      default:
        fail(`Unknown option ${flag}.`);
    }
    index += 1;
  }
  return options;
}

function listSvgFiles(root, topLevelOnly = false) {
  const files = [];
  function visit(directory) {
    for (const entry of readdirSync(directory, { withFileTypes: true })) {
      const path = join(directory, entry.name);
      if (entry.isDirectory() && !topLevelOnly) visit(path);
      else if (entry.isFile() && entry.name.toLowerCase().endsWith('.svg')) files.push(relative(root, path));
    }
  }
  visit(root);
  return files.sort((left, right) => left.localeCompare(right));
}

function listFiles(root) {
  const files = [];
  function visit(directory) {
    for (const entry of readdirSync(directory, { withFileTypes: true })) {
      const path = join(directory, entry.name);
      if (entry.isDirectory()) visit(path);
      else if (entry.isFile()) files.push(relative(root, path));
    }
  }
  visit(root);
  return files.sort((left, right) => left.localeCompare(right));
}

function run(command, args, options = {}) {
  const result = spawnSync(command, args, { encoding: 'utf8', ...options });
  if (result.error) throw result.error;
  if (result.status !== 0) {
    throw new Error(
      `${command} exited with ${result.status}.\n${result.stdout ?? ''}${result.stderr ?? ''}`.trim(),
    );
  }
  return (result.stdout ?? '').trim();
}

function copyCorpus(config, inputRoot) {
  rmSync(inputRoot, { recursive: true, force: true });
  for (const path of config.files) {
    const target = join(inputRoot, path);
    mkdirSync(dirname(target), { recursive: true });
    cpSync(join(config.corpus, path), target);
  }
}

function prepareTimed(config) {
  for (const tool of ['vgo', 'svgo']) {
    const root = join(workRoot, 'timed', tool);
    rmSync(root, { recursive: true, force: true });
    copyCorpus(config, join(root, 'input'));
    mkdirSync(join(root, 'output'), { recursive: true });
  }
}

function runTool(config, tool, inputRoot, outputRoot) {
  mkdirSync(outputRoot, { recursive: true });
  for (let offset = 0; offset < config.files.length; offset += config.batchSize) {
    const batch = config.files.slice(offset, offset + config.batchSize);
    const inputs = batch.map((path) => join(inputRoot, path));
    const outputs = batch.map((path) => join(outputRoot, path));
    for (const output of outputs) mkdirSync(dirname(output), { recursive: true });

    if (tool === 'vgo') {
      const outputArgs = outputs.flatMap((output) => ['--output', output]);
      run(config.vgoBin, [...outputArgs, ...inputs]);
    } else if (tool === 'svgo') {
      run(config.svgoBin, [...inputs, '--output', ...outputs, '--quiet']);
    } else {
      throw new Error(`Unsupported tool ${tool}.`);
    }
  }
}

function shellQuote(value) {
  return `'${value.replaceAll("'", `'\\''`)}'`;
}

function internalCommand(configPath, operation, tool) {
  return [process.execPath, fileURLToPath(import.meta.url), operation, configPath, tool]
    .filter(Boolean)
    .map(shellQuote)
    .join(' ');
}

function validateSvg(path) {
  const source = readFileSync(path, 'utf8');
  try {
    optimize(source, { plugins: [] });
  } catch (error) {
    throw new Error(`${path} is not well-formed SVG: ${error.message}`);
  }
}

function materializeAndValidate(config, tool) {
  const validationRoot = join(workRoot, 'validation', tool);
  rmSync(validationRoot, { recursive: true, force: true });
  const outputRoots = [];

  for (const runNumber of [1, 2]) {
    const runRoot = join(validationRoot, `run-${runNumber}`);
    const inputRoot = join(runRoot, 'input');
    const outputRoot = join(runRoot, 'output');
    copyCorpus(config, inputRoot);
    runTool(config, tool, inputRoot, outputRoot);
    outputRoots.push(outputRoot);
  }

  for (const path of config.files) {
    const first = join(outputRoots[0], path);
    const second = join(outputRoots[1], path);
    if (!existsSync(first) || !statSync(first).isFile()) throw new Error(`${tool} did not produce ${path}.`);
    if (!existsSync(second) || !statSync(second).isFile()) throw new Error(`${tool} did not reproduce ${path}.`);
    validateSvg(first);
    validateSvg(second);
    const firstHash = createHash('sha256').update(readFileSync(first)).digest('hex');
    const secondHash = createHash('sha256').update(readFileSync(second)).digest('hex');
    if (firstHash !== secondHash) throw new Error(`${tool} produced nondeterministic output for ${path}.`);
  }

  const actual = listFiles(outputRoots[0]);
  if (JSON.stringify(actual) !== JSON.stringify(config.files)) {
    throw new Error(`${tool} output inventory does not match the input manifest.`);
  }
  return outputRoots[0];
}

function bytes(path) {
  return statSync(path).size;
}

function formatSeconds(value) {
  return `${value.toFixed(3)} s`;
}

function escapeMarkdown(value) {
  return value.replaceAll('|', '\\|');
}

function createReports(config, hyperfineJson, outputRoots, versions, reportRoot) {
  const timingByTool = Object.fromEntries(hyperfineJson.results.map((result) => [result.command, result]));
  const originalBytes = config.files.reduce((total, path) => total + bytes(join(config.corpus, path)), 0);
  const fileResults = config.files.map((path) => {
    const original = bytes(join(config.corpus, path));
    const vgo = bytes(join(outputRoots.vgo, path));
    const svgo = bytes(join(outputRoots.svgo, path));
    return { path, original, vgo, svgo, winner: vgo === svgo ? 'tie' : vgo < svgo ? 'vgo' : 'svgo' };
  });
  const sizeByTool = {
    vgo: fileResults.reduce((total, file) => total + file.vgo, 0),
    svgo: fileResults.reduce((total, file) => total + file.svgo, 0),
  };
  const tools = ['vgo', 'svgo'].map((tool) => {
    const timing = timingByTool[tool];
    if (!timing) throw new Error(`Hyperfine result for ${tool} is missing.`);
    return {
      tool,
      version: versions[tool],
      meanSeconds: timing.mean,
      standardDeviationSeconds: timing.stddev ?? 0,
      medianSeconds: timing.median,
      minSeconds: timing.min,
      maxSeconds: timing.max,
      outputBytes: sizeByTool[tool],
      percentSaved: ((originalBytes - sizeByTool[tool]) / originalBytes) * 100,
      throughputMiBPerSecond: originalBytes / 1024 / 1024 / timing.mean,
      relativeSpeed: timing.mean / Math.min(timingByTool.vgo.mean, timingByTool.svgo.mean),
    };
  });
  const summary = {
    generatedAt: new Date().toISOString(),
    corpus: config.corpus,
    fileCount: config.files.length,
    originalBytes,
    settings: { runs: config.runs, warmup: config.warmup, batchSize: config.batchSize },
    versions,
    tools,
    files: fileResults,
  };
  writeFileSync(join(reportRoot, 'summary.json'), `${JSON.stringify(summary, null, 2)}\n`);

  const markdown = [
    '# vgo vs. SVGO benchmark',
    '',
    `Generated: ${summary.generatedAt}`,
    '',
    `Corpus: \`${escapeMarkdown(config.corpus)}\` (${config.files.length} files, ${originalBytes} bytes)`,
    '',
    `Runs: ${config.runs}; warmups: ${config.warmup}; batch size: ${config.batchSize}`,
    '',
    '| Tool | Version | Mean ± σ | Median | Min | Max | Output bytes | Saved | MiB/s | Relative |',
    '|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|',
    ...tools.map((tool) =>
      `| ${tool.tool} | ${escapeMarkdown(tool.version)} | ${formatSeconds(tool.meanSeconds)} ± ${formatSeconds(tool.standardDeviationSeconds)} | ${formatSeconds(tool.medianSeconds)} | ${formatSeconds(tool.minSeconds)} | ${formatSeconds(tool.maxSeconds)} | ${tool.outputBytes} | ${tool.percentSaved.toFixed(2)}% | ${tool.throughputMiBPerSecond.toFixed(2)} | ${tool.relativeSpeed.toFixed(2)}× |`,
    ),
    '',
    '| File | Original | vgo | SVGO | Smaller output |',
    '|---|---:|---:|---:|---|',
    ...fileResults.map((file) =>
      `| ${escapeMarkdown(file.path)} | ${file.original} | ${file.vgo} | ${file.svgo} | ${file.winner} |`,
    ),
    '',
    '> Timing includes CLI startup, parsing, optimization, and writes. Corpus setup and validation are excluded. Structural validation does not prove visual equivalence.',
    '',
  ].join('\n');
  writeFileSync(join(reportRoot, 'summary.md'), markdown);
}

function loadConfig(path) {
  return JSON.parse(readFileSync(path, 'utf8'));
}

function runInternal(args) {
  const [operation, configPath, tool] = args;
  const config = loadConfig(configPath);
  if (operation === '__prepare') prepareTimed(config);
  else if (operation === '__run') runTool(config, tool, join(workRoot, 'timed', tool, 'input'), join(workRoot, 'timed', tool, 'output'));
  else fail(`Unknown internal operation ${operation}.`);
}

function main(args) {
  if (args[0]?.startsWith('__')) return runInternal(args);
  const options = parseOptions(args);
  if (!existsSync(options.corpus) || !statSync(options.corpus).isDirectory()) fail(`Corpus directory not found: ${options.corpus}`);

  const files = listSvgFiles(options.corpus, !options.customCorpus);
  if (files.length === 0) fail(`No SVG files found in ${options.corpus}.`);

  if (!args.includes('--vgo-bin')) {
    console.log('Building the optimized vgo CLI...');
    run(join(repositoryRoot, 'gradlew'), [':vgo-cli:binary'], { cwd: repositoryRoot, stdio: 'inherit' });
  }
  if (!existsSync(options.vgoBin)) fail(`vgo executable not found: ${options.vgoBin}`);

  const svgoBin = join(benchmarkRoot, 'node_modules', '.bin', process.platform === 'win32' ? 'svgo.cmd' : 'svgo');
  if (!existsSync(svgoBin)) fail('SVGO is not installed. Run `npm --prefix benchmark ci`.');
  try {
    run(options.hyperfine, ['--version']);
  } catch {
    fail('Hyperfine is required. Install it or pass --hyperfine PATH.');
  }

  rmSync(workRoot, { recursive: true, force: true });
  mkdirSync(workRoot, { recursive: true });
  const configPath = join(workRoot, 'config.json');
  const config = {
    corpus: options.corpus,
    files,
    runs: options.runs,
    warmup: options.warmup,
    batchSize: options.batchSize,
    vgoBin: options.vgoBin,
    svgoBin,
  };
  writeFileSync(configPath, JSON.stringify(config));

  const stamp = new Date().toISOString().replaceAll(':', '-').replace(/\.\d{3}Z$/, 'Z');
  const reportRoot = join(benchmarkRoot, 'results', stamp);
  mkdirSync(reportRoot, { recursive: true });
  const rawResult = join(reportRoot, 'hyperfine.json');
  const prepare = internalCommand(configPath, '__prepare');
  const vgoCommand = internalCommand(configPath, '__run', 'vgo');
  const svgoCommand = internalCommand(configPath, '__run', 'svgo');

  console.log(`Benchmarking ${files.length} SVG files...`);
  run(options.hyperfine, [
    '--warmup', String(options.warmup),
    '--runs', String(options.runs),
    '--prepare', prepare,
    '--export-json', rawResult,
    '--command-name', 'vgo', vgoCommand,
    '--command-name', 'svgo', svgoCommand,
  ], { stdio: 'inherit' });

  console.log('Validating outputs and generating reports...');
  const outputRoots = {
    vgo: materializeAndValidate(config, 'vgo'),
    svgo: materializeAndValidate(config, 'svgo'),
  };
  const javaVersionResult = spawnSync('java', ['-version'], { encoding: 'utf8' });
  if (javaVersionResult.error || javaVersionResult.status !== 0) fail('Unable to read the Java version.');
  const versions = {
    node: process.version,
    java: (javaVersionResult.stderr || javaVersionResult.stdout).trim(),
    hyperfine: run(options.hyperfine, ['--version']),
    vgo: run(options.vgoBin, ['--version']),
    svgo: run(svgoBin, ['--version']),
  };
  createReports(config, JSON.parse(readFileSync(rawResult, 'utf8')), outputRoots, versions, reportRoot);
  console.log(`Reports written to ${relative(repositoryRoot, reportRoot)}.`);
}

try {
  main(process.argv.slice(2));
} catch (error) {
  fail(error.stack ?? error.message);
}
