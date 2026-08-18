package com.streamflex.player.core

sealed interface PlayerError {
    val message: String

    data class Http(val code: Int, override val message: String = "HTTP Error $code") : PlayerError
    data class Timeout(override val message: String = "Connection Timeout") : PlayerError
    data class UnsupportedCodec(override val message: String = "Unsupported Codec") : PlayerError
    data class Decoder(override val message: String = "Decoder Error") : PlayerError
    data class InvalidSource(override val message: String = "Invalid Media Source") : PlayerError
    data class Unknown(override val message: String, val exception: Exception? = null) : PlayerError
}
