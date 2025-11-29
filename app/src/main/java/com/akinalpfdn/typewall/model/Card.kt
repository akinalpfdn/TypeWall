package com.akinalpfdn.typewall.model

import java.util.UUID

data class Card(
    val id: String = UUID.randomUUID().toString(),
    var x: Float,
    var y: Float,
    var width: Float,
    var content: String
)