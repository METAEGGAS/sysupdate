package com.sys.update2

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object LocationHelper {

    private val timeFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    fun hasPermission(ctx: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    fun getPreciseLocation(ctx: Context, timeoutSec: Long = 15): Location? {
        if (!hasPermission(ctx)) return null
        val last = getLastKnown(ctx)
        val fresh = requestFresh(ctx, timeoutSec) ?: return last
        return fresh
    }

    @SuppressLint("MissingPermission")
    private fun getLastKnown(ctx: Context): Location? {
        try {
            val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            var best: Location? = null
            for (p in providers) {
                try {
                    val loc = lm.getLastKnownLocation(p) ?: continue
                    if (best == null || loc.time > best.time) best = loc
                } catch (_: Exception) {}
            }
            return best
        } catch (_: Exception) { return null }
    }

    @SuppressLint("MissingPermission")
    private fun requestFresh(ctx: Context, timeoutSec: Long): Location? {
        try {
            val client: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(ctx)
            val latch = CountDownLatch(1)
            var result: Location? = null

            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
                .setMinUpdateIntervalMillis(500L)
                .setMaxUpdates(5)
                .setWaitForAccurateLocation(true)
                .build()

            val callback = object : LocationCallback() {
                override fun onLocationResult(r: LocationResult) {
                    val loc = r.lastLocation
                    if (loc != null) {
                        if (result == null || loc.accuracy < (result?.accuracy ?: Float.MAX_VALUE)) {
                            result = loc
                        }
                        if (loc.accuracy <= 5f) latch.countDown()
                    }
                }
            }

            try {
                client.requestLocationUpdates(request, callback, Looper.getMainLooper())
                latch.await(timeoutSec, TimeUnit.SECONDS)
            } finally {
                try { client.removeLocationUpdates(callback) } catch (_: Exception) {}
            }

            return result
        } catch (e: Exception) {
            Log.e("LocationHelper", "fresh err: ${e.message}")
            return null
        }
    }

    fun formatLocation(ctx: Context, loc: Location?): String {
        if (loc == null) {
            return "📍 *الموقع*\n\n❌ تعذّر الحصول على الموقع\n\n" +
                    "تأكد من:\n• تفعيل GPS\n• منح صلاحية الموقع\n• الاتصال بالإنترنت"
        }

        val sb = StringBuilder()
        sb.append("📍 *الموقع الحالي*\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━\n\n")
        sb.append("🌐 *Latitude:* `${loc.latitude}`\n")
        sb.append("🌐 *Longitude:* `${loc.longitude}`\n")
        sb.append("📏 *الدقة:* ±${loc.accuracy.toInt()} متر\n")

        if (loc.hasAltitude()) sb.append("⛰ *الارتفاع:* ${loc.altitude.toInt()} م\n")
        if (loc.hasSpeed()) sb.append("🏃 *السرعة:* ${String.format("%.2f", loc.speed * 3.6)} كم/س\n")

        sb.append("\n🔗 *الخريطة:*\n")
        sb.append("https://www.google.com/maps?q=${loc.latitude},${loc.longitude}\n\n")
        sb.append("🕐 *وقت القياس:* ${timeFmt.format(Date(loc.time))}\n")

        return sb.toString()
    }
}
