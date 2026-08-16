package com.streamflex.player.core

data class PlayerError(
    val code: Int,
    val message: String,
    val exception: Exception? = null
)
