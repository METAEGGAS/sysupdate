package com.sys.update

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AudioRecorder {

    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var running = false

    @Volatile
    var isActive: Boolean = false
        private set

    private val timeFmt = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    @Synchronized
    fun startChunk(ctx: Context): File? {
        try {
            stopChunk() // stop previous if any

            val dir = File(ctx.cacheDir, "audio").apply { if (!exists()) mkdirs() }
            val file = File(dir, "rec_${timeFmt.format(Date())}.m4a")

            val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(ctx)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            rec.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(Config.AUDIO_SAMPLE_RATE)
                setAudioEncodingBitRate(Config.AUDIO_BITRATE)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            recorder = rec
            currentFile = file
            return file
        } catch (e: Exception) {
            Log.e("AudioRecorder", "start err: ${e.message}")
            cleanup()
            return null
        }
    }

    @Synchronized
    fun stopChunk(): File? {
        val file = currentFile
        try {
            recorder?.apply {
                try { stop() } catch (_: Exception) {}
                try { release() } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
        recorder = null
        currentFile = null
        return if (file != null && file.exists() && file.length() > 1000) file else null
    }

    @Synchronized
    fun startLoop(ctx: Context) {
        if (running) return
        running = true
        isActive = true
    }

    @Synchronized
    fun stopLoop(ctx: Context) {
        running = false
        isActive = false
        stopChunk()
    }

    fun isRunning(): Boolean = running

    private fun cleanup() {
        try { recorder?.release() } catch (_: Exception) {}
        recorder = null
        currentFile = null
    }
}
