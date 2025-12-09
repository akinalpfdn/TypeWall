package com.akinalpfdn.typewall.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RichCardEditor(
    state: RichTextState,
    initialContent: String,
    isEditable: Boolean,
    onContentChanged: (String) -> Unit
) {
    // 1. Load initial content when the component mounts
    LaunchedEffect(Unit) {
        // We assume content is stored as HTML.
        // If it's a new card, this might be empty.
        state.setHtml(initialContent)
    }

    // 2. Sync changes back to the parent (Card Data)
    // RichTextEditor doesn't have a direct "onValueChange" for string content,
    // so we assume the parent saves state.toHtml() when focus is lost or periodically.

    RichTextEditor(
        state = state,
        readOnly = !isEditable,
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        colors = RichTextEditorDefaults.richTextEditorColors(
            containerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            textColor = Color.Black // Or dynamic based on theme
        ),
        placeholder = {
            // Only show placeholder if not read-only
            if (isEditable) androidx.compose.material3.Text("Type here...")
        }
    )
}