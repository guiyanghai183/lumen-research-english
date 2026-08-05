package com.lumen.researchenglish.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FormatColorFill
import androidx.compose.material.icons.outlined.FormatUnderlined
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lumen.researchenglish.data.ReaderAnnotation
import com.lumen.researchenglish.data.RecognizedWord
import com.lumen.researchenglish.ui.theme.Indigo
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

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
    val selectionBusy by viewModel.selectionBusy.collectAsStateWithLifecycle()
    val translation by viewModel.translation.collectAsStateWithLifecycle()
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
            Image(
                bitmap = currentBitmap.asImageBitmap(),
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
                    selectedWords = listOf(word)
                    selectedSource = word.text
                    lineMode = false
                    translationExpanded = false
                    controlsVisible = false
                },
                onWordTap = { word ->
                    if (selectedWords.isNotEmpty()) {
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
                onSpeak = { viewModel.speak(selectedSource, speechRequestId) },
                onSave = { viewModel.saveSelection(selectedSource, translation) },
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
                onBack = onBack,
                onToggleColorMenu = { showColorMenu = !showColorMenu },
                onDismissColorMenu = { showColorMenu = false },
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
}

@Composable
private fun ReaderTopBar(
    title: String,
    page: Int,
    pageCount: Int,
    readerMode: String,
    showColorMenu: Boolean,
    onBack: () -> Unit,
    onToggleColorMenu: () -> Unit,
    onDismissColorMenu: () -> Unit,
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
    onHighlight: (String) -> Unit,
    onRemoveHighlight: () -> Unit,
    onUnderline: (String) -> Unit,
    onRemoveUnderline: () -> Unit,
    onTranslate: () -> Unit,
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
    val touchPadding = with(density) { 3.dp.toPx() } * scale

    Box(Modifier.fillMaxSize()) {
        words.forEach { word ->
            val left = metrics.x(metrics.pageLeft + word.left * metrics.shownWidth) - touchPadding
            val top = metrics.y(metrics.pageTop + word.top * metrics.shownHeight) - touchPadding
            val right = metrics.x(metrics.pageLeft + word.right * metrics.shownWidth) + touchPadding
            val bottom = metrics.y(metrics.pageTop + word.bottom * metrics.shownHeight) + touchPadding
            Box(
                modifier = Modifier
                    .offset { IntOffset(left.roundToInt(), top.roundToInt()) }
                    .size(
                        width = with(density) { (right - left).coerceAtLeast(1f).toDp() },
                        height = with(density) { (bottom - top).coerceAtLeast(1f).toDp() },
                    )
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        if (word in selectedWords) Indigo.copy(alpha = 0.28f)
                        else Color.Transparent,
                    )
                    .pointerInput(word, selectedWords) {
                        detectTapGestures(
                            onTap = { onWordTap(word) },
                            onLongPress = { onWordLongPress(word) },
                        )
                    },
            )
        }

        if (selectedWords.isNotEmpty() && selectedSource.isNotBlank()) {
            val panelWidthPx = min(
                with(density) { 360.dp.toPx() },
                containerWidthPx - with(density) { 20.dp.toPx() },
            ).coerceAtLeast(with(density) { 250.dp.toPx() })
            val estimatedPanelHeight = with(density) {
                (if (translationExpanded) 230.dp else 132.dp).toPx()
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
    onSpeak: () -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F8FC)),
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    source,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = if (lineMode) 3 else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                )
                TextButton(onClick = onToggleLine) {
                    Text(if (lineMode) "Word" else "Line")
                }
                IconButton(onClick = onClose, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Outlined.Close, "Close selection")
                }
            }

            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth(),
            ) {
                AnnotationStyleTool(
                    icon = Icons.Outlined.FormatColorFill,
                    label = "Highlight",
                    removeLabel = "Remove highlight",
                    onColor = onHighlight,
                    onRemove = onRemoveHighlight,
                )
                AnnotationStyleTool(
                    icon = Icons.Outlined.FormatUnderlined,
                    label = "Underline",
                    removeLabel = "Remove underline",
                    onColor = onUnderline,
                    onRemove = onRemoveUnderline,
                )
                SelectionTool(Icons.Outlined.Translate, "Translate", onTranslate)
                SelectionTool(
                    Icons.Outlined.VolumeUp,
                    if (selectionSpeaking) "Stop" else "Read",
                    onSpeak,
                    active = selectionSpeaking,
                )
            }

            if (translationExpanded) {
                Text(
                    text = translation.ifBlank { "Translating…" },
                    color = Color(0xFF4D4A55),
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                )
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Button(onClick = onSave, enabled = translation.isNotBlank()) {
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
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        SelectionTool(icon, label, onClick = { expanded = true })
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            Text(
                "Choose a color",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF5C5963),
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
    active: Boolean = false,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(if (active) Indigo.copy(alpha = 0.12f) else Color.Transparent)
            .padding(horizontal = 7.dp, vertical = 6.dp),
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (active) Indigo else Color(0xFF3E3B45),
            modifier = Modifier.size(21.dp),
        )
        Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
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
