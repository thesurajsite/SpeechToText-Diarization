package com.suraj.sttdiarization.engine

import com.suraj.sttdiarization.models.SpeakerSegment
import com.k2fsa.sherpa.onnx.FastClusteringConfig
import com.k2fsa.sherpa.onnx.OfflineSpeakerDiarization
import com.k2fsa.sherpa.onnx.OfflineSpeakerDiarizationConfig
import com.k2fsa.sherpa.onnx.OfflineSpeakerSegmentationModelConfig
import com.k2fsa.sherpa.onnx.OfflineSpeakerSegmentationPyannoteModelConfig
import com.k2fsa.sherpa.onnx.SpeakerEmbeddingExtractorConfig

object DiarizationEngine {
    fun init(segmentationPath: String, embeddingPath: String): OfflineSpeakerDiarization {
        val config = OfflineSpeakerDiarizationConfig(
            segmentation = OfflineSpeakerSegmentationModelConfig(
                pyannote = OfflineSpeakerSegmentationPyannoteModelConfig(
                    model = segmentationPath,
                ),
                numThreads = 2,
                debug = false,
                provider = "cpu",
            ),
            embedding = SpeakerEmbeddingExtractorConfig(
                model = embeddingPath,
                numThreads = 2,
                debug = false,
                provider = "cpu",
            ),
            clustering = FastClusteringConfig(
                numClusters = -1,
                threshold = 0.5f,
            ),
        )
        return OfflineSpeakerDiarization(config = config)
    }

    fun runDiarization(
        diarizer: OfflineSpeakerDiarization,
        samples: FloatArray,
    ): List<SpeakerSegment> {
        return diarizer.process(samples)
            .map {
                SpeakerSegment(
                    startTime = it.start,
                    endTime = it.end,
                    speakerId = it.speaker,
                )
            }
            .sortedBy { it.startTime }
    }
}
