package com.suraj.sttdiarization.engine

import java.io.File

object ModelPaths {
    lateinit var segmentationModel: String
        private set
    lateinit var embeddingModel: String
        private set
    lateinit var whisperEncoder: String
        private set
    lateinit var whisperDecoder: String
        private set
    lateinit var whisperTokens: String
        private set

    fun init(baseDir: File) {
        segmentationModel = File(baseDir, "segmentation/model.onnx").absolutePath
        embeddingModel = File(baseDir, "embedding/titanet_large.onnx").absolutePath
        whisperEncoder = File(baseDir, "whisper/tiny.en-encoder.int8.onnx").absolutePath
        whisperDecoder = File(baseDir, "whisper/tiny.en-decoder.int8.onnx").absolutePath
        whisperTokens = File(baseDir, "whisper/tiny.en-tokens.txt").absolutePath
    }
}
