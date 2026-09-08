package com.cinetheta.player.episodes

data class IntroSegment(
    val startMs: Long,
    val endMs: Long,
    val label: String = "Skip Intro"
)
