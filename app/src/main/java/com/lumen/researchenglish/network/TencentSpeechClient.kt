package com.lumen.researchenglish.network

import android.util.Base64
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class TencentSpeechClient(private val client: OkHttpClient = OkHttpClient()) {
    suspend fun synthesize(
        secretId: String,
        secretKey: String,
        text: String,
        voiceType: Int,
        speed: Float,
    ): ByteArray = withContext(Dispatchers.IO) {
        require(secretId.isNotBlank() && secretKey.isNotBlank()) {
            "Please add Tencent Cloud credentials in Settings."
        }
        require(text.isNotBlank()) { "There is no text to read aloud." }

        val host = "tts.tencentcloudapi.com"
        val action = "TextToVoice"
        val service = "tts"
        val version = "2019-08-23"
        val region = "ap-beijing"
        val timestamp = Instant.now().epochSecond
        val date = DateTimeFormatter.ISO_LOCAL_DATE
            .withZone(ZoneOffset.UTC)
            .format(Instant.ofEpochSecond(timestamp))
        val payload = JSONObject()
            .put("Text", text)
            .put("SessionId", UUID.randomUUID().toString().replace("-", ""))
            .put("VoiceType", voiceType)
            .put("Codec", "mp3")
            .put("SampleRate", 24000)
            .put("Speed", speed.toDouble())
            .put("Volume", 0)
            .toString()

        val canonicalHeaders = "content-type:application/json; charset=utf-8\nhost:$host\n"
        val signedHeaders = "content-type;host"
        val canonicalRequest = listOf(
            "POST",
            "/",
            "",
            canonicalHeaders,
            signedHeaders,
            sha256(payload),
        ).joinToString("\n")
        val credentialScope = "$date/$service/tc3_request"
        val stringToSign = listOf(
            "TC3-HMAC-SHA256",
            timestamp.toString(),
            credentialScope,
            sha256(canonicalRequest),
        ).joinToString("\n")
        val secretDate = hmac(("TC3$secretKey").toByteArray(), date)
        val secretService = hmac(secretDate, service)
        val secretSigning = hmac(secretService, "tc3_request")
        val signature = hmac(secretSigning, stringToSign).joinToString("") { "%02x".format(it) }
        val authorization =
            "TC3-HMAC-SHA256 Credential=$secretId/$credentialScope, " +
                "SignedHeaders=$signedHeaders, Signature=$signature"

        val request = Request.Builder()
            .url("https://$host/")
            .header("Authorization", authorization)
            .header("Content-Type", "application/json; charset=utf-8")
            .header("Host", host)
            .header("X-TC-Action", action)
            .header("X-TC-Timestamp", timestamp.toString())
            .header("X-TC-Version", version)
            .header("X-TC-Region", region)
            .post(payload.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException("Speech synthesis failed (${response.code}).")
            }
            val result = JSONObject(body).getJSONObject("Response")
            result.optJSONObject("Error")?.let {
                throw IllegalStateException(it.optString("Message", "Speech synthesis failed."))
            }
            Base64.decode(result.getString("Audio"), Base64.DEFAULT)
        }
    }
}

private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

private fun sha256(value: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray())
        .joinToString("") { "%02x".format(it) }

private fun hmac(key: ByteArray, value: String): ByteArray =
    Mac.getInstance("HmacSHA256").run {
        init(SecretKeySpec(key, "HmacSHA256"))
        doFinal(value.toByteArray())
    }
