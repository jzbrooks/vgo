package com.jzbrooks.vgo.core.transformation

import com.jzbrooks.vgo.core.graphic.ElementVisitor

/** This transformation should be applied from the leaf [com.jzbrooks.vgo.core.graphic.Element] to the root */
interface BottomUpTransformer : ElementVisitor
