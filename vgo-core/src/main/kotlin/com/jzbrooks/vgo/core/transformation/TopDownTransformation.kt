package com.jzbrooks.vgo.core.transformation

import com.jzbrooks.vgo.core.graphic.ElementVisitor

/** This transformation should be applied from the root [com.jzbrooks.vgo.core.graphic.Element] to the leaves */
interface TopDownTransformation : ElementVisitor
