package com.akinalpfdn.typewall.ui.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akinalpfdn.typewall.model.Card
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
    // FIX: State Hoisting for Gestures
    // pointerInput lambdas capture the initial 'card' and 'scale' values and do not update
    // when the composable recomposes. We use rememberUpdatedState to ensure the gestures
    // always read the very latest data (position, width, scale) without restarting the gesture.
    val currentCard by rememberUpdatedState(card)
    val currentScale by rememberUpdatedState(scale)

    var isFocused by remember { mutableStateOf(false) }
    var hasGainedFocus by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Ghost Mode Logic
    val isEmpty = card.content.isEmpty()
    val isGhost = isEmpty && !isFocused

    val backgroundColor = if (isGhost) Color.Transparent else MaterialTheme.colorScheme.surface
    val borderColor = if (isGhost) Color.Transparent else MaterialTheme.colorScheme.outlineVariant
    val shadowElevation = if (isGhost) 0.dp else if (isFocused) 8.dp else 2.dp

    LaunchedEffect(Unit) {
        if (isEmpty) {
            Log.d(TAG, "New empty card created id=${card.id}. Requesting focus.")
            focusRequester.requestFocus()
            delay(100)
            keyboardController?.show()
        }
    }

    Box(
        modifier = Modifier
            // We use 'card' here for rendering because Composable functions re-run on change,
            // so 'offset' and 'width' modifiers are naturally updated.
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
                    // Use 'currentCard' and 'currentScale' to get latest values during drag
                    viewModel.updateCard(
                        id = currentCard.id,
                        x = currentCard.x + (dragAmount.x / currentScale),
                        y = currentCard.y + (dragAmount.y / currentScale)
                    )
                }
            }
    ) {
        Column(modifier = Modifier.padding(12.dp)) { // Polished: Reduced padding from 16.dp
            BasicTextField(
                value = card.content,
                onValueChange = { viewModel.updateCard(id = card.id, content = it) },
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

        if (isFocused) {
            IconButton(
                onClick = {
                    viewModel.removeCard(card.id)
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(32.dp)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.Red.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Resizer Handle
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                // Polished: Use matchParentSize to fit content height exactly
                // instead of fillMaxHeight which can sometimes force expansion.
                .matchParentSize()
                .width(20.dp)
                .offset(x = 10.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        // Use 'currentCard' to prevent width resetting
                        val newWidth = currentCard.width + (dragAmount.x / currentScale)
                        viewModel.updateCard(
                            id = currentCard.id,
                            width = newWidth
                        )
                    }
                }
        )
    }
}