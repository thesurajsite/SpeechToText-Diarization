package com.suraj.sttdiarization.engine

import android.content.Context
import android.net.Uri

object WavLoader {
    private const val TARGET_SAMPLE_RATE = 16000
    private const val TARGET_CHANNELS = 1
    private const val TARGET_BITS_PER_SAMPLE = 16

    fun loadPcmFromUri(context: Context, uri: Uri): FloatArray {
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes()
        } ?: throw IllegalArgumentException("Unable to open WAV file")

        if (bytes.size < 12) {
            throw IllegalArgumentException("Invalid WAV file: header too short")
        }

        if (readAscii(bytes, 0, 4) != "RIFF" || readAscii(bytes, 8, 4) != "WAVE") {
            throw IllegalArgumentException("Invalid WAV file: missing RIFF/WAVE markers")
        }

        var sampleRate = -1
        var channels = -1
        var bitsPerSample = -1
        var dataOffset = -1
        var dataSize = -1

        var offset = 12
        while (offset + 8 <= bytes.size) {
            val chunkId = readAscii(bytes, offset, 4)
            val chunkSize = readInt32LE(bytes, offset + 4)
            val chunkDataStart = offset + 8
            val chunkDataEnd = chunkDataStart + chunkSize

            if (chunkDataEnd > bytes.size) {
                throw IllegalArgumentException("Invalid WAV file: truncated chunk $chunkId")
            }

            when (chunkId) {
                "fmt " -> {
                    if (chunkSize < 16) {
                        throw IllegalArgumentException("Invalid WAV file: fmt chunk too short")
                    }
                    val audioFormat = readInt16LE(bytes, chunkDataStart)
                    channels = readInt16LE(bytes, chunkDataStart + 2)
                    sampleRate = readInt32LE(bytes, chunkDataStart + 4)
                    bitsPerSample = readInt16LE(bytes, chunkDataStart + 14)
                    if (audioFormat != 1) {
                        throw IllegalArgumentException("Invalid WAV file: only PCM encoding is supported")
                    }
                }

                "data" -> {
                    dataOffset = chunkDataStart
                    dataSize = chunkSize
                }
            }

            offset = chunkDataEnd + (chunkSize and 1)
        }

        if (sampleRate != TARGET_SAMPLE_RATE || channels != TARGET_CHANNELS || bitsPerSample != TARGET_BITS_PER_SAMPLE) {
            throw IllegalArgumentException("Please provide 16kHz mono 16-bit WAV audio")
        }

        if (dataOffset < 0 || dataSize < 0) {
            throw IllegalArgumentException("Invalid WAV file: missing data chunk")
        }

        val sampleCount = dataSize / 2
        val samples = FloatArray(sampleCount)
        var sampleOffset = dataOffset
        for (index in 0 until sampleCount) {
            samples[index] = readInt16LE(bytes, sampleOffset) / 32768f
            sampleOffset += 2
        }
        return samples
    }

    private fun readAscii(bytes: ByteArray, offset: Int, length: Int): String {
        return String(bytes, offset, length, Charsets.US_ASCII)
    }

    private fun readInt16LE(bytes: ByteArray, offset: Int): Int {
        val b0 = bytes[offset].toInt() and 0xff
        val b1 = bytes[offset + 1].toInt() and 0xff
        val value = b0 or (b1 shl 8)
        return if (value and 0x8000 != 0) value or -0x10000 else value
    }

    private fun readInt32LE(bytes: ByteArray, offset: Int): Int {
        val b0 = bytes[offset].toInt() and 0xff
        val b1 = bytes[offset + 1].toInt() and 0xff
        val b2 = bytes[offset + 2].toInt() and 0xff
        val b3 = bytes[offset + 3].toInt() and 0xff
        return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
    }
}
