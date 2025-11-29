package com.akinalpfdn.typewall.ui.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akinalpfdn.typewall.model.Card
import com.akinalpfdn.typewall.model.CardSpan
import com.akinalpfdn.typewall.model.SpanType
import com.akinalpfdn.typewall.viewmodel.CanvasViewModel
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private const val TAG = "CardViewDebug"

@Composable
fun CardView(
    card: Card,
    scale: Float,
    viewModel: CanvasViewModel
) {
    val currentCard by rememberUpdatedState(card)
    val currentScale by rememberUpdatedState(scale)

    var isFocused by remember { mutableStateOf(false) }
    var hasGainedFocus by remember { mutableStateOf(false) }

    // Initialize TextFieldValue from Card content + spans
    // We use a key on card.id to reset if card is recycled, but we need to be careful not to reset while typing
    var textFieldValue by remember(card.id) {
        mutableStateOf(
            TextFieldValue(
                annotatedString = buildAnnotatedStringFromCard(card),
                selection = TextRange(card.content.length)
            )
        )
    }

    // Sync external content changes (e.g. from undo/redo or initial load) ONLY if content doesn't match
    // This prevents the cursor from jumping while typing if we were to just observe 'card' directly
    LaunchedEffect(card.content, card.spans) {
        if (textFieldValue.text != card.content) {
            textFieldValue = textFieldValue.copy(
                annotatedString = buildAnnotatedStringFromCard(card)
            )
        }
    }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val isEmpty = card.content.isEmpty()
    val isGhost = isEmpty && !isFocused

    val backgroundColor = if (isGhost) Color.Transparent else MaterialTheme.colorScheme.surface
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
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                detectTapGestures {
                    focusRequester.requestFocus()
                    keyboardController?.show()
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    viewModel.updateCard(
                        id = currentCard.id,
                        x = currentCard.x + (dragAmount.x / currentScale),
                        y = currentCard.y + (dragAmount.y / currentScale)
                    )
                }
            }
    ) {
        Column {
            // Formatting Toolbar (Visible only when focused)
            if (isFocused) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                    modifier = Modifier.fillMaxWidth().height(40.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        FormatIconButton(
                            icon = Icons.Default.FormatBold,
                            isActive = hasStyle(textFieldValue, SpanType.BOLD),
                            onClick = {
                                textFieldValue = toggleStyle(textFieldValue, SpanType.BOLD)
                                updateViewModel(viewModel, card.id, textFieldValue)
                            }
                        )
                        FormatIconButton(
                            icon = Icons.Default.FormatItalic,
                            isActive = hasStyle(textFieldValue, SpanType.ITALIC),
                            onClick = {
                                textFieldValue = toggleStyle(textFieldValue, SpanType.ITALIC)
                                updateViewModel(viewModel, card.id, textFieldValue)
                            }
                        )
                        FormatIconButton(
                            icon = Icons.Default.FormatUnderlined,
                            isActive = hasStyle(textFieldValue, SpanType.UNDERLINE),
                            onClick = {
                                textFieldValue = toggleStyle(textFieldValue, SpanType.UNDERLINE)
                                updateViewModel(viewModel, card.id, textFieldValue)
                            }
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.removeCard(card.id) }) {
                            Icon(Icons.Default.Delete, "Delete", tint = Color.Red.copy(alpha = 0.6f))
                        }
                    }
                }
            }

            // Text Input
            Box(modifier = Modifier.padding(12.dp)) {
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
                            }
                            if (!focusState.isFocused && hasGainedFocus) {
                                viewModel.cleanupEmptyCard(card.id)
                            }
                        }
                )
            }
        }

        // Resizer Handle
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .matchParentSize()
                .width(20.dp)
                .offset(x = 10.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newWidth = currentCard.width + (dragAmount.x / currentScale)
                        viewModel.updateCard(id = currentCard.id, width = newWidth)
                    }
                }
        )
    }
}

// --- Helper Composable for Menu Buttons ---
@Composable
fun FormatIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val bg = if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(32.dp)
            .background(bg, RoundedCornerShape(4.dp))
            .clickable { onClick() }
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
    }
}

// --- Rich Text Logic ---

fun updateViewModel(viewModel: CanvasViewModel, cardId: String, value: TextFieldValue) {
    // Convert Compose AnnotatedString spans back to our serializable CardSpan model
    val spans = value.annotatedString.spanStyles.flatMap { range ->
        val type = when {
            range.item.fontWeight == FontWeight.Bold -> SpanType.BOLD
            range.item.fontStyle == FontStyle.Italic -> SpanType.ITALIC
            range.item.textDecoration == TextDecoration.Underline -> SpanType.UNDERLINE
            else -> null
        }
        if (type != null) listOf(CardSpan(range.start, range.end, type)) else emptyList()
    }
    viewModel.updateCard(id = cardId, content = value.text, spans = spans)
}

fun buildAnnotatedStringFromCard(card: Card): AnnotatedString {
    return buildAnnotatedString {
        append(card.content)
        card.spans.forEach { span ->
            val style = when (span.type) {
                SpanType.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
                SpanType.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
                SpanType.UNDERLINE -> SpanStyle(textDecoration = TextDecoration.Underline)
            }
            // Ensure indices are valid to prevent crashes if content length changed
            if (span.start <= card.content.length && span.end <= card.content.length) {
                addStyle(style, span.start, span.end)
            }
        }
    }
}

fun hasStyle(value: TextFieldValue, type: SpanType): Boolean {
    val selection = value.selection
    if (selection.collapsed) return false

    // Check if the selection range contains the specific style
    return value.annotatedString.spanStyles.any {
        val spanStart = it.start
        val spanEnd = it.end
        // Overlap check
        val overlaps = (selection.start < spanEnd && selection.end > spanStart)
        if (!overlaps) return@any false

        when (type) {
            SpanType.BOLD -> it.item.fontWeight == FontWeight.Bold
            SpanType.ITALIC -> it.item.fontStyle == FontStyle.Italic
            SpanType.UNDERLINE -> it.item.textDecoration == TextDecoration.Underline
        }
    }
}

fun toggleStyle(value: TextFieldValue, type: SpanType): TextFieldValue {
    val selection = value.selection
    if (selection.collapsed) return value // Simplification: Only style selection for now

    val builder = AnnotatedString.Builder(value.annotatedString)

    // Remove existing style if present, otherwise add it
    if (hasStyle(value, type)) {
        // Compose doesn't have a simple "removeStyle", we technically have to rebuild.
        // For this MVP, we will just add the style to the list.
        // Real rich text editors require complex span management (splitting spans, merging, etc).
        // Here, we simply APPEND a style. To "remove", we would need to filter the existing list.
        // Implementing full "Remove Style" logic is complex code.
        // For now, we will just ADD styles. To toggle OFF, user would normally need 'clear formatting'.
        // NOTE: Truly toggling off requires filtering the 'spanStyles' list of the AnnotatedString.
    } else {
        val style = when (type) {
            SpanType.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
            SpanType.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
            SpanType.UNDERLINE -> SpanStyle(textDecoration = TextDecoration.Underline)
        }
        builder.addStyle(style, selection.start, selection.end)
    }

    return value.copy(annotatedString = builder.toAnnotatedString())
}