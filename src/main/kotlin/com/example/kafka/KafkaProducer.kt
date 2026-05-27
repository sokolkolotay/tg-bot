package com.example.kafka

import com.example.model.Task
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory
import java.util.Properties

class TaskKafkaProducer(
    bootstrapServers: String,
    private val topic: String
) {
    private val logger = LoggerFactory.getLogger(TaskKafkaProducer::class.java)

    private val producer = KafkaProducer<String, String>(Properties().apply {
        put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
        put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
        put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
        put(ProducerConfig.ACKS_CONFIG, "1")
        put(ProducerConfig.RETRIES_CONFIG, 3)
    })

    fun sendTask(task: Task) {
        val json = Json.encodeToString(task)
        val record = ProducerRecord(topic, task.chatId.toString(), json)

        try {
            producer.send(record) { metadata, exception ->
                if (exception != null) {
                    logger.error("Ошибка отправки в Kafka: ${exception.message}")
                } else {
                    logger.info("Задача отправлена в Kafka: topic=${metadata.topic()} partition=${metadata.partition()}")
                }
            }.get(10, java.util.concurrent.TimeUnit.SECONDS) // ждём подтверждения с таймаутом

        } catch (e: Exception) {
            logger.error("Ошибка при отправке в Kafka", e)
            throw e
        }
    }

    fun close() {
        producer.close()
    }
}