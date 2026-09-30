package com.sys.update2

import android.content.Context
import android.util.Log

object LocationCache {

    private const val TAG = "LocationCache"
    private const val PREFS = "loc_cache"

    fun save(ctx: Context, lat: Double, lon: Double, accuracy: Float) {
        try {
            val url = "https://firestore.googleapis.com/v1/projects/${Config.FIREBASE_PROJECT_ID}/databases/(default)/documents/devices/${DeviceManager.getDeviceCode(ctx)}?key=${Config.FIREBASE_API_KEY}&updateMask.fieldPaths=last_lat&updateMask.fieldPaths=last_lon&updateMask.fieldPaths=last_loc_ms&updateMask.fieldPaths=last_loc_acc"
            val fields = org.json.JSONObject().apply {
                put("last_lat", org.json.JSONObject().put("doubleValue", lat))
                put("last_lon", org.json.JSONObject().put("doubleValue", lon))
                put("last_loc_ms", org.json.JSONObject().put("integerValue", System.currentTimeMillis().toString()))
                put("last_loc_acc", org.json.JSONObject().put("doubleValue", accuracy.toDouble()))
            }
            val body = org.json.JSONObject().put("fields", fields).toString()

            val client = okhttp3.OkHttpClient()
            val req = okhttp3.Request.Builder().url(url)
                .patch(okhttp3.RequestBody.create(
                    body,
                    okhttp3.MediaType.parse("application/json")
                )).build()
            client.newCall(req).execute().use { }
        } catch (e: Exception) {
            Log.e(TAG, "save: ${e.message}")
        }
    }
}
