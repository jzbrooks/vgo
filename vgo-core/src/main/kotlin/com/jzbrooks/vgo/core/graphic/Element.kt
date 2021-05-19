package com.jzbrooks.vgo.core.graphic

interface Element {
    val id: String?
    val foreign: MutableMap<String, String>
}
