package com.jzbrooks.vgo.vd.optimization

import assertk.assertThat
import assertk.assertions.isEmpty
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.vd.VectorDrawable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class RemoveRedundantAttributesTests {
    @ParameterizedTest
    @MethodSource("provideVectorDrawableAttributes")
    fun testVectorDrawableDefaultsRemoved(key: String, value: String) {
        val vectorDrawable = VectorDrawable(emptyList(), null, mutableMapOf(key to value))
        RemoveRedundantAttributes().visit(vectorDrawable)
        assertThat(vectorDrawable.foreign).isEmpty()
    }

    @ParameterizedTest
    @MethodSource("providePathElementAttributes")
    fun testVectorDrawableAutoMirrored(key: String, value: String) {
        val path = Path(emptyList(), null, mutableMapOf(key to value))
        RemoveRedundantAttributes().visit(path)
        assertThat(path.foreign).isEmpty()
    }

    companion object {
        @JvmStatic
        fun provideVectorDrawableAttributes() = listOf(
            Arguments.of("android:alpha", "1.0"),
            Arguments.of("android:autoMirrored", "false"),
            Arguments.of("android:tintMode", "src_in")
        )

        @JvmStatic
        fun providePathElementAttributes() = listOf(
            Arguments.of("android:strokeWidth", "0"),
            Arguments.of("android:strokeAlpha", "1"),
            Arguments.of("android:strokeLineCap", "butt"),
            Arguments.of("android:strokeLineJoin", "miter"),
            Arguments.of("android:strokeMiterLimit", "4"),
            Arguments.of("android:fillAlpha", "1"),
            Arguments.of("android:fillType", "nonZero"),
            Arguments.of("android:trimPathStart", "0"),
            Arguments.of("android:trimPathEnd", "1"),
            Arguments.of("android:trimPathOffset", "0")
        )
    }
}
