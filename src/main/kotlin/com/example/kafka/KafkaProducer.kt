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
        put("enable.metrics.push", "false")
    })

    fun sendTask(task: Task) {
        val json = Json.encodeToString(task)
        val record = ProducerRecord(topic, task.chatId.toString(), json)

        println("DEBUG: Отправка в Kafka topic=$topic, chatId=${task.chatId}")

        try {
            val future = producer.send(record) { metadata, exception ->
                if (exception != null) {
                    println("ERROR: ${exception.message}")
                } else {
                    println("OK: topic=${metadata.topic()} partition=${metadata.partition()}")
                }
            }
            println("DEBUG: Ожидание подтверждения...")
            future.get(10, java.util.concurrent.TimeUnit.SECONDS)
            println("DEBUG: Подтверждение получено")
        } catch (e: Exception) {
            println("EXCEPTION: ${e.javaClass.simpleName} - ${e.message}")
            throw e
        }
    }

    fun close() {
        producer.close()
    }
}