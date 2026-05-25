package com.example.model

import kotlinx.serialization.Serializable

@Serializable
data class Task(
    val title: String,
    val content: String,
    val chatId: Long,
    val createdAt: String = java.time.LocalDateTime.now().toString()
)