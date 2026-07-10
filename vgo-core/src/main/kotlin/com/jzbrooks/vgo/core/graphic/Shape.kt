package com.jzbrooks.vgo.core.graphic

import com.jzbrooks.vgo.core.Brush
import com.jzbrooks.vgo.core.Color
import com.jzbrooks.vgo.core.util.math.Point
import java.util.Objects

sealed interface Shape : PaintedElement {
    /**
     * The fill paint. Unlike [fill], this can represent gradient paints.
     * The generated data class copy functions do not carry this property;
     * set it explicitly on copies.
     */
    var fillBrush: Brush

    /**
     * The stroke paint. Unlike [stroke], this can represent gradient paints.
     * The generated data class copy functions do not carry this property;
     * set it explicitly on copies.
     */
    var strokeBrush: Brush

    override fun accept(visitor: ElementVisitor) = visitor.visit(this)
}

data class Circle(
    override val id: String?,
    override val foreign: MutableMap<String, String>,
    val cx: Float,
    val cy: Float,
    val r: Float,
    @Deprecated(
        "Unreliable: holds a placeholder color when the paint is a gradient. Read fillBrush for the true paint.",
        ReplaceWith("fillBrush"),
    )
    override val fill: Color,
    override val fillRule: Path.FillRule,
    @Deprecated(
        "Unreliable: holds a placeholder color when the paint is a gradient. Read strokeBrush for the true paint.",
        ReplaceWith("strokeBrush"),
    )
    override val stroke: Color,
    override val strokeWidth: Float,
    override val strokeLineCap: Path.LineCap,
    override val strokeLineJoin: Path.LineJoin,
    override val strokeMiterLimit: Float,
) : Shape {
    @Suppress("DEPRECATION")
    override var fillBrush: Brush = fill

    @Suppress("DEPRECATION")
    override var strokeBrush: Brush = stroke

    @Suppress("DEPRECATION")
    override fun equals(other: Any?): Boolean =
        other is Circle &&
            id == other.id &&
            foreign == other.foreign &&
            cx == other.cx &&
            cy == other.cy &&
            r == other.r &&
            fill == other.fill &&
            fillRule == other.fillRule &&
            stroke == other.stroke &&
            strokeWidth == other.strokeWidth &&
            strokeLineCap == other.strokeLineCap &&
            strokeLineJoin == other.strokeLineJoin &&
            strokeMiterLimit == other.strokeMiterLimit &&
            fillBrush == other.fillBrush &&
            strokeBrush == other.strokeBrush

    @Suppress("DEPRECATION")
    override fun hashCode(): Int =
        Objects.hash(
            id,
            foreign,
            cx,
            cy,
            r,
            fill,
            fillRule,
            stroke,
            strokeWidth,
            strokeLineCap,
            strokeLineJoin,
            strokeMiterLimit,
            fillBrush,
            strokeBrush,
        )
}

data class Ellipse(
    override val id: String?,
    override val foreign: MutableMap<String, String>,
    val cx: Float,
    val cy: Float,
    val rx: Float,
    val ry: Float,
    @Deprecated(
        "Unreliable: holds a placeholder color when the paint is a gradient. Read fillBrush for the true paint.",
        ReplaceWith("fillBrush"),
    )
    override val fill: Color,
    override val fillRule: Path.FillRule,
    @Deprecated(
        "Unreliable: holds a placeholder color when the paint is a gradient. Read strokeBrush for the true paint.",
        ReplaceWith("strokeBrush"),
    )
    override val stroke: Color,
    override val strokeWidth: Float,
    override val strokeLineCap: Path.LineCap,
    override val strokeLineJoin: Path.LineJoin,
    override val strokeMiterLimit: Float,
) : Shape {
    @Suppress("DEPRECATION")
    override var fillBrush: Brush = fill

    @Suppress("DEPRECATION")
    override var strokeBrush: Brush = stroke

    @Suppress("DEPRECATION")
    override fun equals(other: Any?): Boolean =
        other is Ellipse &&
            id == other.id &&
            foreign == other.foreign &&
            cx == other.cx &&
            cy == other.cy &&
            rx == other.rx &&
            ry == other.ry &&
            fill == other.fill &&
            fillRule == other.fillRule &&
            stroke == other.stroke &&
            strokeWidth == other.strokeWidth &&
            strokeLineCap == other.strokeLineCap &&
            strokeLineJoin == other.strokeLineJoin &&
            strokeMiterLimit == other.strokeMiterLimit &&
            fillBrush == other.fillBrush &&
            strokeBrush == other.strokeBrush

    @Suppress("DEPRECATION")
    override fun hashCode(): Int =
        Objects.hash(
            id,
            foreign,
            cx,
            cy,
            rx,
            ry,
            fill,
            fillRule,
            stroke,
            strokeWidth,
            strokeLineCap,
            strokeLineJoin,
            strokeMiterLimit,
            fillBrush,
            strokeBrush,
        )
}

data class Rect(
    override val id: String?,
    override val foreign: MutableMap<String, String>,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val rx: Float,
    val ry: Float,
    @Deprecated(
        "Unreliable: holds a placeholder color when the paint is a gradient. Read fillBrush for the true paint.",
        ReplaceWith("fillBrush"),
    )
    override val fill: Color,
    override val fillRule: Path.FillRule,
    @Deprecated(
        "Unreliable: holds a placeholder color when the paint is a gradient. Read strokeBrush for the true paint.",
        ReplaceWith("strokeBrush"),
    )
    override val stroke: Color,
    override val strokeWidth: Float,
    override val strokeLineCap: Path.LineCap,
    override val strokeLineJoin: Path.LineJoin,
    override val strokeMiterLimit: Float,
) : Shape {
    @Suppress("DEPRECATION")
    override var fillBrush: Brush = fill

    @Suppress("DEPRECATION")
    override var strokeBrush: Brush = stroke

    @Suppress("DEPRECATION")
    override fun equals(other: Any?): Boolean =
        other is Rect &&
            id == other.id &&
            foreign == other.foreign &&
            x == other.x &&
            y == other.y &&
            width == other.width &&
            height == other.height &&
            rx == other.rx &&
            ry == other.ry &&
            fill == other.fill &&
            fillRule == other.fillRule &&
            stroke == other.stroke &&
            strokeWidth == other.strokeWidth &&
            strokeLineCap == other.strokeLineCap &&
            strokeLineJoin == other.strokeLineJoin &&
            strokeMiterLimit == other.strokeMiterLimit &&
            fillBrush == other.fillBrush &&
            strokeBrush == other.strokeBrush

    @Suppress("DEPRECATION")
    override fun hashCode(): Int =
        Objects.hash(
            id,
            foreign,
            x,
            y,
            width,
            height,
            rx,
            ry,
            fill,
            fillRule,
            stroke,
            strokeWidth,
            strokeLineCap,
            strokeLineJoin,
            strokeMiterLimit,
            fillBrush,
            strokeBrush,
        )
}

data class Line(
    override val id: String?,
    override val foreign: MutableMap<String, String>,
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    @Deprecated(
        "Unreliable: holds a placeholder color when the paint is a gradient. Read fillBrush for the true paint.",
        ReplaceWith("fillBrush"),
    )
    override val fill: Color,
    override val fillRule: Path.FillRule,
    @Deprecated(
        "Unreliable: holds a placeholder color when the paint is a gradient. Read strokeBrush for the true paint.",
        ReplaceWith("strokeBrush"),
    )
    override val stroke: Color,
    override val strokeWidth: Float,
    override val strokeLineCap: Path.LineCap,
    override val strokeLineJoin: Path.LineJoin,
    override val strokeMiterLimit: Float,
) : Shape {
    @Suppress("DEPRECATION")
    override var fillBrush: Brush = fill

    @Suppress("DEPRECATION")
    override var strokeBrush: Brush = stroke

    @Suppress("DEPRECATION")
    override fun equals(other: Any?): Boolean =
        other is Line &&
            id == other.id &&
            foreign == other.foreign &&
            x1 == other.x1 &&
            y1 == other.y1 &&
            x2 == other.x2 &&
            y2 == other.y2 &&
            fill == other.fill &&
            fillRule == other.fillRule &&
            stroke == other.stroke &&
            strokeWidth == other.strokeWidth &&
            strokeLineCap == other.strokeLineCap &&
            strokeLineJoin == other.strokeLineJoin &&
            strokeMiterLimit == other.strokeMiterLimit &&
            fillBrush == other.fillBrush &&
            strokeBrush == other.strokeBrush

    @Suppress("DEPRECATION")
    override fun hashCode(): Int =
        Objects.hash(
            id,
            foreign,
            x1,
            y1,
            x2,
            y2,
            fill,
            fillRule,
            stroke,
            strokeWidth,
            strokeLineCap,
            strokeLineJoin,
            strokeMiterLimit,
            fillBrush,
            strokeBrush,
        )
}

data class Polyline(
    override val id: String?,
    override val foreign: MutableMap<String, String>,
    val points: List<Point>,
    @Deprecated(
        "Unreliable: holds a placeholder color when the paint is a gradient. Read fillBrush for the true paint.",
        ReplaceWith("fillBrush"),
    )
    override val fill: Color,
    override val fillRule: Path.FillRule,
    @Deprecated(
        "Unreliable: holds a placeholder color when the paint is a gradient. Read strokeBrush for the true paint.",
        ReplaceWith("strokeBrush"),
    )
    override val stroke: Color,
    override val strokeWidth: Float,
    override val strokeLineCap: Path.LineCap,
    override val strokeLineJoin: Path.LineJoin,
    override val strokeMiterLimit: Float,
) : Shape {
    @Suppress("DEPRECATION")
    override var fillBrush: Brush = fill

    @Suppress("DEPRECATION")
    override var strokeBrush: Brush = stroke

    @Suppress("DEPRECATION")
    override fun equals(other: Any?): Boolean =
        other is Polyline &&
            id == other.id &&
            foreign == other.foreign &&
            points == other.points &&
            fill == other.fill &&
            fillRule == other.fillRule &&
            stroke == other.stroke &&
            strokeWidth == other.strokeWidth &&
            strokeLineCap == other.strokeLineCap &&
            strokeLineJoin == other.strokeLineJoin &&
            strokeMiterLimit == other.strokeMiterLimit &&
            fillBrush == other.fillBrush &&
            strokeBrush == other.strokeBrush

    @Suppress("DEPRECATION")
    override fun hashCode(): Int =
        Objects.hash(
            id,
            foreign,
            points,
            fill,
            fillRule,
            stroke,
            strokeWidth,
            strokeLineCap,
            strokeLineJoin,
            strokeMiterLimit,
            fillBrush,
            strokeBrush,
        )
}

data class Polygon(
    override val id: String?,
    override val foreign: MutableMap<String, String>,
    val points: List<Point>,
    @Deprecated(
        "Unreliable: holds a placeholder color when the paint is a gradient. Read fillBrush for the true paint.",
        ReplaceWith("fillBrush"),
    )
    override val fill: Color,
    override val fillRule: Path.FillRule,
    @Deprecated(
        "Unreliable: holds a placeholder color when the paint is a gradient. Read strokeBrush for the true paint.",
        ReplaceWith("strokeBrush"),
    )
    override val stroke: Color,
    override val strokeWidth: Float,
    override val strokeLineCap: Path.LineCap,
    override val strokeLineJoin: Path.LineJoin,
    override val strokeMiterLimit: Float,
) : Shape {
    @Suppress("DEPRECATION")
    override var fillBrush: Brush = fill

    @Suppress("DEPRECATION")
    override var strokeBrush: Brush = stroke

    @Suppress("DEPRECATION")
    override fun equals(other: Any?): Boolean =
        other is Polygon &&
            id == other.id &&
            foreign == other.foreign &&
            points == other.points &&
            fill == other.fill &&
            fillRule == other.fillRule &&
            stroke == other.stroke &&
            strokeWidth == other.strokeWidth &&
            strokeLineCap == other.strokeLineCap &&
            strokeLineJoin == other.strokeLineJoin &&
            strokeMiterLimit == other.strokeMiterLimit &&
            fillBrush == other.fillBrush &&
            strokeBrush == other.strokeBrush

    @Suppress("DEPRECATION")
    override fun hashCode(): Int =
        Objects.hash(
            id,
            foreign,
            points,
            fill,
            fillRule,
            stroke,
            strokeWidth,
            strokeLineCap,
            strokeLineJoin,
            strokeMiterLimit,
            fillBrush,
            strokeBrush,
        )
}
