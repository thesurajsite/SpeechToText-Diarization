package com.suraj.sttdiarization.utils

import android.content.Context
import java.io.File
import java.io.FileOutputStream

object AssetCopier {
    private const val PREFS_NAME = "model_copy_state"
    private const val PREF_MODELS_COPIED = "models_copied"

    fun copyModelsIfNeeded(context: Context): File {
        val appContext = context.applicationContext
        val destination = File(appContext.filesDir, "models")
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        if (prefs.getBoolean(PREF_MODELS_COPIED, false)) {
            return destination
        }

        copyAssetTree(appContext, "models", destination)
        prefs.edit().putBoolean(PREF_MODELS_COPIED, true).apply()
        return destination
    }

    private fun copyAssetTree(context: Context, assetPath: String, destination: File) {
        val children = context.assets.list(assetPath)
        if (children != null && children.isNotEmpty()) {
            destination.mkdirs()
            for (child in children) {
                copyAssetTree(context, "$assetPath/$child", File(destination, child))
            }
            return
        }

        destination.parentFile?.mkdirs()
        context.assets.open(assetPath).use { input ->
            FileOutputStream(destination).use { output ->
                input.copyTo(output)
            }
        }
    }
}
