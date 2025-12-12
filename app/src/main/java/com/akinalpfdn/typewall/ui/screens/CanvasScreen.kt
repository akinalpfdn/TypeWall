package com.akinalpfdn.typewall.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akinalpfdn.typewall.data.AppTheme
import com.akinalpfdn.typewall.data.ThemePreferences
import com.akinalpfdn.typewall.model.SpanType
import com.akinalpfdn.typewall.ui.components.*
import com.akinalpfdn.typewall.viewmodel.CanvasViewModel
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.abs

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CanvasScreen(viewModel: CanvasViewModel = viewModel()) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val themePreferences = remember { ThemePreferences(context) }
    var toolbarMode by remember { mutableStateOf(ToolbarMode.MAIN) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var isSidebarOpen by remember { mutableStateOf(false) }

    val toolbarHeightPx = with(LocalDensity.current) { 56.dp.toPx() }
    val screenHeightPx = with(LocalDensity.current) { LocalConfiguration.current.screenHeightDp.dp.toPx() }

    // Export/Import Launchers (No changes)
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    val json = viewModel.getJsonData()
                    outputStream.write(json.toByteArray())
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

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
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    // Reset toolbar mode
    LaunchedEffect(viewModel.onApplyStyle) {
        if (viewModel.onApplyStyle == null) {
            toolbarMode = ToolbarMode.MAIN
        }
    }

    // --- AUTO-CENTER LOGIC ---
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    LaunchedEffect(imeBottom, viewModel.focusPointY) {
        if (viewModel.focusPointY != null && imeBottom > 0) {
            val availableHeight = screenHeightPx - imeBottom - toolbarHeightPx
            val targetScreenY = availableHeight / 2f
            val desiredOffsetY = targetScreenY - (viewModel.focusPointY!! * viewModel.scale)

            if (abs(viewModel.offsetY - desiredOffsetY) > 5f) {
                viewModel.offsetY = desiredOffsetY
            }
        }
    }

    // --- NEW LAYOUT: SCAFFOLD ---
    // Scaffold isolates the "bottomBar" from the content body.
    // This stops the "Long Text" scroll conflict.
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        // We set this to 0 because we want the Canvas to be fullscreen behind bars
        contentWindowInsets = WindowInsets(0.dp),

        // THE TOOLBAR (Anchored safely)
        bottomBar = {
            // Only show when editing
            if (viewModel.onApplyStyle != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        // Scaffold handles the placement, but we add imePadding
                        // to ensure it rides UP with the keyboard.
                        .imePadding()
                        .navigationBarsPadding()
                ) {
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
            }
        }
    ) { innerPadding ->
        // --- CONTENT BODY (The Canvas) ---
        // We use a Box here and explicitly IGNORE 'innerPadding' for the graphics layer
        // so the canvas stays infinite/fullscreen.
        // But we respect it for UI controls if needed.

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val oldScale = viewModel.scale
                        val newScale = (oldScale * zoom).coerceIn(0.1f, 5f)
                        val zoomFactor = newScale / oldScale
                        viewModel.scale = newScale
                        viewModel.offsetX = (viewModel.offsetX - centroid.x) * zoomFactor + centroid.x + pan.x
                        viewModel.offsetY = (viewModel.offsetY - centroid.y) * zoomFactor + centroid.y + pan.y
                    }
                }
        ) {
            // 1. Gesture Listener Background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                focusManager.clearFocus()
                                viewModel.onApplyStyle = null
                                viewModel.onInsertList = null
                                viewModel.onApplyCardColor = null
                                keyboardController?.hide()
                            },
                            onLongPress = { tapOffset ->
                                focusManager.clearFocus()
                                viewModel.onApplyStyle = null
                                viewModel.addCard(tapOffset.x, tapOffset.y)
                            }
                        )
                    }
            )

            // 2. Cards Layer
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = viewModel.scale,
                        scaleY = viewModel.scale,
                        translationX = viewModel.offsetX,
                        translationY = viewModel.offsetY,
                        transformOrigin = TransformOrigin(0f, 0f)
                    )
            ) {
                viewModel.cards.forEach { card ->
                    key(card.id) {
                        CardView(
                            card = card,
                            scale = viewModel.scale,
                            viewModel = viewModel
                        )
                    }
                }
            }

            // Edge Indicators
            EdgeIndicators(
                cards = viewModel.cards,
                viewportBounds = Rect.Zero,
                offsetX = viewModel.offsetX,
                offsetY = viewModel.offsetY,
                scale = viewModel.scale
            )

            // 3. Hint Overlay
            if (viewModel.cards.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Double tap or long press to add a note",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }

            // 4. Buttons (Settings, Theme)
            // Note: We use TopEnd alignment, unaffected by keyboard
            IconButton(
                onClick = { showSettingsDialog = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp, 48.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), CircleShape)
            ) {
                Icon(Icons.Default.Settings, "Settings", tint = MaterialTheme.colorScheme.onSurface)
            }

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
                    "Toggle Theme",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // 5. Settings Dialog
            if (showSettingsDialog) {
                AlertDialog(
                    onDismissRequest = { showSettingsDialog = false },
                    title = { Text("Settings") },
                    text = {
                        Column {
                            Text("Manage your data")
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { exportLauncher.launch("typewall_backup.json"); showSettingsDialog = false }) {
                                Text("Export Backup", modifier = Modifier.fillMaxWidth())
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { importLauncher.launch("application/json"); showSettingsDialog = false }) {
                                Text("Import Backup", modifier = Modifier.fillMaxWidth())
                            }
                        }
                    },
                    confirmButton = { TextButton(onClick = { showSettingsDialog = false }) { Text("Close") } }
                )
            }

            // 6. Sidebar
            IconButton(
                onClick = { isSidebarOpen = true },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp, 48.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), CircleShape)
            ) {
                Icon(Icons.Default.Menu, "Menu", tint = MaterialTheme.colorScheme.onSurface)
            }

            AnimatedVisibility(
                visible = isSidebarOpen,
                enter = slideInHorizontally(),
                exit = slideOutHorizontally(),
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                val density = LocalDensity.current
                val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
                val screenHeightPx = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }

                Sidebar(
                    cards = viewModel.cards,
                    onCardClick = { card ->
                        val cardWidthPx = with(density) { card.width.dp.toPx() }
                        val cardCenterX = card.x + cardWidthPx / 2
                        val cardCenterY = card.y

                        viewModel.scale = 1f
                        viewModel.offsetX = (screenWidthPx / 2) - cardCenterX
                        viewModel.offsetY = (screenHeightPx / 2) - cardCenterY

                        isSidebarOpen = false
                    },
                    onClose = { isSidebarOpen = false }
                )
            }

            // 7. Passive Controls (Only when NOT editing)
            if (viewModel.onApplyStyle == null) {
                CanvasControls(
                    viewModel = viewModel,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                        .navigationBarsPadding()
                )
            }
        }
    }
}