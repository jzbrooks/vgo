package com.jzbrooks.vgo.core.util.ir

import com.jzbrooks.vgo.core.Brush
import com.jzbrooks.vgo.core.Color
import com.jzbrooks.vgo.core.Colors
import com.jzbrooks.vgo.core.HexFormat
import com.jzbrooks.vgo.core.LinearGradient
import com.jzbrooks.vgo.core.RadialGradient
import com.jzbrooks.vgo.core.SweepGradient
import com.jzbrooks.vgo.core.graphic.Circle
import com.jzbrooks.vgo.core.graphic.ClipPath
import com.jzbrooks.vgo.core.graphic.Element
import com.jzbrooks.vgo.core.graphic.ElementVisitor
import com.jzbrooks.vgo.core.graphic.Ellipse
import com.jzbrooks.vgo.core.graphic.Extra
import com.jzbrooks.vgo.core.graphic.Graphic
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.Line
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.Polygon
import com.jzbrooks.vgo.core.graphic.Polyline
import com.jzbrooks.vgo.core.graphic.Rect
import com.jzbrooks.vgo.core.graphic.Shape
import com.jzbrooks.vgo.core.graphic.command.ClosePath
import com.jzbrooks.vgo.core.graphic.command.Command
import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import com.jzbrooks.vgo.core.graphic.command.CubicBezierCurve
import com.jzbrooks.vgo.core.graphic.command.EllipticalArcCurve
import com.jzbrooks.vgo.core.graphic.command.HorizontalLineTo
import com.jzbrooks.vgo.core.graphic.command.LineTo
import com.jzbrooks.vgo.core.graphic.command.MoveTo
import com.jzbrooks.vgo.core.graphic.command.QuadraticBezierCurve
import com.jzbrooks.vgo.core.graphic.command.SmoothCubicBezierCurve
import com.jzbrooks.vgo.core.graphic.command.SmoothQuadraticBezierCurve
import com.jzbrooks.vgo.core.graphic.command.VerticalLineTo
import com.jzbrooks.vgo.core.util.math.Matrix3
import com.jzbrooks.vgo.core.util.math.Point
import java.io.PrintStream

class IrPrinter(
    private val out: PrintStream = System.out,
    private val colorScheme: IrColorScheme = PlainColorScheme,
) : ElementVisitor {
    private val prefixStack = ArrayDeque<String>()

    private fun prefix() = prefixStack.joinToString("")

    private fun renderChildren(items: List<() -> Unit>) {
        items.forEachIndexed { i, render ->
            val isLast = i == items.lastIndex
            out.print(colorScheme.dim(prefix() + if (isLast) "└── " else "├── "))
            prefixStack.add(if (isLast) "    " else "│   ")
            render()
            prefixStack.removeLast()
        }
    }

    private fun renderElementChildren(elements: List<Element>) {
        renderChildren(elements.map { el -> { el.accept(this) } })
    }

    override fun visit(graphic: Graphic) {
        val label =
            buildString {
                append(colorScheme.bold(graphic::class.simpleName ?: "Graphic"))
                graphic.id?.let { append(" " + colorScheme.cyan("[$it]")) }
                val vw = graphic.foreign["android:viewportWidth"]
                val vh = graphic.foreign["android:viewportHeight"]
                if (vw != null && vh != null) {
                    append(colorScheme.dim(" (${vw}x$vh)"))
                } else {
                    val viewBox = graphic.foreign["viewBox"]
                    if (viewBox != null) append(colorScheme.dim(" viewBox=$viewBox"))
                }
            }
        out.println(label)
        renderElementChildren(graphic.elements)
    }

    override fun visit(group: Group) {
        val label =
            buildString {
                append(colorScheme.bold("Group"))
                group.id?.let { append(" " + colorScheme.cyan("[$it]")) }
                if (group.transform != Matrix3.IDENTITY) append(colorScheme.dim(" transform=..."))
                if (group.clipPaths.isNotEmpty()) append(colorScheme.dim(" ${group.clipPaths.size} clip(s)"))
            }
        out.println(label)

        val clipPathItems: List<() -> Unit> = group.clipPaths.map { cp -> { renderClipPath(cp) } }
        val elementItems: List<() -> Unit> = group.elements.map { el -> { el.accept(this) } }
        renderChildren(clipPathItems + elementItems)
    }

    override fun visit(path: Path) {
        val label =
            buildString {
                append(colorScheme.bold("Path"))
                path.id?.let { append(" " + colorScheme.cyan("[$it]")) }
                append(" fill=").append(formatBrush(path.fill))
                if (path.stroke != Colors.TRANSPARENT) {
                    append(" stroke=").append(formatBrush(path.stroke))
                    append(" sw=").append(formatFloat(path.strokeWidth))
                }
                append(colorScheme.dim(" (${path.commands.size} cmds)"))
            }
        out.println(label)
        renderChildren(path.commands.map { cmd -> { out.println(formatCommand(cmd)) } })
    }

    override fun visit(extra: Extra) {
        val label =
            buildString {
                append(colorScheme.bold("Extra"))
                extra.id?.let { append(" " + colorScheme.cyan("[$it]")) }
                append(colorScheme.dim(" <${extra.name}>"))
                if (extra.elements.isNotEmpty()) append(colorScheme.dim(" (${extra.elements.size})"))
            }
        out.println(label)
        if (extra.elements.isNotEmpty()) renderElementChildren(extra.elements)
    }

    override fun visit(shape: Shape) {
        val label =
            buildString {
                append(colorScheme.bold(shape::class.simpleName ?: "Shape"))
                shape.id?.let { append(" " + colorScheme.cyan("[$it]")) }
                append(" fill=").append(formatBrush(shape.fill))
                if (shape.stroke != Colors.TRANSPARENT) {
                    append(" stroke=").append(formatBrush(shape.stroke))
                }
                when (shape) {
                    is Circle -> {
                        val cx = formatFloat(shape.cx)
                        val cy = formatFloat(shape.cy)
                        val r = formatFloat(shape.r)
                        append(colorScheme.dim(" cx=$cx cy=$cy r=$r"))
                    }

                    is Ellipse -> {
                        val cx = formatFloat(shape.cx)
                        val cy = formatFloat(shape.cy)
                        val rx = formatFloat(shape.rx)
                        val ry = formatFloat(shape.ry)
                        append(colorScheme.dim(" cx=$cx cy=$cy rx=$rx ry=$ry"))
                    }

                    is Rect -> {
                        val x = formatFloat(shape.x)
                        val y = formatFloat(shape.y)
                        val w = formatFloat(shape.width)
                        val h = formatFloat(shape.height)
                        append(colorScheme.dim(" x=$x y=$y w=$w h=$h"))
                    }

                    is Line -> {
                        val x1 = formatFloat(shape.x1)
                        val y1 = formatFloat(shape.y1)
                        val x2 = formatFloat(shape.x2)
                        val y2 = formatFloat(shape.y2)
                        append(colorScheme.dim(" ($x1,$y1)->($x2,$y2)"))
                    }

                    is Polyline -> {
                        append(colorScheme.dim(" (${shape.points.size} pts)"))
                    }

                    is Polygon -> {
                        append(colorScheme.dim(" (${shape.points.size} pts)"))
                    }
                }
            }
        out.println(label)
    }

    private fun renderClipPath(cp: ClipPath) {
        val label =
            buildString {
                append(colorScheme.bold("ClipPath"))
                cp.id?.let { append(" " + colorScheme.cyan("[$it]")) }
                append(colorScheme.dim(" (${cp.regions.size})"))
            }
        out.println(label)
        renderChildren(cp.regions.map { path -> { path.accept(this) } })
    }

    private fun formatCommand(cmd: Command): String =
        when (cmd) {
            is ClosePath -> {
                colorScheme.green("Z").toString()
            }

            is MoveTo -> {
                letter(cmd.variant, "M", "m") + " " + cmd.parameters.joinToString(" ") { formatPoint(it) }
            }

            is LineTo -> {
                letter(cmd.variant, "L", "l") + " " + cmd.parameters.joinToString(" ") { formatPoint(it) }
            }

            is HorizontalLineTo -> {
                letter(cmd.variant, "H", "h") + " " + cmd.parameters.joinToString(" ") { formatFloat(it) }
            }

            is VerticalLineTo -> {
                letter(cmd.variant, "V", "v") + " " + cmd.parameters.joinToString(" ") { formatFloat(it) }
            }

            is CubicBezierCurve -> {
                letter(cmd.variant, "C", "c") + " " +
                    cmd.parameters.joinToString(" ") { p ->
                        "${formatPoint(p.startControl)} ${formatPoint(p.endControl)} ${formatPoint(p.end)}"
                    }
            }

            is SmoothCubicBezierCurve -> {
                letter(cmd.variant, "S", "s") + " " +
                    cmd.parameters.joinToString(" ") { p ->
                        "${formatPoint(p.endControl)} ${formatPoint(p.end)}"
                    }
            }

            is QuadraticBezierCurve -> {
                letter(cmd.variant, "Q", "q") + " " +
                    cmd.parameters.joinToString(" ") { p ->
                        "${formatPoint(p.control)} ${formatPoint(p.end)}"
                    }
            }

            is SmoothQuadraticBezierCurve -> {
                letter(cmd.variant, "T", "t") + " " + cmd.parameters.joinToString(" ") { formatPoint(it) }
            }

            is EllipticalArcCurve -> {
                letter(cmd.variant, "A", "a") + " " +
                    cmd.parameters.joinToString(" ") { p ->
                        val arc = if (p.arc == EllipticalArcCurve.ArcFlag.LARGE) 1 else 0
                        val sweep = if (p.sweep == EllipticalArcCurve.SweepFlag.CLOCKWISE) 1 else 0
                        "${formatFloat(p.radiusX)},${formatFloat(p.radiusY)} ${formatFloat(p.angle)} $arc,$sweep ${formatPoint(p.end)}"
                    }
            }
        }

    private fun letter(
        variant: CommandVariant,
        abs: String,
        rel: String,
    ): String = colorScheme.green(if (variant == CommandVariant.ABSOLUTE) abs else rel).toString()

    private fun formatBrush(brush: Brush): CharSequence =
        when (brush) {
            is Color -> {
                val hex = brush.toHexString(HexFormat.ARGB)
                val swatch = colorScheme.colorSwatch(brush.red.toInt(), brush.green.toInt(), brush.blue.toInt())
                if (swatch.isEmpty()) colorScheme.yellow(hex) else "$swatch ${colorScheme.yellow(hex)}"
            }

            is LinearGradient -> {
                colorScheme.dim("linear-gradient(${brush.stops.size} stops)")
            }

            is RadialGradient -> {
                colorScheme.dim("radial-gradient(${brush.stops.size} stops)")
            }

            is SweepGradient -> {
                colorScheme.dim("sweep-gradient(${brush.stops.size} stops)")
            }
        }

    private fun formatPoint(p: Point): String = "${formatFloat(p.x)},${formatFloat(p.y)}"

    private fun formatFloat(f: Float): String = if (f % 1f == 0f) f.toInt().toString() else f.toString()
}
