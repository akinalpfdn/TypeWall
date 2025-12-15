package com.akinalpfdn.typewall.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akinalpfdn.typewall.model.SpanType
import com.akinalpfdn.typewall.viewmodel.CanvasViewModel
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.BasicRichTextEditor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ---------------------------------------------------------
// COMPONENT: CHECKLIST EDITOR
// ---------------------------------------------------------

data class HybridItemData(
    val id: String,
    val type: RowType,
    val text: String,
    val isChecked: Boolean = false
)

enum class RowType {
    CHECKBOX, BULLET, TEXT
}

@Composable
fun ChecklistEditor(
    content: String,
    onContentChange: (String) -> Unit,
    viewModel: CanvasViewModel,
    cardId: String,
    onFocus: () -> Unit,
    onBlur: () -> Unit,
    isCardFocused: Boolean,
    onRequestFocus: () -> Unit,
    onScrollRequest: (Float) -> Unit
) {
    // Parse content into Hybrid Items
    val initialItems = remember(content) {
        val lines = content.split('\n')
        lines.mapIndexed { index, line ->
            when {
                line.startsWith("☑") -> {
                    val cleanHtml = line.removePrefix("☑ ")
                    HybridItemData(index.toString(), RowType.CHECKBOX, cleanHtml, isChecked = true)
                }
                line.startsWith("☐") -> {
                    val cleanHtml = line.removePrefix("☐ ")
                    HybridItemData(index.toString(), RowType.CHECKBOX, cleanHtml, isChecked = false)
                }
                line.startsWith("•") -> {
                    val cleanHtml = line.removePrefix("• ")
                    HybridItemData(index.toString(), RowType.BULLET, cleanHtml)
                }
                else -> {
                    HybridItemData(index.toString(), RowType.TEXT, line)
                }
            }
        }
    }

    val items = remember { mutableStateListOf<HybridItemData>().apply { addAll(initialItems) } }
    val focusRequesters = remember { mutableStateMapOf<String, FocusRequester>() }

    fun saveAll() {
        val fullContent = items.joinToString("\n") { item ->
            when (item.type) {
                RowType.CHECKBOX -> {
                    val prefix = if (item.isChecked) "☑ " else "☐ "
                    prefix + item.text
                }
                RowType.BULLET -> "• " + item.text
                RowType.TEXT -> item.text
            }
        }
        onContentChange(fullContent)
    }

    var parentCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { parentCoordinates = it }
    ) {
        items.forEachIndexed { index, item ->
            val itemFocusRequester = focusRequesters.getOrPut(item.id) { FocusRequester() }

            key(item.id) {
                HybridRowItem(
                    initialHtml = item.text,
                    itemType = item.type,
                    isChecked = item.isChecked,
                    focusRequester = itemFocusRequester,
                    onTypeChange = { newType ->
                        items[index] = items[index].copy(type = newType)
                        saveAll()
                    },
                    onCheckedChange = { checked ->
                        items[index] = items[index].copy(isChecked = checked)
                        saveAll()
                    },
                    onHtmlChange = { newHtml ->
                        items[index] = items[index].copy(text = newHtml)
                        saveAll()
                    },
                    onBackspace = {
                        // Logic: If previous item exists, jump to it. Else if type is special, revert to text.
                        if (items.size > 1) {
                            val jumpToId = if (index > 0) items[index - 1].id else items[index + 1].id
                            items.removeAt(index)
                            saveAll()
                            focusRequesters[jumpToId]?.requestFocus()
                        } else {
                            if (item.type != RowType.TEXT) {
                                items[index] = items[index].copy(type = RowType.TEXT)
                                saveAll()
                            }
                        }
                    },
                    onEnter = { isTextEmpty ->
                        if ((item.type == RowType.CHECKBOX || item.type == RowType.BULLET) && isTextEmpty) {
                            // Enter on Empty Bullet/Check -> Convert to Plain Text (in-place)
                            // This matches "Empty Checkbox + Enter => make row normal richtext"
                            items[index] = items[index].copy(type = RowType.TEXT)
                            saveAll()
                        } else {
                            // Standard Enter -> Create New Row of same type
                            val newItem = HybridItemData(
                                id = System.currentTimeMillis().toString() + index,
                                type = item.type,
                                text = ""
                            )
                            items.add(index + 1, newItem)
                            saveAll()
                        }
                    },
                    viewModel = viewModel,
                    cardId = cardId,
                    onFocus = onFocus,
                    onBlur = onBlur,
                    isCardFocused = isCardFocused,
                    onRequestFocus = onRequestFocus,
                    parentCoordinates = parentCoordinates,
                    onScrollRequest = onScrollRequest
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HybridRowItem(
    initialHtml: String,
    itemType: RowType,
    isChecked: Boolean,
    focusRequester: FocusRequester,
    onTypeChange: (RowType) -> Unit,
    onCheckedChange: (Boolean) -> Unit,
    onHtmlChange: (String) -> Unit,
    onBackspace: () -> Unit,
    onEnter: (Boolean) -> Unit,
    viewModel: CanvasViewModel,
    cardId: String,
    onFocus: () -> Unit,
    onBlur: () -> Unit,
    isCardFocused: Boolean,
    onRequestFocus: () -> Unit,
    parentCoordinates: LayoutCoordinates?,
    onScrollRequest: (Float) -> Unit
) {
    val richTextState = rememberRichTextState()
    val ZWSP = "\u200B"
    // Track previous text to detect specific deletions (like deleting the ZWSP)
    var lastTextValue by remember { mutableStateOf<String?>(null) }
    var myCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val scope = rememberCoroutineScope()
    var currentLayoutResult by remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }
    var isLocallyFocused by remember { mutableStateOf(false) }

    val updateScrollPosition = {
        val layoutResult = currentLayoutResult
        if (layoutResult != null && isCardFocused && parentCoordinates != null && myCoordinates != null &&
            parentCoordinates.isAttached && myCoordinates!!.isAttached) {
            val cursorIndex = richTextState.selection.end
            val clampedIndex = cursorIndex.coerceIn(0, richTextState.annotatedString.length)
            val cursorRect = layoutResult.getCursorRect(clampedIndex)

            // Calculate relative Y offset for scrolling
            val rowOffset = parentCoordinates.localPositionOf(myCoordinates!!, androidx.compose.ui.geometry.Offset.Zero).y
            val relativeCursorY = cursorRect.bottom + rowOffset
            onScrollRequest(relativeCursorY)
        }
    }

    LaunchedEffect(Unit) {
        if (initialHtml.isEmpty()) {
            richTextState.setText(ZWSP)
            lastTextValue = ZWSP
        } else {
            richTextState.setHtml(initialHtml)
            lastTextValue = richTextState.annotatedString.text
        }
    }

    LaunchedEffect(Unit) {
        if (initialHtml.isEmpty()) {
            delay(50)
            focusRequester.requestFocus()
        }
    }

    // Keep ViewModel callbacks fresh with latest itemType
    LaunchedEffect(isLocallyFocused, itemType) {
        if (isLocallyFocused) {
            viewModel.activeCardId = cardId
            viewModel.onInsertList = {
                val newType = if (itemType == RowType.BULLET) RowType.TEXT else RowType.BULLET
                onTypeChange(newType)
            }
            
            // Handle Style Applications (including Checkbox toggle) with fresh itemType closure
            viewModel.onApplyStyle = { type, value ->
                if (type == SpanType.CHECKBOX) {
                    val newType = if (itemType == RowType.CHECKBOX) RowType.TEXT else RowType.CHECKBOX
                    onTypeChange(newType)
                } else {
                    handleToolbarAction(type, value, richTextState)
                    onHtmlChange(richTextState.toHtml())
                }
            }
            
            // Sync toolbar state including custom Checkbox status
            syncToolbarState(viewModel, richTextState)
            if (itemType == RowType.CHECKBOX) {
                viewModel.activeStyles[SpanType.CHECKBOX] = "true"
            } else {
                viewModel.activeStyles.remove(SpanType.CHECKBOX)
            }
            if (itemType == RowType.BULLET) { 
                // Optionally highlight bullet icon? (Standard toolbar doesn't track this span-wise usually, but consistent behavior is good)
            }
        }
    }

    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { myCoordinates = it }
    ) {
        // --- Marker Column (Checkbox / Bullet) ---
        Box(
            modifier = Modifier
                .padding(top = 0.dp, end = 8.dp)
                .size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            when (itemType) {
                RowType.CHECKBOX -> {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = onCheckedChange,
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            uncheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.size(14.dp)
                    )
                }
                RowType.BULLET -> {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                RowType.TEXT -> { /* Empty */ }
            }
        }

        // --- Text Editor Column ---
        Box(modifier = Modifier.weight(1f)) {
            BasicRichTextEditor(
                state = richTextState,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = if (isChecked && itemType == RowType.CHECKBOX)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    else
                        MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (isChecked && itemType == RowType.CHECKBOX)
                        TextDecoration.LineThrough
                    else
                        TextDecoration.None
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = {
                        val text = richTextState.annotatedString.text
                        onEnter(text.isEmpty() || text == ZWSP)
                    }
                ),
                enabled = isCardFocused,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        isLocallyFocused = focusState.isFocused
                        if (focusState.isFocused) {
                            onFocus()
                            viewModel.activeCardId = cardId

                            // Link ViewModel actions to this specific editor instance
                            // onApplyStyle is handled in LaunchedEffect to capture dynamic itemType

                            viewModel.onApplyCardColor = { color ->
                                viewModel.updateCard(id = cardId, cardColor = color)
                            }
                            // onInsertList is handled in LaunchedEffect to capture dynamic itemType

                            syncToolbarState(viewModel, richTextState)
                            // Trigger scroll to cursor on focus gain
                            updateScrollPosition()
                        } else {
                            onBlur()
                        }
                    }
                    .onKeyEvent { event ->
                        // Hardware keyboard support
                        if (event.type == KeyEventType.KeyDown && event.key == Key.Delete) {
                            val text = richTextState.annotatedString.text
                            if (text.isEmpty() || text == ZWSP) {
                                onBackspace()
                                true
                            } else false
                        } else if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                            val text = richTextState.annotatedString.text
                            onEnter(text.isEmpty() || text == ZWSP)
                            true
                        } else {
                            false
                        }
                    },
                onTextLayout = { textLayoutResult ->
                    currentLayoutResult = textLayoutResult
                    updateScrollPosition()
                }
            )

            // Touch Overlay for View Mode
            if (!isCardFocused) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = {
                                    onRequestFocus()
                                    // Wait for recomposition to enable the editor
                                    scope.launch {
                                        delay(50) 
                                        focusRequester.requestFocus()
                                    }
                                },
                                onTap = { /* Swallow tap */ }
                            )
                        }
                )
            }
        }
    }

    // --- Logic Loop for ZWSP (Zero Width Space) & Updates ---
    LaunchedEffect(richTextState.annotatedString) {
        val currentText = richTextState.annotatedString.text

        if (lastTextValue != null && currentText == lastTextValue) return@LaunchedEffect

        // 1. Handle Backspace on Empty (User deleted ZWSP)
        if (currentText.isEmpty()) {
            if (lastTextValue == ZWSP) {
                onBackspace()
            } else {
                // User simply cleared text -> Restore ZWSP to keep field "active" logic
                richTextState.setText(ZWSP)
                lastTextValue = ZWSP
                onHtmlChange("")
            }
            return@LaunchedEffect
        }

        // 2. Handle Enter (Newlines)
        if (currentText.contains('\n')) {
            val cleanText = currentText.replace("\n", "").replace(ZWSP, "")
            richTextState.setText(if (cleanText.isEmpty()) ZWSP else cleanText)
            lastTextValue = if (cleanText.isEmpty()) ZWSP else cleanText
            onEnter(cleanText.isEmpty())
            return@LaunchedEffect
        }

        // 3. User typed text (ZWSP + "a") -> Strip ZWSP
        if (currentText.startsWith(ZWSP) && currentText.length > 1) {
            val realText = currentText.substring(1)
            richTextState.setText(realText)
            lastTextValue = realText
            return@LaunchedEffect
        }

        // 4. Normal Text Update
        lastTextValue = currentText
        val currentHtml = richTextState.toHtml()

        if (currentText == ZWSP) {
            if (initialHtml.isNotEmpty()) onHtmlChange("")
        } else {
            if (currentHtml != initialHtml) onHtmlChange(currentHtml)
        }
    }
}

// ---------------------------------------------------------
// HELPER FUNCTIONS
// ---------------------------------------------------------

internal fun handleToolbarAction(type: SpanType, value: String?, state: RichTextState) {
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
        SpanType.CHECKBOX -> { /* Handled via ViewModel state logic */ }
        SpanType.TEXT_COLOR -> value?.toIntOrNull()?.let { state.toggleSpanStyle(SpanStyle(color = Color(it))) }
        //SpanType.BG_COLOR -> value?.toIntOrNull()?.let { state.toggleBackgroundColor(Color(it)) } DONT DELETE
        SpanType.FONT_SIZE -> value?.toFloatOrNull()?.let { state.toggleSpanStyle(SpanStyle(fontSize = it.sp)) }
    }
}

internal fun syncToolbarState(viewModel: CanvasViewModel, state: RichTextState) {
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
    //if (currentSpan.background != Color.Unspecified) viewModel.activeStyles[SpanType.BG_COLOR] = currentSpan.background.value.toString()
    if (currentSpan.fontSize != TextUnit.Unspecified) viewModel.activeStyles[SpanType.FONT_SIZE] = currentSpan.fontSize.value.toString()
}
