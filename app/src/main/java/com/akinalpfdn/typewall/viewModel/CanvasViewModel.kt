package com.akinalpfdn.typewall.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.akinalpfdn.typewall.model.Card
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

private const val TAG = "CanvasVMDebug"

class CanvasViewModel(application: Application) : AndroidViewModel(application) {
    private val _cards = mutableStateListOf<Card>()
    val cards: List<Card> get() = _cards

    var scale by mutableStateOf(1f)
    var offsetX by mutableStateOf(0f)
    var offsetY by mutableStateOf(0f)

    private val gson = Gson()
    private val prefs = application.getSharedPreferences("typewall_prefs", Context.MODE_PRIVATE)

    init {
        loadCards()
    }

    fun addCard(x: Float, y: Float) {
        Log.d(TAG, "addCard called at x=$x, y=$y")

        val canvasX = (x - offsetX) / scale - 100f
        val canvasY = (y - offsetY) / scale - 20f

        val newCard = Card(
            x = canvasX,
            y = canvasY,
            width = 250f,
            content = ""
        )
        _cards.add(newCard)
        Log.d(TAG, "Card added to list. New size: ${_cards.size}")
        saveCards()
    }

    fun updateCard(id: String, content: String? = null, x: Float? = null, y: Float? = null, width: Float? = null) {
        val index = _cards.indexOfFirst { it.id == id }
        if (index != -1) {
            val oldCard = _cards[index]
            _cards[index] = oldCard.copy(
                content = content ?: oldCard.content,
                x = x ?: oldCard.x,
                y = y ?: oldCard.y,
                width = width?.coerceAtLeast(200f) ?: oldCard.width
            )
            saveCards()
        }
    }

    fun removeCard(id: String) {
        Log.d(TAG, "Removing card $id")
        _cards.removeAll { it.id == id }
        saveCards()
    }

    fun cleanupEmptyCard(id: String) {
        val card = _cards.find { it.id == id }
        if (card != null && card.content.isBlank()) {
            Log.d(TAG, "Cleaning up empty card $id")
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
                _cards.addAll(savedCards)
                Log.d(TAG, "Loaded ${savedCards.size} cards from storage")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading cards", e)
            }
        }
    }
}