package com.akinalpfdn.typewall.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akinalpfdn.typewall.model.Card
import com.akinalpfdn.typewall.model.SpanType
import com.akinalpfdn.typewall.viewmodel.CanvasViewModel
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import androidx.compose.ui.text.TextRange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardView(
    card: Card,
    scale: Float,
    viewModel: CanvasViewModel
) {
    val currentCard by rememberUpdatedState(card)
    var isFocused by remember { mutableStateOf(false) }
    var hasGainedFocus by remember { mutableStateOf(false) }
    var headerHeight by remember { mutableStateOf(0f) }

    val richTextState = rememberRichTextState()

    var titleFieldValue by remember(card.id) {
        mutableStateOf(TextFieldValue(text = card.title ?: ""))
    }

    // Sync content changes to ViewModel
    // KEY FIX: Listen to 'card.content' so we react when ViewModel restores old state
    LaunchedEffect(card.content) {
        if (richTextState.toHtml() != card.content) {
            richTextState.setHtml(card.content)
        }
    }

    LaunchedEffect(richTextState.annotatedString) {
        val currentHtml = richTextState.toHtml()
        if (currentHtml != card.content) {
            viewModel.updateCard(
                id = card.id,
                content = currentHtml,
                spans = emptyList(),
                saveHistory = false // Don't save history on every character type
            )
        }
    }

    // Sync Toolbar State & Handle Checkbox Taps
    LaunchedEffect(richTextState.selection, richTextState.annotatedString) {
        if (isFocused) {
            syncToolbarState(viewModel, richTextState)
            
            // Checkbox Toggle Logic (Safe)
            try {
                val selection = richTextState.selection
                if (selection.collapsed) {
                    val text = richTextState.annotatedString.text
                    val cursor = selection.min
                    
                    if (cursor > 0 && cursor <= text.length) {
                        // Find line start
                        val lineStart = text.lastIndexOf('\n', cursor - 1).let { if (it == -1) 0 else it + 1 }
                        
                        // Check if cursor is exactly at lineStart+1 or lineStart+2 (inside the 2-char box)
                        if (cursor >= lineStart && cursor <= lineStart + 2) {
                            val lineEnd = text.indexOf('\n', cursor).let { if (it == -1) text.length else it }
                            if (lineEnd >= lineStart + 2) {
                                val lineSub = text.substring(lineStart, lineStart + 2)
                                if (lineSub == "☐ " || lineSub == "☑ ") {
                                     if (cursor < lineStart + 2) {
                                         // Manual action (checkbox toggle) should save history
                                         viewModel.saveSnapshot(card.id)
                                         handleToolbarAction(SpanType.CHECKBOX, null, richTextState)
                                     }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore transient errors
            }
        }
    }

    LaunchedEffect(card.title) {
        if (titleFieldValue.text != (card.title ?: "")) {
            titleFieldValue = titleFieldValue.copy(text = card.title ?: "")
        }
    }

    val focusRequester = remember { FocusRequester() }
    val titleFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val isEmpty = card.content.isEmpty()
    val isGhost = isEmpty && !isFocused

    val cardBgColor = if (card.cardColor != null) Color(card.cardColor!!.toULong()) else MaterialTheme.colorScheme.surface
    val displayBgColor = if (isGhost) Color.Transparent else cardBgColor
    val borderColor = if (isGhost) Color.Transparent else MaterialTheme.colorScheme.outlineVariant
    val shadowElevation = if (isGhost) 0.dp else if (isFocused) 8.dp else 2.dp

    LaunchedEffect(Unit) {
        if (isEmpty) {
            focusRequester.requestFocus()
            delay(100)
            keyboardController?.show()
        }
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(card.x.roundToInt(), card.y.roundToInt()) }
            .requiredWidth(card.width.dp)
            .wrapContentHeight(Alignment.Top, unbounded = true)
            .shadow(shadowElevation, RoundedCornerShape(8.dp))
            .background(displayBgColor, RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
    ) {
        Column {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        headerHeight = coordinates.size.height.toFloat()
                    }
                    .background(
                        if (card.cardColor != null)
                            Color(card.cardColor!!.toULong()).copy(alpha = 0.1f)
                        else
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                        RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                    )
                    .pointerInput(Unit) {
                        var startX = 0f
                        var startY = 0f
                        var accumDragX = 0f
                        var accumDragY = 0f

                        detectDragGestures(
                            onDragStart = {
                                viewModel.saveSnapshot(currentCard.id) // Save state before drag starts
                                startX = currentCard.x
                                startY = currentCard.y
                                accumDragX = 0f
                                accumDragY = 0f
                                viewModel.activeCardId = currentCard.id // Set active on drag
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                accumDragX += dragAmount.x
                                accumDragY += dragAmount.y
                                viewModel.updateCard(
                                    id = currentCard.id,
                                    x = startX + accumDragX,
                                    y = startY + accumDragY,
                                    saveHistory = false // Continuous update
                                )
                            }
                        )
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BasicTextField(
                        value = titleFieldValue,
                        onValueChange = { newValue ->
                            // If title changes, checking if it's a new "edit session" is hard here
                            // We rely on focus listener to save snapshot before edits
                            titleFieldValue = newValue
                            viewModel.updateCard(id = card.id, title = newValue.text, saveHistory = false)
                        },
                        textStyle = MaterialTheme.typography.titleLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 28.sp // Explicit size override if needed, but family comes from theme
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(titleFocusRequester)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    viewModel.activeCardId = card.id
                                    viewModel.saveSnapshot(card.id) // Save state before title editing
                                }
                            },
                        decorationBox = { innerTextField ->
                            if (titleFieldValue.text.isEmpty()) {
                                Text(
                                    text = "Title",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    fontSize = 14.sp
                                )
                            }
                            innerTextField()
                        }
                    )

                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = "Drag",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp).padding(start = 8.dp)
                    )
                }
            }

            // Editor
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .pointerInput(isEmpty, isFocused) {
                        if (isEmpty || isFocused) {
                            detectTapGestures(
                                onTap = { offset ->
                                    viewModel.focusPointY = card.y + offset.y + (headerHeight.takeIf { it > 0 } ?: 60f)
                                    focusRequester.requestFocus()
                                    keyboardController?.show()
                                }
                            )
                        } else {
                            detectTapGestures(
                                onLongPress = { offset ->
                                    viewModel.focusPointY = card.y + offset.y + (headerHeight.takeIf { it > 0 } ?: 60f)
                                    focusRequester.requestFocus()
                                    keyboardController?.show()
                                },
                                onTap = {}
                            )
                        }
                    }
            ) {
                RichTextEditor(
                    state = richTextState,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            isFocused = focusState.isFocused

                            if (focusState.isFocused) {
                                viewModel.activeCardId = card.id
                                viewModel.saveSnapshot(card.id) // Save state before content editing
                                hasGainedFocus = true
                                keyboardController?.show()

                                viewModel.onApplyStyle = { type, value ->
                                    viewModel.saveSnapshot(card.id) // Save before applying style
                                    handleToolbarAction(type, value, richTextState)
                                }
                                viewModel.onInsertList = {
                                    viewModel.saveSnapshot(card.id) // Save before inserting list
                                    richTextState.toggleUnorderedList()
                                }
                                viewModel.onApplyCardColor = { color ->
                                    // Default updateCard saves history
                                    viewModel.updateCard(id = card.id, cardColor = color)
                                }

                                syncToolbarState(viewModel, richTextState)
                            } else {
                                if (viewModel.onApplyStyle != null) {
                                    viewModel.onApplyStyle = null
                                    viewModel.onInsertList = null
                                    viewModel.onApplyCardColor = null
                                    viewModel.activeStyles.clear()
                                }

                                if (hasGainedFocus) {
                                    viewModel.cleanupEmptyCard(card.id)
                                }
                            }
                        },
                    onTextLayout = { textLayoutResult ->
                        if (isFocused) {
                            val cursorIndex = richTextState.selection.end
                            // Ensure index is within bounds
                            val clampedIndex = cursorIndex.coerceIn(0, richTextState.annotatedString.length)
                            val cursorRect = textLayoutResult.getCursorRect(clampedIndex)
                            
                            // Calculate absolute Y position of the cursor
                            // Card Y + Header Height + Editor vertical Padding (12.dp) + Cursor Bottom
                            val paddingPx = 12 * scale * 2.5f // Approximate padding in px, or better use Density
                            // Actually, just use the card scale = 1f logic since we are passing scale.
                            // But wait, the card logic uses dp.
                            // Let's rely on standard density.
                            
                            // Safe fallback if headerHeight is 0
                            val currentHeaderH = if (headerHeight > 0) headerHeight else 150f 
                            
                            // Note: We need to convert 12.dp to pixels for precision, but let's approximate or just rely on the flow.
                            // Better: use LocalDensity
                            
                            val relativeCursorY = cursorRect.bottom 
                            val totalY = card.y + currentHeaderH + relativeCursorY + 30f // +30f buffer for padding
                            
                            viewModel.focusPointY = totalY
                        }
                    },
                    colors = RichTextEditorDefaults.richTextEditorColors(
                        containerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        textColor = MaterialTheme.colorScheme.onSurface,
                        placeholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    ),
                    placeholder = { Text("Type something...") }
                )

                if (!isEmpty && !isFocused) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = { offset ->
                                        viewModel.focusPointY = card.y + offset.y + 60f
                                        focusRequester.requestFocus()
                                        keyboardController?.show()
                                    },
                                    onTap = {}
                                )
                            }
                    )
                }
            }
        }

        if (isFocused) {
            var showDeleteDialog by remember { mutableStateOf(false) }

            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("Delete Card?") },
                    text = { Text("This card has content. Are you sure you want to delete it?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.removeCard(card.id)
                                showDeleteDialog = false
                            },
                             colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            IconButton(
                onClick = {
                    if (!card.isVisuallyEmpty()) {
                        showDeleteDialog = true
                    } else {
                        viewModel.removeCard(card.id)
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(32.dp)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Stretch Button
        val density = LocalDensity.current
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 8.dp)
                .size(32.dp, 48.dp)
                .pointerInput(density) {
                    var startWidth = 0f
                    var accumDragX = 0f

                    detectDragGestures(
                        onDragStart = {
                            viewModel.saveSnapshot(currentCard.id) // Save before resize
                            startWidth = currentCard.width
                            accumDragX = 0f
                            viewModel.activeCardId = currentCard.id // Set active on resize
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val dragAmountDp = dragAmount.x / density.density
                            accumDragX += dragAmountDp
                            viewModel.updateCard(
                                id = currentCard.id, 
                                width = startWidth + accumDragX,
                                saveHistory = false
                            )
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.size(6.dp, 24.dp),
                shape = RoundedCornerShape(3.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                shadowElevation = 2.dp
            ) {}
        }
    }
}

private fun handleToolbarAction(type: SpanType, value: String?, state: RichTextState) {
    when (type) {
        SpanType.BOLD -> state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
        SpanType.ITALIC -> state.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic))
        SpanType.UNDERLINE -> state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline))
        SpanType.STRIKETHROUGH -> state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))

        SpanType.ALIGN_LEFT -> state.toggleParagraphStyle(ParagraphStyle(textAlign = TextAlign.Left))
        SpanType.ALIGN_CENTER -> state.toggleParagraphStyle(ParagraphStyle(textAlign = TextAlign.Center))
        SpanType.ALIGN_RIGHT -> state.toggleParagraphStyle(ParagraphStyle(textAlign = TextAlign.Right))

        SpanType.CODE -> state.toggleCodeSpan()

        SpanType.QUOTE -> {
            state.toggleSpanStyle(SpanStyle(
                background = Color.Gray.copy(alpha = 0.2f),
                fontStyle = FontStyle.Italic
            ))
        }

        SpanType.LINK -> {
            if (value != null) {
                val selection = state.selection
                val selectedText = if (!selection.collapsed) {
                    state.annotatedString.text.substring(selection.min, selection.max)
                } else {
                    value
                }
                state.addLink(text = selectedText, url = value)
            }
        }

        SpanType.CHECKBOX -> {
            val text = state.annotatedString.text
            val selection = state.selection

            val lineStart = text.lastIndexOf('\n', selection.min - 1).let { if (it == -1) 0 else it + 1 }
            val lineEnd = text.indexOf('\n', selection.max).let { if (it == -1) text.length else it }
            val lineText = text.substring(lineStart, lineEnd)

            if (lineText.startsWith("☐ ")) {
                val current = state.annotatedString
                val prefix = current.subSequence(0, lineStart)
                val suffix = current.subSequence(lineStart + 2, current.length)
                val newContent = prefix + AnnotatedString("☑ ") + suffix

                updateRichTextState(state, newContent, androidx.compose.ui.text.TextRange(lineStart + 2, lineEnd))
                state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                
                state.selection = androidx.compose.ui.text.TextRange(lineEnd)

            } else if (lineText.startsWith("☑ ")) {
                val current = state.annotatedString
                val prefix = current.subSequence(0, lineStart)
                val suffix = current.subSequence(lineStart + 2, current.length)
                val newContent = prefix + AnnotatedString("☐ ") + suffix

                updateRichTextState(state, newContent, androidx.compose.ui.text.TextRange(lineStart + 2, lineEnd))
                state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                
                state.selection = androidx.compose.ui.text.TextRange(lineEnd)

            } else {
                val current = state.annotatedString
                val prefix = current.subSequence(0, lineStart)
                val suffix = current.subSequence(lineStart, current.length)
                val newContent = prefix + AnnotatedString("☐ ") + suffix

                updateRichTextState(state, newContent, androidx.compose.ui.text.TextRange(lineEnd + 2))
            }
        }

        SpanType.TEXT_COLOR -> {
            value?.toLongOrNull()?.let { colorLong ->
                state.toggleSpanStyle(SpanStyle(color = Color(colorLong.toULong())))
            }
        }

        SpanType.BG_COLOR -> {
            value?.toLongOrNull()?.let { colorLong ->
                state.toggleSpanStyle(SpanStyle(background = Color(colorLong.toULong())))
            }
        }

        SpanType.FONT_SIZE -> {
            value?.toFloatOrNull()?.let { size ->
                state.toggleSpanStyle(SpanStyle(fontSize = size.sp))
            }
        }
    }
}

private fun syncToolbarState(viewModel: CanvasViewModel, state: RichTextState) {
    viewModel.activeStyles.clear()
    val currentSpan = state.currentSpanStyle
    val currentParagraph = state.currentParagraphStyle

    if (currentSpan.fontWeight == FontWeight.Bold) viewModel.activeStyles[SpanType.BOLD] = null
    if (currentSpan.fontStyle == FontStyle.Italic) viewModel.activeStyles[SpanType.ITALIC] = null
    if (TextDecoration.Underline in (currentSpan.textDecoration ?: TextDecoration.None)) {
        viewModel.activeStyles[SpanType.UNDERLINE] = null
    }
    if (TextDecoration.LineThrough in (currentSpan.textDecoration ?: TextDecoration.None)) {
        viewModel.activeStyles[SpanType.STRIKETHROUGH] = null
    }

    when (currentParagraph.textAlign) {
        TextAlign.Left -> viewModel.activeStyles[SpanType.ALIGN_LEFT] = null
        TextAlign.Center -> viewModel.activeStyles[SpanType.ALIGN_CENTER] = null
        TextAlign.Right -> viewModel.activeStyles[SpanType.ALIGN_RIGHT] = null
        else -> {}
    }

    if (currentSpan.color != Color.Unspecified) {
        viewModel.activeStyles[SpanType.TEXT_COLOR] = currentSpan.color.value.toString()
    }
    if (currentSpan.background != Color.Unspecified) {
        viewModel.activeStyles[SpanType.BG_COLOR] = currentSpan.background.value.toString()
    }
    if (currentSpan.fontSize != TextUnit.Unspecified) {
        viewModel.activeStyles[SpanType.FONT_SIZE] = currentSpan.fontSize.value.toString()
    }
}

private fun updateRichTextState(state: RichTextState, content: AnnotatedString, selection: androidx.compose.ui.text.TextRange) {
    val newValue = TextFieldValue(content, selection)
    
    val methodNames = listOf("onTextFieldValueChange", "onValueChange", "updateTextFieldValue")
    for (name in methodNames) {
        try {
            val method = state::class.java.getDeclaredMethod(name, TextFieldValue::class.java)
            method.isAccessible = true
            method.invoke(state, newValue)
            return
        } catch (e: Exception) {
        }
    }

    try {
        val field = state::class.java.getDeclaredField("_textFieldValue")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val mutableState = field.get(state) as androidx.compose.runtime.MutableState<TextFieldValue>
        mutableState.value = newValue
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun safeSubsequence(text: String, start: Int, end: Int): String {
    if (start < 0 || end > text.length || start > end) return ""
    return text.substring(start, end)
}