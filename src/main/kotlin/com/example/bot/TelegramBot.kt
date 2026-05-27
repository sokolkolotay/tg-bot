package com.example.bot

import com.example.kafka.TaskKafkaProducer
import com.example.service.TaskService
import com.example.model.Task
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
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

            onCommand("start") { message ->

                sendTextMessage(
                    message.chat,
                    """
                    👋 Привет! Я бот для управления задачами
                    
                    Доступные команды:
                    
                    /addtask — создать задачу
                    /tasks — список задач
                    /help — помощь
                    """.trimIndent()
                )
            }

            onCommand("help") { message ->

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

            onCommand("addtask") { message ->

                val rawText = message.content.text

                val text = rawText
                    .removePrefix("/addtask")
                    .trim()

                if (text.isEmpty()) {

                    sendTextMessage(
                        message.chat,
                        """
                        📌 Формат создания задачи:
                        
                        /addtask Заголовок | Описание
                        
                        Пример:
                        
                        /addtask Купить масло | Заехать на заправку
                        """.trimIndent()
                    )

                    return@onCommand
                }

                val parts = text.split("|")
                    .map { it.trim() }

                val title = parts.getOrNull(0).orEmpty()
                val content = parts.getOrNull(1).orEmpty()

                if (title.isBlank()) {

                    sendTextMessage(
                        message.chat,
                        "❌ Заголовок задачи не может быть пустым"
                    )

                    return@onCommand
                }

                val task = Task(
                    title = title,
                    content = content,
                    chatId = message.chat.id.chatId.long
                )

                try {

                    kafkaProducer.sendTask(task)

                    sendTextMessage(
                        message.chat,
                        """
                        ✅ Заметка успешно создана
                        
                        📌 $title
                        📝 $content
                        """.trimIndent()
                    )

                } catch (e: Exception) {

                    e.printStackTrace()

                    sendTextMessage(
                        message.chat,
                        """
                        ❌ Не удалось создать заметку
                        
                        Возникли ошибки.
                        Обратитесь к администратору.
                        """.trimIndent()
                    )
                }
            }

            onCommand("tasks") { message ->

                try {

                    val tasks = taskService.getTasks(
                        message.chat.id.chatId.long
                    )

                    if (tasks.isEmpty()) {

                        sendTextMessage(
                            message.chat,
                            "📭 У вас пока нет задач"
                        )

                        return@onCommand
                    }

                    val response = buildString {

                        appendLine("📋 Ваши задачи:")
                        appendLine()

                        tasks.forEachIndexed { index, task ->

                            appendLine("${index + 1}. ${task.title}")

                            if (task.content.isNotBlank()) {
                                appendLine("📝 ${task.content}")
                            }

                            appendLine()
                        }
                    }

                    sendTextMessage(
                        message.chat,
                        response
                    )

                } catch (e: Exception) {

                    e.printStackTrace()

                    sendTextMessage(
                        message.chat,
                        "❌ Не удалось получить список задач"
                    )
                }
            }
        }.second.join()
    }
}