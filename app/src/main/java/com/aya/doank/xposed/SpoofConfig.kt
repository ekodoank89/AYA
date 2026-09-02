package com.aya.doank.xposed

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import com.aya.doank.core.ConfigProvider
import com.aya.doank.core.ConfigPusher
import com.aya.doank.core.Keys
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import java.util.Random
import kotlin.math.abs
import kotlin.math.cos

/**
 * Pembaca config SATU target. Rantai: push → remote → xsp.
 * v2.1: + Jitter GPS (random-walk) diterapkan saat penyajian koordinat.
 */
class SpoofConfig(private val targetId: String) {

    // ==== State config (tidak berubah dari v2.0.1) ====
    private var lastReload = 0L
    private var active = false
    private var baseLat = Double.NaN
    private var baseLng = Double.NaN
    private var lastLoggedActive = false
    private var transport = TRANSPORT_NONE
    private var loggedRemoteFail = false

    private var pushActive: Boolean? = null
    private var pushLat = Double.NaN
    private var pushLng = Double.NaN
    private var receiverRegistered = false

    private val xsp by lazy { XSharedPreferences(MODULE_PACKAGE, Keys.PREFS_NAME) }

    // ==== Jitter (v2.1) ====
    private val jitter = Jitter()

    init {
        refresh(now = System.currentTimeMillis(), force = true)
        XposedBridge.log("AYA [$targetId]: modul config dimuat (transport awal: $transport)")
    }

    fun latitude(): Double? { refresh(System.currentTimeMillis()); return jitteredLat() }
    fun longitude(): Double? { refresh(System.currentTimeMillis()); return jitteredLng() }

    /** Koordinat dasar (pin) + offset jitter yang konsisten dalam satu jendela 6 dtk. */
    private fun jitteredLat(): Double? {
        val b = value(baseLat) ?: return null
        return jitter.applyTo(b, baseLng).first
    }
    private fun jitteredLng(): Double? {
        val b = value(baseLng) ?: return null
        return jitter.applyTo(baseLat, b).second
    }

    private fun value(v: Double): Double? = if (active && !v.isNaN()) v else null

    private fun refresh(now: Long, force: Boolean = false) {
        if (!force && now - lastReload < RELOAD_INTERVAL_MS) return
        lastReload = now

        ensurePushReceiver()

        if (pushActive != null) {
            applyState(pushActive!!, pushLat, pushLng, TRANSPORT_PUSH)
            return
        }
        if (readRemote()) return
        readXsp()
    }

    private fun applyState(a: Boolean, la: Double, ln: Double, via: String) {
        val changed = (a != active) || (la != baseLat) || (ln != baseLng)
        active = a; baseLat = la; baseLng = ln
        setTransport(via)
        if (changed) jitter.onBaseChanged(la, ln)   // base pindah → reset walk
        if (active != lastLoggedActive) {
            lastLoggedActive = active
            if (active) XposedBridge.log("AYA [$targetId]: spoof AKTIF via $transport → $la, $ln")
            else XposedBridge.log("AYA [$targetId]: spoof dimatikan (transport: $transport)")
        }
    }

    private fun ensurePushReceiver() {
        if (receiverRegistered) return
        val app = currentApplication() ?: return
        try {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(c: Context?, i: Intent?) {
                    if (i?.getStringExtra(ConfigPusher.EXTRA_TARGET_ID) != targetId) return
                    val a = i.getBooleanExtra("active", false)
                    val la = i.getStringExtra("lat")?.toDoubleOrNull() ?: Double.NaN
                    val ln = i.getStringExtra("lng")?.toDoubleOrNull() ?: Double.NaN
                    pushActive = a; pushLat = la; pushLng = ln
                    XposedBridge.log("AYA [$targetId]: push diterima → active=$a, $la, $ln")
                    applyState(a, la, ln, TRANSPORT_PUSH)
                }
            }
            val filter = IntentFilter(ConfigPusher.ACTION)
            if (Build.VERSION.SDK_INT >= 33) {
                app.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                app.registerReceiver(receiver, filter)
            }
            receiverRegistered = true
            XposedBridge.log("AYA [$targetId]: push receiver terpasang")
        } catch (t: Throwable) {
            XposedBridge.log("AYA [$targetId]: gagal daftar push receiver: $t")
        }
    }

    private fun currentApplication(): Application? = try {
        Class.forName("android.app.ActivityThread")
            .getMethod("currentApplication")
            .invoke(null) as? Application
    } catch (t: Throwable) { null }

    private fun readRemote(): Boolean {
        return try {
            val app = currentApplication() ?: return false
            val b = app.contentResolver.call(
                Uri.parse("content://${ConfigProvider.AUTHORITY}"),
                ConfigProvider.METHOD_SPOOF, targetId, null
            ) ?: return false
            applyState(
                b.getBoolean("active", false),
                b.getString("lat")?.toDoubleOrNull() ?: Double.NaN,
                b.getString("lng")?.toDoubleOrNull() ?: Double.NaN,
                TRANSPORT_REMOTE
            )
            true
        } catch (t: Throwable) {
            if (!loggedRemoteFail) {
                loggedRemoteFail = true
                XposedBridge.log("AYA [$targetId]: jalur remote gagal → fallback. Penyebab: $t")
            }
            false
        }
    }

    private fun readXsp(): Boolean {
        return try {
            xsp.reload()
            applyState(
                xsp.getBoolean(Keys.spoofActive(targetId), false),
                xsp.getString(Keys.spoofLat(targetId), null)?.toDoubleOrNull() ?: Double.NaN,
                xsp.getString(Keys.spoofLng(targetId), null)?.toDoubleOrNull() ?: Double.NaN,
                TRANSPORT_XSP
            )
            true
        } catch (t: Throwable) {
            XposedBridge.log("AYA [$targetId]: XSP fallback juga gagal: $t")
            false
        }
    }

    private fun setTransport(t: String) {
        if (transport != t) {
            transport = t
            XposedBridge.log("AYA [$targetId]: transport config = $t")
        }
    }

    /**
     * Random-walk GPS: offset bergerak bertahap dalam radius RADIUS_METERS,
     * arah/kecepatan baru tiap WINDOW_MS. Satu jendela = satu posisi konsisten
     * untuk SEMUA pembacaan (getter maupun rewrite field) — tidak ada koordinat
     * yang "berpindah" di tengah satu objek Location.
     */
    private class Jitter {
        private val rnd = Random()
        private var oLat = 0.0
        private var oLng = 0.0
        private var windowStart = 0L
        private var baseLat = Double.NaN

        fun onBaseChanged(la: Double, ln: Double) {
            if (la != baseLat) {           // pin dipindah → mulai walk baru dari 0
                oLat = 0.0; oLng = 0.0
                baseLat = la
            }
        }

        /** Mengembalikan (lat, lng) dengan offset jendela berjalan. */
        fun applyTo(baseLat: Double, baseLng: Double): Pair<Double, Double> {
            val now = System.currentTimeMillis()
            if (now - windowStart >= WINDOW_MS) {
                windowStart = now
                val mPerDegLat = 111_320.0
                val mPerDegLng = 111_320.0 * cos(Math.toRadians(baseLat))
                // langkah acak kecil, diklem agar total offset tak lari dari radius
                val dLat = (rnd.nextDouble() - 0.5) * STEP_METERS / mPerDegLat
                val dLng = (rnd.nextDouble() - 0.5) * STEP_METERS / mPerDegLng
                oLat = (oLat + dLat).coerceIn(-MAX_OFF_METERS / mPerDegLat, MAX_OFF_METERS / mPerDegLat)
                oLng = (oLng + dLng).coerceIn(-MAX_OFF_METERS / mPerDegLng, MAX_OFF_METERS / mPerDegLng)
            }
            return (baseLat + oLat) to (baseLng + oLng)
        }

        companion object {
            private const val WINDOW_MS = 6_000L
            private const val STEP_METERS = 2.5
            private const val MAX_OFF_METERS = 5.0
        }
    }

    companion object {
        private const val MODULE_PACKAGE = "com.aya.doank"
        private const val RELOAD_INTERVAL_MS = 1000L
        private const val TRANSPORT_NONE = "belum-terhubung"
        private const val TRANSPORT_REMOTE = "remote"
        private const val TRANSPORT_XSP = "xsp-fallback"
        private const val TRANSPORT_PUSH = "push"
    }
}
