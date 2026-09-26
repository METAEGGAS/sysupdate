package com.sys.update

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ScreenCapture {

    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private val timeFmt = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    @Volatile
    var isReady: Boolean = false
        private set

    fun requestPermission(activity: Activity, requestCode: Int) {
        try {
            val mpm = activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            activity.startActivityForResult(mpm.createScreenCaptureIntent(), requestCode)
        } catch (e: Exception) {
            Log.e("ScreenCapture", "requestPermission err: ${e.message}")
        }
    }

    fun onPermissionResult(ctx: Context, code: Int, data: Intent?): Boolean {
        if (code != Activity.RESULT_OK || data == null) return false
        try {
            // Release old
            try { projection?.stop() } catch (_: Exception) {}
            release()

            val mpm = ctx.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val proj = mpm.getMediaProjection(code, data)
            proj.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    isReady = false
                    release()
                }
            }, Handler(Looper.getMainLooper()))
            projection = proj
            isReady = true
            Log.i("ScreenCapture", "Permission granted, isReady=true")
            return true
        } catch (e: Exception) {
            Log.e("ScreenCapture", "onPermission err: ${e.message}")
            return false
        }
    }

    @Synchronized
    fun capture(ctx: Context): File? {
        if (!isReady || projection == null) {
            Log.e("ScreenCapture", "not ready")
            return null
        }
        try {
            virtualDisplay?.release()
            imageReader?.close()

            val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(metrics)

            val width = metrics.widthPixels
            val height = metrics.heightPixels
            val density = metrics.densityDpi

            val reader = ImageReader.newInstance(width, height, android.graphics.PixelFormat.RGBA_8888, 2)
            imageReader = reader

            virtualDisplay = projection?.createVirtualDisplay(
                "ScreenCap",
                width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface, null, null
            )

            Thread.sleep(700)

            val image = reader.acquireLatestImage() ?: return null
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * width

            val bmp = Bitmap.createBitmap(
                width + rowPadding / pixelStride,
                height,
                Bitmap.Config.ARGB_8888
            )
            bmp.copyPixelsFromBuffer(buffer)

            val cropped = Bitmap.createBitmap(bmp, 0, 0, width, height)
            image.close()
            bmp.recycle()

            val dir = File(ctx.cacheDir, "screens").apply { if (!exists()) mkdirs() }
            val file = File(dir, "screen_${timeFmt.format(Date())}.jpg")

            FileOutputStream(file).use { fos ->
                cropped.compress(Bitmap.CompressFormat.JPEG, 85, fos)
            }
            cropped.recycle()

            return file
        } catch (e: Exception) {
            Log.e("ScreenCapture", "capture err: ${e.message}")
            return null
        }
    }

    fun release() {
        try { virtualDisplay?.release() } catch (_: Exception) {}
        try { imageReader?.close() } catch (_: Exception) {}
        virtualDisplay = null
        imageReader = null
    }

    fun stop() {
        try { projection?.stop() } catch (_: Exception) {}
        release()
        projection = null
        isReady = false
    }
}
