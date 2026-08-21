package com.lumen.researchenglish.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lumen.researchenglish.data.DocumentEntity
import com.lumen.researchenglish.data.GutenbergBook
import com.lumen.researchenglish.domain.DailyCheckInStats
import com.lumen.researchenglish.ui.theme.Indigo
import com.lumen.researchenglish.ui.theme.SoftIndigo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun LibraryScreen(
    viewModel: AppViewModel,
    onOpenDocument: (String) -> Unit,
) {
    val documents by viewModel.documents.collectAsStateWithLifecycle()
    val gutenbergResults by viewModel.gutenbergResults.collectAsStateWithLifecycle()
    val gutenbergLoading by viewModel.gutenbergLoading.collectAsStateWithLifecycle()
    val gutenbergStatus by viewModel.gutenbergStatus.collectAsStateWithLifecycle()
    val dailyCheckIn by viewModel.dailyCheckIn.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<DocumentEntity?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        viewModel.importPdf(uri, "BOOK")
    }

    pendingDelete?.let { document ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Remove this book?") },
            text = {
                Text(
                    if (document.id.startsWith("classic-")) {
                        "${document.title} will be hidden from Lumen."
                    } else {
                        "${document.title} will be removed from Lumen. The original PDF on your device will not be deleted."
                    },
                )
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDocument(document.id)
                        pendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text("Remove") }
            },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 34.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Lumen",
                        fontFamily = FontFamily.Serif,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Read deeply. Remember naturally.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                FilledIconButton(
                    onClick = { launcher.launch(arrayOf("application/pdf")) },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Import PDF")
                }
            }
        }

        item {
            DailyCheckInCard(
                stats = dailyCheckIn,
                onCheckIn = viewModel::checkInToday,
            )
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (gutenbergLoading) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(
                            onClick = { viewModel.searchGutenberg(searchQuery) },
                            enabled = searchQuery.trim().length >= 2,
                        ) {
                            Icon(Icons.Outlined.Search, contentDescription = "Search Project Gutenberg")
                        }
                    }
                },
                placeholder = { Text("Search Project Gutenberg") },
                supportingText = {
                    Text(
                        gutenbergStatus.ifBlank {
                            "Search the public-domain catalog by title or author."
                        },
                    )
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { viewModel.searchGutenberg(searchQuery) },
                ),
                shape = RoundedCornerShape(18.dp),
            )
        }

        if (gutenbergResults.isNotEmpty()) {
            item {
                GutenbergSearchSection(
                    books = gutenbergResults,
                    onImport = viewModel::importGutenberg,
                )
            }
        }

        if (documents.isEmpty()) {
            item {
                EmptyLibrary(onImport = { launcher.launch(arrayOf("application/pdf")) })
            }
        }
        val sortedDocuments = documents.sortedBy { it.importedAt }
        if (sortedDocuments.isNotEmpty()) {
            item {
                LibrarySection(
                    title = "Books",
                    subtitle = "${sortedDocuments.size} book${if (sortedDocuments.size == 1) "" else "s"}",
                    documents = sortedDocuments,
                    onOpenDocument = onOpenDocument,
                    onDeleteDocument = { pendingDelete = it },
                )
            }
        }
    }
}

@Composable
private fun DailyCheckInCard(
    stats: DailyCheckInStats,
    onCheckIn: () -> Unit,
) {
    var displayedMonth by remember { mutableStateOf(YearMonth.now()) }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)),
                ) {
                    Icon(
                        if (stats.checkedInToday) Icons.Outlined.CheckCircle
                        else Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = if (stats.checkedInToday) Color(0xFF197A4A) else Indigo,
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (stats.checkedInToday) "Checked in for today" else "Daily check-in",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        if (stats.checkedInToday) {
                            "Nice work — come back tomorrow to keep the streak going."
                        } else {
                            "Build a steady reading habit and earn 10 XP."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                CheckInMetric(
                    icon = Icons.Outlined.LocalFireDepartment,
                    value = stats.currentStreak.toString(),
                    label = "day streak",
                    modifier = Modifier.weight(1f),
                )
                CheckInMetric(
                    icon = Icons.Outlined.CalendarMonth,
                    value = stats.totalDays.toString(),
                    label = "days total",
                    modifier = Modifier.weight(1f),
                )
                CheckInMetric(
                    icon = Icons.Outlined.CheckCircle,
                    value = stats.longestStreak.toString(),
                    label = "best streak",
                    modifier = Modifier.weight(1f),
                )
                FilledTonalButton(
                    onClick = onCheckIn,
                    enabled = !stats.checkedInToday,
                ) {
                    Text(if (stats.checkedInToday) "Done" else "Check in")
                }
            }
            CheckInCalendar(
                month = displayedMonth,
                checkedDates = stats.checkInDates,
                onPreviousMonth = { displayedMonth = displayedMonth.minusMonths(1) },
                onNextMonth = { displayedMonth = displayedMonth.plusMonths(1) },
            )
        }
    }
}

@Composable
private fun CheckInCalendar(
    month: YearMonth,
    checkedDates: Set<LocalDate>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    val today = LocalDate.now()
    val firstDate = month.atDay(1)
    val leadingDays = firstDate.dayOfWeek.value - 1
    val gridStart = firstDate.minusDays(leadingDays.toLong())
    val dates = remember(month) { List(42) { gridStart.plusDays(it.toLong()) } }
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            IconButton(onClick = onPreviousMonth, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Previous month")
            }
            Text(
                month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            IconButton(onClick = onNextMonth, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, "Next month")
            }
        }
        Row(Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                Text(
                    day,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        dates.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    val checked = date in checkedDates
                    val isToday = date == today
                    val inMonth = YearMonth.from(date) == month
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(3.dp)
                            .clip(CircleShape)
                            .then(
                                if (isToday) Modifier.border(1.5.dp, Indigo, CircleShape)
                                else Modifier,
                            )
                            .background(
                                if (checked) Color(0xFF2E9463)
                                else Color.Transparent,
                            ),
                    ) {
                        Text(
                            date.dayOfMonth.toString(),
                            color = when {
                                checked -> Color.White
                                !inMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (checked || isToday) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
            }
        }
        Text(
            "Green = checked in · outlined = today · +10 XP once per day",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun CheckInMetric(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color(0xFFE46B2B),
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(6.dp))
        Column {
            Text(value, fontWeight = FontWeight.Bold)
            Text(
                label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun GutenbergSearchSection(
    books: List<GutenbergBook>,
    onImport: (GutenbergBook) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Project Gutenberg", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Text(
            "Select a result to download its plain-text edition, create a reading PDF and add it to Novels.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        books.take(12).forEach { book ->
            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(book.title, fontWeight = FontWeight.SemiBold, maxLines = 2)
                        Text(
                            book.author.ifBlank { "Project Gutenberg ebook #${book.id}" },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Button(onClick = { onImport(book) }) { Text("Import") }
                }
            }
        }
    }
}

@Composable
private fun EmptyLibrary(onImport: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SoftIndigo),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Outlined.AutoStories,
                contentDescription = null,
                tint = Indigo,
                modifier = Modifier.size(54.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text("Your reading space", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Import a PDF book. Lumen will create a cover from its first page.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            Button(onClick = onImport) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Import PDF")
            }
        }
    }
}

@Composable
private fun LibrarySection(
    title: String,
    subtitle: String,
    documents: List<DocumentEntity>,
    onOpenDocument: (String) -> Unit,
    onDeleteDocument: (DocumentEntity) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(title, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(10.dp))
            Text(
                subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(end = 8.dp),
        ) {
            items(documents, key = { it.id }) { document ->
                DocumentCard(
                    document = document,
                    onClick = { onOpenDocument(document.id) },
                    onLongClick = { onDeleteDocument(document) },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DocumentCard(
    document: DocumentEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(158.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        ) {
            var cover by remember(document.coverPath) { mutableStateOf<Bitmap?>(null) }
            LaunchedEffect(document.coverPath) {
                cover = withContext(Dispatchers.IO) {
                    document.coverPath?.let(BitmapFactory::decodeFile)
                }
            }
            if (cover != null) {
                Image(
                    bitmap = cover!!.asImageBitmap(),
                    contentDescription = "${document.title} cover",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SoftIndigo),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.AutoStories,
                        contentDescription = null,
                        tint = Indigo,
                        modifier = Modifier.size(46.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = document.title,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Medium,
            lineHeight = 19.sp,
        )
        Spacer(Modifier.height(7.dp))
        LinearProgressIndicator(
            progress = { document.progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${(document.progress * 100).toInt()}% · ${document.pageCount} pages",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
