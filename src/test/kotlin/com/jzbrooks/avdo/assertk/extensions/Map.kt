package com.jzbrooks.avdo.assertk.extensions

import assertk.Assert
import assertk.assertions.support.expected

fun <K, V> Assert<Map<K, V>>.containsKey(key: K): Unit = given { map ->
    if (map.containsKey(key)) {
        return
    }
    expected("to contain key: $key but it was missing")
}

fun <K, V> Assert<Map<K, V>>.doesNotContainKey(key: K): Unit = given { map ->
    if (!map.containsKey(key)) {
        return
    }
    expected("to not contain key: $key but it was present")
}

fun <K, V> Assert<Map<K, V>>.containsKeys(vararg keys: K): Unit = given { map ->
    for (key in keys) {
        if (!map.containsKey(key)) {
            expected("to contain key: $key but it was missing")
        }
    }
}