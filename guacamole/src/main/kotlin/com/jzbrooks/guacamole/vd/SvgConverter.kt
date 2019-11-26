package com.jzbrooks.guacamole.vd

import com.jzbrooks.guacamole.core.graphic.*
import com.jzbrooks.guacamole.core.util.math.Matrix3
import com.jzbrooks.guacamole.svg.ScalableVectorGraphic
import com.jzbrooks.guacamole.vd.graphic.ClipPath
import java.lang.StringBuilder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun VectorDrawable.toSvg(): ScalableVectorGraphic {
    val graphic = traverse(this) as ContainerElement
    return ScalableVectorGraphic(graphic.elements, convertTopLevelAttributes(attributes))
}

private fun traverse(element: Element): Element {
    return when (element) {
        is ContainerElement -> process(element)
        is PathElement -> process(element)
        else -> element
    }
}

private fun process(containerElement: ContainerElement): Element {
    val newElements = mutableListOf<Element>()
    var defs: Extra? = null

    for ((index, element) in containerElement.elements.withIndex()) {
        if (element !is ClipPath) {
            if (defs != null) {
                element.attributes["clip-path"] = "url(#${defs.attributes["id"]})"
            }
            newElements.add(traverse(element))
        } else {
            defs = Extra("defs", listOf(Path(element.commands)), mutableMapOf("id" to "clip_$index"))
        }
    }

    if (defs != null) {
        newElements.add(defs)
    }

    val newAttributes = convertContainerElementAttributes(containerElement.attributes)
    containerElement.attributes.putAll(newAttributes)

    return containerElement.apply { elements = newElements }
}

private fun process(pathElement: PathElement): Element {
    return pathElement.apply {
        val newElements = convertPathElementAttributes(attributes)
        attributes.putAll(newElements)
    }
}

private fun convertPathElementAttributes(attributes: MutableMap<String, String>): MutableMap<String, String> {
    val svgPathElementAttributes = mutableMapOf<String, String>()

    for ((key, value) in attributes) {

        val svgKey = when (key) {
            "android:name" -> "id"
            "android:fillColor" -> "fill"
            "android:fillType" -> "fill-rule"
            "android:fillAlpha" -> "fill-opacity"
            // todo: figure out what to do with these
            // "android:trimPathStart" -> ""
            // "android:trimPathEnd" -> ""
            // "android:trimPathOffset" -> ""
            "android:strokeColor" -> "stroke"
            "android:strokeWidth" -> "stroke-width"
            "android:strokeAlpha" -> "stroke-opacity"
            "android:strokeLineCap" -> "stroke-linecap"
            "android:strokeLineJoin" -> "stroke-linejoin"
            "android:strokeMiterLimit" -> "stroke-miterlimit"
            else -> key
        }

        svgPathElementAttributes[svgKey] = value
    }

    // We've mangled the map at this point...
    attributes.clear()

    return svgPathElementAttributes
}

private fun convertContainerElementAttributes(attributes: MutableMap<String, String>): MutableMap<String, String> {
    val svgPathElementAttributes = mutableMapOf<String, String>()

    for ((key, value) in attributes.filterKeys { !transformationPropertyNames.contains(it) }) {

        val svgKey = when (key) {
            "android:name" -> "id"
            else -> key
        }

        svgPathElementAttributes[svgKey] = value
    }

    val transform = computeTransformationMatrix(attributes.filterKeys(transformationPropertyNames::contains))
    if (transform != Matrix3.IDENTITY) {
        val matrixStringBuilder = StringBuilder("matrix(").apply {
            append(transform[0,0])
            append(", ")
            append(transform[1,0])
            append(", ")
            append(transform[0,1])
            append(", ")
            append(transform[1,1])
            append(", ")
            append(transform[0,2])
            append(", ")
            append(transform[1,2])
            append(")")
        }

        svgPathElementAttributes["transform"] = matrixStringBuilder.toString()
    }

    // We've mangled the map at this point...
    attributes.clear()

    return svgPathElementAttributes
}

private fun convertTopLevelAttributes(attributes: MutableMap<String, String>): MutableMap<String, String> {

    val viewportHeight = attributes.remove("android:viewportHeight") ?: "24"
    val viewportWidth = attributes.remove("android:viewportWidth") ?: "24"
    attributes.remove("xmlns:android")

    val svgElementAttributes = mutableMapOf(
            "xmlns" to "http://www.w3.org/2000/svg",
            "viewPort" to "0 0 $viewportWidth $viewportHeight"
    )

    for ((key, value) in attributes) {

        val svgValue = when (key) {
            "android:height" -> "100%"
            "android:width" -> "100%"
            else -> value
        }

        val svgKey = when (key) {
            "android:name" -> "id"
            "android:width" -> "width"
            "android:height" -> "height"
            else -> key
        }

        svgElementAttributes[svgKey] = svgValue
    }

    // We've mangled the map at this point...
    attributes.clear()

    return svgElementAttributes
}

// Duplicated from vd.BakeTransform
private val transformationPropertyNames = setOf(
        "android:scaleX",
        "android:scaleY",
        "android:translateX",
        "android:translateY",
        "android:pivotX",
        "android:pivotY",
        "android:rotation"
)

private fun computeTransformationMatrix(groupAttributes: Map<String, String>): Matrix3 {
    val scaleX = groupAttributes["android:scaleX"]?.toFloat()
    val scaleY = groupAttributes["android:scaleY"]?.toFloat()

    val translationX = groupAttributes["android:translateX"]?.toFloat()
    val translationY = groupAttributes["android:translateY"]?.toFloat()

    val pivotX = groupAttributes["android:pivotX"]?.toFloat()
    val pivotY = groupAttributes["android:pivotY"]?.toFloat()

    val rotation = groupAttributes["android:rotation"]?.toFloat()

    val scale = Matrix3.from(arrayOf(
            floatArrayOf(scaleX ?: 1f, 0f, 0f),
            floatArrayOf(0f, scaleY ?: 1f, 0f),
            floatArrayOf(0f, 0f, 1f)
    ))

    val translation = Matrix3.from(arrayOf(
            floatArrayOf(1f, 0f, translationX ?: 0f),
            floatArrayOf(0f, 1f, translationY ?: 0f),
            floatArrayOf(0f, 0f, 1f)
    ))

    val pivot = Matrix3.from(arrayOf(
            floatArrayOf(1f, 0f, pivotX ?: 0f),
            floatArrayOf(0f, 1f, pivotY ?: 0f),
            floatArrayOf(0f, 0f, 1f)
    ))

    val pivotInverse = Matrix3.from(arrayOf(
            floatArrayOf(1f, 0f, (pivotX ?: 0f) * -1),
            floatArrayOf(0f, 1f, (pivotY ?: 0f) * -1),
            floatArrayOf(0f, 0f, 1f)
    ))

    val rotate = rotation?.let {
        val radians = it * PI.toFloat() / 180f
        Matrix3.from(arrayOf(
                floatArrayOf(cos(radians), -sin(radians), 0f),
                floatArrayOf(sin(radians), cos(radians), 0f),
                floatArrayOf(0f, 0f, 1f)
        ))
    } ?: Matrix3.IDENTITY

    return listOf(pivot, translation, rotate, scale, pivotInverse).reduce(Matrix3::times)
}
