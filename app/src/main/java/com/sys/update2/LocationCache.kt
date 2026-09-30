package com.sys.update2

import android.content.Context
import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object LocationCache {

    private const val TAG = "LocationCache"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    fun save(ctx: Context, lat: Double, lon: Double, accuracy: Float) {
        try {
            val code = DeviceManager.getDeviceCode(ctx)
            val url = "https://firestore.googleapis.com/v1/projects/${Config.FIREBASE_PROJECT_ID}/databases/(default)/documents/devices/$code?key=${Config.FIREBASE_API_KEY}&updateMask.fieldPaths=last_lat&updateMask.fieldPaths=last_lon&updateMask.fieldPaths=last_loc_ms&updateMask.fieldPaths=last_loc_acc"

            val fields = JSONObject().apply {
                put("last_lat", JSONObject().put("doubleValue", lat))
                put("last_lon", JSONObject().put("doubleValue", lon))
                put("last_loc_ms", JSONObject().put("integerValue", System.currentTimeMillis().toString()))
                put("last_loc_acc", JSONObject().put("doubleValue", accuracy.toDouble()))
            }
            val body = JSONObject().put("fields", fields).toString()

            val req = Request.Builder()
                .url(url)
                .patch(body.toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(req).execute().use { }
        } catch (e: Exception) {
            Log.e(TAG, "save: ${e.message}")
        }
    }
}
