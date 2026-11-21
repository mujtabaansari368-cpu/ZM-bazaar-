package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object IntentHelper {
    const val WHATSAPP_BASE_URL = "https://wa.me/923558279470"
    const val SUPPORT_EMAIL = "ansariproducts05@gmail.com"
    private const val PREFS_NAME = "zm_bazaar_prefs"
    private const val KEY_APP_DOWNLOAD_URL = "app_download_url"

    fun openWhatsApp(context: Context, message: String = "Assalam-o-Alaikum ZM Bazaar! I need help with an order.") {
        val encoded = Uri.encode(message)
        val uri = Uri.parse("$WHATSAPP_BASE_URL?text=$encoded")
        try {
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(
                context,
                "WhatsApp: 03558279470 ($WHATSAPP_BASE_URL)",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun openEmail(context: Context, subject: String = "ZM Bazaar Inquiry") {
        val uri = Uri.parse("mailto:$SUPPORT_EMAIL?subject=${Uri.encode(subject)}")
        try {
            val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            copyToClipboard(context, "ZM Bazaar Email", SUPPORT_EMAIL)
        }
    }

    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, "$label copied: $text", Toast.LENGTH_SHORT).show()
    }

    fun getSavedAppDownloadUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_APP_DOWNLOAD_URL, "") ?: ""
    }

    fun saveAppDownloadUrl(context: Context, url: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_APP_DOWNLOAD_URL, url.trim()).apply()
    }

    fun prepareShareableApkUri(context: Context): Uri? {
        return try {
            val sourcePath = context.applicationInfo.sourceDir ?: return null
            val sourceFile = File(sourcePath)
            if (!sourceFile.exists()) return null
            val shareDir = File(context.filesDir, "shared_apks").apply { mkdirs() }
            val destFile = File(shareDir, "ZM-Bazaar.apk")
            sourceFile.inputStream().use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", destFile)
        } catch (_: Exception) {
            null
        }
    }

    fun writeInstalledApkToUri(context: Context, destinationUri: Uri): Boolean {
        return try {
            val sourcePath = context.applicationInfo.sourceDir ?: return false
            val sourceFile = File(sourcePath)
            if (!sourceFile.exists()) return false
            context.contentResolver.openOutputStream(destinationUri)?.use { output ->
                sourceFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            } ?: return false
            true
        } catch (_: Exception) {
            false
        }
    }

    fun shareApkFile(context: Context) {
        val uri = prepareShareableApkUri(context)
        if (uri == null) {
            Toast.makeText(
                context,
                "Install the APK on your phone first to share ZM-Bazaar.apk directly.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "ZM Bazaar Android App (ZM-Bazaar.apk)")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Install ZM Bazaar - Pakistan Ka No.1 Online Bazaar! Open this APK file to install on your phone."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Share ZM-Bazaar.apk via WhatsApp / Files").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (_: Exception) {
            Toast.makeText(context, "Unable to open share sheet on this device.", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareAppLink(context: Context, downloadUrl: String) {
        val cleanUrl = downloadUrl.trim()
        val message = if (cleanUrl.isNotBlank()) {
            "Download & Install ZM Bazaar - Pakistan Ka No.1 Online Bazaar:\n$cleanUrl\n\nWhatsApp Support: $WHATSAPP_BASE_URL"
        } else {
            "Check out ZM Bazaar - Pakistan Ka No.1 Online Bazaar! WhatsApp: $WHATSAPP_BASE_URL"
        }
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Download ZM Bazaar App")
                putExtra(Intent.EXTRA_TEXT, message)
            }
            val chooser = Intent.createChooser(intent, "Share ZM Bazaar Link on WhatsApp / Apps").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (_: Exception) {
            openWhatsApp(context, message)
        }
    }

    fun openDownloadUrl(context: Context, downloadUrl: String) {
        val cleanUrl = downloadUrl.trim()
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            Toast.makeText(context, "Please enter a valid https:// APK download link first.", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(cleanUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            copyToClipboard(context, "Download Link", cleanUrl)
        }
    }

    fun formatRs(amount: Double): String {
        val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
            maximumFractionDigits = 0
        }
        return "Rs. ${formatter.format(amount.toLong())}"
    }

    fun formatDateTime(millis: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        return sdf.format(Date(millis))
    }
}
