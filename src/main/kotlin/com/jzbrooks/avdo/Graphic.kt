package com.jzbrooks.avdo

interface Graphic {
    val paths: List<Path>
    val groups: List<Group>
    val size: Size
    val viewbox: Viewbox
}