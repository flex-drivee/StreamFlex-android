package com.streamflex.player.episodes

data class IntroSegment(
    val startMs: Long,
    val endMs: Long,
    val label: String = "Skip Intro"
)
