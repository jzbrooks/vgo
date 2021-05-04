package com.jzbrooks.vgo.core.graphic

interface Attributes {
    val name: String?
    val foreign: MutableMap<String, String>

    // todo: maybe move elsewhere later?
    fun isEmpty() = name == null && foreign.isEmpty()
}
