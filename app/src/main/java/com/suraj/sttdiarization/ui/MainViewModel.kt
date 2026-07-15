package com.suraj.sttdiarization.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suraj.sttdiarization.engine.DiarizationEngine
import com.suraj.sttdiarization.engine.ModelPaths
import com.suraj.sttdiarization.engine.WhisperAsrEngine
import com.suraj.sttdiarization.engine.TranscriptionPipeline
import com.suraj.sttdiarization.models.TranscriptItem
import com.suraj.sttdiarization.utils.AssetCopier
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineSpeakerDiarization
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel : ViewModel() {
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _results = MutableStateFlow<List<TranscriptItem>>(emptyList())
    val results: StateFlow<List<TranscriptItem>> = _results.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var diarizer: OfflineSpeakerDiarization? = null
    private var recognizer: OfflineRecognizer? = null
    private var initJob: Job? = null

    fun initModelsIfNeeded(context: Context) {
        if (_isReady.value || initJob?.isActive == true) {
            return
        }

        initJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                val appContext = context.applicationContext
                val modelDir = withContext(Dispatchers.IO) {
                    AssetCopier.copyModelsIfNeeded(appContext)
                }
                ModelPaths.init(modelDir)

                if (diarizer == null) {
                    diarizer = DiarizationEngine.init(
                        segmentationPath = ModelPaths.segmentationModel,
                        embeddingPath = ModelPaths.embeddingModel,
                    )
                }

                if (recognizer == null) {
                    recognizer = WhisperAsrEngine.init(
                        encoderPath = ModelPaths.whisperEncoder,
                        decoderPath = ModelPaths.whisperDecoder,
                        tokensPath = ModelPaths.whisperTokens,
                    )
                }

                _errorMessage.value = null
                _isReady.value = true
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to initialize models"
            } finally {
                initJob = null
            }
        }
    }

    fun transcribe(context: Context, uri: Uri) {
        val diarizerRef = diarizer
        val recognizerRef = recognizer
        if (diarizerRef == null || recognizerRef == null) {
            _errorMessage.value = "Models are not ready yet"
            return
        }

        viewModelScope.launch(Dispatchers.Default) {
            _isLoading.value = true
            _errorMessage.value = null
            _results.value = emptyList()
            try {
                val items = TranscriptionPipeline.process(
                    context = context.applicationContext,
                    uri = uri,
                    diarizer = diarizerRef,
                    recognizer = recognizerRef,
                )
                _results.value = items
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Transcription failed"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        initJob?.cancel()
        recognizer?.release()
        diarizer?.release()
        recognizer = null
        diarizer = null
        super.onCleared()
    }
}
