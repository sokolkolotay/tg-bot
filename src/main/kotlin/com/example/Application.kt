package com.example

import com.example.bot.TelegramBot
import com.example.kafka.TaskKafkaProducer
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val token = System.getenv("TELEGRAM_BOT_TOKEN")
        ?: error("TELEGRAM_BOT_TOKEN не задан")

    val kafkaServers = System.getenv("KAFKA_BOOTSTRAP_SERVERS")
        ?: "localhost:9092"

    val kafkaTopic = System.getenv("KAFKA_TOPIC")
        ?: "tasks-created"

    val kafkaProducer = TaskKafkaProducer(
        bootstrapServers = kafkaServers,
        topic = kafkaTopic
    )

    println("Запуск бота...")
    println("Kafka: $kafkaServers → topic: $kafkaTopic")

    val bot = TelegramBot(
        token = token,
        kafkaProducer = kafkaProducer,
        scope = this
    )

    Runtime.getRuntime().addShutdownHook(Thread {
        kafkaProducer.close()
    })

    bot.start()
}