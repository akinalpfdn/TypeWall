package com.akinalpfdn.typewall.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.akinalpfdn.typewall.model.Card
import com.akinalpfdn.typewall.model.CardSpan
import com.akinalpfdn.typewall.model.SpanType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class CanvasViewModel(application: Application) : AndroidViewModel(application) {
    private val _cards = mutableStateListOf<Card>()
    val cards: List<Card> get() = _cards

    var scale by mutableStateOf(1f)
    var offsetX by mutableStateOf(0f)
    var offsetY by mutableStateOf(0f)

    // Tracks active styles at cursor (Type -> Value)
    // e.g. BOLD -> null, TEXT_COLOR -> "#FF0000"
    val activeStyles = mutableStateMapOf<SpanType, String?>()

    // Callback to apply style with optional value (Color/Size)
    var onApplyStyle: ((SpanType, String?) -> Unit)? by mutableStateOf(null)
    // Callback to apply list formatting (Bullet, Number, Check)
    var onInsertList: ((String) -> Unit)? by mutableStateOf(null)
    // Callback to change the active card's background color
    var onApplyCardColor: ((Long) -> Unit)? by mutableStateOf(null)

    // World Y coordinate of the last focus event (tap/long press)
    var focusPointY by mutableStateOf<Float?>(null)

    private val gson = Gson()
    private val prefs = application.getSharedPreferences("typewall_prefs", Context.MODE_PRIVATE)

    init {
        loadCards()
    }

    fun addCard(screenX: Float, screenY: Float) {
        // Convert screen coordinates to world coordinates
        // Formula: worldCoord = (screenCoord - panOffset) / scale
        val worldX = (screenX - offsetX) / scale
        val worldY = (screenY - offsetY) / scale
        focusPointY = worldY

        // Center the card on the touch point (optional adjustment)
        // You can adjust these values to fine-tune the positioning
        val cardOffsetX = -125f // Half of default card width (250f)
        val cardOffsetY = -20f  // Small offset to position the touch point in the upper part of the card

        val newCard = Card(
            x = worldX + cardOffsetX,
            y = worldY + cardOffsetY,
            width = 250f,
            content = "",
            title = "",
            spans = emptyList()
        )
        _cards.add(newCard)
        saveCards()
    }

    fun updateCard(
        id: String,
        content: String? = null,
        title: String? = null,
        spans: List<CardSpan>? = null,
        x: Float? = null,
        y: Float? = null,
        width: Float? = null,
        cardColor: Long? = null
    ) {
        val index = _cards.indexOfFirst { it.id == id }
        if (index != -1) {
            val oldCard = _cards[index]
            // If cardColor is explicitly passed as -1L, we treat it as null (reset)
            val newColor = if (cardColor == -1L) null else (cardColor ?: oldCard.cardColor)

            _cards[index] = oldCard.copy(
                content = content ?: oldCard.content,
                title = title ?: oldCard.title ?: "",
                spans = spans ?: oldCard.spans,
                x = x ?: oldCard.x,
                y = y ?: oldCard.y,
                width = width?.coerceAtLeast(200f) ?: oldCard.width,
                cardColor = newColor
            )
            saveCards()
        }
    }

    fun removeCard(id: String) {
        _cards.removeAll { it.id == id }
        saveCards()
    }

    fun cleanupEmptyCard(id: String) {
        val card = _cards.find { it.id == id }
        if (card != null && card.content.isBlank()) {
            removeCard(id)
        }
    }

    private fun saveCards() {
        val json = gson.toJson(_cards)
        prefs.edit().putString("cards_data", json).apply()
    }

    private fun loadCards() {
        val json = prefs.getString("cards_data", null)
        if (json != null) {
            try {
                val type = object : TypeToken<List<Card>>() {}.type
                val savedCards: List<Card> = gson.fromJson(json, type)
                // Ensure all cards have non-null titles
                val cardsWithTitles = savedCards.map { card ->
                    card.copy(title = card.title ?: "")
                }
                _cards.addAll(cardsWithTitles)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun getJsonData(): String {
        return gson.toJson(_cards)
    }

    fun loadJsonData(json: String): Boolean {
        return try {
            val type = object : TypeToken<List<Card>>() {}.type
            val loadedCards: List<Card> = gson.fromJson(json, type)
            _cards.clear()
            _cards.addAll(loadedCards)
            saveCards()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}