package com.example.service

import com.example.model.Task

interface TaskService {

    suspend fun getTasks(chatId: Long): List<Task>
}