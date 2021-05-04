package com.jzbrooks.vgo.core.optimization

import com.jzbrooks.vgo.core.graphic.Group

interface GroupVisitor {
    fun visit(group: Group)
}
