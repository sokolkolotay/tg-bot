package com.example.bot

import com.example.kafka.TaskKafkaProducer
import com.example.model.Task
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onText
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import kotlinx.coroutines.CoroutineScope

class TelegramBot(
    private val token: String,
    private val kafkaProducer: TaskKafkaProducer,
    private val scope: CoroutineScope
) {
    suspend fun start() {
        telegramBotWithBehaviourAndLongPolling(token, scope) {
            val me = getMe()
            println("Бот запущен: @${me.username?.username}")

            // /start — приветствие
            onCommand("start") { message ->
                sendTextMessage(
                    message.chat,
                    """
                    👋 Привет! Я бот для управления задачами.
                    
                    Команды:
                    /addtask Заголовок | Описание — создать задачу
                    /help — помощь
                    """.trimIndent()
                )
            }

            // /help
            onCommand("help") { message ->
                sendTextMessage(
                    message.chat,
                    """
                    📋 Как создать задачу:
                    /addtask Купить масло | Заехать на заправку
                    
                    Формат: /addtask Заголовок | Описание
                    """.trimIndent()
                )
            }

            // /addtask
            onCommand("addtask") { message ->
                val text = message.content.text
                    .removePrefix("/addtask")
                    .trim()

                if (text.isEmpty()) {
                    sendTextMessage(
                        message.chat,
                        "❌ Укажи задачу: /addtask Заголовок | Описание"
                    )
                    return@onCommand
                }

                val parts = text.split("|").map { it.trim() }
                val title = parts.getOrElse(0) { text }
                val content = parts.getOrElse(1) { "" }

                val task = Task(
                    title = title,
                    content = content,
                    chatId = message.chat.id.chatId.long
                )

                kafkaProducer.sendTask(task)

                sendTextMessage(
                    message.chat,
                    "✅ Задача отправлена!\n📌 *$title*\n📝 $content"
                )
            }

        }.second.join()
    }
}