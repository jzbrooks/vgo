package com.jzbrooks.vgo.core.util.assertk

import assertk.Assert
import assertk.assertAll
import assertk.assertions.isCloseTo
import com.jzbrooks.vgo.core.util.math.Matrix3
import com.jzbrooks.vgo.core.util.math.Vector3

fun Assert<Matrix3>.isEqualTo(expected: Matrix3) = given { actual ->
    assertAll {
        for (i in 0..2) {
            for (j in 0..2) {
                assertThat(actual[i, j], "product[$i, $j]").isCloseTo(expected[i, j], 0.001f)
            }
        }
    }
}

fun Assert<Vector3>.isEqualTo(expected: Vector3) = given { actual ->
    assertAll {
        assertThat(actual.i, "i").isCloseTo(expected.i, 0.001f)
        assertThat(actual.j, "j").isCloseTo(expected.j, 0.001f)
        assertThat(actual.k, "k").isCloseTo(expected.k, 0.001f)
    }
}
