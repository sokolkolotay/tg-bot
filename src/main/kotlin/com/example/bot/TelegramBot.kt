package com.example.bot

import com.example.kafka.TaskKafkaProducer
import com.example.model.Task
import com.example.service.TaskService
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onText
import kotlinx.coroutines.CoroutineScope

class TelegramBot(
    private val token: String,
    private val kafkaProducer: TaskKafkaProducer,
    private val taskService: TaskService,
    private val scope: CoroutineScope
) {

    suspend fun start() {

        telegramBotWithBehaviourAndLongPolling(token, scope) {

            val me = getMe()
            println("Бот запущен: @${me.username?.username}")

            onText { message ->
                val text = message.content.text.trim()
                println("DEBUG: получено сообщение: '$text'")

                when {
                    text.startsWith("/start") -> {
                        sendTextMessage(
                            message.chat,
                            """
                            👋 Привет! Я бот для управления задачами
                            
                            Доступные команды:
                            /addtask Заголовок | Описание — создать задачу
                            /tasks — список всех задач
                            /help — помощь
                            """.trimIndent()
                        )
                    }

                    text.startsWith("/help") -> {
                        sendTextMessage(
                            message.chat,
                            """
                            📌 Формат создания задачи:
                            
                            /addtask Заголовок | Описание
                            
                            Пример:
                            /addtask Купить масло | Заехать на заправку
                            """.trimIndent()
                        )
                    }

                    text.startsWith("/addtask") -> {
                        val taskText = text.removePrefix("/addtask").trim()
                        println("DEBUG: addtask текст: '$taskText'")

                        if (taskText.isEmpty()) {
                            sendTextMessage(
                                message.chat,
                                """
                                📌 Укажи задачу в формате:
                                /addtask Заголовок | Описание
                                
                                Пример:
                                /addtask Купить масло | Заехать на заправку
                                """.trimIndent()
                            )
                            return@onText
                        }

                        val parts = taskText.split("|").map { it.trim() }
                        val title = parts.getOrNull(0).orEmpty()
                        val content = parts.getOrNull(1).orEmpty()

                        if (title.isBlank()) {
                            sendTextMessage(
                                message.chat,
                                "❌ Заголовок задачи не может быть пустым"
                            )
                            return@onText
                        }

                        val task = Task(
                            title = title,
                            content = content,
                            chatId = message.chat.id.chatId.long
                        )

                        try {
                            kafkaProducer.sendTask(task)
                            println("DEBUG: задача отправлена в Kafka: $title")
                            sendTextMessage(
                                message.chat,
                                """
                                ✅ Заметка успешно создана!
                                
                                📌 $title
                                📝 $content
                                """.trimIndent()
                            )
                        } catch (e: Exception) {
                            println("DEBUG: ошибка Kafka: ${e.message}")
                            e.printStackTrace()
                            sendTextMessage(
                                message.chat,
                                """
                                ❌ Не удалось создать заметку.
                                Возникли ошибки. Обратитесь к администратору.
                                """.trimIndent()
                            )
                        }
                    }

                    text.startsWith("/tasks") -> {
                        try {
                            val tasks = taskService.getTasks(message.chat.id.chatId.long)

                            if (tasks.isEmpty()) {
                                sendTextMessage(
                                    message.chat,
                                    "📭 У вас пока нет задач"
                                )
                                return@onText
                            }

                            val response = buildString {
                                appendLine("📋 Ваши задачи:")
                                appendLine()
                                tasks.forEachIndexed { index, task ->
                                    appendLine("${index + 1}. ${task.title}")
                                    if (task.content.isNotBlank()) {
                                        appendLine("   📝 ${task.content}")
                                    }
                                    appendLine()
                                }
                            }

                            sendTextMessage(message.chat, response)

                        } catch (e: Exception) {
                            println("DEBUG: ошибка получения задач: ${e.message}")
                            e.printStackTrace()
                            sendTextMessage(
                                message.chat,
                                "❌ Не удалось получить список задач"
                            )
                        }
                    }
                }
            }

        }.second.join()
    }
}