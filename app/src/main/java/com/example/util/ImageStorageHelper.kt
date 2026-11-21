package com.example.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageStorageHelper {
    /**
     * Copies a photo selected via Android Photo Picker into app-private internal storage
     * so it persists cleanly across app restarts without requiring broad storage permissions.
     */
    fun persistPickedImage(context: Context, uri: Uri, folder: String = "uploads"): String? {
        return try {
            val dir = File(context.filesDir, folder)
            if (!dir.exists()) dir.mkdirs()
            val outFile = File(dir, "img_${UUID.randomUUID()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            Uri.fromFile(outFile).toString()
        } catch (_: Exception) {
            null
        }
    }
}
