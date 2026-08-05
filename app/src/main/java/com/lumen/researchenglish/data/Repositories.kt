package com.lumen.researchenglish.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.text.Html
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.lumen.researchenglish.domain.ReviewRating
import com.lumen.researchenglish.domain.ReviewScheduler
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class RecognizedWord(
    val text: String,
    val lineText: String,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

data class RecognizedPage(
    val text: String,
    val words: List<RecognizedWord>,
)

data class GutenbergBook(
    val id: Int,
    val title: String,
    val author: String,
    val coverUrl: String,
    val landingUrl: String = "https://www.gutenberg.org/ebooks/$id",
)

class DocumentRepository(
    private val context: Context,
    private val dao: DocumentDao,
) {
    private val webClient = OkHttpClient()
    val documents: Flow<List<DocumentEntity>> = dao.observeAll()

    suspend fun seedBundledBooks() = withContext(Dispatchers.IO) {
        val bookDirectory = File(context.filesDir, "builtin_books").apply { mkdirs() }
        val hiddenBooks = libraryPreferences.getStringSet(HIDDEN_BUNDLED_BOOKS, emptySet()).orEmpty()
        BUNDLED_BOOKS.forEach { book ->
            if (book.id in hiddenBooks) return@forEach
            if (dao.getById(book.id) != null) return@forEach
            val target = File(bookDirectory, book.assetName)
            if (!target.exists() || target.length() == 0L) {
                context.assets.open("builtin_books/${book.assetName}").use { input ->
                    FileOutputStream(target).use(input::copyTo)
                }
            }
            val uri = Uri.fromFile(target)
            val metadata = createCover(uri, book.id)
            dao.insert(
                DocumentEntity(
                    id = book.id,
                    title = book.title,
                    type = "NOVEL",
                    uri = uri.toString(),
                    coverPath = metadata.first,
                    pageCount = metadata.second,
                ),
            )
        }
    }

    suspend fun importPdf(uri: Uri, type: String): String = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val title = queryDisplayName(uri).removeSuffix(".pdf").ifBlank { "Untitled PDF" }
        val metadata = createCover(uri, id)
        dao.insert(
            DocumentEntity(
                id = id,
                title = title,
                type = type,
                uri = uri.toString(),
                coverPath = metadata.first,
                pageCount = metadata.second,
            ),
        )
        id
    }

    suspend fun searchGutenberg(query: String): List<GutenbergBook> = withContext(Dispatchers.IO) {
        val clean = query.trim()
        require(clean.length >= 2) { "Enter at least two characters to search Project Gutenberg." }
        val encoded = URLEncoder.encode(clean, Charsets.UTF_8.name())
        val html = downloadText("https://www.gutenberg.org/ebooks/search/?query=$encoded")
        BOOK_LINK_BLOCK.findAll(html).mapNotNull { match ->
            val block = match.value
            val id = BOOK_ID.find(block)?.groupValues?.getOrNull(1)?.toIntOrNull()
                ?: return@mapNotNull null
            val title = TITLE.find(block)?.groupValues?.getOrNull(1)?.let(::decodeHtml)
                ?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            val author = SUBTITLE.find(block)?.groupValues?.getOrNull(1)?.let(::decodeHtml)
                .orEmpty()
            val rawCover = COVER_SOURCE.find(block)?.groupValues?.getOrNull(1).orEmpty()
            GutenbergBook(
                id = id,
                title = title,
                author = author,
                coverUrl = absoluteGutenbergUrl(rawCover).ifBlank {
                    "https://www.gutenberg.org/cache/epub/$id/pg$id.cover.medium.jpg"
                },
            )
        }.distinctBy { it.id }.take(20).toList()
    }

    suspend fun importGutenberg(book: GutenbergBook): String = withContext(Dispatchers.IO) {
        val documentId = "gutenberg-${book.id}"
        if (dao.getById(documentId) != null) return@withContext documentId
        val text = listOf(
            "https://www.gutenberg.org/ebooks/${book.id}.txt.utf-8",
            "https://www.gutenberg.org/cache/epub/${book.id}/pg${book.id}.txt",
        )
            .firstNotNullOfOrNull { url -> runCatching { downloadText(url) }.getOrNull() }
            ?.takeIf { it.length > 500 }
            ?: error("Project Gutenberg did not provide a readable plain-text edition for this book.")
        val cover = runCatching { downloadBytes(book.coverUrl) }
            .getOrNull()
            ?.let { bytes -> BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }
        val directory = File(context.filesDir, "gutenberg_books").apply { mkdirs() }
        val pdfFile = File(directory, "$documentId.pdf")
        try {
            createGutenbergPdf(pdfFile, book, text, cover)
        } finally {
            cover?.recycle()
        }
        val uri = Uri.fromFile(pdfFile)
        val metadata = createCover(uri, documentId)
        dao.insert(
            DocumentEntity(
                id = documentId,
                title = book.title,
                type = "NOVEL",
                uri = uri.toString(),
                coverPath = metadata.first,
                pageCount = metadata.second,
            ),
        )
        documentId
    }

    suspend fun getDocument(id: String): DocumentEntity? = withContext(Dispatchers.IO) {
        dao.getById(id)
    }

    suspend fun deleteDocument(id: String) = withContext(Dispatchers.IO) {
        val document = dao.getById(id) ?: return@withContext
        dao.delete(id)
        deleteInternalFile(document.coverPath)
        val uri = Uri.parse(document.uri)
        if (uri.scheme == "file") deleteInternalFile(uri.path)
        if (id.startsWith(BUNDLED_BOOK_PREFIX)) {
            val hidden = libraryPreferences
                .getStringSet(HIDDEN_BUNDLED_BOOKS, emptySet())
                .orEmpty()
                .toMutableSet()
                .apply { add(id) }
            libraryPreferences.edit().putStringSet(HIDDEN_BUNDLED_BOOKS, hidden).apply()
        }
    }

    suspend fun renderPage(document: DocumentEntity, pageIndex: Int, width: Int = 1600): Bitmap =
        withContext(Dispatchers.IO) {
            val uri = Uri.parse(document.uri)
            val descriptor = requireNotNull(openFileDescriptor(uri)) {
                "Cannot open this PDF."
            }
            descriptor.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val safePage = pageIndex.coerceIn(0, renderer.pageCount - 1)
                    renderer.openPage(safePage).use { page ->
                        val scale = width.toFloat() / page.width.toFloat()
                        val height = (page.height * scale).toInt().coerceAtLeast(1)
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        Canvas(bitmap).drawColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap
                    }
                }
            }
        }

    suspend fun extractPageText(document: DocumentEntity, pageIndex: Int): String =
        withContext(Dispatchers.IO) {
            val uri = Uri.parse(document.uri)
            val extracted = runCatching {
                openInputStream(uri)?.use { stream ->
                    PDDocument.load(stream).use { pdf ->
                        PDFTextStripper().apply {
                            startPage = pageIndex + 1
                            endPage = pageIndex + 1
                            sortByPosition = true
                        }.getText(pdf).trim()
                    }
                }.orEmpty()
            }.getOrDefault("")

            if (extracted.length >= 12) {
                extracted
            } else {
                val pageBitmap = renderPage(document, pageIndex, 1800)
                try {
                    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    recognizer.process(InputImage.fromBitmap(pageBitmap, 0)).await().text.trim()
                } finally {
                    pageBitmap.recycle()
                }
            }
        }

    suspend fun recognizePagePositions(
        document: DocumentEntity,
        pageIndex: Int,
    ): RecognizedPage = withContext(Dispatchers.IO) {
        val pageBitmap = renderPage(document, pageIndex, 1800)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val result = recognizer.process(InputImage.fromBitmap(pageBitmap, 0)).await()
            val bitmapWidth = pageBitmap.width.toFloat().coerceAtLeast(1f)
            val bitmapHeight = pageBitmap.height.toFloat().coerceAtLeast(1f)
            val words = result.textBlocks.flatMap { block ->
                block.lines.flatMap { line ->
                    line.elements.mapNotNull { element ->
                        val bounds = element.boundingBox ?: return@mapNotNull null
                        RecognizedWord(
                            text = element.text,
                            lineText = line.text,
                            left = (bounds.left / bitmapWidth).coerceIn(0f, 1f),
                            top = (bounds.top / bitmapHeight).coerceIn(0f, 1f),
                            right = (bounds.right / bitmapWidth).coerceIn(0f, 1f),
                            bottom = (bounds.bottom / bitmapHeight).coerceIn(0f, 1f),
                        )
                    }
                }
            }
            RecognizedPage(
                text = result.text.trim(),
                words = words,
            )
        } finally {
            recognizer.close()
            pageBitmap.recycle()
        }
    }

    suspend fun saveProgress(document: DocumentEntity, page: Int) = withContext(Dispatchers.IO) {
        val currentProgress = if (document.pageCount <= 1) 1f else {
            (page + 1).toFloat() / document.pageCount.toFloat()
        }
        dao.update(
            document.copy(
                lastPage = page,
                progress = maxOf(document.progress, currentProgress).coerceIn(0f, 1f),
                lastOpenedAt = System.currentTimeMillis(),
            ),
        )
    }

    private fun queryDisplayName(uri: Uri): String {
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        } ?: uri.lastPathSegment.orEmpty()
    }

    private fun createCover(uri: Uri, id: String): Pair<String?, Int> {
        return runCatching {
            val descriptor = requireNotNull(openFileDescriptor(uri))
            descriptor.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (renderer.pageCount == 0) return null to 0
                    renderer.openPage(0).use { page ->
                        val width = 600
                        val height = (page.height * (width.toFloat() / page.width)).toInt()
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        Canvas(bitmap).drawColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        val coverDir = File(context.filesDir, "covers").apply { mkdirs() }
                        val coverFile = File(coverDir, "$id.png")
                        FileOutputStream(coverFile).use {
                            bitmap.compress(Bitmap.CompressFormat.PNG, 92, it)
                        }
                        bitmap.recycle()
                        coverFile.absolutePath to renderer.pageCount
                    }
                }
            }
        }.getOrElse { null to 0 }
    }

    private fun openFileDescriptor(uri: Uri): ParcelFileDescriptor? =
        if (uri.scheme == "file") {
            uri.path?.let { ParcelFileDescriptor.open(File(it), ParcelFileDescriptor.MODE_READ_ONLY) }
        } else {
            context.contentResolver.openFileDescriptor(uri, "r")
        }

    private fun openInputStream(uri: Uri) =
        if (uri.scheme == "file") {
            uri.path?.let { File(it).inputStream() }
        } else {
            context.contentResolver.openInputStream(uri)
        }

    private fun deleteInternalFile(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching {
            val file = File(path).canonicalFile
            val internalRoot = context.filesDir.canonicalFile
            if (file.path.startsWith(internalRoot.path + File.separator) && file.isFile) {
                file.delete()
            }
        }
    }

    private fun downloadText(url: String): String =
        downloadBytes(url).toString(Charsets.UTF_8)

    private fun downloadBytes(url: String): ByteArray {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Lumen Research English/1.1 (Android; single-page catalog search)")
            .build()
        webClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Project Gutenberg request failed (${response.code}).")
            }
            return response.body?.bytes()
                ?: throw IllegalStateException("Project Gutenberg returned an empty response.")
        }
    }

    private fun createGutenbergPdf(
        target: File,
        book: GutenbergBook,
        sourceText: String,
        cover: Bitmap?,
    ) {
        val width = 1080
        val height = 1920
        val margin = 82f
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(36, 38, 44)
            textSize = 34f
            typeface = Typeface.create("serif", Typeface.NORMAL)
        }
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(82, 84, 94)
            textSize = 22f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(31, 41, 78)
            textSize = 58f
            typeface = Typeface.create("serif", Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val document = PdfDocument()
        var pageNumber = 1
        run {
            val page = document.startPage(PdfDocument.PageInfo.Builder(width, height, pageNumber++).create())
            page.canvas.drawColor(Color.rgb(248, 247, 243))
            if (cover != null) {
                val maxWidth = width - 180
                val maxHeight = height - 360
                val scale = minOf(maxWidth / cover.width.toFloat(), maxHeight / cover.height.toFloat())
                val drawWidth = (cover.width * scale).toInt()
                val drawHeight = (cover.height * scale).toInt()
                val left = (width - drawWidth) / 2
                val top = 110
                page.canvas.drawBitmap(cover, null, Rect(left, top, left + drawWidth, top + drawHeight), null)
            } else {
                page.canvas.drawText(book.title.take(48), width / 2f, height * 0.42f, titlePaint)
                titlePaint.textSize = 34f
                page.canvas.drawText(book.author.take(60), width / 2f, height * 0.49f, titlePaint)
            }
            headerPaint.textAlign = Paint.Align.CENTER
            page.canvas.drawText(
                "Project Gutenberg ebook #${book.id}",
                width / 2f,
                height - 70f,
                headerPaint,
            )
            headerPaint.textAlign = Paint.Align.LEFT
            document.finishPage(page)
        }

        val maxTextWidth = width - margin * 2
        val lineHeight = 48f
        var currentPage: PdfDocument.Page? = null
        var y = 0f

        fun startTextPage() {
            currentPage = document.startPage(
                PdfDocument.PageInfo.Builder(width, height, pageNumber++).create(),
            )
            currentPage!!.canvas.drawColor(Color.rgb(250, 249, 246))
            currentPage!!.canvas.drawText(book.title.take(72), margin, 52f, headerPaint)
            y = 104f
        }

        fun finishTextPage() {
            val page = currentPage ?: return
            headerPaint.textAlign = Paint.Align.CENTER
            page.canvas.drawText((pageNumber - 1).toString(), width / 2f, height - 42f, headerPaint)
            headerPaint.textAlign = Paint.Align.LEFT
            document.finishPage(page)
            currentPage = null
        }

        fun ensureLine() {
            if (currentPage == null) startTextPage()
            if (y + lineHeight > height - 78f) {
                finishTextPage()
                startTextPage()
            }
        }

        sourceText.replace("\r\n", "\n").replace('\r', '\n').lines().forEach { rawLine ->
            var remaining = rawLine.trimEnd()
            if (remaining.isBlank()) {
                ensureLine()
                y += lineHeight * 0.62f
                return@forEach
            }
            while (remaining.isNotEmpty()) {
                ensureLine()
                var count = bodyPaint.breakText(remaining, true, maxTextWidth, null).coerceAtLeast(1)
                if (count < remaining.length) {
                    val whitespace = remaining.lastIndexOf(' ', count - 1)
                    if (whitespace > count / 2) count = whitespace + 1
                }
                val line = remaining.take(count).trim()
                currentPage!!.canvas.drawText(line, margin, y, bodyPaint)
                y += lineHeight
                remaining = remaining.drop(count).trimStart()
            }
        }
        finishTextPage()
        FileOutputStream(target).use(document::writeTo)
        document.close()
    }

    private fun decodeHtml(value: String): String =
        Html.fromHtml(value, Html.FROM_HTML_MODE_LEGACY).toString().trim()

    private fun absoluteGutenbergUrl(value: String): String = when {
        value.startsWith("//") -> "https:$value"
        value.startsWith("/") -> "https://www.gutenberg.org$value"
        value.startsWith("http") -> value
        else -> ""
    }

    private val libraryPreferences by lazy {
        context.getSharedPreferences("lumen_library", Context.MODE_PRIVATE)
    }

    private data class BundledBook(
        val id: String,
        val title: String,
        val assetName: String,
    )

    companion object {
        private const val BUNDLED_BOOK_PREFIX = "classic-"
        private const val HIDDEN_BUNDLED_BOOKS = "hidden_bundled_books"
        private val BOOK_LINK_BLOCK = Regex(
            "<li[^>]*class=\\\"booklink\\\"[^>]*>.*?</li>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
        private val BOOK_ID = Regex("href=\\\"/ebooks/(\\d+)\\\"", RegexOption.IGNORE_CASE)
        private val TITLE = Regex(
            "<span[^>]*class=\\\"title\\\"[^>]*>(.*?)</span>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
        private val SUBTITLE = Regex(
            "<span[^>]*class=\\\"subtitle\\\"[^>]*>(.*?)</span>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
        private val COVER_SOURCE = Regex(
            "<img[^>]*src=\\\"([^\\\"]+)\\\"",
            RegexOption.IGNORE_CASE,
        )
        private val BUNDLED_BOOKS = listOf(
            BundledBook("classic-pride-and-prejudice", "Pride and Prejudice", "pride-and-prejudice.pdf"),
            BundledBook("classic-jane-eyre", "Jane Eyre", "jane-eyre.pdf"),
            BundledBook("classic-wuthering-heights", "Wuthering Heights", "wuthering-heights.pdf"),
            BundledBook("classic-frankenstein", "Frankenstein", "frankenstein.pdf"),
            BundledBook("classic-alice-in-wonderland", "Alice's Adventures in Wonderland", "alice-in-wonderland.pdf"),
            BundledBook("classic-a-tale-of-two-cities", "A Tale of Two Cities", "a-tale-of-two-cities.pdf"),
            BundledBook("classic-the-picture-of-dorian-gray", "The Picture of Dorian Gray", "the-picture-of-dorian-gray.pdf"),
            BundledBook("classic-the-time-machine", "The Time Machine", "the-time-machine.pdf"),
            BundledBook("classic-dracula", "Dracula", "dracula.pdf"),
            BundledBook("classic-the-great-gatsby", "The Great Gatsby", "the-great-gatsby.pdf"),
        )
    }
}

class VocabularyRepository(private val dao: VocabularyDao) {
    val cards: Flow<List<VocabularyCardEntity>> = dao.observeAll()
    fun dueCards(now: Long): Flow<List<VocabularyCardEntity>> = dao.observeDue(now)
    fun dueCardCount(now: Long): Flow<Int> = dao.observeDueCount(now)

    suspend fun saveCard(
        term: String,
        translation: String,
        context: String,
        sourceTitle: String,
        sourcePage: Int,
    ) {
        val cleanTerm = term.trim().replace(Regex("\\s+"), " ")
        if (cleanTerm.isBlank()) return
        val lemma = cleanTerm.lowercase()
        val existing = dao.findByLemma(lemma)
        val now = System.currentTimeMillis()
        if (existing != null) {
            val newContext = if (context.isBlank() || existing.context.contains(context)) {
                existing.context
            } else {
                listOf(existing.context, context).filter { it.isNotBlank() }.joinToString("\n\n")
            }
            dao.update(
                existing.copy(
                    translation = translation.ifBlank { existing.translation },
                    context = newContext,
                ),
            )
        } else {
            dao.insert(
                VocabularyCardEntity(
                    id = UUID.randomUUID().toString(),
                    term = cleanTerm,
                    lemma = lemma,
                    translation = translation,
                    context = context,
                    sourceTitle = sourceTitle,
                    sourcePage = sourcePage,
                    createdAt = now,
                    dueAt = now,
                ),
            )
        }
    }

    suspend fun review(card: VocabularyCardEntity, rating: ReviewRating) {
        val result = ReviewScheduler.schedule(card, rating)
        dao.update(result)
        dao.insertLog(
            ReviewLogEntity(
                id = UUID.randomUUID().toString(),
                cardId = card.id,
                rating = rating.value,
                reviewedAt = result.lastReviewedAt,
                nextDueAt = result.dueAt,
                intervalDays = result.intervalDays,
            ),
        )
    }

    suspend fun learnFromDeck(
        word: DeckWord,
        deckName: String,
        rating: ReviewRating,
    ) {
        saveCard(
            term = word.word,
            translation = word.chineseDefinition.ifBlank { word.definition },
            context = word.example,
            sourceTitle = listOf(deckName, word.theme)
                .filter { it.isNotBlank() }
                .distinct()
                .joinToString(" · "),
            sourcePage = 0,
        )
        dao.findByLemma(word.word.trim().lowercase())?.let { review(it, rating) }
    }

    /** Repairs legacy deck cards after definition formatting or translations are improved. */
    suspend fun backfillDeckChineseDefinitions(sourcePrefix: String, words: List<DeckWord>) {
        val translations = words.asSequence()
            .mapNotNull { word ->
                word.chineseDefinition.trim()
                    .takeIf { it.isNotBlank() }
                    ?.let { translation -> word.word.trim().lowercase() to translation }
            }
            .toMap()
        if (translations.isEmpty()) return

        dao.findBySourceTitlePrefix("$sourcePrefix%").forEach { card ->
            val translation = translations[card.lemma] ?: return@forEach
            if (card.translation != translation) {
                dao.update(card.copy(translation = translation))
            }
        }
    }
}

class ChatRepository(private val dao: ChatDao) {
    val sessions: Flow<List<ChatSessionEntity>> = dao.observeSessions()

    fun messages(conversationId: String): Flow<List<ChatMessageEntity>> =
        dao.observeMessages(conversationId)

    suspend fun ensureDefaultSession() {
        if (dao.getSession(ProfileStore.DEFAULT_CHAT_SESSION) == null) {
            dao.insertSession(
                ChatSessionEntity(
                    id = ProfileStore.DEFAULT_CHAT_SESSION,
                    title = "New chat",
                ),
            )
        }
    }

    suspend fun add(conversationId: String, role: String, content: String) {
        val existingSession = dao.getSession(conversationId)
            ?: ChatSessionEntity(id = conversationId, title = "New chat")
        val firstUserMessage = role == "user" && dao.messageCount(conversationId) == 0
        dao.insertSession(
            existingSession.copy(
                title = if (firstUserMessage) content.trim().take(42) else existingSession.title,
                updatedAt = System.currentTimeMillis(),
            ),
        )
        dao.insert(
            ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                role = role,
                content = content,
                conversationId = conversationId,
            ),
        )
    }

    suspend fun createSession(): ChatSessionEntity {
        val session = ChatSessionEntity(id = UUID.randomUUID().toString(), title = "New chat")
        dao.insertSession(session)
        return session
    }

    suspend fun togglePin(id: String) {
        dao.getSession(id)?.let { dao.updateSession(it.copy(pinned = !it.pinned)) }
    }

    suspend fun clear(conversationId: String) = dao.clear(conversationId)

    suspend fun pruneUnpinned(limit: Int, keepId: String) {
        dao.getUnpinnedSessions()
            .filterNot { it.id == keepId }
            .drop((limit - 1).coerceAtLeast(0))
            .forEach { session ->
                dao.clear(session.id)
                dao.deleteSession(session.id)
            }
    }
}

class MemoryRepository(private val context: Context) {
    private val memoryFile = File(context.filesDir, "memory.md")
    private val memoryLock = Any()

    private data class ExplicitFact(
        val section: String,
        val value: String,
        val field: String? = null,
        val description: String,
    )

    fun ensureDefaultMemory() {
        synchronized(memoryLock) {
            if (!memoryFile.exists()) {
                memoryFile.writeText(DEFAULT_MEMORY)
            }
        }
    }

    fun read(): String = synchronized(memoryLock) {
        memoryFile.takeIf { it.exists() }?.readText().orEmpty()
    }

    fun write(content: String) {
        synchronized(memoryLock) {
            memoryFile.writeText(content.trim() + "\n")
        }
    }

    /** Stores clear, non-sensitive, user-stated long-term facts before any network call. */
    fun recordExplicitFacts(userMessage: String): List<String> {
        val facts = extractExplicitFacts(userMessage)
        if (facts.isEmpty()) return emptyList()
        return synchronized(memoryLock) {
            ensureDefaultMemory()
            val lines = read().trimEnd().lines().toMutableList()
            val saved = facts.filter { fact ->
                if (fact.field == null) {
                    appendSectionFact(lines, fact.section, fact.value)
                } else {
                    replaceSectionField(lines, fact.section, fact.field, fact.value)
                }
            }
            if (saved.isNotEmpty()) {
                memoryFile.writeText(lines.joinToString("\n").trimEnd() + "\n")
            }
            saved.map(ExplicitFact::description)
        }
    }

    fun writeAutoSummary(summary: String) {
        val cleanSummary = summary
            .replace(AUTO_MEMORY_START, "")
            .replace(AUTO_MEMORY_END, "")
            .trim()
        if (cleanSummary.isBlank()) return
        val autoSection = """
            $AUTO_MEMORY_START
            # Tutor auto-memory
            $cleanSummary
            $AUTO_MEMORY_END
        """.trimIndent()
        synchronized(memoryLock) {
            val current = read().trim()
            val start = current.indexOf(AUTO_MEMORY_START)
            val end = current.indexOf(AUTO_MEMORY_END)
            val updated = if (start >= 0 && end >= start) {
                current.replaceRange(start, end + AUTO_MEMORY_END.length, autoSection)
            } else {
                listOf(current, autoSection).filter { it.isNotBlank() }.joinToString("\n\n")
            }
            memoryFile.writeText(updated.trim() + "\n")
        }
    }

    private fun extractExplicitFacts(message: String): List<ExplicitFact> {
        if (SENSITIVE_FACT_PATTERN.containsMatchIn(message)) return emptyList()
        return buildList {
            extractValue(message, RESEARCH_FIELD_PATTERNS)?.let { field ->
                add(
                    ExplicitFact(
                        section = "# Research",
                        field = "Field",
                        value = canonicalResearchField(field),
                        description = "research field",
                    ),
                )
            }
            extractValue(message, RESEARCH_TOPIC_PATTERNS)?.let { topic ->
                add(
                    ExplicitFact(
                        section = "# Research",
                        field = "Current topic",
                        value = topic,
                        description = "research topic",
                    ),
                )
            }
            extractValue(message, RESEARCH_METHOD_PATTERNS)?.let { methods ->
                add(
                    ExplicitFact(
                        section = "# Research",
                        field = "Common methods",
                        value = methods,
                        description = "research methods",
                    ),
                )
            }
            extractValue(message, REMEMBER_PATTERNS)?.let { fact ->
                add(
                    ExplicitFact(
                        section = "# User-stated facts",
                        value = fact,
                        description = "requested memory",
                    ),
                )
            }
            message.split(Regex("[\\n。；;]+"))
                .filterNot(::isQuestionStatement)
                .map(::normalizeFact)
                .filter(String::isNotBlank)
                .forEach { statement ->
                    when {
                        RESPONSE_PREFERENCE_PATTERN.containsMatchIn(statement) -> add(
                            ExplicitFact(
                                section = "# Response preferences",
                                value = statement,
                                description = "response preference",
                            ),
                        )

                        ENGLISH_GOAL_PATTERN.containsMatchIn(statement) -> add(
                            ExplicitFact(
                                section = "# English goals",
                                value = statement,
                                description = "English goal",
                            ),
                        )

                        LEARNING_DIFFICULTY_PATTERN.containsMatchIn(statement) ||
                            DURABLE_SELF_STATEMENT_PATTERN.containsMatchIn(statement) -> add(
                            ExplicitFact(
                                section = "# User-stated facts",
                                value = statement,
                                description = "learner fact",
                            ),
                        )
                    }
                }
        }.distinctBy { fact ->
            listOf(fact.section.lowercase(), fact.field?.lowercase().orEmpty(), fact.value.lowercase())
                .joinToString("|")
        }
    }

    private fun extractValue(message: String, patterns: List<Regex>): String? {
        for (pattern in patterns) {
            val match = pattern.find(message) ?: continue
            val candidate = normalizeFact(match.groupValues.getOrNull(1).orEmpty())
            val following = message.substring(match.range.last + 1).trimStart()
            if (
                candidate.isBlank() || isQuestionStatement(candidate) ||
                following.startsWith('?') || following.startsWith('？')
            ) {
                continue
            }
            return candidate
        }
        return null
    }

    private fun canonicalResearchField(value: String): String = when (value.lowercase()) {
        "ai", "a.i.", "artificial intelligence", "人工智能" -> "AI (Artificial Intelligence)"
        else -> value
    }

    private fun normalizeFact(value: String): String = value
        .trim()
        .trim('"', '\'', '“', '”', '‘', '’', '。', '，', ',', '；', ';', '：', ':', '！', '!', '？', '?')
        .replace(Regex("\\s+"), " ")
        .take(MAX_FACT_LENGTH)

    private fun isQuestionStatement(value: String): Boolean {
        val clean = value.trim()
        return clean.contains('?') || clean.contains('？') ||
            clean.endsWith("吗") || clean.endsWith("么") || clean.endsWith("呢") ||
            QUESTION_PREFIX_PATTERN.containsMatchIn(clean)
    }

    private fun replaceSectionField(
        lines: MutableList<String>,
        section: String,
        field: String,
        value: String,
    ): Boolean {
        val (start, end) = sectionBounds(lines, section)
        val prefix = "- $field:"
        val replacement = "$prefix $value"
        val existing = (start until end).firstOrNull { index ->
            lines[index].trim().startsWith(prefix, ignoreCase = true)
        }
        if (existing == null) {
            lines.add(start, replacement)
            return true
        }
        if (lines[existing].trim() == replacement) return false
        lines[existing] = replacement
        return true
    }

    private fun appendSectionFact(lines: MutableList<String>, section: String, value: String): Boolean {
        val (start, end) = sectionBounds(lines, section)
        val entry = "- $value"
        if ((start until end).any { index -> lines[index].trim().equals(entry, ignoreCase = true) }) {
            return false
        }
        lines.add(end, entry)
        return true
    }

    private fun sectionBounds(lines: MutableList<String>, section: String): Pair<Int, Int> {
        var sectionIndex = lines.indexOfFirst { it.trim().equals(section, ignoreCase = true) }
        if (sectionIndex < 0) {
            var insertAt = lines.indexOfFirst { it.trim().equals("# Never remember", ignoreCase = true) }
            if (insertAt < 0) insertAt = lines.indexOfFirst { it.contains(AUTO_MEMORY_START) }
            if (insertAt < 0) insertAt = lines.size
            if (insertAt > 0 && lines[insertAt - 1].isNotBlank()) {
                lines.add(insertAt, "")
                insertAt += 1
            }
            lines.add(insertAt, section)
            sectionIndex = insertAt
        }
        val sectionEnd = (sectionIndex + 1 until lines.size)
            .firstOrNull { index -> lines[index].trim().startsWith("# ") }
            ?: lines.size
        return sectionIndex + 1 to sectionEnd
    }

    companion object {
        private const val MAX_FACT_LENGTH = 180
        private const val AUTO_MEMORY_START = "<!-- LUMEN_AUTO_MEMORY_START -->"
        private const val AUTO_MEMORY_END = "<!-- LUMEN_AUTO_MEMORY_END -->"
        private val SENSITIVE_FACT_PATTERN = Regex(
            "(?:\\b(?:password|api[- ]?key|secret(?:\\s*key)?|access token|bearer token|bank card|credit card|email|phone number)\\b|密码|密钥|令牌|身份证|银行卡|手机号|电话号码|邮箱|住址)",
            RegexOption.IGNORE_CASE,
        )
        private val QUESTION_PREFIX_PATTERN = Regex(
            "^(?:我想问|我想知道|我有(?:一个)?问题|请问|能否|可以|你能|what\\b|why\\b|how\\b|can you\\b|could you\\b|i wonder\\b|i have a question\\b)",
            RegexOption.IGNORE_CASE,
        )
        private val RESEARCH_FIELD_PATTERNS = listOf(
            Regex(
                "(?:我的|我(?:的)?)?研究(?:方向|领域)\\s*(?:是|为|:|：)\\s*([^\\n。！？!?，,；;]+)",
                RegexOption.IGNORE_CASE,
            ),
            Regex(
                "\\b(?:my\\s+)?research\\s+(?:field|area|focus)\\s*(?:is|:|=)\\s*([^\\n.!?;,]+)",
                RegexOption.IGNORE_CASE,
            ),
        )
        private val RESEARCH_TOPIC_PATTERNS = listOf(
            Regex(
                "(?:我的|我(?:的)?)?研究(?:主题|课题|项目|兴趣)\\s*(?:是|为|:|：)\\s*([^\\n。！？!?，,；;]+)",
                RegexOption.IGNORE_CASE,
            ),
            Regex(
                "\\b(?:my\\s+)?research\\s+(?:topic|project|interest)\\s*(?:is|:|=)\\s*([^\\n.!?;,]+)",
                RegexOption.IGNORE_CASE,
            ),
        )
        private val RESEARCH_METHOD_PATTERNS = listOf(
            Regex(
                "(?:我(?:常用|正在使用)?(?:的)?)?(?:研究)?(?:方法|方法论)\\s*(?:是|为|:|：)\\s*([^\\n。！？!?，,；;]+)",
                RegexOption.IGNORE_CASE,
            ),
            Regex(
                "\\b(?:my\\s+)?(?:research\\s+)?methods?\\s*(?:are|is|:|=)\\s*([^\\n.!?;,]+)",
                RegexOption.IGNORE_CASE,
            ),
        )
        private val REMEMBER_PATTERNS = listOf(
            Regex(
                "(?:请(?:帮我)?记住|记住|(?:please\\s+)?remember(?:\\s+that)?)\\s*(?:[:：,，-]\\s*)?([^\\n。！？!?]+)",
                RegexOption.IGNORE_CASE,
            ),
        )
        private val RESPONSE_PREFERENCE_PATTERN = Regex(
            "^(?:我(?:喜欢|偏好|更喜欢|更倾向于)|我的?(?:回答|学习)?(?:偏好|喜好)\\s*(?:是|为|:|：)|我希望(?:你|回答|解释|纠错|反馈)|请(?:用|以|保持)|i\\s+(?:prefer|like)\\b|please\\s+(?:use|answer|respond|correct|explain)\\b)",
            RegexOption.IGNORE_CASE,
        )
        private val ENGLISH_GOAL_PATTERN = Regex(
            "^(?:我的?(?:(?:英语|学习))?目标\\s*(?:是|为|:|：)|我(?:想|希望|计划)(?:提高|学习|练习|通过|准备|掌握|改善|增强)|我正在(?:学习|准备)|(?:my\\s+)?(?:english\\s+)?goal\\s*(?:is|:|=)|i\\s+(?:want|hope|plan|am\\s+preparing|am\\s+learning)\\b)",
            RegexOption.IGNORE_CASE,
        )
        private val LEARNING_DIFFICULTY_PATTERN = Regex(
            "^(?:我(?:总是|经常|不太会|不擅长|很难|容易).*(?:英语|发音|语法|词汇|写作|阅读|听力|口语)|i\\s+(?:struggle\\s+with|often\\s+find|find\\s+.+\\s+difficult|have\\s+(?:difficulty|trouble)\\s+(?:with|in))\\b)",
            RegexOption.IGNORE_CASE,
        )
        private val DURABLE_SELF_STATEMENT_PATTERN = Regex(
            "^(?:我(?:是|在读|从事|研究|正在研究|常用|正在学习|目前(?:在|是))|我对.+感兴趣|我的(?:兴趣|背景|项目|学习计划)\\s*(?:是|为|:|：)|i(?:\\s+am|'m)\\s+(?:a|an|currently|studying|working|researching|learning|interested)\\b|i\\s+(?:work|study|use|research)\\b|my\\s+(?:background|project|research|interests?)\\s+(?:is|are|:|=))",
            RegexOption.IGNORE_CASE,
        )
        private val DEFAULT_MEMORY = """
            # About me
            - I am learning English for research and academic communication.

            # Research
            - Field:
            - Current topic:
            - Common methods:

            # English goals
            - Read papers more efficiently.
            - Write clearer academic English.
            - Build an active research vocabulary.

            # Response preferences
            - Speak mainly in English.
            - Correct important errors after answering.
            - Use clear examples and concise explanations.

            # User-stated facts
            - Explicit, non-sensitive facts you tell Lumen are saved here.

            # Never remember
            - Passwords, API keys, payment data, or private identifiers.

            $AUTO_MEMORY_START
            # Tutor auto-memory
            - Lumen will summarize durable learning preferences and goals here every four user messages.
            $AUTO_MEMORY_END
        """.trimIndent()
    }
}
