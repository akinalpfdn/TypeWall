package com.akinalpfdn.typewall.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.akinalpfdn.typewall.model.Card
import kotlin.math.pow
import kotlin.math.sqrt

@Composable
fun EdgeIndicators(
    cards: List<Card>,
    viewportBounds: Rect,
    offsetX: Float,
    offsetY: Float,
    scale: Float,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidth = with(LocalDensity.current) { maxWidth.toPx() }
        val screenHeight = with(LocalDensity.current) { maxHeight.toPx() }

        // Calculate canvas bounds in world coordinates
        val canvasLeft = (-offsetX) / scale
        val canvasTop = (-offsetY) / scale
        val canvasRight = (screenWidth - offsetX) / scale
        val canvasBottom = (screenHeight - offsetY) / scale
        val canvasBounds = Rect(canvasLeft, canvasTop, canvasRight, canvasBottom)

        // Get off-screen cards and group them by edge
        val offScreenCards = cards.filter { card ->
            val cardBounds = Rect(
                card.x,
                card.y,
                card.x + card.width,
                card.y + 100f // Approximate height
            )
            !canvasBounds.overlaps(cardBounds)
        }

        // Group cards by their primary edge
        val leftCards = mutableListOf<CardWithPosition>()
        val rightCards = mutableListOf<CardWithPosition>()
        val topCards = mutableListOf<CardWithPosition>()
        val bottomCards = mutableListOf<CardWithPosition>()

        offScreenCards.forEach { card ->
            val cardCenterX = card.x + card.width / 2
            val cardCenterY = card.y + 50f

            val clampedX = cardCenterX.coerceIn(canvasLeft, canvasRight)
            val clampedY = cardCenterY.coerceIn(canvasTop, canvasBottom)

            val screenX = clampedX * scale + offsetX
            val screenY = clampedY * scale + offsetY

            val cardWithPos = CardWithPosition(
                card = card,
                screenX = screenX,
                screenY = screenY,
                worldX = cardCenterX,
                worldY = cardCenterY
            )

            when {
                cardCenterX < canvasLeft -> leftCards.add(cardWithPos)
                cardCenterX > canvasRight -> rightCards.add(cardWithPos)
                cardCenterY < canvasTop -> topCards.add(cardWithPos)
                cardCenterY > canvasBottom -> bottomCards.add(cardWithPos)
            }
        }

        val indicatorColor = MaterialTheme.colorScheme.primary // Matches your app theme color

        // Draw canvas indicators
        Canvas(modifier = Modifier.fillMaxSize()) {
            val indicatorLength = 24.dp.toPx()
            val indicatorThickness = 4.dp.toPx()
            val edgePadding = 4.dp.toPx()

            // Helper function to draw a pill/capsule indicator
            fun drawPillIndicator(
                cardPos: CardWithPosition,
                topLeft: Offset,
                size: Size
            ) {
                // Calculate distance for opacity (fade out if very far away)
                val distance = sqrt(
                    ((topLeft.x - cardPos.screenX).pow(2) + (topLeft.y - cardPos.screenY).pow(2))
                )
                val maxDistance = 800f * scale
                val alpha = (1f - (distance / maxDistance).coerceIn(0f, 1f)).coerceAtLeast(0.2f)

                drawRoundRect(
                    color = indicatorColor.copy(alpha = alpha),
                    topLeft = topLeft,
                    size = size,
                    cornerRadius = CornerRadius(indicatorThickness / 2)
                )
            }

            // Draw Left Indicators (Vertical pills)
            leftCards.forEach { cardPos ->
                val finalX = edgePadding
                val finalY = cardPos.screenY.coerceIn(indicatorLength, screenHeight - indicatorLength) - (indicatorLength / 2)

                drawPillIndicator(
                    cardPos,
                    Offset(finalX, finalY),
                    Size(indicatorThickness, indicatorLength)
                )
            }

            // Draw Right Indicators (Vertical pills)
            rightCards.forEach { cardPos ->
                val finalX = screenWidth - edgePadding - indicatorThickness
                val finalY = cardPos.screenY.coerceIn(indicatorLength, screenHeight - indicatorLength) - (indicatorLength / 2)

                drawPillIndicator(
                    cardPos,
                    Offset(finalX, finalY),
                    Size(indicatorThickness, indicatorLength)
                )
            }

            // Draw Top Indicators (Horizontal pills)
            topCards.forEach { cardPos ->
                val finalX = cardPos.screenX.coerceIn(indicatorLength, screenWidth - indicatorLength) - (indicatorLength / 2)
                val finalY = edgePadding

                drawPillIndicator(
                    cardPos,
                    Offset(finalX, finalY),
                    Size(indicatorLength, indicatorThickness)
                )
            }

            // Draw Bottom Indicators (Horizontal pills)
            bottomCards.forEach { cardPos ->
                val finalX = cardPos.screenX.coerceIn(indicatorLength, screenWidth - indicatorLength) - (indicatorLength / 2)
                val finalY = screenHeight - edgePadding - indicatorThickness

                drawPillIndicator(
                    cardPos,
                    Offset(finalX, finalY),
                    Size(indicatorLength, indicatorThickness)
                )
            }
        }
    }
}

private data class CardWithPosition(
    val card: Card,
    val screenX: Float,
    val screenY: Float,
    val worldX: Float,
    val worldY: Float
)