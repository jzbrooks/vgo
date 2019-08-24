package com.jzbrooks.avdo

interface Graphic {
    val paths: List<Path>
    val groups: List<Group>
    val width: Int
    val height: Int
}