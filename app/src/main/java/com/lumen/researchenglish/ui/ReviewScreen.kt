package com.lumen.researchenglish.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lumen.researchenglish.R
import com.lumen.researchenglish.data.DeckWord
import com.lumen.researchenglish.data.VocabularyCardEntity
import com.lumen.researchenglish.data.VocabularyDeck
import com.lumen.researchenglish.domain.LearningProgress
import com.lumen.researchenglish.domain.MemoryModel
import com.lumen.researchenglish.domain.MemoryStage
import com.lumen.researchenglish.domain.ReviewRating
import com.lumen.researchenglish.ui.theme.Indigo
import com.lumen.researchenglish.ui.theme.Sage
import com.lumen.researchenglish.ui.theme.SoftIndigo
import java.util.Locale
import kotlin.math.roundToInt

private enum class ReviewMode { LEARN, REVIEW }

@Composable
fun ReviewScreen(viewModel: AppViewModel) {
    val dueCards by viewModel.dueCards.collectAsStateWithLifecycle()
    val dueCardCount by viewModel.dueCardCount.collectAsStateWithLifecycle()
    val allCards by viewModel.vocabulary.collectAsStateWithLifecycle()
    val reviewNow by viewModel.reviewNow.collectAsStateWithLifecycle()
    val selectedDeckId by viewModel.selectedVocabularyDeckId.collectAsStateWithLifecycle()
    val deckPosition by viewModel.activeDeckPosition.collectAsStateWithLifecycle()
    val learningProgress by viewModel.learningProgress.collectAsStateWithLifecycle()
    val speechLoadingId by viewModel.speechLoadingId.collectAsStateWithLifecycle()
    val speakingId by viewModel.speakingId.collectAsStateWithLifecycle()
    var mode by rememberSaveable { mutableStateOf(ReviewMode.LEARN) }
    var reviewedThisSession by rememberSaveable { mutableIntStateOf(0) }
    var rememberedThisSession by rememberSaveable { mutableIntStateOf(0) }
    var manualPracticeQueue by remember { mutableStateOf<List<String>>(emptyList()) }
    var manualPracticeIndex by remember { mutableIntStateOf(0) }

    val selectedDeck = viewModel.vocabularyDecks.firstOrNull { it.id == selectedDeckId }
        ?: viewModel.vocabularyDecks.first()
    val activeWord = selectedDeck.words.getOrNull(deckPosition)
    val rememberedCount = allCards.count {
        MemoryModel.stage(it, reviewNow) == MemoryStage.REMEMBERED
    }
    val learningCount = allCards.count {
        MemoryModel.stage(it, reviewNow) == MemoryStage.LEARNING
    }
    val newCount = allCards.count {
        MemoryModel.stage(it, reviewNow) == MemoryStage.NEW
    }
    val memoryRate = MemoryModel.averageStrength(allCards, reviewNow) * 100
    val manualPracticeCard = manualPracticeQueue
        .takeIf { it.isNotEmpty() }
        ?.let { queue -> queue[manualPracticeIndex % queue.size] }
        ?.let { id -> allCards.firstOrNull { it.id == id } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 30.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("Review", fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "Learn actively, then let spaced repetition keep it fresh.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item { DinosaurProgressCard(learningProgress) }

        item {
            ModeSelector(mode = mode, onSelect = { mode = it })
        }

        if (mode == ReviewMode.LEARN) {
            item {
                DeckPicker(
                    decks = viewModel.vocabularyDecks,
                    selected = selectedDeck,
                    onSelect = viewModel::selectVocabularyDeck,
                )
            }
            if (activeWord == null) {
                item {
                    DeckComplete(
                        deck = selectedDeck,
                        onRestart = viewModel::restartVocabularyDeck,
                    )
                }
            } else {
                item {
                    key("${selectedDeck.id}-$deckPosition") {
                        val speechId = "deck-${selectedDeck.id}-$deckPosition"
                        ActiveLearningCard(
                            word = activeWord,
                            position = deckPosition + 1,
                            total = selectedDeck.words.size,
                            speechLoading = speechLoadingId == speechId,
                            speaking = speakingId == speechId,
                            onSpeak = { viewModel.speak(activeWord.word, speechId) },
                            onRate = { rating ->
                                reviewedThisSession += 1
                                if (rating == ReviewRating.GOOD || rating == ReviewRating.EASY) {
                                    rememberedThisSession += 1
                                }
                                viewModel.learnDeckWord(activeWord, rating)
                            },
                        )
                    }
                }
            }
        } else {
            item {
                MemoryDashboard(
                    dueToday = dueCardCount,
                    remembered = rememberedCount,
                    learning = learningCount,
                    memoryRate = memoryRate,
                )
            }
            if (dueCards.isEmpty()) {
                if (manualPracticeCard == null) {
                    item {
                        ReviewComplete(
                            practiceAvailable = allCards.isNotEmpty(),
                            onStartPractice = {
                                val eligibleCards = allCards
                                    .filter { it.repetitions > 0 }
                                    .ifEmpty { allCards }
                                manualPracticeQueue = eligibleCards
                                    .sortedBy { it.lastReviewedAt }
                                    .map { it.id }
                                manualPracticeIndex = 0
                            },
                        )
                    }
                } else {
                    item {
                        key("manual-${manualPracticeCard.id}-$manualPracticeIndex") {
                            val speechId = "practice-${manualPracticeCard.id}"
                            SavedVocabularyCard(
                                card = manualPracticeCard,
                                position = manualPracticeIndex % manualPracticeQueue.size + 1,
                                total = manualPracticeQueue.size,
                                speechLoading = speechLoadingId == speechId,
                                speaking = speakingId == speechId,
                                onSpeak = { viewModel.speak(manualPracticeCard.term, speechId) },
                                onRate = { rating ->
                                    reviewedThisSession += 1
                                    if (rating == ReviewRating.GOOD || rating == ReviewRating.EASY) {
                                        rememberedThisSession += 1
                                    }
                                    manualPracticeIndex =
                                        if (manualPracticeIndex == Int.MAX_VALUE) 0 else manualPracticeIndex + 1
                                    viewModel.review(manualPracticeCard, rating)
                                },
                            )
                        }
                    }
                }
            } else {
                item {
                    key(dueCards.first().id) {
                        val card = dueCards.first()
                        val speechId = "review-${card.id}"
                        SavedVocabularyCard(
                            card = card,
                            position = reviewedThisSession + 1,
                            total = reviewedThisSession + dueCards.size,
                            speechLoading = speechLoadingId == speechId,
                            speaking = speakingId == speechId,
                            onSpeak = { viewModel.speak(card.term, speechId) },
                            onRate = { rating ->
                                reviewedThisSession += 1
                                if (rating == ReviewRating.GOOD || rating == ReviewRating.EASY) {
                                    rememberedThisSession += 1
                                }
                                viewModel.review(card, rating)
                            },
                        )
                    }
                }
            }
        }

        if (allCards.isNotEmpty()) {
            item {
                Spacer(Modifier.height(4.dp))
                Text("My word cards", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "$newCount new · $learningCount learning · $rememberedCount memory",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            items(allCards.take(60), key = { it.id }) { card ->
                val speechId = "word-list-${card.id}"
                WordMemoryCard(
                    card = card,
                    now = reviewNow,
                    speechLoading = speechLoadingId == speechId,
                    speaking = speakingId == speechId,
                    onSpeak = { viewModel.speak(card.term, speechId) },
                )
            }
        }
    }
}

@Composable
private fun DinosaurProgressCard(progress: LearningProgress) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = SoftIndigo.copy(alpha = 0.68f)),
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        "Level ${progress.level}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Indigo,
                    )
                    Text(
                        progress.evolutionName,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                Text(
                    if (progress.level == 100) "MAX" else {
                        "${progress.xpIntoLevel} / ${progress.xpForNextLevel} XP"
                    },
                    color = Indigo,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp),
            ) {
                LinearProgressIndicator(
                    progress = { progress.progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .align(Alignment.BottomCenter)
                        .clip(CircleShape),
                    color = Indigo,
                    trackColor = Color.White.copy(alpha = 0.72f),
                )
                val travel = if (maxWidth > 68.dp) maxWidth - 68.dp else 0.dp
                DinosaurStage(
                    stage = progress.evolutionStage,
                    modifier = Modifier
                        .size(72.dp)
                        .align(Alignment.BottomStart)
                        .offset(x = travel * progress.progress.coerceIn(0f, 1f), y = (-7).dp),
                )
            }
            Text(
                "Evolution at levels 30, 60 and 90",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun DinosaurStage(stage: Int, modifier: Modifier = Modifier) {
    val sprite = ImageBitmap.imageResource(R.drawable.dinosaur_stages)
    Canvas(modifier) {
        val quadrant = sprite.width / 2
        val inset = (quadrant * 0.09f).roundToInt()
        val sourceSize = quadrant - inset * 2
        val safeStage = stage.coerceIn(0, 3)
        val column = safeStage % 2
        val row = safeStage / 2
        drawImage(
            image = sprite,
            srcOffset = IntOffset(column * quadrant + inset, row * quadrant + inset),
            srcSize = IntSize(sourceSize, sourceSize),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
        )
    }
}

@Composable
private fun ModeSelector(mode: ReviewMode, onSelect: (ReviewMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ReviewMode.entries.forEach { item ->
            val selected = mode == item
            Button(
                onClick = { onSelect(item) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selected) Indigo else Color.Transparent,
                    contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            ) {
                Text(if (item == ReviewMode.LEARN) "Learn new" else "Review due")
            }
        }
    }
}

@Composable
private fun DeckPicker(
    decks: List<VocabularyDeck>,
    selected: VocabularyDeck,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 13.dp),
        ) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                Text(selected.name, fontWeight = FontWeight.SemiBold)
                Text(
                    "${selected.words.size} words · ${selected.description}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Icons.Outlined.ExpandMore, contentDescription = "Choose vocabulary deck")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            decks.forEach { deck ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(deck.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${deck.words.size} words · ${deck.description}",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelect(deck.id)
                    },
                )
            }
        }
    }
}

@Composable
private fun ActiveLearningCard(
    word: DeckWord,
    position: Int,
    total: Int,
    speechLoading: Boolean,
    speaking: Boolean,
    onSpeak: () -> Unit,
    onRate: (ReviewRating) -> Unit,
) {
    val details = buildList {
        word.phonetic.takeIf { it.isNotBlank() }
            ?.let { add("Pronunciation: /$it/") }
        if (word.chineseDefinition.isNotBlank() && word.definition.isNotBlank()) {
            add("English: ${word.definition}")
        }
        word.synonyms.take(4).takeIf { it.isNotEmpty() }
            ?.joinToString(", ")
            ?.let { add("Synonyms: $it") }
    }.joinToString("\n")
    StudyCard(
        identity = "$position-${word.word}",
        term = word.word,
        definition = word.chineseDefinition.ifBlank { word.definition },
        context = word.example,
        meta = listOf(word.partOfSpeech, word.theme).filter { it.isNotBlank() }.joinToString(" · "),
        extra = details,
        position = position,
        total = total,
        speechLoading = speechLoading,
        speaking = speaking,
        onSpeak = onSpeak,
        onRate = onRate,
    )
}

@Composable
private fun SavedVocabularyCard(
    card: VocabularyCardEntity,
    position: Int,
    total: Int,
    speechLoading: Boolean,
    speaking: Boolean,
    onSpeak: () -> Unit,
    onRate: (ReviewRating) -> Unit,
) {
    StudyCard(
        identity = card.id,
        term = card.term,
        definition = card.translation.ifBlank { "No definition saved" },
        context = card.context,
        meta = listOf(card.sourceTitle, card.sourcePage.takeIf { it > 0 }?.let { "p. $it" })
            .filterNotNull().filter { it.isNotBlank() }.joinToString(" · "),
        extra = "",
        position = position,
        total = total.coerceAtLeast(1),
        speechLoading = speechLoading,
        speaking = speaking,
        onSpeak = onSpeak,
        onRate = onRate,
    )
}

@Composable
private fun StudyCard(
    identity: String,
    term: String,
    definition: String,
    context: String,
    meta: String,
    extra: String,
    position: Int,
    total: Int,
    speechLoading: Boolean,
    speaking: Boolean,
    onSpeak: () -> Unit,
    onRate: (ReviewRating) -> Unit,
) {
    var revealed by remember(identity) { mutableStateOf(false) }
    Card(
        shape = RoundedCornerShape(30.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 400.dp)
            .clickable(enabled = !revealed) { revealed = true },
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("$position / $total", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(if (revealed) 20.dp else 58.dp))
            Text(
                term,
                fontFamily = FontFamily.Serif,
                fontSize = if (revealed) 34.sp else 42.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            )
            SpeechButton(speechLoading, speaking, onSpeak)
            if (!revealed) {
                Spacer(Modifier.weight(1f))
                Icon(Icons.Outlined.TouchApp, contentDescription = null, tint = Indigo)
                Spacer(Modifier.height(8.dp))
                Text("Tap to reveal", color = Indigo, fontWeight = FontWeight.SemiBold)
            } else {
                if (meta.isNotBlank()) {
                    Text(meta, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    definition,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Indigo,
                    textAlign = TextAlign.Center,
                )
                if (extra.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(extra, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (context.isNotBlank()) {
                    Spacer(Modifier.height(18.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SoftIndigo),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Text(context, modifier = Modifier.padding(16.dp), lineHeight = 22.sp)
                    }
                }
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.height(20.dp))
                RecallButtons(onRate)
            }
        }
    }
}

@Composable
private fun RecallButtons(onRate: (ReviewRating) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RecallButton("不认识", "Again", Color(0xFFC64747), Modifier.weight(1f)) {
                onRate(ReviewRating.AGAIN)
            }
            RecallButton("有点模糊", "Hard", Color(0xFFB47A23), Modifier.weight(1f)) {
                onRate(ReviewRating.HARD)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RecallButton("认识", "Good", Indigo, Modifier.weight(1f)) {
                onRate(ReviewRating.GOOD)
            }
            RecallButton("很熟", "Easy", Sage, Modifier.weight(1f)) {
                onRate(ReviewRating.EASY)
            }
        }
    }
}

@Composable
private fun RecallButton(
    chinese: String,
    english: String,
    color: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(15.dp),
        contentPadding = PaddingValues(vertical = 10.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(chinese, fontWeight = FontWeight.SemiBold)
            Text(english, fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun MemoryDashboard(dueToday: Int, remembered: Int, learning: Int, memoryRate: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatTile(dueToday.toString(), "Due", Color(0xFFDB7B38), Modifier.weight(1f))
            StatTile(learning.toString(), "Learning", Indigo, Modifier.weight(1f))
            StatTile(remembered.toString(), "Memory", Sage, Modifier.weight(1f))
        }
        Text(
            "Predicted recall ${String.format(Locale.getDefault(), "%.1f%%", memoryRate)} · average across all saved words, not the share in Memory. Based on reviews, intervals, and lapses.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun StatTile(value: String, label: String, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 13.dp)) {
            Text(value, fontSize = 23.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun DeckComplete(deck: VocabularyDeck, onRestart: () -> Unit) {
    Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(SoftIndigo)) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Outlined.CheckCircleOutline, null, tint = Sage, modifier = Modifier.size(50.dp))
            Spacer(Modifier.height(12.dp))
            Text("Deck completed", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Text("You studied all ${deck.words.size} words in ${deck.name}.", textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onRestart) {
                Icon(Icons.Outlined.Refresh, null)
                Spacer(Modifier.width(7.dp))
                Text("Study again")
            }
        }
    }
}

@Composable
private fun ReviewComplete(
    practiceAvailable: Boolean,
    onStartPractice: () -> Unit,
) {
    Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(SoftIndigo)) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Outlined.CheckCircleOutline, null, tint = Sage, modifier = Modifier.size(50.dp))
            Spacer(Modifier.height(12.dp))
            Text("You’re all caught up", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Text(
                if (practiceAvailable) {
                    "No cards are due right now. You can still practice saved words as often as you like."
                } else {
                    "New and due cards will appear here."
                },
                textAlign = TextAlign.Center,
            )
            if (practiceAvailable) {
                Spacer(Modifier.height(16.dp))
                Button(onClick = onStartPractice) {
                    Icon(Icons.Outlined.Refresh, null)
                    Spacer(Modifier.width(7.dp))
                    Text("Practice saved words")
                }
            }
        }
    }
}

@Composable
private fun WordMemoryCard(
    card: VocabularyCardEntity,
    now: Long,
    speechLoading: Boolean,
    speaking: Boolean,
    onSpeak: () -> Unit,
) {
    val stage = MemoryModel.stage(card, now)
    val stageColor = when (stage) {
        MemoryStage.NEW -> Color(0xFF8A8A86)
        MemoryStage.LEARNING -> Indigo
        MemoryStage.REMEMBERED -> Sage
    }
    val stageIcon = when (stage) {
        MemoryStage.NEW -> Icons.Outlined.School
        MemoryStage.LEARNING -> Icons.Outlined.Psychology
        MemoryStage.REMEMBERED -> Icons.Outlined.CheckCircle
    }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(17.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(card.term, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        card.translation.ifBlank { card.context.take(100) },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                SpeechButton(speechLoading, speaking, onSpeak, compact = true)
                Spacer(Modifier.width(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(stageColor.copy(alpha = 0.12f))
                        .padding(horizontal = 9.dp, vertical = 6.dp),
                ) {
                    Icon(stageIcon, null, tint = stageColor, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stage.label, color = stageColor, style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.height(11.dp))
            LinearProgressIndicator(
                progress = { MemoryModel.strength(card, now) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = stageColor,
                trackColor = stageColor.copy(alpha = 0.1f),
            )
        }
    }
}

@Composable
private fun SpeechButton(
    loading: Boolean,
    speaking: Boolean,
    onClick: () -> Unit,
    compact: Boolean = false,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(if (compact) 36.dp else 46.dp)) {
        when {
            loading -> CircularProgressIndicator(
                modifier = Modifier.size(if (compact) 17.dp else 20.dp),
                strokeWidth = 2.dp,
            )
            speaking -> Icon(Icons.Outlined.StopCircle, "Stop pronunciation", tint = Indigo)
            else -> Icon(Icons.Outlined.VolumeUp, "Pronounce word", tint = Indigo)
        }
    }
}
