package com.suraj.sttdiarization.models

data class TranscriptItem(
    val speakerLabel: String,
    val text: String,
    val startTime: Float,
    val endTime: Float,
)
