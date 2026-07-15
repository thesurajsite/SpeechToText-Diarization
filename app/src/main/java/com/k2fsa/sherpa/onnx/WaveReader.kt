package com.k2fsa.sherpa.onnx

import android.content.res.AssetManager

data class WaveData(
    val samples: FloatArray,
    val sampleRate: Int,
)

class WaveReader {
    companion object {
        fun readWave(assetManager: AssetManager, filename: String): WaveData {
            return readWaveFromAsset(assetManager, filename)
        }

        fun readWave(filename: String): WaveData {
            return readWaveFromFile(filename)
        }

        external fun readWaveFromAsset(assetManager: AssetManager, filename: String): WaveData
        external fun readWaveFromFile(filename: String): WaveData

        init {
            System.loadLibrary("sherpa-onnx-jni")
        }
    }
}
