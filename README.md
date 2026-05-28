# 🤖 Telegram Bot — Notes Task Manager

Telegram бот на Kotlin для создания и просмотра задач. Работает в связке с Notes API через Apache Kafka.

## 🏗️ Архитектура
Пользователь → Telegram → tg-bot → Kafka (topic: tasks-created) → Ktor API → PostgreSQL
↑
GET /tasks (HTTP)

## ⚙️ Технологии

| Слой | Технология |
|---|---|
| Язык | Kotlin |
| Telegram | TelegramBotAPI (dev.inmo) |
| Message Broker | Apache Kafka 3.7 |
| HTTP клиент | Ktor Client CIO |
| Сериализация | kotlinx.serialization |
| Контейнеризация | Docker |
| CI/CD | GitHub Actions → Docker Hub → k3s |

## 💬 Команды бота

| Команда | Описание |
|---|---|
| `/start` | Приветствие и список команд |
| `/addtask Заголовок \| Описание` | Создать задачу |
| `/tasks` | Список всех задач |
| `/help` | Помощь по командам |

## 🔄 Как работает

1. Пользователь отправляет `/addtask Купить масло | Заехать на заправку`
2. Бот публикует сообщение в Kafka топик `tasks-created`
3. Ktor API читает сообщение из Kafka и сохраняет в PostgreSQL
4. `/tasks` запрашивает задачи напрямую через HTTP GET `/tasks`

## 🔄 CI/CD Pipeline

При каждом пуше в `main`:
1. GitHub Actions собирает Docker образ
2. Пушит на Docker Hub
3. Деплоит на k3s через Helm upgrade
