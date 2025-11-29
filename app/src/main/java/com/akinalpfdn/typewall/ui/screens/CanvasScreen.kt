package com.akinalpfdn.typewall.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akinalpfdn.typewall.ui.components.CardView
import com.akinalpfdn.typewall.viewmodel.CanvasViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import kotlin.math.roundToInt

private const val TAG = "CanvasScreenDebug"

@Composable
fun CanvasScreen(viewModel: CanvasViewModel = viewModel()) {
    val focusManager = LocalFocusManager.current

    // Log when screen recomposes
    SideEffect {
        Log.d(TAG, "CanvasScreen recomposing. Cards count: ${viewModel.cards.size}")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Background / Input Layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Helper to debug pointer input
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            // Log.d(TAG, "Pointer event: ${event.type}") // Uncomment for verbose touch logging
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        // Log.d(TAG, "Transform gesture detected")
                        viewModel.scale = (viewModel.scale * zoom).coerceIn(0.1f, 5f)
                        viewModel.offsetX += pan.x
                        viewModel.offsetY += pan.y
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            Log.d(TAG, "Tap detected at $offset")
                            focusManager.clearFocus()
                            viewModel.addCard(offset.x, offset.y)
                        }
                    )
                }
        )

        // 2. Content Layer (Cards)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = viewModel.scale,
                    scaleY = viewModel.scale,
                    translationX = viewModel.offsetX,
                    translationY = viewModel.offsetY
                )
        ) {
            viewModel.cards.forEach { card ->
                // Log creation of each CardView
                key(card.id) { // key helps Compose track items
                    Log.d(TAG, "Rendering CardView for id=${card.id}")
                    CardView(card = card, scale = viewModel.scale, viewModel = viewModel)
                }
            }
        }

        // 3. Hint Overlay
        if (viewModel.cards.isEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .pointerInput(Unit) {},
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Click anywhere to add a note",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Drag to pan • Pinch to zoom",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
        }

        // 4. HUD / Controls
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f), MaterialTheme.shapes.extraLarge)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(onClick = { viewModel.scale = (viewModel.scale - 0.1f).coerceAtLeast(0.1f) }) {
                Text("-", style = MaterialTheme.typography.titleLarge)
            }
            Text("${(viewModel.scale * 100).roundToInt()}%")
            IconButton(onClick = { viewModel.scale = (viewModel.scale + 0.1f).coerceAtMost(5f) }) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In")
            }
            Divider(modifier = Modifier.height(20.dp).width(1.dp))
            IconButton(onClick = {
                viewModel.scale = 1f
                viewModel.offsetX = 0f
                viewModel.offsetY = 0f
            }) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset")
            }
        }
    }
}