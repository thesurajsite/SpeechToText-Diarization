package com.suraj.sttdiarization

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.suraj.sttdiarization.ui.MainViewModel
import com.suraj.sttdiarization.ui.screens.MainScreen
import com.suraj.sttdiarization.ui.theme.STTDiarizationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    private val pickFile =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                try {
                    contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                } catch (_: SecurityException) {
                }
                viewModel.transcribe(this, it)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            STTDiarizationTheme {
                MainScreen(
                    viewModel = viewModel,
                    onPickFile = { pickFile.launch(arrayOf("audio/wav", "audio/x-wav")) }
                )
            }
        }

        viewModel.initModelsIfNeeded(this)
    }
}
