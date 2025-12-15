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
import com.akinalpfdn.typewall.model.Connection
import com.akinalpfdn.typewall.model.SpanType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class CanvasViewModel(application: Application) : AndroidViewModel(application) {
    private val _cards = mutableStateListOf<Card>()
    val cards: List<Card> get() = _cards

    var scale by mutableStateOf(0.6f)
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

    // Connections State
    private val _connections = mutableStateListOf<Connection>()
    val connections: List<Connection> get() = _connections

    var isConnectionMode by mutableStateOf(false)
    var connectionStartCardId by mutableStateOf<String?>(null)

    // World Y coordinate of the last focus event (tap/long press)
    var focusPointY by mutableStateOf<Float?>(null)

    private val gson = Gson()
    private val prefs = application.getSharedPreferences("typewall_prefs", Context.MODE_PRIVATE)

    init {
        loadCards()
        loadConnections()
    }
    // --- Card-Based History Management ---
    // Map<CardId, Stack<CardState>>
    private val cardHistory = mutableStateMapOf<String, ArrayDeque<Card>>()
    private val cardRedoStack = mutableStateMapOf<String, ArrayDeque<Card>>()
    private val MAX_HISTORY_SIZE = 50

    var activeCardId by mutableStateOf<String?>(null)

    fun saveSnapshot(cardId: String) {
        val currentCard = _cards.find { it.id == cardId } ?: return
        
        // Initialize stack if needed
        if (!cardHistory.containsKey(cardId)) {
            cardHistory[cardId] = ArrayDeque()
        }
        val historyStack = cardHistory[cardId]!!

        // Deep copy
        val snapshot = currentCard.copy()
        
        // Avoid duplicates
        if (historyStack.isNotEmpty() && historyStack.last() == snapshot) return

        historyStack.addLast(snapshot)
        if (historyStack.size > MAX_HISTORY_SIZE) {
            historyStack.removeFirst()
        }
        
        // Clear redo stack for this card
        cardRedoStack[cardId]?.clear()
        
        Log.d("CanvasViewModel", "Snapshot saved for card $cardId. Stack size: ${historyStack.size}")
    }

    fun undo() {
        val cardId = activeCardId ?: return
        val historyStack = cardHistory[cardId]
        val redoStack = cardRedoStack.getOrPut(cardId) { ArrayDeque() }

        if (!historyStack.isNullOrEmpty()) {
            val currentCard = _cards.find { it.id == cardId } ?: return
            
            // Push current state to redo
            redoStack.addLast(currentCard.copy())

            // Pop previous state
            val previousState = historyStack.removeLast()
            
            // Apply state
            updateCardState(previousState)
            
            Log.d("CanvasViewModel", "Undo performed for card $cardId")
        }
    }

    fun redo() {
        val cardId = activeCardId ?: return
        val historyStack = cardHistory.getOrPut(cardId) { ArrayDeque() }
        val redoStack = cardRedoStack[cardId]

        if (!redoStack.isNullOrEmpty()) {
            val currentCard = _cards.find { it.id == cardId } ?: return
            
            // Push current to history
            historyStack.addLast(currentCard.copy())

            // Pop next state
            val nextState = redoStack.removeLast()
            
            // Apply state
            updateCardState(nextState)
            
            Log.d("CanvasViewModel", "Redo performed for card $cardId")
        }
    }

    private fun updateCardState(newState: Card) {
        val index = _cards.indexOfFirst { it.id == newState.id }
        if (index != -1) {
            _cards[index] = newState
            saveCards()
        }
    }

    // ---------------------------

    fun addCard(screenX: Float, screenY: Float) {
        // Convert screen coordinates to world coordinates
        // Formula: worldCoord = (screenCoord - panOffset) / scale
        val worldX = (screenX - offsetX) / scale
        val worldY = (screenY - offsetY) / scale
        focusPointY = worldY

        // Center the card on the touch point (optional adjustment)
        val cardOffsetX = -125f 
        val cardOffsetY = -20f  

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
        cardColor: Long? = null,
        saveHistory: Boolean = true
    ) {
        if (saveHistory) {
            saveSnapshot(id)
        }

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
        // No history for deletion as per request
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


    // --- Connection Logic (Click-to-Connect) ---

    fun toggleConnectionMode() {
        isConnectionMode = !isConnectionMode
        connectionStartCardId = null
    }

    fun handleCardTap(cardId: String) {
        if (!isConnectionMode) return

        val startId = connectionStartCardId
        if (startId == null) {
            // Select first card
            connectionStartCardId = cardId
        } else if (startId == cardId) {
            // Deselect if same card tapped
            connectionStartCardId = null
        } else {
            // Second card tapped -> Toggle Connection
            val existingIndex = _connections.indexOfFirst {
                (it.startCardId == startId && it.endCardId == cardId) ||
                (it.startCardId == cardId && it.endCardId == startId)
            }

            if (existingIndex != -1) {
                _connections.removeAt(existingIndex)
            } else {
                _connections.add(Connection(startCardId = startId, endCardId = cardId))
            }
            saveConnections()
            
            // Chain: This card becomes the new start
            connectionStartCardId = cardId
        }
    }

    private fun saveConnections() {
        val json = gson.toJson(_connections)
        prefs.edit().putString("connections_data", json).apply()
    }

    private fun loadConnections() {
        val json = prefs.getString("connections_data", null)
        if (json != null) {
            try {
                val type = object : TypeToken<List<Connection>>() {}.type
                val saved: List<Connection> = gson.fromJson(json, type)
                _connections.clear()
                _connections.addAll(saved)
            } catch (e: Exception) { }
        }
    }
}