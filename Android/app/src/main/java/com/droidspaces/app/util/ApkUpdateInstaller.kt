package com.droidspaces.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.droidspaces.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

sealed class ApkInstallStatus {
    data class Progress(val percent: Int) : ApkInstallStatus()
    data object Ready : ApkInstallStatus()
    data class Failed(val reason: String) : ApkInstallStatus()
}

object ApkUpdateInstaller {

    fun downloadAndInstall(context: Context, apkUrl: String, versionLabel: String): Flow<ApkInstallStatus> =
        flow {
            if (!apkUrl.startsWith("https://", ignoreCase = true)) {
                emit(ApkInstallStatus.Failed(context.getString(R.string.app_update_download_failed)))
                return@flow
            }
            val dir = File(context.cacheDir, "apk-updates").apply { mkdirs() }
            val safeName = "AetherBox-${versionLabel.replace(Regex("[^A-Za-z0-9._-]"), "_")}.apk"
            val outFile = File(dir, safeName)
            if (outFile.exists()) outFile.delete()

            try {
                val conn = (URL(apkUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 30_000
                    readTimeout = 120_000
                    instanceFollowRedirects = true
                    setRequestProperty("Accept", "application/vnd.android.package-archive,*/*")
                }
                if (conn.responseCode !in 200..299) {
                    conn.disconnect()
                    emit(ApkInstallStatus.Failed(context.getString(R.string.app_update_download_failed)))
                    return@flow
                }
                val total = conn.contentLengthLong.coerceAtLeast(0L)
                conn.inputStream.use { input ->
                    outFile.outputStream().use { output ->
                        val buf = ByteArray(64 * 1024)
                        var readTotal = 0L
                        var lastEmit = -1
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            output.write(buf, 0, n)
                            readTotal += n
                            val percent = if (total > 0) {
                                ((readTotal * 100) / total).toInt().coerceIn(0, 99)
                            } else {
                                0
                            }
                            if (percent != lastEmit) {
                                lastEmit = percent
                                emit(ApkInstallStatus.Progress(percent))
                            }
                        }
                        output.flush()
                    }
                }
                conn.disconnect()
                emit(ApkInstallStatus.Progress(100))
                launchInstall(context, outFile)
                emit(ApkInstallStatus.Ready)
            } catch (e: Exception) {
                emit(
                    ApkInstallStatus.Failed(
                        e.message?.takeIf { it.isNotBlank() }
                            ?: context.getString(R.string.app_update_download_failed)
                    )
                )
            }
        }.flowOn(Dispatchers.IO)

    fun launchInstall(context: Context, apkFile: File) {
        if (!apkFile.exists()) return
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun openReleasePage(context: Context, releaseUrl: String) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(releaseUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }
}
