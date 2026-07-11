package com.jzbrooks.vgo.iv

import assertk.assertThat
import assertk.assertions.first
import assertk.assertions.hasSize
import assertk.assertions.index
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.prop
import assertk.assertions.single
import com.jzbrooks.vgo.core.Color
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.command.ClosePath
import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import com.jzbrooks.vgo.core.graphic.command.CubicBezierCurve
import com.jzbrooks.vgo.core.graphic.command.EllipticalArcCurve
import com.jzbrooks.vgo.core.graphic.command.LineTo
import com.jzbrooks.vgo.core.graphic.command.MoveTo
import com.jzbrooks.vgo.core.util.ExperimentalVgoApi
import com.jzbrooks.vgo.core.util.math.Point
import org.jetbrains.kotlin.com.intellij.openapi.Disposable
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.InputStream

@OptIn(ExperimentalVgoApi::class)
class ImageVectorReaderTest {
    private lateinit var inputStream: InputStream
    private lateinit var disposable: Disposable

    @BeforeEach
    fun setup() {
        inputStream = javaClass.getResourceAsStream("/star.kt")!!
        disposable = Disposer.newDisposable()
    }

    @AfterEach
    fun teardown() {
        disposable.dispose()
        inputStream.close()
    }

    @Test
    fun `graphic is parsed`() {
        val psiFile = parseKotlinFile(disposable, inputStream)
        val graphic = parse(psiFile)

        assertThat(graphic).isNotNull()
    }

    @Test
    fun `path elements are parsed`() {
        val psiFile = parseKotlinFile(disposable, inputStream)
        val graphic = parse(psiFile)

        assertThat(graphic::elements)
            .single()
            .isInstanceOf<Path>()
            .prop(Path::commands)
            .hasSize(11)
    }

    @Test
    fun `hex color literals are parsed`() {
        val psiFile = parseKotlinFile(disposable, inputStream)
        val graphic = parse(psiFile)

        assertThat(graphic::elements)
            .single()
            .isInstanceOf<Path>()
            .prop(Path::fill)
            .isEqualTo(Color(0xFFFFC107u))
    }

    @Test
    fun `compose color constants are parsed`() {
        val source =
            """
            import androidx.compose.ui.graphics.Color
            import androidx.compose.ui.graphics.SolidColor
            import androidx.compose.ui.graphics.vector.ImageVector
            import androidx.compose.ui.graphics.vector.path
            import androidx.compose.ui.unit.dp

            public val sample: ImageVector
              get() = _sample ?: ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
                .path(fill = SolidColor(Color.Red)) {
                  moveTo(2f, 2f)
                  lineTo(4f, 4f)
                  close()
                }
              .build().also { _sample = it }

            private var _sample: ImageVector? = null
            """.trimIndent()

        val graphic = parse(parseKotlinFile(disposable, ByteArrayInputStream(source.toByteArray())))

        assertThat(graphic::elements)
            .single()
            .isInstanceOf<Path>()
            .prop(Path::fill)
            .isEqualTo(Color(0xFFFF0000u))
    }

    @Test
    fun `float dp dimensions are parsed`() {
        val source =
            """
            import androidx.compose.ui.graphics.Color
            import androidx.compose.ui.graphics.SolidColor
            import androidx.compose.ui.graphics.vector.ImageVector
            import androidx.compose.ui.graphics.vector.path
            import androidx.compose.ui.unit.dp

            public val sample: ImageVector
              get() = _sample ?: ImageVector.Builder(defaultWidth = 24f.dp, defaultHeight = 12f.dp, viewportWidth = 24f, viewportHeight = 24f)
                .path(fill = SolidColor(Color(0, 0, 0, 255))) {
                  moveTo(2f, 2f)
                  close()
                }
              .build().also { _sample = it }

            private var _sample: ImageVector? = null
            """.trimIndent()

        val graphic = parse(parseKotlinFile(disposable, ByteArrayInputStream(source.toByteArray())))

        assertThat(graphic::defaultWidthDp).isEqualTo(24f)
        assertThat(graphic::defaultHeightDp).isEqualTo(12f)
    }

    @Test
    fun `positional builder arguments are parsed`() {
        val source =
            """
            import androidx.compose.ui.graphics.Color
            import androidx.compose.ui.graphics.SolidColor
            import androidx.compose.ui.graphics.vector.ImageVector
            import androidx.compose.ui.graphics.vector.path
            import androidx.compose.ui.unit.dp

            public val sample: ImageVector
              get() = _sample ?: ImageVector.Builder("sample", 24.dp, 24.dp, 12f, viewportHeight = 12f)
                .path(fill = SolidColor(Color(0, 0, 0, 255))) {
                  moveTo(2f, 2f)
                  close()
                }
              .build().also { _sample = it }

            private var _sample: ImageVector? = null
            """.trimIndent()

        val graphic = parse(parseKotlinFile(disposable, ByteArrayInputStream(source.toByteArray())))

        assertThat(graphic::id).isEqualTo("sample")
        assertThat(graphic::defaultWidthDp).isEqualTo(24f)
        assertThat(graphic::defaultHeightDp).isEqualTo(24f)
        assertThat(graphic::viewportWidth).isEqualTo(12f)
        assertThat(graphic::viewportHeight).isEqualTo(12f)
    }

    @Test
    fun `apply block builder form is parsed`() {
        val source =
            """
            import androidx.compose.ui.graphics.Color
            import androidx.compose.ui.graphics.SolidColor
            import androidx.compose.ui.graphics.vector.ImageVector
            import androidx.compose.ui.graphics.vector.group
            import androidx.compose.ui.graphics.vector.path
            import androidx.compose.ui.unit.dp

            public val sample: ImageVector
              get() = _sample ?: ImageVector.Builder(
                name = "sample",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
              ).apply {
                path(fill = SolidColor(Color(0xFF232F34))) {
                  moveTo(2f, 2f)
                  lineTo(4f, 4f)
                  close()
                }
                group(rotate = 45f) {
                  path(fill = SolidColor(Color(0, 0, 0, 255))) {
                    moveTo(6f, 6f)
                    close()
                  }
                }
              }.build().also { _sample = it }

            private var _sample: ImageVector? = null
            """.trimIndent()

        val graphic = parse(parseKotlinFile(disposable, ByteArrayInputStream(source.toByteArray())))

        assertThat(graphic::id).isEqualTo("sample")
        assertThat(graphic::elements).hasSize(2)
        assertThat(graphic::elements)
            .index(0)
            .isInstanceOf<Path>()
            .prop(Path::commands)
            .hasSize(3)
        assertThat(graphic::elements)
            .index(1)
            .isInstanceOf<Group>()
            .prop(Group::elements)
            .single()
            .isInstanceOf<Path>()
    }

    @Test
    fun `lazy delegate builder form is parsed`() {
        val source =
            """
            import androidx.compose.ui.graphics.Color
            import androidx.compose.ui.graphics.SolidColor
            import androidx.compose.ui.graphics.vector.ImageVector
            import androidx.compose.ui.graphics.vector.path
            import androidx.compose.ui.unit.dp

            public val sample: ImageVector by lazy {
                ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
                    .apply {
                        path(fill = SolidColor(Color(0, 0, 0, 255))) {
                            moveTo(2f, 2f)
                            close()
                        }
                    }.build()
            }
            """.trimIndent()

        val graphic = parse(parseKotlinFile(disposable, ByteArrayInputStream(source.toByteArray())))

        assertThat(graphic.foreign["propertyName"]).isEqualTo("sample")
        assertThat(graphic::elements)
            .single()
            .isInstanceOf<Path>()
            .prop(Path::commands)
            .hasSize(2)
    }

    @Test
    fun `imported builder without receiver is parsed`() {
        val source =
            """
            import androidx.compose.ui.graphics.Color
            import androidx.compose.ui.graphics.SolidColor
            import androidx.compose.ui.graphics.vector.ImageVector
            import androidx.compose.ui.graphics.vector.ImageVector.Builder
            import androidx.compose.ui.graphics.vector.path
            import androidx.compose.ui.unit.dp

            public val sample: ImageVector by lazy {
                Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
                    .apply {
                        path(fill = SolidColor(Color(0, 0, 0, 255))) {
                            moveTo(2f, 2f)
                            close()
                        }
                    }.build()
            }
            """.trimIndent()

        val graphic = parse(parseKotlinFile(disposable, ByteArrayInputStream(source.toByteArray())))

        assertThat(graphic::elements).single().isInstanceOf<Path>()
    }

    @Test
    fun `plain initializer builder form is parsed`() {
        val source =
            """
            import androidx.compose.ui.graphics.Color
            import androidx.compose.ui.graphics.SolidColor
            import androidx.compose.ui.graphics.vector.path
            import androidx.compose.ui.unit.dp

            val sample = androidx.compose.ui.graphics.vector.ImageVector.Builder(
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).path(fill = SolidColor(Color(0, 0, 0, 255))) {
                moveTo(2f, 2f)
                lineTo(4f, 4f)
                close()
            }.build()
            """.trimIndent()

        val graphic = parse(parseKotlinFile(disposable, ByteArrayInputStream(source.toByteArray())))

        assertThat(graphic.foreign["propertyName"]).isEqualTo("sample")
        assertThat(graphic::elements)
            .single()
            .isInstanceOf<Path>()
            .prop(Path::commands)
            .hasSize(3)
    }

    @Test
    fun `function builder form is parsed`() {
        val source =
            """
            import androidx.compose.ui.graphics.Color
            import androidx.compose.ui.graphics.SolidColor
            import androidx.compose.ui.graphics.vector.ImageVector
            import androidx.compose.ui.graphics.vector.path
            import androidx.compose.ui.unit.dp

            fun sampleIcon(): ImageVector =
                ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
                    .path(fill = SolidColor(Color(0, 0, 0, 255))) {
                        moveTo(2f, 2f)
                        close()
                    }.build()
            """.trimIndent()

        val graphic = parse(parseKotlinFile(disposable, ByteArrayInputStream(source.toByteArray())))

        assertThat(graphic.foreign["propertyName"]).isEqualTo("sampleIcon")
        assertThat(graphic::elements).single().isInstanceOf<Path>()
    }

    @Test
    fun `receiver qualified property keeps its package foreign key`() {
        val source =
            """
            import androidx.compose.ui.graphics.Color
            import androidx.compose.ui.graphics.SolidColor
            import androidx.compose.ui.graphics.vector.ImageVector
            import androidx.compose.ui.graphics.vector.path
            import androidx.compose.ui.unit.dp

            val Icons.Outlined.Sample: ImageVector
              get() = _sample ?: ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
                .path(fill = SolidColor(Color(0, 0, 0, 255))) {
                  moveTo(2f, 2f)
                  close()
                }
              .build().also { _sample = it }

            private var _sample: ImageVector? = null
            """.trimIndent()

        val graphic = parse(parseKotlinFile(disposable, ByteArrayInputStream(source.toByteArray())))

        assertThat(graphic.foreign["propertyName"]).isEqualTo("Sample")
        assertThat(graphic.foreign["packageName"]).isEqualTo("Icons.Outlined")
    }

    @Test
    fun `path styling arguments are parsed`() {
        val source =
            """
            import androidx.compose.ui.graphics.Color
            import androidx.compose.ui.graphics.PathFillType
            import androidx.compose.ui.graphics.SolidColor
            import androidx.compose.ui.graphics.StrokeCap
            import androidx.compose.ui.graphics.StrokeJoin
            import androidx.compose.ui.graphics.vector.ImageVector
            import androidx.compose.ui.graphics.vector.path
            import androidx.compose.ui.unit.dp

            public val sample: ImageVector
              get() = _sample ?: ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
                .path(
                  name = "outline",
                  fill = SolidColor(Color(0, 0, 0, 255)),
                  stroke = SolidColor(Color.White),
                  strokeLineWidth = 2f,
                  strokeLineCap = StrokeCap.Round,
                  strokeLineJoin = StrokeJoin.Bevel,
                  strokeLineMiter = 3f,
                  pathFillType = PathFillType.EvenOdd,
                ) {
                  moveTo(2f, 2f)
                  close()
                }
              .build().also { _sample = it }

            private var _sample: ImageVector? = null
            """.trimIndent()

        val graphic = parse(parseKotlinFile(disposable, ByteArrayInputStream(source.toByteArray())))

        val path = assertThat(graphic::elements).single().isInstanceOf<Path>()
        path.prop(Path::id).isEqualTo("outline")
        path.prop(Path::strokeWidth).isEqualTo(2f)
        path.prop(Path::strokeLineCap).isEqualTo(Path.LineCap.ROUND)
        path.prop(Path::strokeLineJoin).isEqualTo(Path.LineJoin.BEVEL)
        path.prop(Path::strokeMiterLimit).isEqualTo(3f)
        path.prop(Path::fillRule).isEqualTo(Path.FillRule.EVEN_ODD)
    }

    @Test
    fun `unspecified path fill type defaults to non-zero`() {
        val psiFile = parseKotlinFile(disposable, inputStream)
        val graphic = parse(psiFile)

        assertThat(graphic::elements)
            .single()
            .isInstanceOf<Path>()
            .prop(Path::fillRule)
            .isEqualTo(Path.FillRule.NON_ZERO)
    }

    @Test
    fun `positional path arguments are parsed`() {
        val source =
            """
            import androidx.compose.ui.graphics.Color
            import androidx.compose.ui.graphics.SolidColor
            import androidx.compose.ui.graphics.vector.ImageVector
            import androidx.compose.ui.graphics.vector.path
            import androidx.compose.ui.unit.dp

            public val sample: ImageVector
              get() = _sample ?: ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
                .path("outline", SolidColor(Color.Red), 0.5f) {
                  moveTo(2f, 2f)
                  close()
                }
              .build().also { _sample = it }

            private var _sample: ImageVector? = null
            """.trimIndent()

        val graphic = parse(parseKotlinFile(disposable, ByteArrayInputStream(source.toByteArray())))

        val path = assertThat(graphic::elements).single().isInstanceOf<Path>()
        path.prop(Path::id).isEqualTo("outline")
        path.prop(Path::fill).isEqualTo(Color(0x7FFF0000u))
    }

    @Test
    fun `named command arguments are parsed`() {
        val source =
            """
            import androidx.compose.ui.graphics.Color
            import androidx.compose.ui.graphics.SolidColor
            import androidx.compose.ui.graphics.vector.ImageVector
            import androidx.compose.ui.graphics.vector.path
            import androidx.compose.ui.unit.dp

            public val sample: ImageVector
              get() = _sample ?: ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
                .path(fill = SolidColor(Color(0, 0, 0, 255))) {
                  moveTo(y = 2f, x = 1f)
                  arcTo(
                    horizontalEllipseRadius = 5f,
                    verticalEllipseRadius = 6f,
                    theta = 0f,
                    isMoreThanHalf = true,
                    isPositiveArc = false,
                    x1 = 10f,
                    y1 = 11f,
                  )
                  curveTo(1f, 2f, x2 = 3f, y2 = 4f, x3 = 5f, y3 = 6f)
                  close()
                }
              .build().also { _sample = it }

            private var _sample: ImageVector? = null
            """.trimIndent()

        val graphic = parse(parseKotlinFile(disposable, ByteArrayInputStream(source.toByteArray())))

        val commands =
            assertThat(graphic::elements)
                .single()
                .isInstanceOf<Path>()
                .prop(Path::commands)
        commands.hasSize(4)
        commands
            .first()
            .isInstanceOf<MoveTo>()
            .prop(MoveTo::parameters)
            .single()
            .isEqualTo(Point(1f, 2f))
        val arc =
            commands
                .index(1)
                .isInstanceOf<EllipticalArcCurve>()
                .prop(EllipticalArcCurve::parameters)
                .single()
        arc.prop(EllipticalArcCurve.Parameter::radiusX).isEqualTo(5f)
        arc.prop(EllipticalArcCurve.Parameter::arc).isEqualTo(EllipticalArcCurve.ArcFlag.LARGE)
        arc.prop(EllipticalArcCurve.Parameter::sweep).isEqualTo(EllipticalArcCurve.SweepFlag.ANTICLOCKWISE)
        arc.prop(EllipticalArcCurve.Parameter::end).isEqualTo(Point(10f, 11f))
        commands
            .index(2)
            .isInstanceOf<CubicBezierCurve>()
            .prop(CubicBezierCurve::parameters)
            .single()
            .isEqualTo(CubicBezierCurve.Parameter(Point(1f, 2f), Point(3f, 4f), Point(5f, 6f)))
    }

    @Test
    fun `named path node arguments are parsed in clip path data`() {
        val source =
            """
            import androidx.compose.ui.graphics.Color
            import androidx.compose.ui.graphics.SolidColor
            import androidx.compose.ui.graphics.vector.ImageVector
            import androidx.compose.ui.graphics.vector.PathNode
            import androidx.compose.ui.graphics.vector.group
            import androidx.compose.ui.graphics.vector.path
            import androidx.compose.ui.unit.dp

            public val sample: ImageVector
              get() = _sample ?: ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
                .group(
                  clipPathData = listOf(
                    PathNode.MoveTo(y = 2f, x = 1f),
                    PathNode.LineTo(24f, 0f),
                    PathNode.Close,
                  ),
                ) {
                  path(fill = SolidColor(Color(0, 0, 0, 255))) {
                    moveTo(12f, 0f)
                    close()
                  }
                }
              .build().also { _sample = it }

            private var _sample: ImageVector? = null
            """.trimIndent()

        val graphic = parse(parseKotlinFile(disposable, ByteArrayInputStream(source.toByteArray())))

        assertThat(graphic::elements)
            .single()
            .isInstanceOf<Group>()
            .prop(Group::clipPaths)
            .single()
            .prop("regions") { it.regions }
            .single()
            .prop(Path::commands)
            .first()
            .isInstanceOf<MoveTo>()
            .prop(MoveTo::parameters)
            .single()
            .isEqualTo(Point(1f, 2f))
    }

    @Test
    fun `local builder variable form is parsed`() {
        val source =
            """
            import androidx.compose.ui.graphics.Color
            import androidx.compose.ui.graphics.SolidColor
            import androidx.compose.ui.graphics.vector.ImageVector
            import androidx.compose.ui.graphics.vector.group
            import androidx.compose.ui.graphics.vector.path
            import androidx.compose.ui.unit.dp

            public val sample: ImageVector
              get() {
                val builder = ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
                builder.path(fill = SolidColor(Color(0, 0, 0, 255))) {
                  moveTo(2f, 2f)
                  lineTo(4f, 4f)
                  close()
                }
                builder.group(rotate = 45f) {
                  path(fill = SolidColor(Color(0, 0, 0, 255))) {
                    moveTo(6f, 6f)
                    close()
                  }
                }
                return builder.build()
              }
            """.trimIndent()

        val graphic = parse(parseKotlinFile(disposable, ByteArrayInputStream(source.toByteArray())))

        assertThat(graphic.foreign["propertyName"]).isEqualTo("sample")
        assertThat(graphic::elements).hasSize(2)
        assertThat(graphic::elements)
            .index(0)
            .isInstanceOf<Path>()
            .prop(Path::commands)
            .hasSize(3)
        assertThat(graphic::elements)
            .index(1)
            .isInstanceOf<Group>()
            .prop(Group::elements)
            .single()
            .isInstanceOf<Path>()
    }

    @Test
    fun `clipPathData on a group is parsed into Group clipPaths`() {
        val source =
            """
            import androidx.compose.ui.graphics.Color
            import androidx.compose.ui.graphics.SolidColor
            import androidx.compose.ui.graphics.vector.ImageVector
            import androidx.compose.ui.graphics.vector.PathNode
            import androidx.compose.ui.graphics.vector.group
            import androidx.compose.ui.graphics.vector.path
            import androidx.compose.ui.unit.dp

            public val sample: ImageVector
              get() = _sample ?: ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
                .group(
                  clipPathData = listOf(
                    PathNode.MoveTo(0f, 0f),
                    PathNode.LineTo(24f, 0f),
                    PathNode.LineTo(24f, 24f),
                    PathNode.LineTo(0f, 24f),
                    PathNode.Close,
                  ),
                ) {
                  path(fill = SolidColor(Color(0, 0, 0, 255))) {
                    moveTo(12f, 0f)
                    lineTo(12f, 24f)
                    close()
                  }
                }
              .build().also { _sample = it }

            private var _sample: ImageVector? = null
            """.trimIndent()

        val graphic = parse(parseKotlinFile(disposable, ByteArrayInputStream(source.toByteArray())))

        val group =
            assertThat(graphic::elements)
                .single()
                .isInstanceOf<Group>()
        group
            .prop(Group::clipPaths)
            .single()
            .prop("regions") { it.regions }
            .single()
            .prop(Path::commands)
            .hasSize(5)
    }

    @Test
    fun `moveToRelative is parsed inside a path body`() {
        val source =
            """
            import androidx.compose.ui.graphics.Color
            import androidx.compose.ui.graphics.SolidColor
            import androidx.compose.ui.graphics.vector.ImageVector
            import androidx.compose.ui.graphics.vector.path
            import androidx.compose.ui.unit.dp

            public val sample: ImageVector
              get() = _sample ?: ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
                .path(fill = SolidColor(Color(0, 0, 0, 255))) {
                  moveTo(2f, 2f)
                  lineTo(4f, 4f)
                  close()
                  moveToRelative(5f, 5f)
                  lineTo(8f, 8f)
                  close()
                }
              .build().also { _sample = it }

            private var _sample: ImageVector? = null
            """.trimIndent()

        val graphic = parse(parseKotlinFile(disposable, ByteArrayInputStream(source.toByteArray())))

        val path =
            assertThat(graphic::elements)
                .single()
                .isInstanceOf<Path>()
        path
            .prop(Path::commands)
            .index(3)
            .isInstanceOf<MoveTo>()
            .prop(MoveTo::variant)
            .isEqualTo(CommandVariant.RELATIVE)
        path.prop(Path::commands).first().isInstanceOf<MoveTo>()
        path.prop(Path::commands).index(2).isInstanceOf<ClosePath>()
        path.prop(Path::commands).index(4).isInstanceOf<LineTo>()
    }
}
