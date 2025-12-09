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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.akinalpfdn.typewall.viewmodel.CanvasViewModel
import com.akinalpfdn.typewall.model.SpanType
import kotlin.math.roundToInt

// Enum to track which toolbar sub-menu is open
enum class ToolbarMode { MAIN, TEXT_COLOR, BG_COLOR, CARD_COLOR, FONT_SIZE }


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
        ToolbarIcon(Icons.AutoMirrored.Filled.Undo, false) { /* Call Undo */ }
        ToolbarIcon(Icons.AutoMirrored.Filled.Redo, false) { /* Call Redo */ }

        VerticalDivider()

        // --- Formatting Group ---
        ToolbarIcon(Icons.Default.FormatBold, SpanType.BOLD in activeStyles) { onToggleStyle(SpanType.BOLD) }
        ToolbarIcon(Icons.Default.FormatItalic, SpanType.ITALIC in activeStyles) { onToggleStyle(SpanType.ITALIC) }
        ToolbarIcon(Icons.Default.FormatUnderlined, SpanType.UNDERLINE in activeStyles) { onToggleStyle(SpanType.UNDERLINE) }
        ToolbarIcon(Icons.Default.FormatStrikethrough, SpanType.STRIKETHROUGH in activeStyles) { onToggleStyle(SpanType.STRIKETHROUGH) }

        VerticalDivider()

        // --- Color & Size Group ---
        ToolbarIcon(Icons.Default.FormatColorText, false) { onOpenMode(ToolbarMode.TEXT_COLOR) }
        ToolbarIcon(Icons.Default.FormatColorFill, false) { onOpenMode(ToolbarMode.BG_COLOR) }
        ToolbarIcon(Icons.Default.FormatSize, false) { onOpenMode(ToolbarMode.FONT_SIZE) }

        VerticalDivider()

        // --- Alignment Group ---
        ToolbarIcon(Icons.Default.FormatAlignLeft, SpanType.ALIGN_LEFT in activeStyles) { onToggleStyle(SpanType.ALIGN_LEFT) }
        ToolbarIcon(Icons.Default.FormatAlignCenter, SpanType.ALIGN_CENTER in activeStyles) { onToggleStyle(SpanType.ALIGN_CENTER) }
        ToolbarIcon(Icons.Default.FormatAlignRight, SpanType.ALIGN_RIGHT in activeStyles) { onToggleStyle(SpanType.ALIGN_RIGHT) }

        VerticalDivider()

        // --- Insert Group ---
        ToolbarIcon(Icons.AutoMirrored.Filled.FormatListBulleted, false) { onInsertList("• ") }
        // CHANGE THIS LINE:
        ToolbarIcon(Icons.Default.CheckBox, SpanType.CHECKBOX in activeStyles) { onToggleStyle(SpanType.CHECKBOX) }
        ToolbarIcon(Icons.Default.FormatQuote, SpanType.QUOTE in activeStyles) { onToggleStyle(SpanType.QUOTE) }
        ToolbarIcon(Icons.Default.Code, SpanType.CODE in activeStyles) { onToggleStyle(SpanType.CODE) }
        ToolbarIcon(Icons.Default.Link, false) { /* Open Link Dialog */ }

        VerticalDivider()

        // --- Card Properties ---
        ToolbarIcon(Icons.Default.Palette, false) { onOpenMode(ToolbarMode.CARD_COLOR) }
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
    colors: List<Color> = listOf(
        MaterialTheme.colorScheme.onSurface,
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.outline,
        MaterialTheme.colorScheme.onSurfaceVariant,
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    ),
    onSelect: (Color) -> Unit,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
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
fun FontSizeSelector(
    onSelect: (Int) -> Unit,
    onBack: () -> Unit
) {
    val sizes = listOf(12, 14, 16, 18, 20, 24, 32)
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
        }
        sizes.forEach { size ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (size == 16) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface
                    )
                    .clickable { onSelect(size) }
            ) {
                Text(
                    text = "${size}sp",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (size == 16) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface
                )
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
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg, RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
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
            viewModel.scale = 1f
            viewModel.offsetX = 0f
            viewModel.offsetY = 0f
        }) {
            Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}