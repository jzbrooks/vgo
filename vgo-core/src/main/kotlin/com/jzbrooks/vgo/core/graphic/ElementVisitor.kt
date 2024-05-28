package com.jzbrooks.vgo.core.graphic

interface ElementVisitor {
    fun visit(graphic: Graphic)

    fun visit(clipPath: ClipPath)

    fun visit(group: Group)

    fun visit(extra: Extra)

    fun visit(path: Path)
}
