package com.lumen.researchenglish.ui

import android.graphics.Color as AndroidColor
import android.graphics.Typeface
import android.view.Gravity
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun SelectableActionText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color,
    fontSize: TextUnit,
    bold: Boolean = false,
    centered: Boolean = false,
    onRead: (String) -> Unit,
    onTranslate: (String) -> Unit,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextView(context).apply {
                setTextIsSelectable(true)
                setBackgroundColor(AndroidColor.TRANSPARENT)
                includeFontPadding = false
                setLineSpacing(0f, 1.12f)
            }
        },
        update = { view ->
            if (view.text.toString() != text) view.text = text
            view.setTextColor(color.toArgb())
            view.textSize = fontSize.value
            view.setTypeface(null, if (bold) Typeface.BOLD else Typeface.NORMAL)
            view.gravity = if (centered) Gravity.CENTER_HORIZONTAL else Gravity.START
            view.customSelectionActionModeCallback = selectedTextActionMode(
                view = view,
                onRead = onRead,
                onTranslate = onTranslate,
            )
        },
    )
}

@Composable
fun InlineDirectTranslation(
    state: DirectTranslationUiState?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    state ?: return
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.52f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(start = 11.dp, top = 8.dp, bottom = 8.dp, end = 3.dp),
        ) {
            if (state.loading) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                Text("正在直译…", style = MaterialTheme.typography.bodySmall)
            } else {
                Text(
                    state.translation.ifBlank { state.error ?: "没有得到翻译结果。" },
                    color = if (state.error == null) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.width(2.dp))
            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Outlined.Close, "关闭直译", modifier = Modifier.size(16.dp))
            }
        }
    }
}

private fun selectedTextActionMode(
    view: TextView,
    onRead: (String) -> Unit,
    onTranslate: (String) -> Unit,
): ActionMode.Callback = object : ActionMode.Callback {
    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        menu ?: return false
        menu.add(Menu.NONE, READ_ACTION, 10, "Read")
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        menu.add(Menu.NONE, TRANSLATE_ACTION, 11, "直接翻译")
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = false

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        val start = minOf(view.selectionStart, view.selectionEnd).coerceAtLeast(0)
        val end = maxOf(view.selectionStart, view.selectionEnd).coerceAtMost(view.text.length)
        val selected = view.text.subSequence(start, end).toString().trim()
        if (selected.isBlank()) return false
        return when (item?.itemId) {
            READ_ACTION -> true.also {
                onRead(selected)
                mode?.finish()
            }
            TRANSLATE_ACTION -> true.also {
                onTranslate(selected)
                mode?.finish()
            }
            else -> false
        }
    }

    override fun onDestroyActionMode(mode: ActionMode?) = Unit
}

private const val READ_ACTION = 0x4C554D01
private const val TRANSLATE_ACTION = 0x4C554D02
