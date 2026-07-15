package com.suraj.sttdiarization.engine

import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineStream
import com.k2fsa.sherpa.onnx.OfflineWhisperModelConfig

object WhisperAsrEngine {
    private const val SAMPLE_RATE = 16000

    fun init(encoderPath: String, decoderPath: String, tokensPath: String): OfflineRecognizer {
        val config = OfflineRecognizerConfig(
            featConfig = FeatureConfig(sampleRate = SAMPLE_RATE, featureDim = 80),
            modelConfig = OfflineModelConfig(
                whisper = OfflineWhisperModelConfig(
                    encoder = encoderPath,
                    decoder = decoderPath,
                    language = "en",
                    task = "transcribe",
                ),
                numThreads = 2,
                provider = "cpu",
                tokens = tokensPath,
                modelType = "whisper",
            ),
            decodingMethod = "greedy_search",
        )
        return OfflineRecognizer(config = config)
    }

    fun transcribeSegment(recognizer: OfflineRecognizer, samples: FloatArray): String {
        val stream: OfflineStream = recognizer.createStream()
        try {
            stream.acceptWaveform(samples, SAMPLE_RATE)
            recognizer.decode(stream)
            return recognizer.getResult(stream).text.trim()
        } finally {
            stream.release()
        }
    }
}
