package com.jzbrooks.guacamole.core.graphic

/**
 * Represents an image element that is not otherwise
 * explicitly represented by the core api
 */
data class Extra(
    var name: String,
    override var elements: List<Element>,
    override val attributes: MutableMap<String, String>
): ContainerElement