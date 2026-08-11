package com.lumen.researchenglish.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FormatColorFill
import androidx.compose.material.icons.outlined.FormatUnderlined
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lumen.researchenglish.data.ReaderAnnotation
import com.lumen.researchenglish.data.ReaderBookmark
import com.lumen.researchenglish.data.RecognizedWord
import com.lumen.researchenglish.ui.theme.Indigo
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    documentId: String,
    viewModel: AppViewModel,
    onBack: () -> Unit,
) {
    val document by viewModel.readerDocument.collectAsStateWithLifecycle()
    val bitmap by viewModel.readerBitmap.collectAsStateWithLifecycle()
    val page by viewModel.readerPage.collectAsStateWithLifecycle()
    val words by viewModel.recognizedWords.collectAsStateWithLifecycle()
    val annotations by viewModel.readerAnnotations.collectAsStateWithLifecycle()
    val bookmarks by viewModel.readerBookmarks.collectAsStateWithLifecycle()
    val selectionBusy by viewModel.selectionBusy.collectAsStateWithLifecycle()
    val translation by viewModel.translation.collectAsStateWithLifecycle()
    val tutorSelection by viewModel.readerTutorSelection.collectAsStateWithLifecycle()
    val tutorMessages by viewModel.readerTutorMessages.collectAsStateWithLifecycle()
    val tutorStreamingReply by viewModel.readerTutorStreamingReply.collectAsStateWithLifecycle()
    val tutorStreaming by viewModel.readerTutorStreaming.collectAsStateWithLifecycle()
    val tutorError by viewModel.readerTutorError.collectAsStateWithLifecycle()
    val readerMode by viewModel.readerMode.collectAsStateWithLifecycle()
    val speechLoadingId by viewModel.speechLoadingId.collectAsStateWithLifecycle()
    val speakingId by viewModel.speakingId.collectAsStateWithLifecycle()
    val audiobookActive by viewModel.audiobookActive.collectAsStateWithLifecycle()

    var selectedWords by remember(page) { mutableStateOf<List<RecognizedWord>>(emptyList()) }
    var selectedSource by remember(page) { mutableStateOf("") }
    var lineMode by remember(page) { mutableStateOf(false) }
    var translationExpanded by remember(page) { mutableStateOf(false) }
    var scale by remember(page) { mutableFloatStateOf(1f) }
    var offsetX by remember(page) { mutableFloatStateOf(0f) }
    var offsetY by remember(page) { mutableFloatStateOf(0f) }
    var controlsVisible by remember(page) { mutableStateOf(false) }
    var showColorMenu by remember { mutableStateOf(false) }
    var showBookmarkMenu by remember { mutableStateOf(false) }
    var showTutorSheet by remember(page) { mutableStateOf(false) }
    val tutorSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val context = LocalContext.current
    val view = LocalView.current
    DisposableEffect(context, view) {
        val activity = context.findActivity()
        val controller = activity?.let { WindowCompat.getInsetsController(it.window, view) }
        controller?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            viewModel.closeReaderTutor()
            viewModel.stopSpeech()
        }
    }

    val pageBackground = when (readerMode) {
        "paper" -> Color(0xFFDED5BF)
        "night" -> Color(0xFF080A0D)
        else -> Color(0xFF202124)
    }
    val pageColorFilter = remember(readerMode) { readerColorFilter(readerMode) }
    val speechRequestId = remember(documentId, page, selectedSource) {
        "reader-$documentId-$page-${selectedSource.hashCode()}"
    }
    val selectionSpeaking = speechLoadingId == speechRequestId || speakingId == speechRequestId

    fun closeSelection() {
        showTutorSheet = false
        viewModel.closeReaderTutor()
        selectedWords = emptyList()
        selectedSource = ""
        lineMode = false
        translationExpanded = false
    }

    LaunchedEffect(documentId) {
        viewModel.openDocument(documentId)
    }
    LaunchedEffect(document?.id, page) {
        if (document != null) viewModel.preparePositionSelection()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBackground)
            .pointerInput(page, bitmap, selectedWords.isNotEmpty()) {
                val currentBitmap = bitmap
                val baseScale = if (currentBitmap == null) 1f else max(
                    size.width / currentBitmap.width.toFloat().coerceAtLeast(1f),
                    size.height / currentBitmap.height.toFloat().coerceAtLeast(1f),
                )
                val baseWidth = (currentBitmap?.width ?: size.width) * baseScale
                val baseHeight = (currentBitmap?.height ?: size.height) * baseScale

                awaitEachGesture {
                    val firstDown = awaitFirstDown(requireUnconsumed = false)
                    val startedAt = firstDown.uptimeMillis
                    var totalHorizontal = 0f
                    var totalVertical = 0f
                    var zoomGesture = false
                    var lastEventTime = startedAt
                    do {
                        val event = awaitPointerEvent()
                        lastEventTime = event.changes.maxOfOrNull { it.uptimeMillis } ?: lastEventTime
                        val zoom = event.calculateZoom()
                        val pan = event.calculatePan()
                        if (event.changes.count { it.pressed } > 1 || abs(zoom - 1f) > 0.01f) {
                            zoomGesture = true
                        }

                        if (zoomGesture || scale > 1.01f) {
                            val nextScale = (scale * zoom).coerceIn(1f, 5f)
                            val maxOffsetX = ((baseWidth * nextScale - size.width) / 2f)
                                .coerceAtLeast(0f)
                            val maxOffsetY = ((baseHeight * nextScale - size.height) / 2f)
                                .coerceAtLeast(0f)
                            scale = nextScale
                            if (nextScale > 1.01f) {
                                offsetX = (offsetX + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                                offsetY = (offsetY + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                            } else {
                                offsetX = 0f
                                offsetY = 0f
                            }
                        } else {
                            totalHorizontal += pan.x
                            totalVertical += pan.y
                        }

                        event.changes.forEach { change ->
                            if (change.position != change.previousPosition) change.consume()
                        }
                    } while (event.changes.any { it.pressed })

                    val swipe = !zoomGesture &&
                        scale <= 1.01f &&
                        abs(totalHorizontal) > 72.dp.toPx() &&
                        abs(totalHorizontal) > abs(totalVertical) * 1.25f
                    if (swipe) {
                        closeSelection()
                        controlsVisible = false
                        viewModel.changePage(if (totalHorizontal < 0f) 1 else -1)
                    } else {
                        val shortTap = !zoomGesture &&
                            abs(totalHorizontal) < 12.dp.toPx() &&
                            abs(totalVertical) < 12.dp.toPx() &&
                            lastEventTime - startedAt < 350L
                        if (shortTap && selectedWords.isEmpty()) {
                            controlsVisible = !controlsVisible
                        }
                    }
                }
            },
    ) {
        val currentBitmap = bitmap
        if (currentBitmap != null) {
            Crossfade(
                targetState = currentBitmap,
                animationSpec = tween(durationMillis = 160),
                label = "reader-page-crossfade",
            ) { shownBitmap ->
                Image(
                    bitmap = shownBitmap.asImageBitmap(),
                    contentDescription = "PDF page ${page + 1}",
                    contentScale = ContentScale.Crop,
                    colorFilter = pageColorFilter,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY,
                        ),
                )
            }

            AnnotationLayer(
                bitmapWidth = currentBitmap.width,
                bitmapHeight = currentBitmap.height,
                containerWidthPx = constraints.maxWidth.toFloat(),
                containerHeightPx = constraints.maxHeight.toFloat(),
                scale = scale,
                offsetX = offsetX,
                offsetY = offsetY,
                annotations = annotations,
            )
        }

        if (currentBitmap != null && words.isNotEmpty()) {
            PositionAwareSelectionLayer(
                bitmapWidth = currentBitmap.width,
                bitmapHeight = currentBitmap.height,
                containerWidthPx = constraints.maxWidth.toFloat(),
                containerHeightPx = constraints.maxHeight.toFloat(),
                scale = scale,
                offsetX = offsetX,
                offsetY = offsetY,
                words = words,
                selectedWords = selectedWords,
                selectedSource = selectedSource,
                lineMode = lineMode,
                translation = translation,
                translationExpanded = translationExpanded,
                selectionSpeaking = selectionSpeaking,
                onWordLongPress = { word ->
                    showTutorSheet = false
                    viewModel.closeReaderTutor()
                    selectedWords = listOf(word)
                    selectedSource = word.text
                    lineMode = false
                    translationExpanded = false
                    controlsVisible = false
                },
                onWordTap = { word ->
                    if (selectedWords.isNotEmpty()) {
                        showTutorSheet = false
                        viewModel.closeReaderTutor()
                        val firstIndex = words.indexOf(selectedWords.first()).coerceAtLeast(0)
                        val tappedIndex = words.indexOf(word).coerceAtLeast(0)
                        val from = min(firstIndex, tappedIndex)
                        val to = max(firstIndex, tappedIndex)
                        selectedWords = words.subList(from, to + 1)
                        selectedSource = selectedWords.joinToString(" ") { it.text }
                        lineMode = false
                        translationExpanded = false
                    }
                },
                onToggleLine = {
                    val anchor = selectedWords.firstOrNull() ?: return@PositionAwareSelectionLayer
                    showTutorSheet = false
                    viewModel.closeReaderTutor()
                    lineMode = !lineMode
                    selectedWords = if (lineMode) {
                        words.filter {
                            it.lineText == anchor.lineText && abs(it.top - anchor.top) < 0.035f
                        }.ifEmpty { listOf(anchor) }
                    } else {
                        listOf(anchor)
                    }
                    selectedSource = selectedWords.joinToString(" ") { it.text }
                    translationExpanded = false
                },
                onSelectionEdgeDrag = { movingStart, word ->
                    val fixedWord = if (movingStart) selectedWords.lastOrNull()
                    else selectedWords.firstOrNull()
                    if (fixedWord != null) {
                        val movingIndex = words.indexOf(word).coerceAtLeast(0)
                        val fixedIndex = words.indexOf(fixedWord).coerceAtLeast(0)
                        val from = min(movingIndex, fixedIndex)
                        val to = max(movingIndex, fixedIndex)
                        selectedWords = words.subList(from, to + 1)
                        selectedSource = selectedWords.joinToString(" ") { it.text }
                        lineMode = false
                        translationExpanded = false
                        showTutorSheet = false
                        viewModel.closeReaderTutor()
                    }
                },
                onHighlight = { color ->
                    viewModel.addReaderAnnotation("highlight", color, selectedSource, selectedWords)
                },
                onRemoveHighlight = {
                    viewModel.removeReaderAnnotations("highlight", selectedWords)
                },
                onUnderline = { color ->
                    viewModel.addReaderAnnotation("underline", color, selectedSource, selectedWords)
                },
                onRemoveUnderline = {
                    viewModel.removeReaderAnnotations("underline", selectedWords)
                },
                onTranslate = {
                    translationExpanded = true
                    viewModel.translateSelection(selectedSource)
                },
                onAskTutor = {
                    showTutorSheet = true
                    if (
                        tutorSelection != selectedSource ||
                        (tutorMessages.isEmpty() && !tutorStreaming)
                    ) {
                        viewModel.askReaderTutor(selectedSource)
                    }
                },
                onSpeak = { viewModel.speak(selectedSource, speechRequestId) },
                onSave = {
                    viewModel.saveSelection(selectedSource, tutorMarkdownPlainText(translation))
                },
                onClose = ::closeSelection,
            )
        }

        if (controlsVisible) {
            ReaderTopBar(
                title = document?.title.orEmpty(),
                page = page,
                pageCount = document?.pageCount ?: 0,
                readerMode = readerMode,
                showColorMenu = showColorMenu,
                bookmarks = bookmarks,
                currentPageBookmarked = bookmarks.any { it.page == page },
                showBookmarkMenu = showBookmarkMenu,
                onBack = onBack,
                onToggleColorMenu = { showColorMenu = !showColorMenu },
                onDismissColorMenu = { showColorMenu = false },
                onToggleBookmarkMenu = { showBookmarkMenu = !showBookmarkMenu },
                onDismissBookmarkMenu = { showBookmarkMenu = false },
                onToggleCurrentBookmark = viewModel::toggleCurrentBookmark,
                onOpenBookmark = { bookmarkPage ->
                    showBookmarkMenu = false
                    controlsVisible = false
                    closeSelection()
                    viewModel.openBookmark(bookmarkPage)
                },
                onReaderMode = {
                    viewModel.setReaderMode(it)
                    showColorMenu = false
                },
                audiobookActive = audiobookActive,
                onToggleAudiobook = viewModel::toggleAudiobook,
                modifier = Modifier.align(Alignment.TopCenter),
            )
            ReaderBottomBar(
                page = page,
                pageCount = document?.pageCount ?: 0,
                onPrevious = { viewModel.changePage(-1) },
                onNext = { viewModel.changePage(1) },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        if (selectionBusy) {
            Surface(
                color = Color.Black.copy(alpha = 0.68f),
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 14.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Preparing selectable text…",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }

    if (showTutorSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTutorSheet = false },
            sheetState = tutorSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            ReaderTutorConversation(
                selection = tutorSelection.ifBlank { selectedSource },
                messages = tutorMessages,
                streamingReply = tutorStreamingReply,
                streaming = tutorStreaming,
                error = tutorError,
                onSend = viewModel::sendReaderTutorFollowUp,
            )
        }
    }
}

@Composable
private fun ReaderTopBar(
    title: String,
    page: Int,
    pageCount: Int,
    readerMode: String,
    showColorMenu: Boolean,
    bookmarks: List<ReaderBookmark>,
    currentPageBookmarked: Boolean,
    showBookmarkMenu: Boolean,
    onBack: () -> Unit,
    onToggleColorMenu: () -> Unit,
    onDismissColorMenu: () -> Unit,
    onToggleBookmarkMenu: () -> Unit,
    onDismissBookmarkMenu: () -> Unit,
    onToggleCurrentBookmark: () -> Unit,
    onOpenBookmark: (Int) -> Unit,
    onReaderMode: (String) -> Unit,
    audiobookActive: Boolean,
    onToggleAudiobook: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.66f))
            .statusBarsPadding()
            .padding(horizontal = 6.dp, vertical = 6.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = Color.White)
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                "${page + 1} / $pageCount · swipe pages · pinch to zoom · long-press text",
                color = Color.White.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Box {
            IconButton(onClick = onToggleBookmarkMenu) {
                Icon(
                    if (currentPageBookmarked) Icons.Outlined.Bookmark
                    else Icons.Outlined.BookmarkBorder,
                    if (currentPageBookmarked) "Manage page bookmark" else "Bookmark this page",
                    tint = if (currentPageBookmarked) Color(0xFFFFD56A) else Color.White,
                )
            }
            DropdownMenu(
                expanded = showBookmarkMenu,
                onDismissRequest = onDismissBookmarkMenu,
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            if (currentPageBookmarked) "Remove bookmark from page ${page + 1}"
                            else "Bookmark page ${page + 1}",
                        )
                    },
                    leadingIcon = {
                        Icon(
                            if (currentPageBookmarked) Icons.Outlined.Bookmark
                            else Icons.Outlined.BookmarkBorder,
                            contentDescription = null,
                        )
                    },
                    onClick = onToggleCurrentBookmark,
                )
                HorizontalDivider()
                if (bookmarks.isEmpty()) {
                    Text(
                        "No bookmarked pages yet",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                } else {
                    Text(
                        "Bookmarked pages",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    bookmarks.forEach { bookmark ->
                        DropdownMenuItem(
                            text = { Text("Page ${bookmark.page + 1}") },
                            leadingIcon = {
                                if (bookmark.page == page) {
                                    Icon(Icons.Outlined.Check, contentDescription = null)
                                } else {
                                    Icon(Icons.Outlined.Bookmarks, contentDescription = null)
                                }
                            },
                            onClick = { onOpenBookmark(bookmark.page) },
                        )
                    }
                }
            }
        }
        IconButton(onClick = onToggleAudiobook) {
            Icon(
                if (audiobookActive) Icons.Outlined.StopCircle else Icons.Outlined.Headphones,
                if (audiobookActive) "Stop audiobook" else "Start audiobook",
                tint = if (audiobookActive) Color(0xFF69D7FF) else Color.White,
            )
        }
        Box {
            IconButton(onClick = onToggleColorMenu) {
                Icon(Icons.Outlined.Palette, "Reading colors", tint = Color.White)
            }
            DropdownMenu(expanded = showColorMenu, onDismissRequest = onDismissColorMenu) {
                listOf(
                    "light" to "Bright",
                    "paper" to "Warm paper",
                    "night" to "Night",
                ).forEach { (mode, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        leadingIcon = {
                            if (readerMode == mode) Icon(Icons.Outlined.Check, null)
                            else Spacer(Modifier.size(24.dp))
                        },
                        onClick = { onReaderMode(mode) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ReaderBottomBar(
    page: Int,
    pageCount: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .navigationBarsPadding()
            .padding(bottom = 8.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.66f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        IconButton(onClick = onPrevious, enabled = page > 0) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowBack,
                "Previous page",
                tint = if (page > 0) Color.White else Color.White.copy(alpha = 0.3f),
            )
        }
        Text("${page + 1} / $pageCount", color = Color.White)
        IconButton(onClick = onNext, enabled = page + 1 < pageCount) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForward,
                "Next page",
                tint = if (page + 1 < pageCount) Color.White else Color.White.copy(alpha = 0.3f),
            )
        }
    }
}

@Composable
private fun AnnotationLayer(
    bitmapWidth: Int,
    bitmapHeight: Int,
    containerWidthPx: Float,
    containerHeightPx: Float,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    annotations: List<ReaderAnnotation>,
) {
    val density = LocalDensity.current
    val metrics = pageMetrics(
        bitmapWidth,
        bitmapHeight,
        containerWidthPx,
        containerHeightPx,
        scale,
        offsetX,
        offsetY,
    )
    Box(Modifier.fillMaxSize()) {
        annotations.forEach { annotation ->
            annotation.rects.forEach { rect ->
                val left = metrics.x(metrics.pageLeft + rect.left * metrics.shownWidth)
                val top = metrics.y(metrics.pageTop + rect.top * metrics.shownHeight)
                val right = metrics.x(metrics.pageLeft + rect.right * metrics.shownWidth)
                val bottom = metrics.y(metrics.pageTop + rect.bottom * metrics.shownHeight)
                val underline = annotation.style == "underline"
                val lineHeightPx = with(density) { 2.5.dp.toPx() }
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                left.roundToInt(),
                                (if (underline) bottom - lineHeightPx else top).roundToInt(),
                            )
                        }
                        .size(
                            width = with(density) { (right - left).coerceAtLeast(1f).toDp() },
                            height = with(density) {
                                (if (underline) lineHeightPx else bottom - top)
                                    .coerceAtLeast(1f)
                                    .toDp()
                            },
                        )
                        .background(
                            annotationColor(annotation.color).copy(
                                alpha = if (underline) 0.92f else 0.4f,
                            ),
                        ),
                )
            }
        }
    }
}

@Composable
private fun PositionAwareSelectionLayer(
    bitmapWidth: Int,
    bitmapHeight: Int,
    containerWidthPx: Float,
    containerHeightPx: Float,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    words: List<RecognizedWord>,
    selectedWords: List<RecognizedWord>,
    selectedSource: String,
    lineMode: Boolean,
    translation: String,
    translationExpanded: Boolean,
    selectionSpeaking: Boolean,
    onWordLongPress: (RecognizedWord) -> Unit,
    onWordTap: (RecognizedWord) -> Unit,
    onToggleLine: () -> Unit,
    onSelectionEdgeDrag: (Boolean, RecognizedWord) -> Unit,
    onHighlight: (String) -> Unit,
    onRemoveHighlight: () -> Unit,
    onUnderline: (String) -> Unit,
    onRemoveUnderline: () -> Unit,
    onTranslate: () -> Unit,
    onAskTutor: () -> Unit,
    onSpeak: () -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
) {
    val density = LocalDensity.current
    val metrics = pageMetrics(
        bitmapWidth,
        bitmapHeight,
        containerWidthPx,
        containerHeightPx,
        scale,
        offsetX,
        offsetY,
    )
    val touchPadding = with(density) { 4.dp.toPx() } * scale

    fun closestWord(x: Float, y: Float): RecognizedWord? = words.minByOrNull { word ->
        val centerX = metrics.x(
            metrics.pageLeft + ((word.left + word.right) / 2f) * metrics.shownWidth,
        )
        val centerY = metrics.y(
            metrics.pageTop + ((word.top + word.bottom) / 2f) * metrics.shownHeight,
        )
        val dx = centerX - x
        val dy = centerY - y
        dx * dx + dy * dy
    }

    Box(Modifier.fillMaxSize()) {
        if (selectedWords.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(selectedWords) {
                        detectTapGestures(onTap = { onClose() })
                    },
            )
        }
        words.forEach { word ->
            val left = metrics.x(metrics.pageLeft + word.left * metrics.shownWidth) - touchPadding
            val top = metrics.y(metrics.pageTop + word.top * metrics.shownHeight) - touchPadding
            val right = metrics.x(metrics.pageLeft + word.right * metrics.shownWidth) + touchPadding
            val bottom = metrics.y(metrics.pageTop + word.bottom * metrics.shownHeight) + touchPadding
            val selected = word in selectedWords
            Box(
                modifier = Modifier
                    .offset { IntOffset(left.roundToInt(), top.roundToInt()) }
                    .size(
                        width = with(density) { (right - left).coerceAtLeast(1f).toDp() },
                        height = with(density) { (bottom - top).coerceAtLeast(1f).toDp() },
                    )
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (selected) Indigo.copy(alpha = 0.24f)
                        else Color.Transparent,
                    )
                    .then(
                        if (selected) {
                            Modifier.border(
                                width = 0.75.dp,
                                color = Indigo.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(4.dp),
                            )
                        } else {
                            Modifier
                        },
                    )
                    .pointerInput(word, selectedWords) {
                        detectTapGestures(
                            onTap = { onWordTap(word) },
                            onLongPress = { onWordLongPress(word) },
                        )
                    },
            )
        }

        selectedWords.firstOrNull()?.let { first ->
            SelectionHandle(
                x = metrics.x(metrics.pageLeft + first.left * metrics.shownWidth),
                y = metrics.y(metrics.pageTop + first.bottom * metrics.shownHeight),
                onDrag = { x, y -> closestWord(x, y)?.let { onSelectionEdgeDrag(true, it) } },
            )
        }
        selectedWords.lastOrNull()?.let { last ->
            SelectionHandle(
                x = metrics.x(metrics.pageLeft + last.right * metrics.shownWidth),
                y = metrics.y(metrics.pageTop + last.bottom * metrics.shownHeight),
                onDrag = { x, y -> closestWord(x, y)?.let { onSelectionEdgeDrag(false, it) } },
            )
        }

        if (selectedWords.isNotEmpty() && selectedSource.isNotBlank()) {
            val panelWidthPx = min(
                with(density) { 360.dp.toPx() },
                containerWidthPx - with(density) { 20.dp.toPx() },
            ).coerceAtLeast(with(density) { 250.dp.toPx() })
            val estimatedPanelHeight = with(density) {
                (if (translationExpanded) 440.dp else 238.dp).toPx()
            }
            val horizontalMargin = with(density) { 10.dp.toPx() }
            val verticalMargin = with(density) { 10.dp.toPx() }
            val selectionLeft = selectedWords.minOf { word ->
                metrics.x(metrics.pageLeft + word.left * metrics.shownWidth)
            }
            val selectionRight = selectedWords.maxOf { word ->
                metrics.x(metrics.pageLeft + word.right * metrics.shownWidth)
            }
            val selectionTop = selectedWords.minOf { word ->
                metrics.y(metrics.pageTop + word.top * metrics.shownHeight)
            }
            val selectionBottom = selectedWords.maxOf { word ->
                metrics.y(metrics.pageTop + word.bottom * metrics.shownHeight)
            }
            val anchorCenter = (selectionLeft + selectionRight) / 2f
            val panelX = (anchorCenter - panelWidthPx / 2f).coerceIn(
                horizontalMargin,
                (containerWidthPx - panelWidthPx - horizontalMargin)
                    .coerceAtLeast(horizontalMargin),
            )
            val desiredY = if (selectionTop > estimatedPanelHeight + verticalMargin * 2f) {
                selectionTop - estimatedPanelHeight - verticalMargin
            } else {
                selectionBottom + verticalMargin
            }
            val panelY = desiredY.coerceIn(
                verticalMargin,
                (containerHeightPx - estimatedPanelHeight - verticalMargin)
                    .coerceAtLeast(verticalMargin),
            )

            SelectionActionCard(
                source = selectedSource,
                lineMode = lineMode,
                translation = translation,
                translationExpanded = translationExpanded,
                selectionSpeaking = selectionSpeaking,
                onToggleLine = onToggleLine,
                onHighlight = onHighlight,
                onRemoveHighlight = onRemoveHighlight,
                onUnderline = onUnderline,
                onRemoveUnderline = onRemoveUnderline,
                onTranslate = onTranslate,
                onAskTutor = onAskTutor,
                onSpeak = onSpeak,
                onSave = onSave,
                onClose = onClose,
                modifier = Modifier
                    .offset { IntOffset(panelX.roundToInt(), panelY.roundToInt()) }
                    .width(with(density) { panelWidthPx.toDp() }),
            )
        }
    }
}

@Composable
private fun SelectionActionCard(
    source: String,
    lineMode: Boolean,
    translation: String,
    translationExpanded: Boolean,
    selectionSpeaking: Boolean,
    onToggleLine: () -> Unit,
    onHighlight: (String) -> Unit,
    onRemoveHighlight: () -> Unit,
    onUnderline: (String) -> Unit,
    onRemoveUnderline: () -> Unit,
    onTranslate: () -> Unit,
    onAskTutor: () -> Unit,
    onSpeak: () -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures(onTap = {})
        },
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(width = 4.dp, height = 34.dp)
                        .clip(CircleShape)
                        .background(Indigo),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    source,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = if (lineMode) 3 else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f),
                )
                Surface(
                    color = Indigo.copy(alpha = 0.1f),
                    shape = CircleShape,
                    modifier = Modifier.clickable(onClick = onToggleLine),
                ) {
                    Text(
                        if (lineMode) "Word" else "Line",
                        color = Indigo,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                    )
                }
                Spacer(Modifier.width(2.dp))
                IconButton(onClick = onClose, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Outlined.Close, "Close selection")
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 5.dp),
            ) {
                SelectionTool(
                    Icons.Outlined.School,
                    "Ask Tutor",
                    onAskTutor,
                    emphasized = true,
                    modifier = Modifier.weight(1f),
                )
                SelectionTool(
                    Icons.Outlined.Translate,
                    "Translate",
                    onTranslate,
                    modifier = Modifier.weight(1f),
                )
                SelectionTool(
                    Icons.AutoMirrored.Outlined.VolumeUp,
                    if (selectionSpeaking) "Stop" else "Read",
                    onSpeak,
                    active = selectionSpeaking,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                AnnotationStyleTool(
                    icon = Icons.Outlined.FormatColorFill,
                    label = "Highlight",
                    removeLabel = "Remove highlight",
                    onColor = onHighlight,
                    onRemove = onRemoveHighlight,
                    modifier = Modifier.weight(1f),
                )
                AnnotationStyleTool(
                    icon = Icons.Outlined.FormatUnderlined,
                    label = "Underline",
                    removeLabel = "Remove underline",
                    onColor = onUnderline,
                    onRemove = onRemoveUnderline,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.weight(1f))
            }

            if (translationExpanded) {
                Surface(
                    color = Indigo.copy(alpha = 0.07f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                ) {
                    Text(
                        text = renderTutorMarkdown(
                            markdown = translation.ifBlank { "Translating…" },
                            accentColor = Indigo,
                            codeBackground = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .heightIn(max = 220.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 13.dp, vertical = 11.dp),
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    FilledTonalButton(onClick = onSave, enabled = translation.isNotBlank()) {
                        Icon(
                            Icons.Outlined.AddCircleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text("Save word")
                    }
                }
            }
        }
    }
}

@Composable
private fun AnnotationStyleTool(
    icon: ImageVector,
    label: String,
    removeLabel: String,
    onColor: (String) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        SelectionTool(
            icon,
            label,
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            Text(
                "Choose a color",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
            ) {
                AnnotationColors.forEach { option ->
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(option.color)
                            .clickable {
                                onColor(option.key)
                                expanded = false
                            },
                    )
                }
            }
            HorizontalDivider(Modifier.padding(top = 7.dp))
            DropdownMenuItem(
                text = { Text(removeLabel) },
                leadingIcon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
                onClick = {
                    onRemove()
                    expanded = false
                },
            )
        }
    }
}

@Composable
private fun SelectionTool(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    emphasized: Boolean = false,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(
                when {
                    active -> Indigo.copy(alpha = 0.16f)
                    emphasized -> Indigo.copy(alpha = 0.1f)
                    else -> Color.Transparent
                },
            )
            .padding(horizontal = 9.dp, vertical = 7.dp),
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (active || emphasized) Indigo else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(21.dp),
        )
        Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

@Composable
private fun SelectionHandle(
    x: Float,
    y: Float,
    onDrag: (Float, Float) -> Unit,
) {
    val density = LocalDensity.current
    val handleSize = 12.dp
    val radiusPx = with(density) { handleSize.toPx() / 2f }
    var dragX by remember(x) { mutableFloatStateOf(x) }
    var dragY by remember(y) { mutableFloatStateOf(y) }
    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (x - radiusPx).roundToInt(),
                    (y - radiusPx).roundToInt(),
                )
            }
            .size(handleSize)
            .border(2.dp, Color.White, CircleShape)
            .background(Indigo, CircleShape)
            .pointerInput(x, y) {
                detectDragGestures(
                    onDragStart = {
                        dragX = x
                        dragY = y
                    },
                    onDragEnd = { onDrag(dragX, dragY) },
                    onDrag = { change, amount ->
                        change.consume()
                        dragX += amount.x
                        dragY += amount.y
                    },
                )
            },
    )
}

@Composable
private fun ReaderTutorConversation(
    selection: String,
    messages: List<ReaderTutorMessage>,
    streamingReply: String,
    streaming: Boolean,
    error: String?,
    onSend: (String) -> Unit,
) {
    var draft by remember(selection) { mutableStateOf("") }
    val listState = rememberLazyListState()
    val visibleMessageCount = messages.size + if (streaming) 1 else 0

    LaunchedEffect(visibleMessageCount, streamingReply.length) {
        if (visibleMessageCount > 0) {
            listState.scrollToItem(visibleMessageCount - 1)
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.84f)
            .imePadding()
            .padding(start = 18.dp, end = 18.dp, bottom = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TutorAvatar(size = 44.dp)
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Tutor note",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Ask follow-up questions without leaving the reader",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Surface(
            color = Indigo.copy(alpha = 0.08f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
                Text(
                    "Selected passage",
                    color = Indigo,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    selection,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .heightIn(min = 120.dp),
        ) {
            items(messages, key = ReaderTutorMessage::id) { message ->
                ReaderTutorBubble(message.role, message.content)
            }
            if (streaming) {
                item(key = "streaming-reader-tutor") {
                    ReaderTutorBubble(
                        role = "assistant",
                        content = streamingReply.ifBlank { "Tutor is reading the passage…" } +
                            if (streamingReply.isBlank()) "" else " ▍",
                    )
                }
            }
            error?.let { message ->
                item(key = "reader-tutor-error") {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            enabled = !streaming,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Ask about meaning, grammar, or research usage…") },
            trailingIcon = {
                IconButton(
                    enabled = draft.isNotBlank() && !streaming,
                    onClick = {
                        val message = draft.trim()
                        draft = ""
                        onSend(message)
                    },
                ) {
                    if (streaming) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "Send follow-up")
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(
                onSend = {
                    val message = draft.trim()
                    if (message.isNotBlank() && !streaming) {
                        draft = ""
                        onSend(message)
                    }
                },
            ),
            maxLines = 4,
            shape = RoundedCornerShape(18.dp),
        )
    }
}

@Composable
private fun ReaderTutorBubble(role: String, content: String) {
    val isUser = role == "user"
    Row(
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (!isUser) {
            TutorAvatar(size = 30.dp)
            Spacer(Modifier.width(7.dp))
        }
        Surface(
            color = if (isUser) Indigo else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
            shape = RoundedCornerShape(
                topStart = if (isUser) 18.dp else 5.dp,
                topEnd = if (isUser) 5.dp else 18.dp,
                bottomStart = 18.dp,
                bottomEnd = 18.dp,
            ),
            modifier = Modifier.fillMaxWidth(0.86f),
        ) {
            Text(
                text = if (isUser) {
                    androidx.compose.ui.text.AnnotatedString(content)
                } else {
                    renderTutorMarkdown(
                        markdown = content,
                        accentColor = Indigo,
                        codeBackground = MaterialTheme.colorScheme.surface,
                    )
                },
                color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            )
        }
    }
}

private data class AnnotationColorOption(
    val key: String,
    val color: Color,
)

private val AnnotationColors = listOf(
    AnnotationColorOption("yellow", Color(0xFFFFD84D)),
    AnnotationColorOption("green", Color(0xFF63D488)),
    AnnotationColorOption("blue", Color(0xFF5B8FF9)),
    AnnotationColorOption("pink", Color(0xFFFF7FA5)),
    AnnotationColorOption("purple", Color(0xFF9B78F6)),
)

private fun annotationColor(key: String): Color =
    AnnotationColors.firstOrNull { it.key == key }?.color ?: AnnotationColors.first().color

private data class PageMetrics(
    val shownWidth: Float,
    val shownHeight: Float,
    val pageLeft: Float,
    val pageTop: Float,
    val centerX: Float,
    val centerY: Float,
    val scale: Float,
    val offsetX: Float,
    val offsetY: Float,
) {
    fun x(value: Float): Float = centerX + (value - centerX) * scale + offsetX
    fun y(value: Float): Float = centerY + (value - centerY) * scale + offsetY
}

private fun pageMetrics(
    bitmapWidth: Int,
    bitmapHeight: Int,
    containerWidthPx: Float,
    containerHeightPx: Float,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
): PageMetrics {
    val fitScale = max(
        containerWidthPx / bitmapWidth.toFloat().coerceAtLeast(1f),
        containerHeightPx / bitmapHeight.toFloat().coerceAtLeast(1f),
    )
    val shownWidth = bitmapWidth * fitScale
    val shownHeight = bitmapHeight * fitScale
    return PageMetrics(
        shownWidth = shownWidth,
        shownHeight = shownHeight,
        pageLeft = (containerWidthPx - shownWidth) / 2f,
        pageTop = (containerHeightPx - shownHeight) / 2f,
        centerX = containerWidthPx / 2f,
        centerY = containerHeightPx / 2f,
        scale = scale,
        offsetX = offsetX,
        offsetY = offsetY,
    )
}

private fun readerColorFilter(mode: String): ColorFilter? {
    val values = when (mode) {
        "paper" -> floatArrayOf(
            1.00f, 0f, 0f, 0f, 0f,
            0f, 0.96f, 0f, 0f, 0f,
            0f, 0f, 0.82f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        )
        "night" -> floatArrayOf(
            -1f, 0f, 0f, 0f, 235f,
            0f, -1f, 0f, 0f, 235f,
            0f, 0f, -1f, 0f, 235f,
            0f, 0f, 0f, 1f, 0f,
        )
        else -> return null
    }
    return ColorFilter.colorMatrix(ColorMatrix(values))
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
