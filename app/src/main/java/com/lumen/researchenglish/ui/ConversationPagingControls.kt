package com.lumen.researchenglish.ui

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** Compact page-by-page navigation shared by both Tutor conversations. */
@Composable
internal fun ConversationPagingControls(
    listState: LazyListState,
    onPagingAwayFromLatest: () -> Unit,
    onReturnToLatest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canPageUp by remember(listState) { derivedStateOf { listState.canScrollBackward } }
    val canPageDown by remember(listState) { derivedStateOf { listState.canScrollForward } }
    val scope = rememberCoroutineScope()

    if (!canPageUp && !canPageDown) return

    fun pageDistance(): Float {
        val layout = listState.layoutInfo
        return ((layout.viewportEndOffset - layout.viewportStartOffset) * 0.82f)
            .coerceAtLeast(1f)
    }

    Surface(
        modifier = modifier.width(68.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
                enabled = canPageUp,
                onClick = {
                    onPagingAwayFromLatest()
                    scope.launch { listState.animateScrollBy(-pageDistance()) }
                },
            ) {
                Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = "Previous Tutor screen")
            }
            HorizontalDivider(modifier = Modifier.width(34.dp))
            IconButton(
                enabled = canPageDown,
                onClick = {
                    scope.launch { listState.animateScrollBy(pageDistance()) }
                },
            ) {
                Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Next Tutor screen")
            }
            if (canPageDown) {
                HorizontalDivider(modifier = Modifier.width(34.dp))
                TextButton(
                    contentPadding = PaddingValues(horizontal = 6.dp),
                    onClick = {
                        onReturnToLatest()
                        scope.launch {
                            val target = listState.layoutInfo.totalItemsCount - 1
                            if (target >= 0) listState.animateScrollToItem(target)
                        }
                    },
                ) {
                    Text("Latest", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
