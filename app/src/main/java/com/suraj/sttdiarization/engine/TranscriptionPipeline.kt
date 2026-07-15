package com.suraj.sttdiarization.engine

import android.content.Context
import android.net.Uri
import com.suraj.sttdiarization.models.TranscriptItem
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineSpeakerDiarization
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object TranscriptionPipeline {
    private const val SAMPLE_RATE = 16000
    private const val MIN_SEGMENT_SAMPLES = 1600

    suspend fun process(
        context: Context,
        uri: Uri,
        diarizer: OfflineSpeakerDiarization,
        recognizer: OfflineRecognizer,
    ): List<TranscriptItem> = withContext(Dispatchers.Default) {
        val samples = WavLoader.loadPcmFromUri(context, uri)
        val segments = DiarizationEngine.runDiarization(diarizer, samples)
        if (segments.isEmpty()) {
            return@withContext emptyList()
        }

        val items = mutableListOf<TranscriptItem>()
        val speakerMap = mutableMapOf<Int, Int>()
        var nextSpeakerIndex = 1

        for (segment in segments) {
            val startIndex = (segment.startTime * SAMPLE_RATE).toInt().coerceAtLeast(0)
            val endIndex = (segment.endTime * SAMPLE_RATE).toInt().coerceAtMost(samples.size)
            if (endIndex - startIndex < MIN_SEGMENT_SAMPLES) {
                continue
            }

            val segmentSamples = samples.copyOfRange(startIndex, endIndex)
            val text = WhisperAsrEngine.transcribeSegment(recognizer, segmentSamples)
            if (text.isBlank()) {
                continue
            }

            val displaySpeakerId = speakerMap.getOrPut(segment.speakerId) { nextSpeakerIndex++ }

            items.add(
                TranscriptItem(
                    speakerLabel = "Speaker $displaySpeakerId",
                    text = text,
                    startTime = segment.startTime,
                    endTime = segment.endTime,
                )
            )
        }

        mergeConsecutive(items)
    }

    private fun mergeConsecutive(items: List<TranscriptItem>): List<TranscriptItem> {
        if (items.isEmpty()) {
            return emptyList()
        }

        val merged = mutableListOf<TranscriptItem>()
        for (item in items) {
            val last = merged.lastOrNull()
            if (last != null && last.speakerLabel == item.speakerLabel) {
                merged[merged.lastIndex] = last.copy(
                    text = listOf(last.text, item.text).joinToString(" ").trim(),
                    startTime = minOf(last.startTime, item.startTime),
                    endTime = maxOf(last.endTime, item.endTime),
                )
            } else {
                merged.add(item)
            }
        }
        return merged
    }
}
