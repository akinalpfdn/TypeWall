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
import java.io.BufferedReader
import java.io.InputStreamReader

// Enum to track which toolbar sub-menu is open
enum class ToolbarMode { MAIN, TEXT_COLOR, BG_COLOR, CARD_COLOR, FONT_SIZE }

@Composable
fun CanvasScreen(viewModel: CanvasViewModel = viewModel()) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var toolbarMode by remember { mutableStateOf(ToolbarMode.MAIN) }
    var showSettingsDialog by remember { mutableStateOf(false) }

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
                            // Create card on long press
                            focusManager.clearFocus()
                            viewModel.onApplyStyle = null
                            viewModel.addCard(offset.x, offset.y)
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
            Icon(Icons.Default.Settings, contentDescription = "Settings")
        }

        // 7. Settings Dialog
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
                            activeStyles = viewModel.activeStyles,
                            onToggleStyle = { type -> viewModel.onApplyStyle?.invoke(type, null) },
                            onOpenMode = { newMode -> toolbarMode = newMode },
                            onInsertList = { prefix -> viewModel.onInsertList?.invoke(prefix) }
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
                            colors = listOf(Color(0xFFFFFFFF), Color(0xFFFFF8E1), Color(0xFFE3F2FD), Color(0xFFE8F5E9), Color(0xFFF3E5F5), Color(0xFFFFEBEE)),
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

// --- Sub-Components ---

@Composable
fun MainToolbar(
    activeStyles: Map<SpanType, String?>,
    onToggleStyle: (SpanType) -> Unit,
    onOpenMode: (ToolbarMode) -> Unit,
    onInsertList: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Toggles
        ToolbarButton(Icons.Default.FormatBold, activeStyles.containsKey(SpanType.BOLD)) { onToggleStyle(SpanType.BOLD) }
        ToolbarButton(Icons.Default.FormatItalic, activeStyles.containsKey(SpanType.ITALIC)) { onToggleStyle(SpanType.ITALIC) }
        ToolbarButton(Icons.Default.FormatUnderlined, activeStyles.containsKey(SpanType.UNDERLINE)) { onToggleStyle(SpanType.UNDERLINE) }

        VerticalDivider()

        // Sub-menus
        ToolbarButton(Icons.Default.FormatColorText, false) { onOpenMode(ToolbarMode.TEXT_COLOR) }
        ToolbarButton(Icons.Default.FormatColorFill, false) { onOpenMode(ToolbarMode.BG_COLOR) }
        ToolbarButton(Icons.Default.FormatSize, false) { onOpenMode(ToolbarMode.FONT_SIZE) }

        VerticalDivider()

        // Lists
        ToolbarButton(Icons.Default.FormatListBulleted, false) { onInsertList("• ") }
        ToolbarButton(Icons.Default.FormatListNumbered, false) { onInsertList("1. ") }
        ToolbarButton(Icons.Default.CheckBox, false) { onInsertList("[ ] ") }

        VerticalDivider()

        // Card Props
        ToolbarButton(Icons.Default.Palette, false) { onOpenMode(ToolbarMode.CARD_COLOR) }
    }
}

@Composable
fun ColorPalette(
    title: String,
    colors: List<Color> = listOf(Color.Black, Color.Red, Color.Blue, Color.Green, Color.Yellow, Color.Magenta, Color.Cyan, Color.Gray),
    onSelect: (Color) -> Unit,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
        Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        colors.forEach { color ->
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.dp, Color.Gray, CircleShape)
                    .clickable { onSelect(color) }
            )
        }
    }
}

@Composable
fun FontSizeSelector(
    onSelect: (Int) -> Unit,
    onBack: () -> Unit
) {
    val sizes = listOf(12, 14, 16, 18, 20, 24, 30)
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
        sizes.forEach { size ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onSelect(size) }
            ) {
                Text(text = size.toString(), fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun ToolbarButton(icon: ImageVector, isActive: Boolean, onClick: () -> Unit) {
    val tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val bg = if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)).background(bg).clickable { onClick() }
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
    }
}

@Composable
fun VerticalDivider() {
    Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.Gray.copy(alpha = 0.3f)))
}

@Composable
fun CanvasControls(
    viewModel: CanvasViewModel,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
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
        Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color.Gray))
        IconButton(onClick = {
            viewModel.scale = 1f
            viewModel.offsetX = 0f
            viewModel.offsetY = 0f
        }) {
            Icon(Icons.Default.Refresh, contentDescription = "Reset")
        }
    }
}