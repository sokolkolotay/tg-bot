package com.example.service

import com.example.model.Task
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

class HttpTaskService(
    private val client: HttpClient,
    private val baseUrl: String
) : TaskService {

    override suspend fun getTasks(
        chatId: Long
    ): List<Task> {

        return client.get("$baseUrl/tasks") {
            parameter("chatId", chatId)
        }.body()
    }
}