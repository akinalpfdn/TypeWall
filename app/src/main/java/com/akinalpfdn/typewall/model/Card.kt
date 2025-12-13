package com.akinalpfdn.typewall.model

import java.util.UUID

data class Card(
    val id: String = UUID.randomUUID().toString(),
    var x: Float,
    var y: Float,
    var width: Float,
    var content: String,
    var title: String = "",
    var spans: List<CardSpan>? = emptyList(),
    var cardColor: Long? = null // ARGB Long
) {
    fun isVisuallyEmpty(): Boolean {
        // Strip HTML tags and zero-width spaces to check true emptiness
        val plain = content.replace(Regex("<[^>]*>"), "")
            .replace("&nbsp;", " ")
            .trim()
        return plain.isBlank() && title.isBlank()
    }
}

data class CardSpan(
    val start: Int,
    val end: Int,
    val type: SpanType,
    val value: String? = null // Store Color Hex or Font Size
)

enum class SpanType {
    BOLD, ITALIC, UNDERLINE, STRIKETHROUGH,
    TEXT_COLOR, BG_COLOR, FONT_SIZE,
    ALIGN_LEFT, ALIGN_CENTER, ALIGN_RIGHT,
    QUOTE, CODE, LINK, CHECKBOX
}