package com.akinalpfdn.typewall.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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

    // DETECT CHECKLIST MODE
    // If the content starts with a checkbox marker, we switch to Native Checklist Mode
    val isChecklistMode = remember(card.content) {
        card.content.trimStart().startsWith("☐") || card.content.trimStart().startsWith("☑")
    }

    val richTextState = rememberRichTextState()
    var layoutResult by remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }
    var titleFieldValue by remember(card.id) {
        mutableStateOf(TextFieldValue(text = card.title ?: ""))
    }

    // Sync content changes to ViewModel (Only for RichText Mode)
    LaunchedEffect(card.content) {
        if (!isChecklistMode && richTextState.toHtml() != card.content) {
            richTextState.setHtml(card.content)
        }
    }

    LaunchedEffect(richTextState.annotatedString) {
        if (!isChecklistMode) {
            val currentHtml = richTextState.toHtml()
            if (currentHtml != card.content) {
                viewModel.updateCard(
                    id = card.id,
                    content = currentHtml,
                    spans = emptyList(),
                    saveHistory = false
                )
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
                                viewModel.saveSnapshot(currentCard.id)
                                startX = currentCard.x
                                startY = currentCard.y
                                accumDragX = 0f
                                accumDragY = 0f
                                viewModel.activeCardId = currentCard.id
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                accumDragX += dragAmount.x
                                accumDragY += dragAmount.y
                                viewModel.updateCard(
                                    id = currentCard.id,
                                    x = startX + accumDragX,
                                    y = startY + accumDragY,
                                    saveHistory = false
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
                            viewModel.updateCard(id = card.id, title = newValue.text, saveHistory = false)
                        },
                        textStyle = MaterialTheme.typography.titleLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 28.sp
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(titleFocusRequester)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    viewModel.activeCardId = card.id
                                    viewModel.saveSnapshot(card.id)
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
            ) {
                if (isChecklistMode) {
                    // --- SAMSUNG STYLE CHECKLIST MODE ---
                    ChecklistEditor(
                        content = card.content,
                        onContentChange = { newContent ->
                            // Directly update content string (format: ☐ Item\n☑ Item)
                            viewModel.updateCard(card.id, content = newContent, saveHistory = false)
                        },
                        viewModel = viewModel, // Pass the VM
                        cardId = card.id,
                        onFocus = {
                            // This runs when ANY checklist item gets clicked
                            isFocused = true
                            hasGainedFocus = true
                        }

                    )
                } else {
                    // --- STANDARD RICH TEXT MODE ---
                    // Wrapping in Box for pointer input to coexist with RichTextEditor
                    Box(
                        modifier = Modifier.pointerInput(isEmpty, isFocused) {
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
                                    }
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
                                        viewModel.saveSnapshot(card.id)
                                        hasGainedFocus = true
                                        keyboardController?.show()

                                        viewModel.onApplyStyle = { type, value ->
                                            if (type == SpanType.CHECKBOX) {
                                                // SWITCH TO CHECKLIST MODE
                                                // Convert current text to lines prefixed with ☐
                                                val text = richTextState.annotatedString.text
                                                val checklistContent = if (text.isEmpty()) "☐ " else text.lines().joinToString("\n") {
                                                    if (it.startsWith("☐ ") || it.startsWith("☑ ")) it else "☐ $it"
                                                }
                                                viewModel.updateCard(card.id, content = checklistContent)
                                            } else {
                                                viewModel.saveSnapshot(card.id)
                                                handleToolbarAction(type, value, richTextState)
                                            }
                                        }
                                        viewModel.onInsertList = {
                                            viewModel.saveSnapshot(card.id)
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
                            onTextLayout = { textLayoutResult ->
                                layoutResult = textLayoutResult
                                if (isFocused) {
                                    val cursorIndex = richTextState.selection.end
                                    val clampedIndex = cursorIndex.coerceIn(0, richTextState.annotatedString.length)
                                    val cursorRect = textLayoutResult.getCursorRect(clampedIndex)
                                    val currentHeaderH = if (headerHeight > 0) headerHeight else 150f
                                    val relativeCursorY = cursorRect.bottom
                                    val totalY = card.y + currentHeaderH + relativeCursorY + 30f
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
                    }
                }

                if (!isEmpty && !isFocused && !isChecklistMode) {
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
                        ) { Text("Delete") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
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
                            viewModel.saveSnapshot(currentCard.id)
                            startWidth = currentCard.width
                            accumDragX = 0f
                            viewModel.activeCardId = currentCard.id
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

// --- CHECKLIST IMPLEMENTATION ---

// --- SAMSUNG STYLE CHECKLIST MODE ---

// --- SAMSUNG STYLE CHECKLIST MODE ---

data class ChecklistItemData(
    val id: String,
    val isChecked: Boolean,
    val text: String
)

@Composable
fun ChecklistEditor(
    content: String,
    onContentChange: (String) -> Unit,
    viewModel: CanvasViewModel,
    cardId: String,
    onFocus: () -> Unit
) {
    // 1. Parse content
    val initialItems = remember(content) {
        val lines = content.split('\n')
        lines.mapIndexed { index, line ->
            val isChecked = line.startsWith("☑")
            val cleanHtml = line.removePrefix("☐ ").removePrefix("☑ ")
            ChecklistItemData(index.toString(), isChecked, cleanHtml)
        }
    }

    val items = remember { mutableStateListOf<ChecklistItemData>().apply { addAll(initialItems) } }

    fun saveAll() {
        val fullContent = items.joinToString("\n") { item ->
            val prefix = if (item.isChecked) "☑ " else "☐ "
            prefix + item.text
        }
        onContentChange(fullContent)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        items.forEachIndexed { index, item ->
            key(item.id) {
                ChecklistRowItem(
                    initialHtml = item.text,
                    isChecked = item.isChecked,
                    onCheckedChange = { checked ->
                        items[index] = items[index].copy(isChecked = checked)
                        saveAll()
                    },
                    onHtmlChange = { newHtml ->
                        items[index] = items[index].copy(text = newHtml)
                        saveAll()
                    },
                    onDelete = {
                        if (items.size > 1) {
                            items.removeAt(index)
                            saveAll()
                        }
                    },
                    onEnter = {
                        val newItem = ChecklistItemData(
                            id = System.currentTimeMillis().toString() + index,
                            isChecked = false,
                            text = ""
                        )
                        items.add(index + 1, newItem)
                        saveAll()
                    },
                    viewModel = viewModel,
                    cardId = cardId,
                    isLastItem = index == items.lastIndex,
                    onFocus = onFocus
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChecklistRowItem(
    initialHtml: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onHtmlChange: (String) -> Unit,
    onDelete: () -> Unit,
    onEnter: () -> Unit,
    viewModel: CanvasViewModel,
    cardId: String,
    isLastItem: Boolean,
    onFocus: () -> Unit
) {
    val richTextState = rememberRichTextState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { richTextState.setHtml(initialHtml) }

    // Auto-focus new empty items
    LaunchedEffect(Unit) {
        if (initialHtml.isEmpty()) {
            delay(50)
            focusRequester.requestFocus()
        }
    }

    Row(
        // FIX 1: Align Top to prevent centering when text is multiline
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            ),
            modifier = Modifier
                .size(32.dp)
                // FIX 1.1: Push checkbox down slightly to match text baseline
                .padding(top = 8.dp)
        )

        Box(modifier = Modifier.weight(1f)) {
            RichTextEditor(
                state = richTextState,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = if (isChecked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None,
                    lineHeight = 24.sp // Enforce line height for better alignment
                ),
                // FIX 2: Handle Soft Keyboard "Enter" action
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = {
                        onEnter()
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            onFocus()
                            viewModel.activeCardId = cardId
                            viewModel.onApplyStyle = { type, value ->
                                handleToolbarAction(type, value, richTextState)
                                onHtmlChange(richTextState.toHtml())
                            }
                            viewModel.onApplyCardColor = { color ->
                                viewModel.updateCard(id = cardId, cardColor = color)
                            }
                            syncToolbarState(viewModel, richTextState)
                        }
                    }
                    // FIX 3: Handle Hardware/Software Backspace and Enter
                    .onKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown) {
                            if (event.key == Key.Backspace && richTextState.annotatedString.isEmpty()) {
                                onDelete()
                                true
                            } else if (event.key == Key.Enter) {
                                onEnter()
                                true
                            } else {
                                false
                            }
                        } else {
                            false
                        }
                    },
                colors = RichTextEditorDefaults.richTextEditorColors(
                    containerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    textColor = MaterialTheme.colorScheme.onSurface,
                    placeholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                ),
                placeholder = { if (isLastItem) Text("List item...") }
            )
        }
    }

    // Safety check: if user managed to insert a newline manually, trigger enter
    LaunchedEffect(richTextState.annotatedString) {
        val currentHtml = richTextState.toHtml()
        if (richTextState.annotatedString.text.contains('\n')) {
            // Strip the newline and trigger enter
            val cleanText = richTextState.annotatedString.text.replace("\n", "")
            richTextState.setText(cleanText)
            onEnter()
        } else if (currentHtml != initialHtml) {
            onHtmlChange(currentHtml)
        }
    }
}

private fun serializeChecklist(items: List<ChecklistItemData>): String {
    return items.joinToString("\n") { item ->
        val prefix = if (item.isChecked) "☑ " else "☐ "
        prefix + item.text
    }
}

// ... (Rest of existing helper functions like handleToolbarAction, getParagraphBounds etc.)
// KEEP EXISTING HELPER FUNCTIONS HERE
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
        SpanType.QUOTE -> state.toggleSpanStyle(SpanStyle(background = Color.Gray.copy(alpha = 0.2f), fontStyle = FontStyle.Italic))
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
        // Checkbox is now handled via mode switching, but we keep this for legacy or mixed text support
        SpanType.CHECKBOX -> { /* Handled in lambda now */ }
        SpanType.TEXT_COLOR -> value?.toLongOrNull()?.let { state.toggleSpanStyle(SpanStyle(color = Color(it.toULong()))) }
        SpanType.BG_COLOR -> value?.toLongOrNull()?.let { state.toggleSpanStyle(SpanStyle(background = Color(it.toULong()))) }
        SpanType.FONT_SIZE -> value?.toFloatOrNull()?.let { state.toggleSpanStyle(SpanStyle(fontSize = it.sp)) }
    }
}

private fun getParagraphBounds(state: RichTextState): Pair<Int, Int> {
    val text = state.annotatedString.text
    val selection = state.selection
    val cursor = selection.min
    val paragraph = state.annotatedString.paragraphStyles.firstOrNull { style -> cursor >= style.start && cursor <= style.end }
    if (paragraph != null) return paragraph.start to paragraph.end
    val start = text.lastIndexOf('\n', cursor - 1).let { if (it == -1) 0 else it + 1 }
    val end = text.indexOf('\n', cursor).let { if (it == -1) text.length else it }
    return start to end
}

private fun syncToolbarState(viewModel: CanvasViewModel, state: RichTextState) {
    viewModel.activeStyles.clear()
    val currentSpan = state.currentSpanStyle
    val currentParagraph = state.currentParagraphStyle
    if (currentSpan.fontWeight == FontWeight.Bold) viewModel.activeStyles[SpanType.BOLD] = null
    if (currentSpan.fontStyle == FontStyle.Italic) viewModel.activeStyles[SpanType.ITALIC] = null
    if (TextDecoration.Underline in (currentSpan.textDecoration ?: TextDecoration.None)) viewModel.activeStyles[SpanType.UNDERLINE] = null
    if (TextDecoration.LineThrough in (currentSpan.textDecoration ?: TextDecoration.None)) viewModel.activeStyles[SpanType.STRIKETHROUGH] = null
    when (currentParagraph.textAlign) {
        TextAlign.Left -> viewModel.activeStyles[SpanType.ALIGN_LEFT] = null
        TextAlign.Center -> viewModel.activeStyles[SpanType.ALIGN_CENTER] = null
        TextAlign.Right -> viewModel.activeStyles[SpanType.ALIGN_RIGHT] = null
        else -> {}
    }
    if (currentSpan.color != Color.Unspecified) viewModel.activeStyles[SpanType.TEXT_COLOR] = currentSpan.color.value.toString()
    if (currentSpan.background != Color.Unspecified) viewModel.activeStyles[SpanType.BG_COLOR] = currentSpan.background.value.toString()
    if (currentSpan.fontSize != TextUnit.Unspecified) viewModel.activeStyles[SpanType.FONT_SIZE] = currentSpan.fontSize.value.toString()
}

private fun updateRichTextState(state: RichTextState, content: AnnotatedString, selection: androidx.compose.ui.text.TextRange) {
    // Keep existing reflection hacks if needed
    val newValue = TextFieldValue(content, selection)
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