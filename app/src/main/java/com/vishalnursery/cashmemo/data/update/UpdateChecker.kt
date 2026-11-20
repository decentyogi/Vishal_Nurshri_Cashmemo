package com.vishalnursery.cashmemo.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.vishalnursery.cashmemo.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

sealed interface UpdateCheckResult {
    object UpToDate : UpdateCheckResult
    data class Available(
        val version: String,
        val apkUrl: String,
        val sizeBytes: Long,
        val htmlUrl: String = ""
    ) : UpdateCheckResult
    data class Error(val message: String) : UpdateCheckResult
}

object UpdateChecker {
    private const val REPO_SLUG = "decentyogi/Vishal_Nurshri_Cashmemo"
    private const val LATEST_RELEASE_URL = "https://api.github.com/repos/$REPO_SLUG/releases/latest"
    private val VERSION_REGEX = Regex("""(\d+)\.(\d+)\.(\d+)""")

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    suspend fun checkForUpdate(currentVersionName: String = BuildConfig.VERSION_NAME): UpdateCheckResult = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(LATEST_RELEASE_URL)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "VishalNursery-Cashmemo-App")
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext UpdateCheckResult.Error("GitHub Releases unavailable (HTTP ${response.code})")
                }
                val bodyStr = response.body?.string() ?: return@withContext UpdateCheckResult.Error("Empty response from server")
                val json = JSONObject(bodyStr)
                val tagName = json.optString("tag_name", "")
                val htmlUrl = json.optString("html_url", "")
                val assets = json.optJSONArray("assets")

                var apkDownloadUrl = ""
                var apkSize = 0L

                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.optJSONObject(i) ?: continue
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apkDownloadUrl = asset.optString("browser_download_url", "")
                            apkSize = asset.optLong("size", 0L)
                            break
                        }
                    }
                }

                if (apkDownloadUrl.isBlank()) {
                    return@withContext UpdateCheckResult.UpToDate
                }

                val remoteMatch = VERSION_REGEX.find(tagName)
                val currentMatch = VERSION_REGEX.find(currentVersionName)

                if (remoteMatch == null || currentMatch == null) {
                    return@withContext UpdateCheckResult.UpToDate
                }

                val rMaj = remoteMatch.groupValues[1].toIntOrNull() ?: 0
                val rMin = remoteMatch.groupValues[2].toIntOrNull() ?: 0
                val rPat = remoteMatch.groupValues[3].toIntOrNull() ?: 0

                val cMaj = currentMatch.groupValues[1].toIntOrNull() ?: 0
                val cMin = currentMatch.groupValues[2].toIntOrNull() ?: 0
                val cPat = currentMatch.groupValues[3].toIntOrNull() ?: 0

                val isNewer = when {
                    rMaj != cMaj -> rMaj > cMaj
                    rMin != cMin -> rMin > cMin
                    else -> rPat > cPat
                }

                if (isNewer) {
                    val cleanVersion = "$rMaj.$rMin.$rPat"
                    UpdateCheckResult.Available(
                        version = cleanVersion,
                        apkUrl = apkDownloadUrl,
                        sizeBytes = apkSize,
                        htmlUrl = htmlUrl
                    )
                } else {
                    UpdateCheckResult.UpToDate
                }
            }
        } catch (e: UnknownHostException) {
            UpdateCheckResult.Error("Could not check for updates. Please check your internet connection.")
        } catch (e: IOException) {
            UpdateCheckResult.Error("Could not check for updates: ${e.localizedMessage ?: "Network error"}")
        } catch (e: Exception) {
            UpdateCheckResult.Error("Update check failed: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    suspend fun downloadApk(
        context: Context,
        apkUrl: String,
        targetVersion: String,
        onProgress: suspend (bytesRead: Long, totalBytes: Long) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val updateDir = File(context.cacheDir, "updates").apply { mkdirs() }
        updateDir.listFiles()?.forEach { file ->
            runCatching { file.delete() }
        }

        val tempFile = File(updateDir, "update_download.tmp")
        if (tempFile.exists()) tempFile.delete()

        val request = Request.Builder()
            .url(apkUrl)
            .header("User-Agent", "VishalNursery-Cashmemo-App")
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Failed to download APK: HTTP ${response.code}")
            }
            val body = response.body ?: throw IOException("Empty response body")
            val totalBytes = body.contentLength()

            body.byteStream().use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesReadTotal = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        coroutineContext.ensureActive()
                        output.write(buffer, 0, read)
                        bytesReadTotal += read
                        onProgress(bytesReadTotal, totalBytes)
                    }
                    output.flush()
                }
            }

            if (totalBytes > 0 && tempFile.length() != totalBytes) {
                tempFile.delete()
                throw IOException("Downloaded file size mismatch (expected $totalBytes, got ${tempFile.length()})")
            }

            val finalFile = File(updateDir, "Cashmemo-$targetVersion.apk")
            if (finalFile.exists()) finalFile.delete()
            if (!tempFile.renameTo(finalFile)) {
                tempFile.copyTo(finalFile, overwrite = true)
                tempFile.delete()
            }
            finalFile
        }
    }

    fun canInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun requestInstallPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun installApk(context: Context, apkFile: File) {
        if (!canInstallPackages(context)) {
            requestInstallPermission(context)
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
