package com.lumen.researchenglish.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.AddComment
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lumen.researchenglish.data.ChatMessageEntity
import com.lumen.researchenglish.data.ChatSessionEntity
import com.lumen.researchenglish.ui.theme.Indigo
import com.lumen.researchenglish.ui.theme.SoftIndigo
import kotlin.math.roundToInt

@Composable
fun ChatScreen(viewModel: AppViewModel) {
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val sessions by viewModel.chatSessions.collectAsStateWithLifecycle()
    val currentSessionId by viewModel.currentChatSessionId.collectAsStateWithLifecycle()
    val streamingReply by viewModel.streamingReply.collectAsStateWithLifecycle()
    val chatStreaming by viewModel.chatStreaming.collectAsStateWithLifecycle()
    val userAvatarUri by viewModel.userAvatarUri.collectAsStateWithLifecycle()
    val speechLoadingId by viewModel.speechLoadingId.collectAsStateWithLifecycle()
    val speakingId by viewModel.speakingId.collectAsStateWithLifecycle()
    val speechProgress by viewModel.speechProgress.collectAsStateWithLifecycle()
    var input by remember { mutableStateOf("") }
    var showHistory by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    var followLatest by remember { mutableStateOf(true) }

    if (showHistory) {
        ChatHistoryDialog(
            sessions = sessions,
            currentSessionId = currentSessionId,
            onDismiss = { showHistory = false },
            onSelect = {
                followLatest = true
                viewModel.selectChat(it.id)
                showHistory = false
            },
            onTogglePin = viewModel::toggleChatPin,
            onNewChat = {
                followLatest = true
                viewModel.newChat()
                showHistory = false
            },
        )
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress to listState.canScrollForward }
            .collect { (scrolling, hasNewerContent) ->
                when {
                    !hasNewerContent -> followLatest = true
                    scrolling -> followLatest = false
                }
            }
    }

    LaunchedEffect(currentSessionId) {
        followLatest = true
    }

    LaunchedEffect(messages.size, streamingReply?.length?.div(80)) {
        val target = messages.size + if (streamingReply != null) 1 else 0
        if (followLatest && target > 0) listState.animateScrollToItem(target - 1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(top = 30.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TutorAvatar(size = 46.dp)
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "English Tutor",
                    fontSize = 22.sp,
                    lineHeight = 25.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Memory,
                        contentDescription = null,
                        tint = Indigo,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        if (chatStreaming) "Replying live…" else "Uses your editable memory",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            IconButton(
                onClick = { showHistory = true },
                enabled = !chatStreaming,
                modifier = Modifier.size(38.dp),
            ) {
                Icon(Icons.Outlined.History, contentDescription = "Chat history")
            }
            IconButton(
                onClick = {
                    followLatest = true
                    viewModel.newChat()
                },
                enabled = !chatStreaming,
                modifier = Modifier.size(38.dp),
            ) {
                Icon(Icons.Outlined.AddComment, contentDescription = "New chat")
            }
            if (messages.isNotEmpty()) {
                IconButton(
                    onClick = viewModel::clearChat,
                    enabled = !chatStreaming,
                    modifier = Modifier.size(38.dp),
                ) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Clear chat")
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 88.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (messages.isEmpty() && streamingReply == null) item { WelcomeMessage() }
                items(messages, key = { it.id }) { message ->
                    val speechId = "chat-${message.id}"
                    MessageBubble(
                        message = message,
                        userAvatarUri = userAvatarUri,
                        speechLoading = speechLoadingId == speechId,
                        speaking = speakingId == speechId,
                        speechProgress = if (speakingId == speechId) speechProgress else 0f,
                        streaming = false,
                        onSpeak = {
                            viewModel.speak(tutorMarkdownPlainText(message.content), speechId)
                        },
                    )
                }
                streamingReply?.let { reply ->
                    item(key = "streaming-reply") {
                        MessageBubble(
                            message = ChatMessageEntity(
                                id = "streaming-reply",
                                role = "assistant",
                                content = reply,
                            ),
                            userAvatarUri = userAvatarUri,
                            speechLoading = false,
                            speaking = false,
                            speechProgress = 0f,
                            streaming = true,
                            onSpeak = {},
                        )
                    }
                }
            }
            ConversationPagingControls(
                listState = listState,
                onPagingAwayFromLatest = { followLatest = false },
                onReturnToLatest = { followLatest = true },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask in English…") },
                shape = RoundedCornerShape(22.dp),
                maxLines = 5,
            )
            Spacer(Modifier.width(6.dp))
            IconButton(
                onClick = {
                    val message = input
                    input = ""
                    followLatest = true
                    viewModel.sendChat(message)
                },
                enabled = input.isNotBlank() && !chatStreaming,
                modifier = Modifier.size(50.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.Send,
                    contentDescription = "Send",
                    tint = Indigo,
                )
            }
        }
    }
}

@Composable
private fun ChatHistoryDialog(
    sessions: List<ChatSessionEntity>,
    currentSessionId: String,
    onDismiss: () -> Unit,
    onSelect: (ChatSessionEntity) -> Unit,
    onTogglePin: (ChatSessionEntity) -> Unit,
    onNewChat: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chat history") },
        text = {
            if (sessions.isEmpty()) {
                Text("No saved chats yet.")
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(sessions, key = { it.id }) { session ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(session) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (session.id == currentSessionId) {
                                    SoftIndigo
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                },
                            ),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 14.dp, top = 8.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        session.title.ifBlank { "New chat" },
                                        maxLines = 2,
                                        fontWeight = if (session.id == currentSessionId) {
                                            FontWeight.SemiBold
                                        } else {
                                            FontWeight.Normal
                                        },
                                    )
                                    Text(
                                        if (session.pinned) "Pinned · kept permanently" else "Recent chat",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                IconButton(onClick = { onTogglePin(session) }) {
                                    Icon(
                                        Icons.Outlined.PushPin,
                                        contentDescription = if (session.pinned) "Unpin" else "Pin",
                                        tint = if (session.pinned) Indigo else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        confirmButton = { Button(onClick = onNewChat) { Text("New chat") } },
    )
}

@Composable
private fun WelcomeMessage() {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        TutorAvatar(size = 38.dp)
        Spacer(Modifier.width(9.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = SoftIndigo),
            shape = RoundedCornerShape(6.dp, 22.dp, 22.dp, 22.dp),
            modifier = Modifier.weight(1f),
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("Hello — I’m your research English tutor.", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Ask me to explain a difficult sentence, polish academic writing, or quiz you with vocabulary from your reading.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessageEntity,
    userAvatarUri: String,
    speechLoading: Boolean,
    speaking: Boolean,
    speechProgress: Float,
    streaming: Boolean,
    onSpeak: () -> Unit,
) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        if (!isUser) {
            TutorAvatar(size = 36.dp)
            Spacer(Modifier.width(8.dp))
        }
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) Indigo else MaterialTheme.colorScheme.surface,
            ),
            shape = RoundedCornerShape(
                topStart = if (isUser) 20.dp else 6.dp,
                topEnd = if (isUser) 6.dp else 20.dp,
                bottomStart = 20.dp,
                bottomEnd = 20.dp,
            ),
            modifier = Modifier.fillMaxWidth(0.82f),
        ) {
            Column {
                if (isUser) {
                    Text(
                        text = message.content,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                        lineHeight = 22.sp,
                    )
                } else {
                    val shownText = when {
                        streaming && message.content.isEmpty() -> "Tutor is thinking…"
                        streaming -> "${message.content} ▍"
                        else -> message.content
                    }
                    TutorFollowText(
                        text = shownText,
                        progress = speechProgress,
                        active = speaking && !streaming,
                        modifier = Modifier.padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 13.dp,
                            bottom = if (streaming) 13.dp else 2.dp,
                        ),
                    )
                }
                if (!isUser && !streaming) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 6.dp, end = 8.dp, bottom = 5.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        IconButton(onClick = onSpeak, modifier = Modifier.size(38.dp)) {
                            when {
                                speechLoading -> CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                )
                                speaking -> Icon(
                                    Icons.Outlined.StopCircle,
                                    contentDescription = "Stop Tutor voice",
                                    tint = Indigo,
                                )
                                else -> Icon(
                                    Icons.AutoMirrored.Outlined.VolumeUp,
                                    contentDescription = "Read Tutor reply aloud",
                                    tint = Indigo,
                                )
                            }
                        }
                    }
                }
            }
        }
        if (isUser) {
            Spacer(Modifier.width(8.dp))
            UserAvatar(uri = userAvatarUri, size = 36.dp)
        }
    }
}

@Composable
private fun TutorFollowText(
    text: String,
    progress: Float,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    var textLayout by remember(text) { mutableStateOf<TextLayoutResult?>(null) }
    val renderedText = renderTutorMarkdown(
        markdown = text,
        accentColor = Indigo,
        codeBackground = MaterialTheme.colorScheme.surfaceVariant,
    )
    val density = LocalDensity.current
    val markerWidth = 16.dp
    val markerHeight = 3.dp
    val markerWidthPx = with(density) { markerWidth.toPx() }
    val gapPx = with(density) { 1.dp.toPx() }

    Box(modifier = modifier) {
        Text(
            text = renderedText,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 22.sp,
            onTextLayout = { textLayout = it },
            modifier = Modifier.padding(bottom = 7.dp),
        )
        val layout = textLayout
        if (active && progress > 0f && renderedText.isNotEmpty() && layout != null) {
            val characterIndex = ((renderedText.length - 1) * progress.coerceIn(0f, 1f))
                .roundToInt()
                .coerceIn(0, renderedText.lastIndex)
            val bounds = runCatching { layout.getBoundingBox(characterIndex) }.getOrNull()
            if (bounds != null) {
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (bounds.center.x - markerWidthPx / 2f).roundToInt(),
                                y = (bounds.bottom + gapPx).roundToInt(),
                            )
                        }
                        .size(width = markerWidth, height = markerHeight)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(2.dp),
                            ambientColor = Color(0xFF6DEAFF),
                            spotColor = Color(0xFF6DEAFF),
                        )
                        .background(Color(0xFF22C7F2), RoundedCornerShape(2.dp)),
                )
            }
        }
    }
}
