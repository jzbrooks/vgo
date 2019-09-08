package com.jzbrooks.avdo.graphic

import com.jzbrooks.avdo.graphic.command.Command

data class ClipPath(val data: List<Command>, override val metadata: Map<String, String> = emptyMap()) : Element