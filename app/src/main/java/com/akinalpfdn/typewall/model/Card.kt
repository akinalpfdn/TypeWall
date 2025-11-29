package com.akinalpfdn.typewall.model

import java.util.UUID

data class Card(
    val id: String = UUID.randomUUID().toString(),
    var x: Float,
    var y: Float,
    var width: Float,
    var content: String,
    // Store styling information for JSON serialization
    var spans: List<CardSpan> = emptyList()
)

data class CardSpan(
    val start: Int,
    val end: Int,
    val type: SpanType
)

enum class SpanType { BOLD, ITALIC, UNDERLINE }