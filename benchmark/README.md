# vgo vs. SVGO benchmark

This harness compares the production vgo and SVGO command-line tools. It reports end-to-end runtime and optimized output size over identical, isolated copies of an SVG corpus.

## Prerequisites

- The repository-supported JDK
- Node.js and npm
- [Hyperfine](https://github.com/sharkdp/hyperfine)

Install the pinned Node dependencies once:

```shell
npm --prefix benchmark ci
```

## Running

Run the default benchmark over the ten top-level SVG fixtures:

```shell
npm --prefix benchmark run bench
```

Use a custom corpus or shorter exploratory run:

```shell
npm --prefix benchmark run bench -- --corpus ./icons --warmup 1 --runs 3
```

The harness recursively discovers custom corpora, preserves relative paths, and applies the same batch boundaries to both tools. Run `npm --prefix benchmark run bench -- --help` for all options.

The optimized vgo binary is built before measurement. Dependency installation, compilation, corpus copying, validation, and report generation are excluded from timing. Timed samples include process startup, parsing, optimization, and output writes.

Reports are written beneath `benchmark/results/`:

- `hyperfine.json` contains the raw timing samples.
- `summary.json` contains timing, size, version, and per-file data.
- `summary.md` presents the same results as readable tables.

Each optimizer is also run twice outside the timed samples. The harness requires successful exits, the expected file inventory, parseable SVG output, and byte-identical repeated output. These structural checks do not establish visual equivalence.

Compare reports only when they were collected on the same machine under similar system load. SVGO is intentionally run with its pinned default, single-pass preset.
