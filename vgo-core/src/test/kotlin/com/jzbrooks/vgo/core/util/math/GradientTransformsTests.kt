package com.jzbrooks.vgo.core.util.math

import assertk.all
import assertk.assertThat
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.prop
import com.jzbrooks.vgo.core.Color
import com.jzbrooks.vgo.core.GradientStop
import com.jzbrooks.vgo.core.LinearGradient
import com.jzbrooks.vgo.core.RadialGradient
import com.jzbrooks.vgo.core.SweepGradient
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class GradientTransformsTests {
    private val stops =
        listOf(
            GradientStop(0f, Color(0xFFB125EAu)),
            GradientStop(1f, Color(0xFF008AFFu)),
        )

    private val linear = LinearGradient(0f, 0f, 1f, 0f, stops)
    private val radial = RadialGradient(5f, 5f, 2f, stops)
    private val sweep = SweepGradient(5f, 5f, stops)

    @Test
    fun testLinearGradientTranslates() {
        val translation = Matrix3.from(floatArrayOf(1f, 0f, 10f, 0f, 1f, 20f, 0f, 0f, 1f))

        val transformed = linear.transformedOrNull(translation)

        assertThat(transformed).isNotNull().all {
            prop(LinearGradient::startX).isEqualTo(10f)
            prop(LinearGradient::startY).isEqualTo(20f)
            prop(LinearGradient::endX).isEqualTo(11f)
            prop(LinearGradient::endY).isEqualTo(20f)
        }
    }

    @Test
    fun testAxisAlignedLinearGradientBakesNonUniformScale() {
        // The Illustrator export form: axis-aligned unit gradient vector
        // with a diagonal scale + translation matrix
        val transform = Matrix3.from(floatArrayOf(687.601f, 0f, 56.1995f, 0f, 363.444f, 75.278f, 0f, 0f, 1f))

        val transformed = linear.transformedOrNull(transform)

        assertThat(transformed).isNotNull().all {
            prop(LinearGradient::startX).isCloseTo(56.1995f, 1e-3f)
            prop(LinearGradient::startY).isCloseTo(75.278f, 1e-3f)
            prop(LinearGradient::endX).isCloseTo(743.8f, 1e-3f)
            prop(LinearGradient::endY).isCloseTo(75.278f, 1e-3f)
        }
    }

    @Test
    fun testLinearGradientBakesTinyUniformScale() {
        val transform = Matrix3.from(floatArrayOf(0.0026178f, 0f, -0.336242f, 0f, 0.0026178f, -0.834771f, 0f, 0f, 1f))

        val transformed = linear.transformedOrNull(transform)

        assertThat(transformed).isNotNull().all {
            prop(LinearGradient::startX).isCloseTo(-0.336242f, 1e-6f)
            prop(LinearGradient::endX).isCloseTo(-0.333624f, 1e-6f)
        }
    }

    @Test
    fun testRotatedLinearGradientBakesRotation() {
        val radians = 30f * PI.toFloat() / 180f
        val rotation = Matrix3.from(floatArrayOf(cos(radians), -sin(radians), 0f, sin(radians), cos(radians), 0f, 0f, 0f, 1f))

        val transformed = linear.transformedOrNull(rotation)

        assertThat(transformed).isNotNull().all {
            prop(LinearGradient::endX).isCloseTo(cos(radians), 1e-4f)
            prop(LinearGradient::endY).isCloseTo(sin(radians), 1e-4f)
        }
    }

    @Test
    fun testDiagonalLinearGradientRejectsNonUniformScale() {
        val diagonal = LinearGradient(0f, 0f, 1f, 1f, stops)
        val scale = Matrix3.from(floatArrayOf(2f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f))

        assertThat(diagonal.transformedOrNull(scale)).isNull()
    }

    @Test
    fun testLinearGradientRejectsSkew() {
        val skew = Matrix3.from(floatArrayOf(1f, 1f, 0f, 0f, 1f, 0f, 0f, 0f, 1f))

        assertThat(linear.transformedOrNull(skew)).isNull()
    }

    @Test
    fun testDegenerateLinearGradientRejected() {
        val point = LinearGradient(1f, 1f, 1f, 1f, stops)

        assertThat(point.transformedOrNull(Matrix3.IDENTITY)).isNull()
    }

    @Test
    fun testRadialGradientBakesUniformScaleAndRotation() {
        val radians = 45f * PI.toFloat() / 180f
        val scale = 3f
        val transform =
            Matrix3.from(
                floatArrayOf(
                    scale * cos(radians),
                    scale * -sin(radians),
                    10f,
                    scale * sin(radians),
                    scale * cos(radians),
                    0f,
                    0f,
                    0f,
                    1f,
                ),
            )

        val transformed = radial.transformedOrNull(transform)

        assertThat(transformed).isNotNull().all {
            prop(RadialGradient::radius).isCloseTo(6f, 1e-4f)
            prop(RadialGradient::centerX).isCloseTo(10f, 1e-3f)
            prop(RadialGradient::centerY).isCloseTo(5f * sqrt(2f) * scale, 1e-3f)
        }
    }

    @Test
    fun testRadialGradientRejectsNonUniformScale() {
        val scale = Matrix3.from(floatArrayOf(2f, 0f, 0f, 0f, 3f, 0f, 0f, 0f, 1f))

        assertThat(radial.transformedOrNull(scale)).isNull()
    }

    @Test
    fun testSweepGradientBakesUniformScale() {
        val transform = Matrix3.from(floatArrayOf(2f, 0f, 1f, 0f, 2f, 2f, 0f, 0f, 1f))

        val transformed = sweep.transformedOrNull(transform)

        assertThat(transformed).isNotNull().all {
            prop(SweepGradient::centerX).isEqualTo(11f)
            prop(SweepGradient::centerY).isEqualTo(12f)
        }
    }

    @Test
    fun testSweepGradientRejectsRotation() {
        val radians = 30f * PI.toFloat() / 180f
        val rotation = Matrix3.from(floatArrayOf(cos(radians), -sin(radians), 0f, sin(radians), cos(radians), 0f, 0f, 0f, 1f))

        assertThat(sweep.transformedOrNull(rotation)).isNull()
    }
}
