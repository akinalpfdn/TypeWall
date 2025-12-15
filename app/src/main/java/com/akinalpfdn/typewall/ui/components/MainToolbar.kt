package com.akinalpfdn.typewall.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.akinalpfdn.typewall.viewmodel.CanvasViewModel
import com.akinalpfdn.typewall.model.SpanType
import kotlin.math.roundToInt

import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.drop

// Enum to track which toolbar sub-menu is open
enum class ToolbarMode { MAIN, TEXT_COLOR, //BG_COLOR,
    CARD_COLOR }


@Composable
fun MainToolbar(
    activeStyles: Set<SpanType>,
    onToggleStyle: (SpanType) -> Unit,
    onOpenMode: (ToolbarMode) -> Unit,
    onInsertList: (String) -> Unit,
    viewModel: CanvasViewModel
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()) // Enables the scrollable layout
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // --- History Group ---
        ToolbarIcon(Icons.AutoMirrored.Filled.Undo, false) { viewModel.undo() }
        ToolbarIcon(Icons.AutoMirrored.Filled.Redo, false) { viewModel.redo() }

        VerticalDivider()

        // CHANGE THIS LINE:
        ToolbarIcon(Icons.Default.CheckBox, SpanType.CHECKBOX in activeStyles) { onToggleStyle(SpanType.CHECKBOX) }
        // --- Insert Group ---
        ToolbarIcon(Icons.AutoMirrored.Filled.FormatListBulleted, false) { onInsertList("• ") }
        // --- Formatting Group ---
        ToolbarIcon(Icons.Default.FormatBold, SpanType.BOLD in activeStyles) { onToggleStyle(SpanType.BOLD) }
        ToolbarIcon(Icons.Default.FormatItalic, SpanType.ITALIC in activeStyles) { onToggleStyle(SpanType.ITALIC) }
        ToolbarIcon(Icons.Default.FormatUnderlined, SpanType.UNDERLINE in activeStyles) { onToggleStyle(SpanType.UNDERLINE) }
        ToolbarIcon(Icons.Default.FormatStrikethrough, SpanType.STRIKETHROUGH in activeStyles) { onToggleStyle(SpanType.STRIKETHROUGH) }

        VerticalDivider()

        // --- Color & Size Group ---
        ToolbarIcon(Icons.Default.FormatColorText, false) { onOpenMode(ToolbarMode.TEXT_COLOR) }
                // ToolbarIcon(Icons.Default.FormatColorFill, false) { onOpenMode(ToolbarMode.BG_COLOR) }

        ToolbarIcon(Icons.Default.Palette, false) { onOpenMode(ToolbarMode.CARD_COLOR) }
        // Font Size Wheel Picker
        Box {
            var showFontSizeMenu by remember { mutableStateOf(false) }
            // Get current font size if possible, or default to 16
            // We'll just pass 16 for now or need to track it in ViewModel if we want persistence across closing menu
            val initialSize = viewModel.activeStyles[SpanType.FONT_SIZE]?.toFloatOrNull()?.toInt() ?: 16
            
            ToolbarIcon(Icons.Default.FormatSize, false) { showFontSizeMenu = true }
            
            if (showFontSizeMenu) {
                FontSizeWheelPicker(
                    initialSize = initialSize,
                    onSizeSelected = { size ->
                        viewModel.onApplyStyle?.invoke(SpanType.FONT_SIZE, size.toString())
                    },
                    onDismiss = { showFontSizeMenu = false }
                )
            }
        }

        VerticalDivider()

        // --- Alignment Group ---
        ToolbarIcon(Icons.Default.FormatAlignLeft, SpanType.ALIGN_LEFT in activeStyles) { onToggleStyle(SpanType.ALIGN_LEFT) }
        ToolbarIcon(Icons.Default.FormatAlignCenter, SpanType.ALIGN_CENTER in activeStyles) { onToggleStyle(SpanType.ALIGN_CENTER) }
        ToolbarIcon(Icons.Default.FormatAlignRight, SpanType.ALIGN_RIGHT in activeStyles) { onToggleStyle(SpanType.ALIGN_RIGHT) }






    }
}

// ------------------------------------
// UI HELPERS
// ------------------------------------

@Composable
fun ToolbarIcon(icon: ImageVector, isActive: Boolean, onClick: () -> Unit) {
    val tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val bg = if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent

    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp) // Touch target size
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
fun VerticalDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .width(1.dp)
            .height(24.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    )
}

@Composable
fun ColorPalette(
    title: String,
    // Expanded list with vibrant Material Design colors
    colors: List<Color> = listOf(
        MaterialTheme.colorScheme.surface, // Default Black/Dark
        MaterialTheme.colorScheme.onSurface, // Default Black/Dark
        Color(0xFFE57373), // Pastel Red
        Color(0xFFF06292), // Pastel Pink
        Color(0xFFBA68C8), // Pastel Purple
        Color(0xFF9575CD), // Pastel Deep Purple
        Color(0xFF7986CB), // Pastel Indigo
        Color(0xFF64B5F6), // Pastel Blue
        Color(0xFF4FC3F7), // Pastel Light Blue
        Color(0xFF4DD0E1), // Pastel Cyan
        Color(0xFF4DB6AC), // Pastel Teal
        Color(0xFF81C784), // Pastel Green
        Color(0xFFAED581), // Pastel Light Green
        Color(0xFFFFD54F), // Pastel Mustard/Yellow
        Color(0xFFFFB74D), // Pastel Orange
        Color(0xFFFF8A65), // Pastel Deep Orange
        Color(0xFFA1887F), // Pastel Brown
        Color(0xFFE0E0E0), // Pastel Grey
        Color(0xFF90A4AE)  // Pastel Blue Grey
         , // Default Black/Dark
        Color(0xFFE53935), // Red
        Color(0xFFD81B60), // Pink
        Color(0xFF8E24AA), // Purple
        Color(0xFF5E35B1), // Deep Purple
        Color(0xFF3949AB), // Indigo
        Color(0xFF1E88E5), // Blue
        Color(0xFF039BE5), // Light Blue
        Color(0xFF00ACC1), // Cyan
        Color(0xFF00897B), // Teal
        Color(0xFF43A047), // Green
        Color(0xFF7CB342), // Light Green
        Color(0xFFC0CA33), // Lime
        Color(0xFFFDD835), // Yellow
        Color(0xFFFFB300), // Amber
        Color(0xFFFB8C00), // Orange
        Color(0xFFF4511E), // Deep Orange
        Color(0xFF6D4C41), // Brown
        Color(0xFF757575), // Grey
        Color(0xFF546E7A)  // Blue Grey
    ),
    onSelect: (Color) -> Unit,
    onBack: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 8.dp)
            .horizontalScroll(rememberScrollState()), // Added scroll support for the longer list
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
        }
        Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        colors.forEach { color ->
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    .clickable { onSelect(color) }
            )
        }
    }
}



@Composable
fun CanvasControls(
    viewModel: CanvasViewModel,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f), MaterialTheme.shapes.extraLarge)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        IconButton(onClick = { viewModel.scale = (viewModel.scale - 0.1f).coerceAtLeast(0.1f) }) {
            Text("-", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        }
        Text("${(viewModel.scale * 100).roundToInt()}%", color = MaterialTheme.colorScheme.onSurface)
        IconButton(onClick = { viewModel.scale = (viewModel.scale + 0.1f).coerceAtMost(5f) }) {
            Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = MaterialTheme.colorScheme.onSurface)
        }
        Box(modifier = Modifier.width(1.dp).height(20.dp).background(MaterialTheme.colorScheme.outline))
        IconButton(onClick = {
            viewModel.scale = 0.6f
            viewModel.offsetX = 0f
            viewModel.offsetY = 0f
        }) {
            Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun FontSizeWheelPicker(
    initialSize: Int,
    onSizeSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sizes = (8..42).toList()
    // Default to index of 16 if initialSize not found
    val startIndex = sizes.indexOf(initialSize).takeIf { it != -1 } ?: sizes.indexOf(16)
    
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(initialPage = startIndex) { sizes.size }
    
    // Update selection as we scroll, but DROP the first emission (initial state)
    // to prevent resetting the font size just by opening the menu.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .drop(1)
            .collect { page ->
                onSizeSelected(sizes[page])
            }
    }

    androidx.compose.ui.window.Popup(
        onDismissRequest = onDismiss,
        alignment = Alignment.TopCenter,
        offset = androidx.compose.ui.unit.IntOffset(0, 100) // Slight offset downwards
    ) {
        Box(
            modifier = Modifier
                .width(80.dp)
                .height(150.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .shadow(8.dp, RoundedCornerShape(12.dp))
        ) {
            // Selection Indicator
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            )

            androidx.compose.foundation.pager.VerticalPager(
                state = pagerState,
                pageSize = androidx.compose.foundation.pager.PageSize.Fixed(40.dp),
                contentPadding = PaddingValues(vertical = 55.dp), // Center content: (150 - 40) / 2
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val size = sizes[page]
                val isSelected = pagerState.currentPage == page
                
                // Simple Graphics Layer for "Wheel" effect could be added here
                // For now, simple text centering
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${size}", // Just number for cleaner look in wheel
                        style = if (isSelected) 
                            MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        else 
                            MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    )
                }
            }
        }
    }
}