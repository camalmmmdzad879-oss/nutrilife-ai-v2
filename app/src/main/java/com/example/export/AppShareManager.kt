package com.example.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object AppShareManager {

    fun hasPublishedPlayStoreLink(): Boolean = BuildConfig.PLAY_STORE_URL.isNotBlank()

    fun shareTextOrPlayLinkIntent(
        appName: String,
        inviteText: String,
        chooserTitle: String
    ): Intent {
        val shareUrl = BuildConfig.PLAY_STORE_URL.ifBlank { BuildConfig.OFFICIAL_DOWNLOAD_URL }
        val message = "$appName (v${BuildConfig.VERSION_NAME})\n$inviteText\n$shareUrl"
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, appName)
            putExtra(Intent.EXTRA_TEXT, message)
        }
        return Intent.createChooser(sendIntent, chooserTitle)
    }

    suspend fun prepareShareableApkUri(context: Context): Uri? = withContext(Dispatchers.IO) {
        try {
            val sourcePath = context.applicationInfo.publicSourceDir ?: return@withContext null
            val sourceFile = File(sourcePath)
            if (!sourceFile.exists() || !sourceFile.canRead()) return@withContext null

            val apkDir = File(context.cacheDir, "shared_apk").apply { mkdirs() }
            // Clean previous cached APKs to minimize storage on 64 GB devices
            apkDir.listFiles()?.forEach { old -> old.delete() }

            val targetApk = File(apkDir, "NutriLifeAI-v${BuildConfig.VERSION_NAME}.apk")
            sourceFile.inputStream().use { input ->
                targetApk.outputStream().use { output ->
                    input.copyTo(output, bufferSize = 32 * 1024)
                }
            }

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                targetApk
            )
        } catch (_: Exception) {
            null
        }
    }

    fun buildApkShareIntent(
        apkUri: Uri,
        appName: String,
        inviteText: String,
        chooserTitle: String
    ): Intent {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.android.package-archive"
            putExtra(Intent.EXTRA_SUBJECT, "$appName v${BuildConfig.VERSION_NAME}")
            putExtra(Intent.EXTRA_TEXT, "$appName — $inviteText")
            putExtra(Intent.EXTRA_STREAM, apkUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(sendIntent, chooserTitle)
    }
}
