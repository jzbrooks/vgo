package com.jzbrooks.avdo.assertk.extensions

import assertk.Assert
import assertk.assertions.support.expected

fun <K, V> Assert<Map<K, V>>.containsKey(key: K): Unit = given { map ->
    if (map.containsKey(key)) {
        return
    }
    expected("to contain key: $key but it was missing")
}