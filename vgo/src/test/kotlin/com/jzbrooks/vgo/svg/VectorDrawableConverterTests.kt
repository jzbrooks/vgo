package com.jzbrooks.vgo.svg

import assertk.assertThat
import assertk.assertions.*
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.command.CommandString
import com.jzbrooks.vgo.svg.graphic.ClipPath
import com.jzbrooks.vgo.vd.graphic.ClipPath as AndroidClipPath
import org.junit.jupiter.api.Test

class VectorDrawableConverterTests {
    @Test
    fun testRootElementAttributesAreConverted() {
        val attributes = mutableMapOf(
                "xmlns" to "http://www.w3.org/2000/svg",
                "id" to "visibilitystrike",
                "viewBox" to "0 0 24 24"
        )

        val svg = ScalableVectorGraphic(emptyList(), attributes)

        val vectorDrawable = svg.toVectorDrawable()

        assertThat(vectorDrawable.attributes).containsOnly(
                "xmlns:android" to "http://schemas.android.com/apk/res/android",
                "android:name" to "visibilitystrike",
                "android:height" to "24dp",
                "android:width" to "24dp",
                "android:viewportHeight" to "24",
                "android:viewportWidth" to "24"
        )
    }

    @Test
    fun testRootElementStyleAttributeIsConverted() {
        val attributes = mutableMapOf(
                "xmlns" to "http://www.w3.org/2000/svg",
                "id" to "visibilitystrike",
                "viewBox" to "0 0 24 24",
                "style" to "stroke-linejoin:round;stroke-miterlimit:2;"
        )

        val svg = ScalableVectorGraphic(emptyList(), attributes)

        val vectorDrawable = svg.toVectorDrawable()

        assertThat(vectorDrawable.attributes).containsOnly(
                "xmlns:android" to "http://schemas.android.com/apk/res/android",
                "android:name" to "visibilitystrike",
                "android:height" to "24dp",
                "android:width" to "24dp",
                "android:viewportHeight" to "24",
                "android:viewportWidth" to "24",
                "android:strokeLineJoin" to "round",
                "android:strokeMiterLimit" to "2"
        )
    }

    @Test
    fun testPathElementAttributesAreConverted() {
        val attributes = mutableMapOf(
                "xmlns" to "http://www.w3.org/2000/svg",
                "id" to "visibilitystrike",
                "viewBox" to "0 0 24 24"
        )

        val svg = ScalableVectorGraphic(
                listOf(
                        Path(CommandString("""M 10,30"
                                A 20,20 0,0,1 50,30
                                A 20,20 0,0,1 90,30
                                Q 90,60 50,90
                                Q 10,60 10,30 z""").toCommandList(),
                                mutableMapOf(
                                        "id" to "heart",
                                        "stroke" to "red",
                                        "fill" to "none"
                                )
                        )
                ),
                attributes
        )

        val vectorDrawable = svg.toVectorDrawable()

        assertThat(vectorDrawable.elements.first().attributes).isEqualTo(mapOf(
                "android:name" to "heart",
                "android:strokeColor" to "#ff0000",
                "android:strokeWidth" to "1"
        ))
    }

    @Test
    fun testPathStyleElementAttributeIsConverted() {

        val svg = ScalableVectorGraphic(
                listOf(
                        Path(CommandString("""M 10,30"
                                A 20,20 0,0,1 50,30
                                A 20,20 0,0,1 90,30
                                Q 90,60 50,90
                                Q 10,60 10,30 z""").toCommandList(),
                                mutableMapOf(
                                        "id" to "heart",
                                        "style" to "stroke:red"
                                )
                        )
                ),
                mutableMapOf("viewBox" to "0 0 24 24")
        )

        val vectorDrawable = svg.toVectorDrawable()

        assertThat(vectorDrawable.elements.first().attributes).isEqualTo(mapOf(
                "android:name" to "heart",
                "android:strokeColor" to "#ff0000",
                "android:strokeWidth" to "1"
        ))
    }

    @Test
    fun testContainerElementAttributesAreConverted() {
        val attributes = mutableMapOf(
                "id" to "transform_group",
                "transform" to "matrix(1,0,0,1,3,4)"
        )

        val svg = ScalableVectorGraphic(
                listOf(Group(emptyList(), attributes)),
                mutableMapOf("viewBox" to "0 0 24 24")
        )

        val vectorDrawable = svg.toVectorDrawable()

        assertThat(vectorDrawable.elements.first().attributes).isEqualTo(mapOf(
                "android:name" to "transform_group",
                "android:translateX" to "3",
                "android:translateY" to "4"
        ))
    }

    @Test
    fun testClipPathsAreConverted() {
        val svg = ScalableVectorGraphic(
                listOf(
                        ClipPath(
                                listOf(
                                        Path(CommandString("""M859.613,320.594L294.248,265.525L284.349,367.149L849.714,422.218L859.613,320.594Z""").toCommandList())
                                ),
                                mutableMapOf("id" to "_clip1")
                        ),
                        Group(
                                listOf(
                                        Path(CommandString("""M445.029,297.939C450.143,297.036 454.801,295.143 458.681,292.52C464.396,299.598 474.274,304.279 485.491,304.279C494.561,304.279 502.756,301.218 508.582,296.304C514.875,298.354 522.11,299.521 529.803,299.521C539.01,299.521 547.56,297.85 554.626,294.994C558.611,305.628 570.826,313.378 585.259,313.378C590.263,313.378 595.001,312.446 599.222,310.785C604.808,316.939 615.236,321.082 627.168,321.082C633.38,321.082 639.185,319.959 644.111,318.013C649.97,324.089 659.101,327.998 669.352,327.998C677.368,327.998 684.699,325.608 690.316,321.661C696.238,326.06 705.013,328.847 714.801,328.847C723.126,328.847 730.719,326.83 736.446,323.521C742.227,330.263 751.86,334.682 762.759,334.682C769.16,334.682 775.124,333.158 780.128,330.533C784.439,333.141 789.492,334.84 794.941,335.32C799.829,339.849 807.479,343.07 816.332,343.988C822.204,349.774 831.111,353.468 841.074,353.468C841.643,353.468 842.207,353.456 842.768,353.432C839.729,356.575 837.974,360.277 837.974,364.239C837.974,371.291 843.534,377.521 852.006,381.237C829.272,454.418 703.604,500.927 558.064,486.751C418.852,473.191 308.194,408.66 291.672,336.394C299.558,332.635 304.668,326.631 304.668,319.87C304.668,319.296 304.631,318.727 304.559,318.165C306.607,318.426 308.725,318.563 310.892,318.563C322.983,318.563 333.529,314.309 339.058,308.019C341.819,308.606 344.707,308.918 347.68,308.918C351.32,308.918 354.833,308.451 358.141,307.582C363.998,312.805 372.451,316.086 381.843,316.086C391.381,316.086 399.949,312.702 405.815,307.338C409.243,308.13 412.939,308.563 416.793,308.563C428.935,308.563 439.519,304.273 445.029,297.939ZM836.59,301.647L836.761,301.894C836.566,301.915 836.371,301.938 836.176,301.962C836.265,301.784 836.351,301.604 836.434,301.423L836.59,301.647ZM324.899,255.368C322.902,254.502 320.777,253.803 318.555,253.293C367.036,204.39 470.731,176.906 587.14,188.244C677,196.997 754.963,226.987 803.39,266.587C790.204,267.598 779.337,275.096 775.72,285.091C771.757,283.666 767.371,282.875 762.759,282.875C758.088,282.875 753.651,283.686 749.649,285.144L749.649,285.14C749.649,270.843 735.308,259.236 717.645,259.236C702.114,259.236 689.151,268.21 686.248,280.095C681.343,277.621 675.553,276.191 669.352,276.191C668.213,276.191 667.088,276.239 665.98,276.333C665.981,276.246 665.982,276.158 665.982,276.07C665.982,261.773 646.283,250.166 622.019,250.166C606.868,250.166 593.496,254.692 585.59,261.572C585.479,261.571 585.369,261.57 585.259,261.57C580.08,261.57 575.187,262.568 570.857,264.338C564.519,254.619 548.519,247.713 529.803,247.713C517.944,247.713 507.176,250.486 499.264,254.99C495.092,253.376 490.421,252.472 485.491,252.472C477.696,252.472 470.549,254.732 464.995,258.486C459.28,251.408 449.403,246.727 438.185,246.727C431.783,246.727 425.818,248.252 420.814,250.876C414.478,241.154 398.475,234.246 379.756,234.246C362.253,234.246 347.126,240.285 340.054,249.017C334.377,250.077 329.205,252.296 324.899,255.368Z""").toCommandList())
                                ),
                                mutableMapOf("clip-path" to "url(#_clip1)")
                        )
                ),
                mutableMapOf("viewBox" to "0 0 24 24")
        )

        val vectorDrawable = svg.toVectorDrawable()

        assertThat(vectorDrawable.elements[0]).isInstanceOf(Group::class.java)

        val outerGroup = vectorDrawable.elements[0] as Group

        assertThat(outerGroup.elements[0]).isInstanceOf(AndroidClipPath::class.java)
        assertThat(outerGroup.elements[1]).isInstanceOf(Group::class.java)

        // Groups used only for clipping are removed
        // after the CollapseGroup pass
        val innerGroup = outerGroup.elements[1] as Group

        assertThat(innerGroup.elements[0]).isInstanceOf(Path::class.java)
        assertThat(innerGroup.attributes).isEmpty()
    }

    @Test
    fun testRgbColorIsConverted() {

        val svg = ScalableVectorGraphic(
                listOf(
                        Path(CommandString("""M 10,30"
                                A 20,20 0,0,1 50,30
                                A 20,20 0,0,1 90,30
                                Q 90,60 50,90
                                Q 10,60 10,30 z""").toCommandList(),
                                mutableMapOf("stroke" to "rgb(23, 0, 23)")
                        )
                ),
                mutableMapOf("viewBox" to "0 0 24 24")
        )

        val vectorDrawable = svg.toVectorDrawable()

        val path = vectorDrawable.elements.first() as Path

        assertThat(path.attributes).contains("android:strokeColor" to "#170017")
    }
}