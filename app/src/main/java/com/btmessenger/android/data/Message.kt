package com.btmessenger.android.data

import java.util.Date
import java.util.UUID

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val timestamp: Date = Date(),
    val isOutgoing: Boolean,
    val senderName: String = ""
)
