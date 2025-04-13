package com.jzbrooks.vgo.iv

import com.jzbrooks.vgo.vd.VectorDrawable

fun ImageVector.toVectorDrawable(): VectorDrawable {
    val vdElementAttributes =
        mutableMapOf(
            "xmlns:android" to "http://schemas.android.com/apk/res/android",
            "android:viewportWidth" to foreign.getValue("viewportWidth"),
            "android:viewportHeight" to foreign.getValue("viewportHeight"),
            "android:width" to "${foreign["defaultWidth"]}dp",
            "android:height" to "${foreign["defaultHeight"]}dp",
        )

    foreign.clear()

    return VectorDrawable(elements, id, vdElementAttributes)
}
