package com.jzbrooks.vgo.iv

import com.jzbrooks.vgo.util.parseKotlinSource
import org.jetbrains.kotlin.com.intellij.openapi.Disposable
import org.jetbrains.kotlin.psi.KtFile
import java.io.InputStream

fun parseKotlinFile(
    disposable: Disposable,
    input: InputStream,
): KtFile = parseKotlinSource(disposable, "test.kt", input.reader().readText())
