package com.jzbrooks.guacamole.core.optimization

import com.jzbrooks.guacamole.core.graphic.Group

interface GroupVisitor {
    fun visit(group: Group)
}