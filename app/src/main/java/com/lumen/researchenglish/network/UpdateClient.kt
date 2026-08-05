package com.lumen.researchenglish.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class AppUpdate(
    val versionCode: Long,
    val versionName: String,
    val apkUrl: String,
    val notes: String,
    val sha256: String,
)

class UpdateClient(private val client: OkHttpClient = OkHttpClient()) {
    suspend fun fetch(source: String): AppUpdate = withContext(Dispatchers.IO) {
        val manifestUrl = resolveManifestUrl(source)
        val request = Request.Builder()
            .url(manifestUrl)
            .header("Cache-Control", "no-cache")
            .build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException("Update check failed (${response.code}).")
            }
            if (!response.request.url.isHttps) {
                throw IllegalStateException("Update manifest was redirected away from HTTPS.")
            }
            val json = JSONObject(body)
            val apkUrl = json.getString("apkUrl")
            require(apkUrl.startsWith("https://")) { "The APK download URL must use HTTPS." }
            val sha256 = json.optString("sha256").trim()
            require(SHA256_PATTERN.matches(sha256)) {
                "The update manifest must provide a 64-character SHA-256 checksum."
            }
            AppUpdate(
                versionCode = json.getLong("versionCode"),
                versionName = json.getString("versionName"),
                apkUrl = apkUrl,
                notes = json.optString("notes"),
                sha256 = sha256.lowercase(),
            )
        }
    }

    private fun resolveManifestUrl(source: String): String {
        val clean = source.trim().removeSuffix("/")
        require(clean.startsWith("https://")) { "Enter an HTTPS GitHub repository or update.json URL." }
        if (clean.endsWith(".json", ignoreCase = true)) return clean
        val github = Regex("https://github\\.com/([^/]+)/([^/]+)", RegexOption.IGNORE_CASE)
            .matchEntire(clean)
            ?: throw IllegalArgumentException("Enter a GitHub repository URL or direct update.json URL.")
        return "https://github.com/${github.groupValues[1]}/${github.groupValues[2]}/" +
            "releases/latest/download/update.json"
    }

    private companion object {
        val SHA256_PATTERN = Regex("[a-fA-F0-9]{64}")
    }
}
