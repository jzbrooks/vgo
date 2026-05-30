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
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.command.ClosePath
import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import com.jzbrooks.vgo.core.graphic.command.LineTo
import com.jzbrooks.vgo.core.graphic.command.MoveTo
import com.jzbrooks.vgo.core.util.ExperimentalVgoApi
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
