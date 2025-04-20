package com.jzbrooks.vgo.iv

import com.jzbrooks.vgo.core.util.ExperimentalVgoApi
import com.jzbrooks.vgo.vd.VectorDrawable
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

@ExperimentalVgoApi
fun ImageVector.toVectorDrawable(): VectorDrawable {
    val decimalFormat =
        DecimalFormat().apply {
            maximumFractionDigits = 2
            isDecimalSeparatorAlwaysShown = false
            isGroupingUsed = false
            roundingMode = RoundingMode.HALF_UP
            minimumIntegerDigits = 0
            decimalFormatSymbols = DecimalFormatSymbols(Locale.US)
        }

    val vdElementAttributes =
        mutableMapOf(
            "xmlns:android" to "http://schemas.android.com/apk/res/android",
            "android:viewportWidth" to decimalFormat.format(viewportWidth),
            "android:viewportHeight" to decimalFormat.format(viewportHeight),
            "android:width" to "${decimalFormat.format(defaultWidthDp)}dp",
            "android:height" to "${decimalFormat.format(defaultHeightDp)}dp",
        )

    if (id != null) {
        vdElementAttributes["android:name"] = id
    }

    foreign.clear()

    return VectorDrawable(elements, id, vdElementAttributes)
}
