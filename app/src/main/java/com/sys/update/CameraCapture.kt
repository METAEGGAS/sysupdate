package com.sys.update

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object CameraCapture {

    private val timeFmt = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    @SuppressLint("MissingPermission")
    fun capture(ctx: Context, front: Boolean, timeoutSec: Long = 10): File? {
        val cm = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        var cameraId: String? = null

        try {
            for (id in cm.cameraIdList) {
                val chars = cm.getCameraCharacteristics(id)
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                if (front && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    cameraId = id; break
                }
                if (!front && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    cameraId = id; break
                }
            }
            if (cameraId == null) return null

            val chars = cm.getCameraCharacteristics(cameraId)
            val map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val size = map?.getOutputSizes(ImageFormat.JPEG)?.maxByOrNull { it.width * it.height }
                ?: return null

            val dir = File(ctx.cacheDir, "camera").apply { if (!exists()) mkdirs() }
            val outFile = File(dir, "cam_${timeFmt.format(Date())}.jpg")

            val thread = HandlerThread("cam").apply { start() }
            val handler = Handler(thread.looper)

            val latch = CountDownLatch(1)
            var device: CameraDevice? = null
            var session: CameraCaptureSession? = null
            var reader: ImageReader? = null
            var saved = false

            try {
                reader = ImageReader.newInstance(size.width, size.height, ImageFormat.JPEG, 1)
                reader.setOnImageAvailableListener({ r ->
                    try {
                        val img = r.acquireLatestImage() ?: return@setOnImageAvailableListener
                        val buf = img.planes[0].buffer
                        val bytes = ByteArray(buf.remaining())
                        buf.get(bytes)
                        FileOutputStream(outFile).use { it.write(bytes) }
                        img.close()
                        saved = true
                    } catch (e: Exception) {
                        Log.e("CameraCapture", "save err: ${e.message}")
                    } finally {
                        latch.countDown()
                    }
                }, handler)

                val stateCallback = object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        device = camera
                        try {
                            val surface = reader!!.surface
                            val builder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                            builder.addTarget(surface)
                            camera.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                                override fun onConfigured(s: CameraCaptureSession) {
                                    session = s
                                    try {
                                        val req = builder.build()
                                        s.capture(req, null, handler)
                                    } catch (e: Exception) {
                                        Log.e("CameraCapture", "capture err: ${e.message}")
                                        latch.countDown()
                                    }
                                }
                                override fun onConfigureFailed(s: CameraCaptureSession) {
                                    latch.countDown()
                                }
                            }, handler)
                        } catch (e: Exception) {
                            Log.e("CameraCapture", "open err: ${e.message}")
                            latch.countDown()
                        }
                    }
                    override fun onDisconnected(camera: CameraDevice) {
                        camera.close(); device = null; latch.countDown()
                    }
                    override fun onError(camera: CameraDevice, error: Int) {
                        camera.close(); device = null; latch.countDown()
                    }
                }

                cm.openCamera(cameraId, stateCallback, handler)
                latch.await(timeoutSec, TimeUnit.SECONDS)

                try { session?.close() } catch (_: Exception) {}
                try { device?.close() } catch (_: Exception) {}
                try { reader?.close() } catch (_: Exception) {}
                thread.quitSafely()
            } catch (e: Exception) {
                Log.e("CameraCapture", "err: ${e.message}")
                try { thread.quitSafely() } catch (_: Exception) {}
            }

            return if (saved && outFile.exists() && outFile.length() > 0) outFile else null
        } catch (e: Exception) {
            Log.e("CameraCapture", "fatal: ${e.message}")
            return null
        }
    }
}
