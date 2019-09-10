package com.jzbrooks.avdo.graphic

import com.jzbrooks.avdo.graphic.command.Command

data class ClipPath(var data: List<Command>, override var metadata: Map<String, String> = emptyMap()) : Element