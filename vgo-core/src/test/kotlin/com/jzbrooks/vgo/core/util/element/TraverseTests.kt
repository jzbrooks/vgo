package com.jzbrooks.vgo.core.util.element

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.jzbrooks.vgo.core.graphic.ContainerElement
import com.jzbrooks.vgo.core.graphic.Extra
import com.jzbrooks.vgo.core.graphic.Graphic
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.Shape
import com.jzbrooks.vgo.core.transformation.TopDownTransformer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TraverseTests {
    @Test
    fun testTopDownTransformerLifecycleIsScopedAndOrdered() {
        val events = mutableListOf<String>()
        val graphic = createGraphic(listOf(Group(listOf(createPath()))))

        traverseTopDown(
            graphic,
            listOf(
                RecordingTransformer("first", events),
                RecordingTransformer("second", events),
            ),
        )

        assertThat(events).isEqualTo(
            listOf(
                "first.visit.graphic",
                "second.visit.graphic",
                "first.visit.group",
                "second.visit.group",
                "first.visit.path",
                "second.visit.path",
                "second.exit.group",
                "first.exit.group",
                "second.exit.graphic",
                "first.exit.graphic",
            ),
        )
    }

    @Test
    fun testTopDownTransformerLifecycleUnwindsWhenVisitThrows() {
        val events = mutableListOf<String>()
        val graphic = createGraphic(listOf(Group(listOf(createPath()))))

        assertThrows<IllegalStateException> {
            traverseTopDown(graphic, listOf(RecordingTransformer("transformer", events, throwOnPath = true)))
        }

        assertThat(events).isEqualTo(
            listOf(
                "transformer.visit.graphic",
                "transformer.visit.group",
                "transformer.visit.path",
                "transformer.exit.group",
                "transformer.exit.graphic",
            ),
        )
    }

    private class RecordingTransformer(
        private val name: String,
        private val events: MutableList<String>,
        private val throwOnPath: Boolean = false,
    ) : TopDownTransformer {
        override fun exit(container: ContainerElement) = record("exit.${container.label}")

        override fun visit(graphic: Graphic) = record("visit.graphic")

        override fun visit(group: Group) = record("visit.group")

        override fun visit(extra: Extra) = record("visit.extra")

        override fun visit(shape: Shape) = record("visit.shape")

        override fun visit(path: Path) {
            record("visit.path")
            if (throwOnPath) error("failure")
        }

        private fun record(event: String) {
            events.add("$name.$event")
        }

        private val ContainerElement.label: String
            get() =
                when (this) {
                    is Graphic -> "graphic"
                    is Group -> "group"
                    is Extra -> "extra"
                    else -> error("Unexpected container: ${this::class}")
                }
    }
}
