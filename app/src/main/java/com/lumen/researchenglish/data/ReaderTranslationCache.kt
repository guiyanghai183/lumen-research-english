package com.lumen.researchenglish.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

class ReaderTranslationCache(context: Context) {
    private val preferences = context.getSharedPreferences(
        "lumen_reader_translation_cache",
        Context.MODE_PRIVATE,
    )

    @Synchronized
    fun get(documentId: String, page: Int, provider: String, text: String): String? {
        val key = cacheKey(documentId, page, provider, text)
        return preferences.getString(key, null)?.takeIf(String::isNotBlank)?.also {
            touch(key)
        }
    }

    @Synchronized
    fun put(documentId: String, page: Int, provider: String, text: String, translation: String) {
        if (translation.isBlank()) return
        val key = cacheKey(documentId, page, provider, text)
        val entries = readIndex().filterNot { it.key == key }.toMutableList()
        entries += CacheEntry(key, System.currentTimeMillis())
        val overflow = (entries.size - MAX_ENTRIES).coerceAtLeast(0)
        val removed = entries.sortedBy(CacheEntry::updatedAt).take(overflow)
        val kept = entries.filterNot { candidate -> removed.any { it.key == candidate.key } }
        preferences.edit().apply {
            putString(key, translation)
            removed.forEach { remove(it.key) }
            putString(INDEX, writeIndex(kept))
        }.apply()
    }

    private fun touch(key: String) {
        val entries = readIndex().filterNot { it.key == key } +
            CacheEntry(key, System.currentTimeMillis())
        preferences.edit().putString(INDEX, writeIndex(entries)).apply()
    }

    private fun readIndex(): List<CacheEntry> {
        val raw = preferences.getString(INDEX, "[]").orEmpty()
        val array = runCatching { JSONArray(raw) }.getOrElse { JSONArray() }
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val key = item.optString("key")
                if (key.isNotBlank()) add(CacheEntry(key, item.optLong("updatedAt")))
            }
        }
    }

    private fun writeIndex(entries: List<CacheEntry>): String = JSONArray().apply {
        entries.forEach { entry ->
            put(
                JSONObject()
                    .put("key", entry.key)
                    .put("updatedAt", entry.updatedAt),
            )
        }
    }.toString()

    private data class CacheEntry(val key: String, val updatedAt: Long)

    companion object {
        private const val INDEX = "index"
        private const val MAX_ENTRIES = 200

        internal fun cacheKey(documentId: String, page: Int, provider: String, text: String): String {
            val normalized = listOf(documentId, page.toString(), provider, text.trim())
                .joinToString("\u001f")
            val digest = MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray())
            return "translation_" + digest.joinToString("") { "%02x".format(it) }
        }
    }
}
