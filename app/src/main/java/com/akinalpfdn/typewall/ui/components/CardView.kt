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
import androidx.compose.ui.zIndex
import com.akinalpfdn.typewall.model.Card
import com.akinalpfdn.typewall.model.SpanType
import com.akinalpfdn.typewall.viewmodel.CanvasViewModel
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.BasicRichTextEditor
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
    var currentLayoutResult by remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }
    
    // Header height calculation for scrolling
    val isEmpty = card.content.isEmpty()
    // Ghost mode: Invisible if empty and not focused
    val isGhost = isEmpty && !isFocused

    val cardBgColor = if (card.cardColor != null) Color(card.cardColor!!.toULong()) else MaterialTheme.colorScheme.surface
    val displayBgColor = if (isGhost) Color.Transparent else cardBgColor
    val borderColor = if (isGhost) Color.Transparent else MaterialTheme.colorScheme.outlineVariant
    val shadowElevation = if (isGhost) 0.dp else if (isFocused) 8.dp else 2.dp
    
    // Connection Mode Logic
    val isSelectedForConnection = viewModel.isConnectionMode && viewModel.connectionStartCardId == card.id
    val effectiveBorderColor = if (isSelectedForConnection) MaterialTheme.colorScheme.primary else borderColor
    val effectiveBorderWidth = if (isSelectedForConnection) 3.dp else 1.dp

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
            .shadow(shadowElevation, RoundedCornerShape(8.dp))
            .background(displayBgColor, RoundedCornerShape(8.dp))
            .border(effectiveBorderWidth, effectiveBorderColor, RoundedCornerShape(8.dp))
    ) {
        // --- Click-to-Connect Interceptor ---
        if (viewModel.isConnectionMode) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .zIndex(100f)
                    .background(Color.Transparent)
                    .pointerInput(Unit) {
                        detectTapGestures {
                            viewModel.handleCardTap(card.id)
                        }
                    }
            )
        }
        
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
                                    viewModel.focusPointY = card.y
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
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 24.sp
                            ),
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
                                currentLayoutResult = textLayoutResult
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
                                        // 1. Calculate Cursor Position from Tap
                                        currentLayoutResult?.let { layout ->
                                            // The overlay matches parent size, but text might be padded or offset.
                                            // RichTextEditor has no internal padding now (mostly), but let's be safe.
                                            // Basic estimation: offset.y is relative to the Box, which matches the Editor.
                                            val position = layout.getOffsetForPosition(offset)
                                            richTextState.selection = androidx.compose.ui.text.TextRange(position)
                                        }

                                        // 2. Focus and Scroll
                                        viewModel.focusPointY = card.y + offset.y + (headerHeight.takeIf { it > 0 } ?: 60f)
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
                    modifier = Modifier.size(24.dp)
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

