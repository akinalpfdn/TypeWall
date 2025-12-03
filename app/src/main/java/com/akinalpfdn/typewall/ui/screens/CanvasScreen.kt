package com.akinalpfdn.typewall.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akinalpfdn.typewall.ui.components.CardView
import com.akinalpfdn.typewall.viewmodel.CanvasViewModel
import com.akinalpfdn.typewall.model.SpanType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import kotlin.math.roundToInt
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.akinalpfdn.typewall.data.AppTheme
import com.akinalpfdn.typewall.data.ThemePreferences
import com.akinalpfdn.typewall.ui.components.MainToolbar
import com.akinalpfdn.typewall.ui.components.ColorPalette
import com.akinalpfdn.typewall.ui.components.FontSizeSelector
import com.akinalpfdn.typewall.ui.components.CanvasControls
import com.akinalpfdn.typewall.ui.components.ToolbarMode
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader


@Composable
fun CanvasScreen(viewModel: CanvasViewModel = viewModel()) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val themePreferences = remember { ThemePreferences(context) }
    var toolbarMode by remember { mutableStateOf(ToolbarMode.MAIN) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Calculate toolbar height for keyboard area detection
    val toolbarHeightPx = with(LocalDensity.current) { 56.dp.toPx() }
    val screenHeightPx = with(LocalDensity.current) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    val keyboardThresholdPx = screenHeightPx * 0.4f // 40% from bottom

    // Handle additional scrolling when card is created in keyboard area
    LaunchedEffect(viewModel.cardCreatedInKeyboardArea, viewModel.onApplyStyle != null) {
        if (viewModel.cardCreatedInKeyboardArea && viewModel.onApplyStyle != null) {
            // When keyboard appears for a card created in keyboard area, scroll up additional toolbar height
            viewModel.offsetY -= toolbarHeightPx
            viewModel.cardCreatedInKeyboardArea = false // Reset the flag
        }
    }

    // Export Launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    val json = viewModel.getJsonData()
                    outputStream.write(json.toByteArray())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Import Launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val json = reader.readText()
                    viewModel.loadJsonData(json)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Reset toolbar mode when losing focus
    LaunchedEffect(viewModel.onApplyStyle) {
        if (viewModel.onApplyStyle == null) {
            toolbarMode = ToolbarMode.MAIN
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Background Layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        viewModel.scale = (viewModel.scale * zoom).coerceIn(0.1f, 5f)
                        viewModel.offsetX += pan.x
                        viewModel.offsetY += pan.y
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            // Dismiss keyboard when tapping anywhere on canvas
                            focusManager.clearFocus()
                            viewModel.onApplyStyle = null
                            viewModel.onInsertList = null
                            viewModel.onApplyCardColor = null
                            keyboardController?.hide()
                        },
                        onLongPress = { offset ->
    focusManager.clearFocus()
    viewModel.onApplyStyle = null

    // 1. Calculate Safety Variables
    val paddingPx = 20.dp.toPx()
    val safeLimitY = keyboardThresholdPx - toolbarHeightPx - paddingPx

    // 2. ACTION 1: Create the card FIRST
    // We do this first so the card is anchored to the correct world coordinates 
    // based on the CURRENT viewport state.
    viewModel.addCard(offset.x, offset.y)

    // 3. ACTION 2: Scroll the Canvas SECOND
    // Now that the card exists on the canvas, we move the camera (offsetY).
    // The card will move UP along with the background.
    if (offset.y > safeLimitY) {
        val overflowAmount = offset.y - safeLimitY
        
        // Apply the scroll
        viewModel.offsetY -= overflowAmount
        
        // IMPORTANT: Ensure we don't trigger the old LaunchedEffect logic
        // by explicitly setting this to false (or ensuring it's not set to true)
        viewModel.cardCreatedInKeyboardArea = false 
    }
}
                    )
                }
        )

        // 2. Content Layer
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
                key(card.id) {
                    CardView(card = card, scale = viewModel.scale, viewModel = viewModel)
                }
            }
        }

        // 3. Hint Overlay
        if (viewModel.cards.isEmpty()) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Click anywhere to add a note",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
        }

        // 6. Settings Button (Top Right)
        IconButton(
            onClick = { showSettingsDialog = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp, 48.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), CircleShape)
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        // 7. Theme Switch Button (Below Settings) - Direct toggle
        val scope = rememberCoroutineScope()
        val currentTheme by themePreferences.themeMode.collectAsState(initial = AppTheme.SYSTEM)

        IconButton(
            onClick = {
                scope.launch {
                    val nextTheme = when (currentTheme) {
                        AppTheme.SYSTEM -> AppTheme.LIGHT
                        AppTheme.LIGHT -> AppTheme.DARK
                        AppTheme.DARK -> AppTheme.SYSTEM
                    }
                    themePreferences.setThemeMode(nextTheme)
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp, 100.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), CircleShape)
        ) {
            Icon(
                when (currentTheme) {
                    AppTheme.DARK -> Icons.Default.LightMode
                    AppTheme.LIGHT -> Icons.Default.DarkMode
                    AppTheme.SYSTEM -> Icons.Default.BrightnessAuto
                },
                contentDescription = "Toggle Theme",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        // 8. Settings Dialog
        if (showSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showSettingsDialog = false },
                title = { Text("Settings") },
                text = {
                    Column {
                        Text("Manage your data")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                exportLauncher.launch("typewall_backup.json")
                                showSettingsDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Export Backup")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                importLauncher.launch("application/json")
                                showSettingsDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Import Backup")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSettingsDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        
        // 4. Expanded Rich Toolbar
        if (viewModel.onApplyStyle != null) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.ime)
                    .wrapContentHeight()
            ) {
                // We use AnimatedContent to swap between Main Toolbar and Sub-menus (Colors, Sizes)
                AnimatedContent(targetState = toolbarMode, label = "toolbar") { mode ->
                    when (mode) {
                        ToolbarMode.MAIN -> MainToolbar(
                            activeStyles = viewModel.activeStyles.keys,
                            onToggleStyle = { type -> viewModel.onApplyStyle?.invoke(type, null) },
                            onOpenMode = { newMode -> toolbarMode = newMode },
                            onInsertList = { prefix -> viewModel.onInsertList?.invoke(prefix) },
                            viewModel = viewModel
                        )
                        ToolbarMode.TEXT_COLOR -> ColorPalette(
                            title = "Text Color",
                            onSelect = { color -> viewModel.onApplyStyle?.invoke(SpanType.TEXT_COLOR, color.value.toString()) },
                            onBack = { toolbarMode = ToolbarMode.MAIN }
                        )
                        ToolbarMode.BG_COLOR -> ColorPalette(
                            title = "Highlight Color",
                            onSelect = { color -> viewModel.onApplyStyle?.invoke(SpanType.BG_COLOR, color.value.toString()) },
                            onBack = { toolbarMode = ToolbarMode.MAIN }
                        )
                        ToolbarMode.CARD_COLOR -> ColorPalette(
                            title = "Card Color",
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer,
                                MaterialTheme.colorScheme.tertiaryContainer,
                                MaterialTheme.colorScheme.errorContainer
                            ),
                            onSelect = { color -> viewModel.onApplyCardColor?.invoke(color.value.toLong()) },
                            onBack = { toolbarMode = ToolbarMode.MAIN }
                        )
                        ToolbarMode.FONT_SIZE -> FontSizeSelector(
                            onSelect = { size -> viewModel.onApplyStyle?.invoke(SpanType.FONT_SIZE, size.toString()) },
                            onBack = { toolbarMode = ToolbarMode.MAIN }
                        )
                    }
                }
            }
        } else {
            // 5. Canvas HUD
            CanvasControls(
                viewModel = viewModel,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

