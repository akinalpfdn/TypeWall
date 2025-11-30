package com.akinalpfdn.typewall.model

import java.util.UUID

data class Card(
    val id: String = UUID.randomUUID().toString(),
    var x: Float,
    var y: Float,
    var width: Float,
    var content: String,
    var spans: List<CardSpan> = emptyList(),
    var cardColor: Long? = null // ARGB Long
)

data class CardSpan(
    val start: Int,
    val end: Int,
    val type: SpanType,
    val value: String? = null // Store Color Hex or Font Size
)

enum class SpanType {
    BOLD, ITALIC, UNDERLINE,
    TEXT_COLOR, BG_COLOR, FONT_SIZE
}