package com.aya.doank.xposed

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import com.aya.doank.core.ConfigProvider
import com.aya.doank.core.ConfigPusher    // ← TAMBAHKAN INI
import com.aya.doank.core.Keys
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge

/**
 * Rantai jalur config: push (broadcast dari manager) → remote (provider) → XSP.
 * Push adalah satu-satunya jalur yang bebas dari dinding package visibility target.
 */
class SpoofConfig(private val targetId: String) {

    private var lastReload = 0L
    private var active = false
    private var lat = Double.NaN
    private var lng = Double.NaN
    private var lastLoggedActive = false
    private var transport = TRANSPORT_NONE
    private var loggedRemoteFail = false

    private var pushActive: Boolean? = null
    private var pushLat = Double.NaN
    private var pushLng = Double.NaN
    private var receiverRegistered = false

    private val xsp by lazy { XSharedPreferences(MODULE_PACKAGE, Keys.PREFS_NAME) }

    init {
        refresh(now = System.currentTimeMillis(), force = true)
        XposedBridge.log("AYA [$targetId]: modul config dimuat (transport awal: $transport)")
    }

    fun latitude(): Double? { refresh(System.currentTimeMillis()); return value(lat) }
    fun longitude(): Double? { refresh(System.currentTimeMillis()); return value(lng) }

    private fun value(v: Double): Double? = if (active && !v.isNaN()) v else null

    private fun refresh(now: Long, force: Boolean = false) {
        if (!force && now - lastReload < RELOAD_INTERVAL_MS) return
        lastReload = now

        ensurePushReceiver()

        if (pushActive != null) {                       // 1) push — terbaru dari manager
            applyState(pushActive!!, pushLat, pushLng, TRANSPORT_PUSH)
            return
        }
        if (readRemote()) return                        // 2) provider (Gojek: hidup)
        readXsp()                                       // 3) fallback terakhir
    }

    private fun applyState(a: Boolean, la: Double, ln: Double, via: String) {
        active = a; lat = la; lng = ln
        setTransport(via)
        if (active != lastLoggedActive) {
            lastLoggedActive = active
            if (active) XposedBridge.log("AYA [$targetId]: spoof AKTIF via $transport → $lat, $lng")
            else XposedBridge.log("AYA [$targetId]: spoof dimatikan (transport: $transport)")
        }
    }

    /** Receiver di proses target; butuh Application, jadi didaftarkan lazy saat pertama refresh. */
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

    companion object {
        private const val MODULE_PACKAGE = "com.aya.doank"
        private const val RELOAD_INTERVAL_MS = 1000L
        private const val TRANSPORT_NONE = "belum-terhubung"
        private const val TRANSPORT_REMOTE = "remote"
        private const val TRANSPORT_XSP = "xsp-fallback"
        private const val TRANSPORT_PUSH = "push"
    }
}
