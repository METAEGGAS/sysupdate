package com.sys.update

import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object VideoRecorder {

    private var mediaRecorder: MediaRecorder? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var projection: MediaProjection? = null
    private var outputFile: File? = null

    private val timeFmt = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    @Volatile
    var isRecording: Boolean = false
        private set

    fun start(ctx: Context, durationSec: Int = 30): File? {
        if (isRecording) return null
        if (!ScreenCapture.isReady) return null

        try {
            val proj = ScreenCapture.getProjection() ?: return null
            projection = proj

            val dir = File(ctx.cacheDir, "videos").apply { if (!exists()) mkdirs() }
            val file = File(dir, "video_${timeFmt.format(Date())}.mp4")
            outputFile = file

            val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(metrics)

            val width = metrics.widthPixels
            val height = metrics.heightPixels
            val density = metrics.densityDpi

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(ctx)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setVideoSize(width, height)
                setVideoFrameRate(24)
                setVideoEncodingBitRate(3_000_000)
                setOutputFile(file.absolutePath)
                prepare()
            }

            virtualDisplay = projection?.createVirtualDisplay(
                "VideoRec",
                width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                recorder.surface, null, null
            )

            recorder.start()
            mediaRecorder = recorder
            isRecording = true

            // إيقاف تلقائي بعد المدة
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                stop()
            }, durationSec * 1000L)

            return file

        } catch (e: Exception) {
            Log.e("VideoRecorder", "start err: ${e.message}")
            cleanup()
            return null
        }
    }

    fun stop(): File? {
        if (!isRecording) return outputFile
        try {
            mediaRecorder?.apply {
                try { stop() } catch (_: Exception) {}
                try { release() } catch (_: Exception) {}
            }
            virtualDisplay?.release()
        } catch (_: Exception) {}
        cleanup()
        isRecording = false
        return outputFile
    }

    private fun cleanup() {
        mediaRecorder = null
        virtualDisplay = null
        projection = null
    }
}
