package com.lumen.researchenglish.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lumen.researchenglish.network.DeepSeekBalance
import com.lumen.researchenglish.ui.theme.SoftIndigo
import java.text.DateFormat
import java.util.Date

@Composable
fun SettingsScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val memory by viewModel.memory.collectAsStateWithLifecycle()
    val memoryUpdating by viewModel.memoryUpdating.collectAsStateWithLifecycle()
    val memoryStatus by viewModel.memoryStatus.collectAsStateWithLifecycle()
    val userAvatarUri by viewModel.userAvatarUri.collectAsStateWithLifecycle()
    val voiceType by viewModel.voiceType.collectAsStateWithLifecycle()
    val speechRate by viewModel.speechRate.collectAsStateWithLifecycle()
    val speechLoadingId by viewModel.speechLoadingId.collectAsStateWithLifecycle()
    val speakingId by viewModel.speakingId.collectAsStateWithLifecycle()
    val updateSource by viewModel.updateSource.collectAsStateWithLifecycle()
    val checkingForUpdate by viewModel.checkingForUpdate.collectAsStateWithLifecycle()
    val updateStatus by viewModel.updateStatus.collectAsStateWithLifecycle()
    val chatHistoryLimit by viewModel.chatHistoryLimit.collectAsStateWithLifecycle()
    val memoryUpdateFrequency by viewModel.memoryUpdateFrequency.collectAsStateWithLifecycle()
    val hasDeepSeekKey by viewModel.hasDeepSeekKey.collectAsStateWithLifecycle()
    val hasTencentCredentials by viewModel.hasTencentCredentials.collectAsStateWithLifecycle()
    val deepSeekBalance by viewModel.deepSeekBalance.collectAsStateWithLifecycle()
    val deepSeekBalanceRefreshing by viewModel.deepSeekBalanceRefreshing.collectAsStateWithLifecycle()
    val deepSeekBalanceError by viewModel.deepSeekBalanceError.collectAsStateWithLifecycle()
    val deepSeekBalanceUpdatedAt by viewModel.deepSeekBalanceUpdatedAt.collectAsStateWithLifecycle()
    var memoryDraft by rememberSaveable { mutableStateOf(memory) }
    var memoryDraftBase by rememberSaveable { mutableStateOf(memory) }
    var memoryDraftDirty by rememberSaveable { mutableStateOf(false) }
    var memoryChangedWhileEditing by rememberSaveable { mutableStateOf(false) }
    var voiceDraft by remember(voiceType) { mutableStateOf(voiceType.toString()) }
    var updateDraft by remember(updateSource) { mutableStateOf(updateSource) }
    var deepSeekKey by remember { mutableStateOf("") }
    var tencentId by remember { mutableStateOf("") }
    var tencentKey by remember { mutableStateOf("") }
    var historyLimitDraft by remember(chatHistoryLimit) { mutableStateOf(chatHistoryLimit.toString()) }
    var memoryFrequencyDraft by remember(memoryUpdateFrequency) {
        mutableStateOf(memoryUpdateFrequency.toString())
    }
    val voiceTestId = "voice-test"

    LaunchedEffect(memory) {
        if (!memoryDraftDirty) {
            memoryDraft = memory
            memoryDraftBase = memory
            memoryChangedWhileEditing = false
        } else if (memory != memoryDraftBase) {
            memoryChangedWhileEditing = true
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshDeepSeekBalance()
    }

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            viewModel.setUserAvatar(it)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 34.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Text("Settings", fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "Your PDFs, memory, vocabulary, and profile stay on this device.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            SettingsCard(
                icon = { Icon(Icons.Outlined.Tune, contentDescription = null) },
                title = "Tutor behavior",
                subtitle = "Chat retention and automatic memory",
            ) {
                OutlinedTextField(
                    value = historyLimitDraft,
                    onValueChange = { historyLimitDraft = it.filter(Char::isDigit).take(2) },
                    label = { Text("Recent chat histories to keep") },
                    supportingText = { Text("1–30; pinned chats are never removed. Default: 7") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = memoryFrequencyDraft,
                    onValueChange = { memoryFrequencyDraft = it.filter(Char::isDigit).take(2) },
                    label = { Text("Update memory every N user messages") },
                    supportingText = { Text("1–50. Default: 4") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    enabled = historyLimitDraft.toIntOrNull() != null &&
                        memoryFrequencyDraft.toIntOrNull() != null,
                    onClick = {
                        historyLimitDraft.toIntOrNull()?.let(viewModel::setChatHistoryLimit)
                        memoryFrequencyDraft.toIntOrNull()?.let(viewModel::setMemoryUpdateFrequency)
                    },
                ) { Text("Save Tutor settings") }
            }
        }

        item {
            SettingsCard(
                icon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                title = "Chat profile",
                subtitle = "Choose the avatar shown beside your messages",
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    UserAvatar(uri = userAvatarUri, size = 72.dp)
                    Column {
                        Text(
                            if (userAvatarUri.isBlank()) "Default profile" else "Your selected photo",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { avatarPicker.launch(arrayOf("image/*")) }) {
                            Icon(Icons.Outlined.PhotoLibrary, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Choose avatar")
                        }
                    }
                }
            }
        }

        item {
            SettingsCard(
                icon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                title = "API credentials",
                subtitle = "Encrypted with Android Keystore",
            ) {
                Text(
                    "Do not reuse the DeepSeek key previously pasted into chat. Revoke it first and enter a newly generated key here.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(12.dp))
                StatusLine("DeepSeek", hasDeepSeekKey)
                Spacer(Modifier.height(10.dp))
                DeepSeekBalancePanel(
                    configured = hasDeepSeekKey,
                    balance = deepSeekBalance,
                    refreshing = deepSeekBalanceRefreshing,
                    error = deepSeekBalanceError,
                    updatedAt = deepSeekBalanceUpdatedAt,
                    onRefresh = viewModel::refreshDeepSeekBalance,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = deepSeekKey,
                    onValueChange = { deepSeekKey = it },
                    label = { Text("New DeepSeek API key") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                StatusLine("Tencent Translation + Speech", hasTencentCredentials)
                OutlinedTextField(
                    value = tencentId,
                    onValueChange = { tencentId = it },
                    label = { Text("Tencent SecretId") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = tencentKey,
                    onValueChange = { tencentKey = it },
                    label = { Text("Tencent SecretKey") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            viewModel.saveApiSettings(deepSeekKey, tencentId, tencentKey)
                            deepSeekKey = ""
                            tencentId = ""
                            tencentKey = ""
                        },
                    ) { Text("Save securely") }
                    OutlinedButton(onClick = viewModel::clearApiSettings) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Clear")
                    }
                }
            }
        }

        item {
            SettingsCard(
                icon = { Icon(Icons.Outlined.RecordVoiceOver, contentDescription = null) },
                title = "Tutor voice",
                subtitle = "Tencent Cloud TTS · Chinese, English, and mixed text",
            ) {
                Text(
                    "Default 502004 is a polished bilingual female voice. You can replace it with any VoiceType from the Tencent console.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = voiceDraft,
                    onValueChange = { voiceDraft = it.filter(Char::isDigit) },
                    label = { Text("Tencent VoiceType") },
                    supportingText = { Text("Default: 502004") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                Text("Speech speed: ${"%.1f".format(speechRate)}")
                Slider(
                    value = speechRate,
                    onValueChange = viewModel::setSpeechRate,
                    valueRange = -2f..2f,
                    steps = 7,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        enabled = voiceDraft.toIntOrNull()?.let { it > 0 } == true,
                        onClick = { voiceDraft.toIntOrNull()?.let(viewModel::setVoiceType) },
                    ) { Text("Save voice") }
                    OutlinedButton(
                        onClick = {
                            voiceDraft.toIntOrNull()?.let(viewModel::setVoiceType)
                            viewModel.speak(
                                "Good evening. I’m your English tutor. 很高兴陪你学习科研英语。",
                                voiceTestId,
                            )
                        },
                    ) {
                        Text(
                            when {
                                speechLoadingId == voiceTestId -> "Generating…"
                                speakingId == voiceTestId -> "Stop"
                                else -> "Test voice"
                            },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { uriHandler.openUri("https://console.cloud.tencent.com/tts") }) {
                    Icon(Icons.Outlined.OpenInNew, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Open Tencent TTS console")
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Activate Text To Speech and claim a matching voice package before testing.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        item {
            SettingsCard(
                icon = { Icon(Icons.Outlined.SystemUpdate, contentDescription = null) },
                title = "App updates",
                subtitle = "Check a GitHub Release when Lumen starts",
            ) {
                OutlinedTextField(
                    value = updateDraft,
                    onValueChange = { updateDraft = it },
                    label = { Text("GitHub repository or update.json URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.saveUpdateSource(updateDraft) }) {
                        Text("Save source")
                    }
                    OutlinedButton(
                        enabled = !checkingForUpdate,
                        onClick = {
                            viewModel.saveUpdateSource(updateDraft)
                            viewModel.checkForUpdates(showErrors = true)
                        },
                    ) { Text(if (checkingForUpdate) "Checking…" else "Check now") }
                }
                if (updateStatus.isNotBlank()) {
                    Spacer(Modifier.height(9.dp))
                    Text(
                        updateStatus,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Spacer(Modifier.height(9.dp))
                Text(
                    "A release must contain update.json and the APK. The default repository address is saved, but the repository still needs to exist on GitHub.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        item {
            SettingsCard(
                icon = { Icon(Icons.Outlined.Memory, contentDescription = null) },
                title = "Editable memory",
                subtitle = "memory.md · manual notes plus Tutor auto-memory",
            ) {
                Text(
                    "Clear, non-sensitive statements about your background, research, English goals, recurring difficulties, and response preferences are saved immediately into editable sections. Say “remember that …” for any other fact. Every $memoryUpdateFrequency user messages in the current chat, Tutor also summarizes durable context inside the auto-memory markers only. All other notes stay editable.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                if (memoryUpdating || memoryStatus.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (memoryUpdating) "Updating memory…" else memoryStatus,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                if (memoryChangedWhileEditing) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Memory changed while you were editing. Reload the latest version before saving so new facts are not overwritten.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = memoryDraft,
                    onValueChange = {
                        memoryDraft = it
                        memoryDraftDirty = it != memoryDraftBase
                    },
                    minLines = 14,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        enabled = !memoryChangedWhileEditing,
                        onClick = {
                            viewModel.saveMemory(memoryDraft)
                            memoryDraftBase = memoryDraft
                            memoryDraftDirty = false
                        },
                    ) {
                        Text("Save memory")
                    }
                    if (memoryChangedWhileEditing) {
                        OutlinedButton(onClick = {
                            memoryDraft = memory
                            memoryDraftBase = memory
                            memoryDraftDirty = false
                            memoryChangedWhileEditing = false
                        }) {
                            Text("Reload latest")
                        }
                    }
                    OutlinedButton(
                        enabled = !memoryUpdating,
                        onClick = viewModel::updateTutorMemoryNow,
                    ) {
                        Text("Update now")
                    }
                }
            }
        }

        item {
            SettingsCard(
                icon = { Icon(Icons.Outlined.Translate, contentDescription = null) },
                title = "Service choices",
                subtitle = "Optimized for cost and privacy",
            ) {
                Text("Chat · DeepSeek V4 Flash (non-thinking mode)")
                Spacer(Modifier.height(6.dp))
                Text("Translation · Tencent Cloud TMT")
                Spacer(Modifier.height(6.dp))
                Text("Speech · Tencent Cloud TTS")
                Spacer(Modifier.height(6.dp))
                Text("OCR · On-device ML Kit")
                Spacer(Modifier.height(6.dp))
                Text(
                    "OCR runs locally. Only selected translation text, Tutor messages, and text you choose to play are sent to their configured services.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun DeepSeekBalancePanel(
    configured: Boolean,
    balance: DeepSeekBalance?,
    refreshing: Boolean,
    error: String?,
    updatedAt: Long?,
    onRefresh: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("DeepSeek API balance", fontWeight = FontWeight.SemiBold)
                    val availability = when {
                        balance == null -> null
                        balance.isAvailable -> "API calls available"
                        else -> "Insufficient balance"
                    }
                    availability?.let {
                        Text(
                            it,
                            color = if (balance?.isAvailable == true) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
                if (refreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    IconButton(onClick = onRefresh, enabled = configured) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh DeepSeek balance")
                    }
                }
            }

            when {
                !configured -> Text(
                    "Add a DeepSeek API key to view its balance.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )

                balance != null && balance.balances.isNotEmpty() -> {
                    balance.balances.forEachIndexed { index, info ->
                        if (index > 0) Spacer(Modifier.height(9.dp))
                        Text(
                            "${info.totalBalance.ifBlank { "—" }} ${info.currency.ifBlank { "Balance" }}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Topped up ${info.toppedUpBalance.ifBlank { "—" }} · " +
                                "Granted ${info.grantedBalance.ifBlank { "—" }}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                balance != null -> Text(
                    "DeepSeek returned no balance entries.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )

                refreshing -> Text(
                    "Checking the current balance…",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )

                error == null -> Text(
                    "Balance has not been checked yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            error?.let {
                Spacer(Modifier.height(7.dp))
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            updatedAt?.let {
                Spacer(Modifier.height(7.dp))
                Text(
                    "Last checked ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(it))}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SoftIndigo),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icon()
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun StatusLine(label: String, configured: Boolean) {
    Text(
        "$label · ${if (configured) "configured" else "not configured"}",
        color = if (configured) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelLarge,
    )
}
