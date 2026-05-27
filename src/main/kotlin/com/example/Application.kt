package com.example

import com.example.bot.TelegramBot
import com.example.kafka.TaskKafkaProducer
import com.example.service.HttpTaskService
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {

    val token = System.getenv("TELEGRAM_BOT_TOKEN")
        ?: error("TELEGRAM_BOT_TOKEN не задан")

    val kafkaServers = System.getenv("KAFKA_BOOTSTRAP_SERVERS")
        ?: "localhost:9092"

    val kafkaTopic = System.getenv("KAFKA_TOPIC")
        ?: "tasks-created"

    val apiUrl = System.getenv("TASK_API_URL")
        ?: "http://localhost:8080"

    val kafkaProducer = TaskKafkaProducer(
        bootstrapServers = kafkaServers,
        topic = kafkaTopic
    )

    val httpClient = HttpClient(CIO)

    val taskService = HttpTaskService(
        client = httpClient,
        baseUrl = apiUrl
    )

    println("Запуск бота...")
    println("Kafka: $kafkaServers")
    println("API: $apiUrl")

    val bot = TelegramBot(
        token = token,
        kafkaProducer = kafkaProducer,
        taskService = taskService,
        scope = this
    )

    Runtime.getRuntime().addShutdownHook(
        Thread {
            kafkaProducer.close()
        }
    )

    bot.start()
}