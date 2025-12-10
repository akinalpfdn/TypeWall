package com.akinalpfdn.typewall.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle // IMPORTANT IMPORT
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
// Add this helper at the bottom of CardView.kt
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

    val richTextState = rememberRichTextState()

    var titleFieldValue by remember(card.id) {
        mutableStateOf(TextFieldValue(text = card.title ?: ""))
    }

    // Load initial content
    LaunchedEffect(card.id) {
        if (richTextState.toHtml() != card.content) {
            richTextState.setHtml(card.content)
        }
    }

    // Sync content changes to ViewModel
    LaunchedEffect(richTextState.annotatedString) {
        val currentHtml = richTextState.toHtml()
        if (currentHtml != card.content) {
            viewModel.updateCard(
                id = card.id,
                content = currentHtml,
                spans = emptyList()
            )
        }
    }

    // Sync Toolbar State
    LaunchedEffect(richTextState.selection, richTextState.annotatedString) {
        if (isFocused) {
            syncToolbarState(viewModel, richTextState)
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
            .width(card.width.dp)
            .shadow(shadowElevation, RoundedCornerShape(8.dp))
            .background(displayBgColor, RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
    ) {
        Column {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
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
                                startX = currentCard.x
                                startY = currentCard.y
                                accumDragX = 0f
                                accumDragY = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                accumDragX += dragAmount.x
                                accumDragY += dragAmount.y
                                viewModel.updateCard(
                                    id = currentCard.id,
                                    x = startX + accumDragX,
                                    y = startY + accumDragY
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
                            titleFieldValue = newValue
                            viewModel.updateCard(id = card.id, title = newValue.text)
                        },
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(titleFocusRequester),
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
                                    viewModel.focusPointY = card.y + offset.y + 60f
                                    focusRequester.requestFocus()
                                    keyboardController?.show()
                                }
                            )
                        } else {
                            detectTapGestures(
                                onLongPress = { offset ->
                                    viewModel.focusPointY = card.y + offset.y + 60f
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            isFocused = focusState.isFocused

                            if (focusState.isFocused) {
                                hasGainedFocus = true
                                keyboardController?.show()

                                viewModel.onApplyStyle = { type, value ->
                                    handleToolbarAction(type, value, richTextState)
                                }
                                viewModel.onInsertList = {
                                    richTextState.toggleUnorderedList()
                                }
                                viewModel.onApplyCardColor = { color ->
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
            IconButton(
                onClick = { viewModel.removeCard(card.id) },
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

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .matchParentSize()
                .width(20.dp)
                .offset(x = 10.dp)
                .pointerInput(Unit) {
                    var startWidth = 0f
                    var accumDragX = 0f

                    detectDragGestures(
                        onDragStart = {
                            startWidth = currentCard.width
                            accumDragX = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            accumDragX += dragAmount.x
                            viewModel.updateCard(id = currentCard.id, width = startWidth + accumDragX)
                        }
                    )
                }
        )
    }
}

// Helper Functions internal to CardView
// Place this at the bottom of CardView.kt, replacing the existing helpers

// ... other imports

// 2. MAIN HANDLER: The complete function with all cases
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

            // Find start of the current line
            val lineStart = text.lastIndexOf('\n', selection.min - 1).let { if (it == -1) 0 else it + 1 }
            // Find end of the current line
            val lineEnd = text.indexOf('\n', selection.max).let { if (it == -1) text.length else it }

            val lineText = text.substring(lineStart, lineEnd)

            if (lineText.startsWith("☐ ")) {
                // CASE: Unchecked -> Checked (☑)

                // Select "☐ "
                state.selection = androidx.compose.ui.text.TextRange(lineStart, lineStart + 2)
                // Replace with "☑ "
                state.safeInsert("☑ ")

                // Select rest of line and apply Strikethrough
                state.selection = androidx.compose.ui.text.TextRange(lineStart + 2, lineEnd)
                state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))

                // Reset cursor to end of line
                state.selection = androidx.compose.ui.text.TextRange(lineEnd)

            } else if (lineText.startsWith("☑ ")) {
                // CASE: Checked -> Unchecked (☐)

                // Select "☑ "
                state.selection = androidx.compose.ui.text.TextRange(lineStart, lineStart + 2)
                // Replace with "☐ "
                state.safeInsert("☐ ")

                // Select rest of line and remove Strikethrough
                state.selection = androidx.compose.ui.text.TextRange(lineStart + 2, lineEnd)
                state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))

                // Reset cursor
                state.selection = androidx.compose.ui.text.TextRange(lineEnd)

            } else {
                // CASE: New Checkbox

                // Move to start of line
                state.selection = androidx.compose.ui.text.TextRange(lineStart)
                // Insert box
                state.safeInsert("☐ ")
                // Move cursor to end (accounting for added chars)
                state.selection = androidx.compose.ui.text.TextRange(lineEnd + 2)
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

    // Toggles
    if (currentSpan.fontWeight == FontWeight.Bold) viewModel.activeStyles[SpanType.BOLD] = null
    if (currentSpan.fontStyle == FontStyle.Italic) viewModel.activeStyles[SpanType.ITALIC] = null
    if (TextDecoration.Underline in (currentSpan.textDecoration ?: TextDecoration.None)) {
        viewModel.activeStyles[SpanType.UNDERLINE] = null
    }
    if (TextDecoration.LineThrough in (currentSpan.textDecoration ?: TextDecoration.None)) {
        viewModel.activeStyles[SpanType.STRIKETHROUGH] = null
    }

    // Alignment
    when (currentParagraph.textAlign) {
        TextAlign.Left -> viewModel.activeStyles[SpanType.ALIGN_LEFT] = null
        TextAlign.Center -> viewModel.activeStyles[SpanType.ALIGN_CENTER] = null
        TextAlign.Right -> viewModel.activeStyles[SpanType.ALIGN_RIGHT] = null
        else -> {}
    }

    // Values
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
// Helper to bypass internal restrictions and preserve styles
private fun RichTextState.safeInsert(text: String) {
    try {
        val current = this.annotatedString
        val selection = this.selection

        // 1. Construct new AnnotatedString (preserves existing spans)
        val prefix = current.subSequence(0, selection.min)
        val suffix = current.subSequence(selection.max, current.length)
        val newContent = prefix + AnnotatedString(text) + suffix

        // 2. Calculate new cursor position
        val newSelection = androidx.compose.ui.text.TextRange(selection.min + text.length)

        // 3. Use Reflection to call the internal update method
        val method = this::class.java.getDeclaredMethod("onTextFieldValueChange", TextFieldValue::class.java)
        method.isAccessible = true
        method.invoke(this, TextFieldValue(newContent, newSelection))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}