package com.lumen.researchenglish.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ReaderBookmark(
    val documentId: String,
    val page: Int,
    val createdAt: Long,
)

class ReaderBookmarkStore(context: Context) {
    private val preferences = context.getSharedPreferences(
        "lumen_reader_bookmarks",
        Context.MODE_PRIVATE,
    )

    @Synchronized
    fun bookmarksFor(documentId: String): List<ReaderBookmark> =
        readAll()
            .filter { it.documentId == documentId }
            .sortedBy { it.page }

    /** Returns true when the page is bookmarked after the toggle. */
    @Synchronized
    fun toggle(documentId: String, page: Int): Boolean {
        val safePage = page.coerceAtLeast(0)
        val bookmarks = readAll().toMutableList()
        val existingIndex = bookmarks.indexOfFirst {
            it.documentId == documentId && it.page == safePage
        }
        val bookmarked = existingIndex < 0
        if (bookmarked) {
            bookmarks += ReaderBookmark(
                documentId = documentId,
                page = safePage,
                createdAt = System.currentTimeMillis(),
            )
        } else {
            bookmarks.removeAt(existingIndex)
        }
        writeAll(bookmarks)
        return bookmarked
    }

    @Synchronized
    fun removeDocument(documentId: String) {
        val bookmarks = readAll()
        val preserved = bookmarks.filterNot { it.documentId == documentId }
        if (preserved.size != bookmarks.size) writeAll(preserved)
    }

    private fun readAll(): List<ReaderBookmark> {
        val raw = preferences.getString(BOOKMARKS, "[]").orEmpty()
        val array = runCatching { JSONArray(raw) }.getOrElse { JSONArray() }
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val documentId = item.optString("documentId").trim()
                val page = item.optInt("page", -1)
                if (documentId.isBlank() || page < 0) continue
                add(
                    ReaderBookmark(
                        documentId = documentId,
                        page = page,
                        createdAt = item.optLong("createdAt"),
                    ),
                )
            }
        }
    }

    private fun writeAll(bookmarks: List<ReaderBookmark>) {
        val array = JSONArray()
        bookmarks.forEach { bookmark ->
            array.put(
                JSONObject().apply {
                    put("documentId", bookmark.documentId)
                    put("page", bookmark.page)
                    put("createdAt", bookmark.createdAt)
                },
            )
        }
        preferences.edit().putString(BOOKMARKS, array.toString()).apply()
    }

    companion object {
        private const val BOOKMARKS = "bookmarks"
    }
}
