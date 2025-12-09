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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akinalpfdn.typewall.model.Card
import com.akinalpfdn.typewall.model.CardSpan
import com.akinalpfdn.typewall.model.SpanType
import com.akinalpfdn.typewall.viewmodel.CanvasViewModel
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun CardView(
    card: Card,
    scale: Float,
    viewModel: CanvasViewModel
) {
    val currentCard by rememberUpdatedState(card)

    // We do not use scale in calculation anymore, but keep param for compatibility
    val currentScale by rememberUpdatedState(scale)

    var isFocused by remember { mutableStateOf(false) }
    var hasGainedFocus by remember { mutableStateOf(false) }

    var textFieldValue by remember(card.id) {
        mutableStateOf(
            TextFieldValue(
                annotatedString = buildAnnotatedStringFromCard(card),
                selection = TextRange(card.content.length)
            )
        )
    }

    var titleFieldValue by remember(card.id) {
        mutableStateOf(TextFieldValue(text = card.title ?: ""))
    }

    LaunchedEffect(card.content, card.spans) {
        if (textFieldValue.text != card.content) {
            textFieldValue = textFieldValue.copy(
                annotatedString = buildAnnotatedStringFromCard(card)
            )
        }
    }

    LaunchedEffect(card.title) {
        if (titleFieldValue.text != (card.title ?: "")) {
            titleFieldValue = titleFieldValue.copy(text = card.title ?: "")
        }
    }

    // Sync Active Styles
    LaunchedEffect(textFieldValue.selection, textFieldValue.annotatedString) {
        if (isFocused) {
            viewModel.activeStyles.clear()
            if (hasStyle(textFieldValue, SpanType.BOLD)) viewModel.activeStyles[SpanType.BOLD] = null
            if (hasStyle(textFieldValue, SpanType.ITALIC)) viewModel.activeStyles[SpanType.ITALIC] = null
            if (hasStyle(textFieldValue, SpanType.UNDERLINE)) viewModel.activeStyles[SpanType.UNDERLINE] = null

            getStyleValue(textFieldValue, SpanType.TEXT_COLOR)?.let { viewModel.activeStyles[SpanType.TEXT_COLOR] = it }
            getStyleValue(textFieldValue, SpanType.BG_COLOR)?.let { viewModel.activeStyles[SpanType.BG_COLOR] = it }
            getStyleValue(textFieldValue, SpanType.FONT_SIZE)?.let { viewModel.activeStyles[SpanType.FONT_SIZE] = it }
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
            // Header section with title and drag functionality
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
                        // Drag gestures only in header
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

                                // Accumulate raw drag amounts (Screen Pixels = World Units approx)
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

                    // Drag indicator
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = "Drag",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier
                            .size(20.dp)
                            .padding(start = 8.dp)
                    )
                }
            }

            // Content section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .pointerInput(Unit) {
                        detectTapGestures {
                            focusRequester.requestFocus()
                            keyboardController?.show()
                        }
                    }
            ) {
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { newValue ->
                        textFieldValue = newValue
                        updateViewModel(viewModel, card.id, newValue)
                    },
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            isFocused = focusState.isFocused

                            if (focusState.isFocused) {
                                hasGainedFocus = true
                                keyboardController?.show()

                                viewModel.onApplyStyle = { type, value ->
                                    textFieldValue = applyStyle(textFieldValue, type, value)
                                    updateViewModel(viewModel, card.id, textFieldValue)
                                }
                                viewModel.onInsertList = { prefix ->
                                    textFieldValue = insertListPrefix(textFieldValue, prefix)
                                    updateViewModel(viewModel, card.id, textFieldValue)
                                }
                                viewModel.onApplyCardColor = { color ->
                                    viewModel.updateCard(id = card.id, cardColor = color)
                                }
                            } else {
                                if (viewModel.onApplyStyle != null) {
                                    viewModel.onApplyStyle = null
                                    viewModel.onInsertList = null
                                    viewModel.onApplyCardColor = null
                                }
                            }

                            if (!focusState.isFocused && hasGainedFocus) {
                                viewModel.cleanupEmptyCard(card.id)
                            }
                        }
                )
            }
        }

        // Delete button overlay
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

        // Resize handle
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .matchParentSize()
                .width(20.dp)
                .offset(x = 10.dp)
                .pointerInput(Unit) {
                    // Stable Resize Logic
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

// --- Rich Text Logic Helpers ---

fun updateViewModel(viewModel: CanvasViewModel, cardId: String, value: TextFieldValue) {
    val spans = value.annotatedString.spanStyles.flatMap { range ->
        val type = when {
            range.item.fontWeight == FontWeight.Bold -> SpanType.BOLD
            range.item.fontStyle == FontStyle.Italic -> SpanType.ITALIC
            range.item.textDecoration == TextDecoration.Underline -> SpanType.UNDERLINE
            range.item.color != Color.Unspecified -> SpanType.TEXT_COLOR
            range.item.background != Color.Unspecified -> SpanType.BG_COLOR
            range.item.fontSize != TextUnit.Unspecified -> SpanType.FONT_SIZE
            else -> null
        }

        val strValue = when(type) {
            SpanType.TEXT_COLOR -> range.item.color.value.toString()
            SpanType.BG_COLOR -> range.item.background.value.toString()
            SpanType.FONT_SIZE -> range.item.fontSize.value.toString()
            else -> null
        }

        if (type != null) listOf(CardSpan(range.start, range.end, type, strValue)) else emptyList()
    }
    viewModel.updateCard(id = cardId, content = value.text, spans = spans)
}

fun buildAnnotatedStringFromCard(card: Card): AnnotatedString {
    return buildAnnotatedString {
        append(card.content)
        card.spans?.forEach { span ->
            try {
                val style = when (span.type) {
                    SpanType.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
                    SpanType.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
                    SpanType.UNDERLINE -> SpanStyle(textDecoration = TextDecoration.Underline)
                    SpanType.TEXT_COLOR -> SpanStyle(color = Color(span.value!!.toULong()))
                    SpanType.BG_COLOR -> SpanStyle(background = Color(span.value!!.toULong()))
                    SpanType.FONT_SIZE -> SpanStyle(fontSize = span.value!!.toFloat().sp)
                }
                if (span.start <= card.content.length && span.end <= card.content.length) {
                    addStyle(style, span.start, span.end)
                }
            } catch (e: Exception) { }
        }
    }
}

fun hasStyle(value: TextFieldValue, type: SpanType): Boolean {
    val selection = value.selection
    if (selection.collapsed) return false
    return value.annotatedString.spanStyles.any {
        val overlaps = (selection.start < it.end && selection.end > it.start)
        if (!overlaps) return@any false
        when (type) {
            SpanType.BOLD -> it.item.fontWeight == FontWeight.Bold
            SpanType.ITALIC -> it.item.fontStyle == FontStyle.Italic
            SpanType.UNDERLINE -> it.item.textDecoration == TextDecoration.Underline
            else -> false
        }
    }
}

fun getStyleValue(value: TextFieldValue, type: SpanType): String? {
    val selection = value.selection
    if (selection.collapsed) return null
    val match = value.annotatedString.spanStyles.find {
        val overlaps = (selection.start < it.end && selection.end > it.start)
        overlaps && when(type) {
            SpanType.TEXT_COLOR -> it.item.color != Color.Unspecified
            SpanType.BG_COLOR -> it.item.background != Color.Unspecified
            SpanType.FONT_SIZE -> it.item.fontSize != TextUnit.Unspecified
            else -> false
        }
    }
    return when(type) {
        SpanType.TEXT_COLOR -> match?.item?.color?.value?.toString()
        SpanType.BG_COLOR -> match?.item?.background?.value?.toString()
        SpanType.FONT_SIZE -> match?.item?.fontSize?.value?.toString()
        else -> null
    }
}

fun applyStyle(value: TextFieldValue, type: SpanType, param: String?): TextFieldValue {
    val selection = value.selection
    if (selection.collapsed) return value
    val builder = AnnotatedString.Builder(value.annotatedString)

    val style = when (type) {
        SpanType.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
        SpanType.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
        SpanType.UNDERLINE -> SpanStyle(textDecoration = TextDecoration.Underline)
        SpanType.TEXT_COLOR -> SpanStyle(color = Color(param!!.toULong()))
        SpanType.BG_COLOR -> SpanStyle(background = Color(param!!.toULong()))
        SpanType.FONT_SIZE -> SpanStyle(fontSize = param!!.toFloat().sp)
    }
    builder.addStyle(style, selection.start, selection.end)
    return value.copy(annotatedString = builder.toAnnotatedString())
}

fun insertListPrefix(value: TextFieldValue, prefix: String): TextFieldValue {
    val cursor = value.selection.start
    val text = value.text
    var lineStart = text.lastIndexOf('\n', cursor - 1) + 1
    if (lineStart < 0) lineStart = 0

    val newText = text.substring(0, lineStart) + prefix + text.substring(lineStart)
    return value.copy(
        text = newText,
        selection = TextRange(cursor + prefix.length)
    )
}