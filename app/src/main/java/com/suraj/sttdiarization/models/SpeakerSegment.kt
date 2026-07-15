package com.suraj.sttdiarization.models

data class SpeakerSegment(
    val startTime: Float,
    val endTime: Float,
    val speakerId: Int,
)
