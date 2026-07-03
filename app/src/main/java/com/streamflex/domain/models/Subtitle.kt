package com.streamflex.domain.models

data class Subtitle(

    val language: String,

    val url: String,

    val label: String = language,

    val isDefault: Boolean = false,

    val isForced: Boolean = false
)