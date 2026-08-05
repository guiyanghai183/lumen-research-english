package com.lumen.researchenglish.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class AnnotationRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

data class ReaderAnnotation(
    val id: String,
    val documentId: String,
    val page: Int,
    val style: String,
    val color: String,
    val text: String,
    val rects: List<AnnotationRect>,
    val createdAt: Long,
)

class ReaderAnnotationStore(context: Context) {
    private val preferences = context.getSharedPreferences(
        "lumen_reader_annotations",
        Context.MODE_PRIVATE,
    )

    @Synchronized
    fun annotationsFor(documentId: String, page: Int): List<ReaderAnnotation> =
        readAll().filter { it.documentId == documentId && it.page == page }

    @Synchronized
    fun add(
        documentId: String,
        page: Int,
        style: String,
        color: String,
        text: String,
        words: List<RecognizedWord>,
    ): ReaderAnnotation {
        val normalizedStyle = normalizeStyle(style)
        val annotation = ReaderAnnotation(
            id = UUID.randomUUID().toString(),
            documentId = documentId,
            page = page,
            style = normalizedStyle,
            color = normalizeColor(color, normalizedStyle),
            text = text.trim(),
            rects = words.map {
                AnnotationRect(
                    left = it.left,
                    top = it.top,
                    right = it.right,
                    bottom = it.bottom,
                )
            },
            createdAt = System.currentTimeMillis(),
        )
        val preservedAnnotations = readAll().mapNotNull { existing ->
            if (
                existing.documentId != documentId ||
                existing.page != page ||
                existing.style != normalizedStyle
            ) {
                return@mapNotNull existing
            }
            val remainingRects = existing.rects.filterNot { existingRect ->
                annotation.rects.any { newRect -> overlaps(existingRect, newRect) }
            }
            if (remainingRects.isEmpty()) null else existing.copy(rects = remainingRects)
        }
        writeAll(preservedAnnotations + annotation)
        return annotation
    }

    @Synchronized
    fun removeOverlapping(
        documentId: String,
        page: Int,
        style: String,
        words: List<RecognizedWord>,
    ): Int {
        if (words.isEmpty()) return 0
        val normalizedStyle = normalizeStyle(style)
        val selectionRects = words.map {
            AnnotationRect(it.left, it.top, it.right, it.bottom)
        }
        var removedRectCount = 0
        val updated = readAll().mapNotNull { annotation ->
            if (
                annotation.documentId != documentId ||
                annotation.page != page ||
                annotation.style != normalizedStyle
            ) {
                return@mapNotNull annotation
            }

            val remainingRects = annotation.rects.filterNot { annotationRect ->
                selectionRects.any { selectionRect -> overlaps(annotationRect, selectionRect) }
            }
            removedRectCount += annotation.rects.size - remainingRects.size
            if (remainingRects.isEmpty()) null else annotation.copy(rects = remainingRects)
        }
        if (removedRectCount > 0) writeAll(updated)
        return removedRectCount
    }

    private fun readAll(): List<ReaderAnnotation> {
        val raw = preferences.getString(ANNOTATIONS, "[]").orEmpty()
        val array = runCatching { JSONArray(raw) }.getOrElse { JSONArray() }
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val rectArray = item.optJSONArray("rects") ?: JSONArray()
                val rects = buildList {
                    for (rectIndex in 0 until rectArray.length()) {
                        val rect = rectArray.optJSONObject(rectIndex) ?: continue
                        add(
                            AnnotationRect(
                                left = rect.optDouble("left").toFloat(),
                                top = rect.optDouble("top").toFloat(),
                                right = rect.optDouble("right").toFloat(),
                                bottom = rect.optDouble("bottom").toFloat(),
                            ),
                        )
                    }
                }
                if (rects.isEmpty()) continue
                val style = normalizeStyle(item.optString("style", "highlight"))
                add(
                    ReaderAnnotation(
                        id = item.optString("id"),
                        documentId = item.optString("documentId"),
                        page = item.optInt("page"),
                        style = style,
                        color = normalizeColor(
                            item.optString("color", defaultColorFor(style)),
                            style,
                        ),
                        text = item.optString("text"),
                        rects = rects,
                        createdAt = item.optLong("createdAt"),
                    ),
                )
            }
        }
    }

    private fun writeAll(annotations: List<ReaderAnnotation>) {
        val array = JSONArray()
        annotations.forEach { annotation ->
            array.put(
                JSONObject().apply {
                    put("id", annotation.id)
                    put("documentId", annotation.documentId)
                    put("page", annotation.page)
                    put("style", annotation.style)
                    put("color", annotation.color)
                    put("text", annotation.text)
                    put("createdAt", annotation.createdAt)
                    put(
                        "rects",
                        JSONArray().apply {
                            annotation.rects.forEach { rect ->
                                put(
                                    JSONObject().apply {
                                        put("left", rect.left.toDouble())
                                        put("top", rect.top.toDouble())
                                        put("right", rect.right.toDouble())
                                        put("bottom", rect.bottom.toDouble())
                                    },
                                )
                            }
                        },
                    )
                },
            )
        }
        preferences.edit().putString(ANNOTATIONS, array.toString()).apply()
    }

    companion object {
        private const val ANNOTATIONS = "annotations"
        private val ALLOWED_COLORS = setOf("yellow", "green", "blue", "pink", "purple")

        private fun normalizeStyle(style: String): String =
            style.takeIf { it == "underline" } ?: "highlight"

        private fun defaultColorFor(style: String): String =
            if (style == "underline") "blue" else "yellow"

        private fun normalizeColor(color: String, style: String): String =
            color.lowercase().takeIf(ALLOWED_COLORS::contains) ?: defaultColorFor(style)

        private fun overlaps(first: AnnotationRect, second: AnnotationRect): Boolean =
            first.left < second.right && first.right > second.left &&
                first.top < second.bottom && first.bottom > second.top
    }
}
