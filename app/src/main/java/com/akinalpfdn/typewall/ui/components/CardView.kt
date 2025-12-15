package com.akinalpfdn.typewall.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    val scope = rememberCoroutineScope()

    // Focus management for checklist items
    var blurJob by remember { mutableStateOf<Job?>(null) }

    // Logic to switch between RichText Mode and Checklist Mode
    // Check if the content starts with a checkbox marker (Stringly typed logic)
    val isChecklistMode = remember(card.content) {
        val trimmed = card.content.trimStart()
        trimmed.startsWith("☐") || trimmed.startsWith("☑")
    }

    val richTextState = rememberRichTextState()
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

    // Update local title state if external model changes
    LaunchedEffect(card.title) {
        if (titleFieldValue.text != (card.title ?: "")) {
            titleFieldValue = titleFieldValue.copy(text = card.title ?: "")
        }
    }

    val focusRequester = remember { FocusRequester() }
    val titleFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val isEmpty = card.content.isEmpty()
    // Ghost mode: Invisible if empty and not focused
    val isGhost = isEmpty && !isFocused

    val cardBgColor = if (card.cardColor != null) Color(card.cardColor!!.toULong()) else MaterialTheme.colorScheme.surface
    val displayBgColor = if (isGhost) Color.Transparent else cardBgColor
    val borderColor = if (isGhost) Color.Transparent else MaterialTheme.colorScheme.outlineVariant
    val shadowElevation = if (isGhost) 0.dp else if (isFocused) 8.dp else 2.dp

    // Auto-focus new cards
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
            // --- Header Section ---
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

            // --- Editor Section ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                if (isChecklistMode) {
                    // --- Checklist Mode ---
                    ChecklistEditor(
                        content = card.content,
                        onContentChange = { newContent ->
                            // Update content directly (format: ☐ Item\n☑ Item)
                            viewModel.updateCard(card.id, content = newContent, saveHistory = false)
                        },
                        viewModel = viewModel,
                        cardId = card.id,
                        onFocus = {
                            // Cancel any pending blur jobs when a child gains focus
                            blurJob?.cancel()
                            isFocused = true
                            hasGainedFocus = true
                        },
                        onBlur = {
                            // Delay blur to check if focus moved to another item within the same card
                            blurJob = scope.launch {
                                delay(50)
                                isFocused = false
                                // Cleanup Logic
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
                        isCardFocused = isFocused,
                        onRequestFocus = {
                            blurJob?.cancel()
                            isFocused = true
                            hasGainedFocus = true
                            keyboardController?.show()
                        },
                        onScrollRequest = { offset ->
                            val currentHeaderH = if (headerHeight > 0) headerHeight else 150f
                            val totalY = card.y + currentHeaderH + offset + 30f
                            viewModel.focusPointY = totalY
                        }
                    )
                } else {
                    // --- Standard Rich Text Mode ---
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
                                                // SWITCH TO CHECKLIST MODE logic
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

                // Overlay to detect clicks when NOT focused and NOT in checklist mode
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

        // --- Delete Button (Visible when focused) ---
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

        // --- Stretch/Resize Handle ---
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

    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .onGloballyPositioned { myCoordinates = it }
    ) {
        // --- Marker Column (Checkbox / Bullet) ---
        Box(
            modifier = Modifier
                .padding(top = 10.dp, end = 8.dp)
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
                        modifier = Modifier.size(24.dp)
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
            RichTextEditor(
                state = richTextState,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = if (isChecked && itemType == RowType.CHECKBOX)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    else
                        MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (isChecked && itemType == RowType.CHECKBOX)
                        TextDecoration.LineThrough
                    else
                        TextDecoration.None,
                    lineHeight = 24.sp
                ),
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
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            onFocus()
                            viewModel.activeCardId = cardId

                            // Link ViewModel actions to this specific editor instance
                            viewModel.onApplyStyle = { type, value ->
                                handleToolbarAction(type, value, richTextState)
                                onHtmlChange(richTextState.toHtml())
                            }
                            viewModel.onApplyCardColor = { color ->
                                viewModel.updateCard(id = cardId, cardColor = color)
                            }
                            viewModel.onInsertList = {
                                val newType = if (itemType == RowType.BULLET) RowType.TEXT else RowType.BULLET
                                onTypeChange(newType)
                            }

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
                },
                colors = RichTextEditorDefaults.richTextEditorColors(
                    containerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    textColor = MaterialTheme.colorScheme.onSurface,
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    placeholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
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
        SpanType.CHECKBOX -> { /* Handled via ViewModel state logic */ }
        SpanType.TEXT_COLOR -> value?.toIntOrNull()?.let { state.toggleSpanStyle(SpanStyle(color = Color(it))) }
        //SpanType.BG_COLOR -> value?.toIntOrNull()?.let { state.toggleBackgroundColor(Color(it)) } DONT DELETE
        SpanType.FONT_SIZE -> value?.toFloatOrNull()?.let { state.toggleSpanStyle(SpanStyle(fontSize = it.sp)) }
    }
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
    //if (currentSpan.background != Color.Unspecified) viewModel.activeStyles[SpanType.BG_COLOR] = currentSpan.background.value.toString()
    if (currentSpan.fontSize != TextUnit.Unspecified) viewModel.activeStyles[SpanType.FONT_SIZE] = currentSpan.fontSize.value.toString()
}