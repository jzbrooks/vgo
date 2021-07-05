package com.jzbrooks.vgo.core.graphic

interface ElementVisitor {
    fun visit(container: ContainerElement)
    fun visit(graphic: Graphic)
    fun visit(clipPath: ClipPath)
    fun visit(group: Group)
    fun visit(extra: Extra)

    fun visit(pathElement: PathElement)
    fun visit(path: Path)
}