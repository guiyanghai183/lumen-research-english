package com.lumen.researchenglish.network

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest

data class UpdateDownloadProgress(
    val bytesDownloaded: Long,
    val totalBytes: Long?,
) {
    val fraction: Float?
        get() = totalBytes
            ?.takeIf { it > 0L }
            ?.let { (bytesDownloaded.toFloat() / it.toFloat()).coerceIn(0f, 1f) }
}

sealed interface UpdateInstallResult {
    data object InstallerLaunched : UpdateInstallResult
    data object PermissionRequired : UpdateInstallResult
}

/**
 * Downloads a release APK to the app cache, verifies its release checksum, then opens Android's
 * package installer. Android deliberately keeps the final install confirmation under system control.
 */
class UpdateInstaller(
    context: Context,
    private val client: OkHttpClient = OkHttpClient(),
) {
    private val appContext = context.applicationContext

    suspend fun download(
        update: AppUpdate,
        onProgress: (UpdateDownloadProgress) -> Unit = {},
    ): File = withContext(Dispatchers.IO) {
        require(update.apkUrl.startsWith("https://")) { "The APK download URL must use HTTPS." }
        val expectedHash = validatedSha256(update.sha256)
        val destination = apkFile(update)
        val partial = File(destination.parentFile, "${destination.name}.download")
        partial.delete()

        try {
            val request = Request.Builder()
                .url(update.apkUrl)
                .header("Cache-Control", "no-cache")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("APK download failed (${response.code}).")
                }
                if (!response.request.url.isHttps) {
                    throw IllegalStateException("APK download was redirected away from HTTPS.")
                }
                val body = response.body ?: throw IllegalStateException("APK download returned no data.")
                val totalBytes = body.contentLength().takeIf { it >= 0L }
                val digest = MessageDigest.getInstance("SHA-256")
                var downloadedBytes = 0L
                onProgress(UpdateDownloadProgress(downloadedBytes, totalBytes))

                body.byteStream().use { input ->
                    FileOutputStream(partial).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            digest.update(buffer, 0, read)
                            downloadedBytes += read
                            onProgress(UpdateDownloadProgress(downloadedBytes, totalBytes))
                        }
                        output.fd.sync()
                    }
                }

                val actualHash = digest.digest().toHex()
                if (!actualHash.equals(expectedHash, ignoreCase = true)) {
                    throw IllegalStateException("Downloaded APK failed its SHA-256 check.")
                }
            }

            if (destination.exists() && !destination.delete()) {
                throw IllegalStateException("Could not replace the previous downloaded update.")
            }
            if (!partial.renameTo(destination)) {
                throw IllegalStateException("Could not finish saving the downloaded update.")
            }
            destination
        } catch (error: Throwable) {
            partial.delete()
            throw error
        }
    }

    suspend fun findVerifiedDownload(update: AppUpdate): File? = withContext(Dispatchers.IO) {
        val destination = apkFile(update)
        if (!destination.isFile) return@withContext null
        val expectedHash = runCatching { validatedSha256(update.sha256) }.getOrNull()
            ?: return@withContext null
        val actualHash = sha256Of(destination)
        if (actualHash.equals(expectedHash, ignoreCase = true)) {
            destination
        } else {
            destination.delete()
            null
        }
    }

    fun launchInstaller(apk: File): UpdateInstallResult {
        check(apk.isFile) { "The downloaded update is no longer available." }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !appContext.packageManager.canRequestPackageInstalls()
        ) {
            return UpdateInstallResult.PermissionRequired
        }

        val uri = FileProvider.getUriForFile(appContext, providerAuthority, apk)
        val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            data = uri
            clipData = ClipData.newRawUri("Lumen update", uri)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        appContext.startActivity(installIntent)
        return UpdateInstallResult.InstallerLaunched
    }

    fun openUnknownSourcesSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${appContext.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(intent)
    }

    private fun apkFile(update: AppUpdate): File {
        require(update.versionCode > 0L) { "The update version code is invalid." }
        val directory = File(appContext.cacheDir, UPDATE_DIRECTORY)
        if (!directory.exists() && !directory.mkdirs()) {
            throw IllegalStateException("Could not create update download storage.")
        }
        check(directory.isDirectory) { "Update download storage is unavailable." }
        return File(directory, "lumen-${update.versionCode}.apk")
    }

    private fun sha256Of(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().toHex()
    }

    private fun validatedSha256(value: String): String {
        val clean = value.trim()
        require(SHA256_PATTERN.matches(clean)) {
            "The update manifest must provide a 64-character SHA-256 checksum."
        }
        return clean.lowercase()
    }

    private fun ByteArray.toHex(): String = joinToString(separator = "") { byte ->
        "%02x".format(byte.toInt() and 0xff)
    }

    private companion object {
        const val UPDATE_DIRECTORY = "updates"
        const val PROVIDER_SUFFIX = ".updateprovider"
        val SHA256_PATTERN = Regex("[a-fA-F0-9]{64}")
    }

    private val providerAuthority: String
        get() = appContext.packageName + PROVIDER_SUFFIX
}
