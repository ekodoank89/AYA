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
import kotlin.math.cos
import kotlin.math.sqrt

/**
 * Pembaca config SATU target. Rantai: push → remote → xsp.
 * v2.6: jitter 3-parameter dinamis (step/window/RADIUS per target)
 * dengan CLAMP VEKTOR — jangkauan lingkaran sempurna, bukan persegi.
 */
class SpoofConfig(private val targetId: String) {

    private var lastReload = 0L
    private var active = false
    private var baseLat = Double.NaN
    private var baseLng = Double.NaN
    private var lastLoggedActive = false
    private var transport = TRANSPORT_NONE
    private var loggedRemoteFail = false

    private var jStep = 2.5f
    private var jWin = 6
    private var jRadius = 3f
    private var lastLoggedJitter: String? = null
    private val jitter = Jitter()

    private var pushActive: Boolean? = null
    private var pushLat = Double.NaN
    private var pushLng = Double.NaN
    private var receiverRegistered = false

    private val xsp by lazy { XSharedPreferences(MODULE_PACKAGE, Keys.PREFS_NAME) }

    init {
        refresh(now = System.currentTimeMillis(), force = true)
        XposedBridge.log(
            "AYA [$targetId]: modul config dimuat (transport awal: $transport, " +
            "jitter: $jStep m / $jWin dtk / R$jRadius m)"
        )
    }

    fun latitude(): Double? = jittered()?.first
    fun longitude(): Double? = jittered()?.second

    private fun jittered(): Pair<Double, Double>? {
        refresh(System.currentTimeMillis())
        if (!active || baseLat.isNaN() || baseLng.isNaN()) return null
        return jitter.applyTo(baseLat, baseLng)
    }

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

    private fun applyJitter(step: Float?, win: Int?, radius: Float?) {
        step?.let { jStep = it }
        win?.let { jWin = it }
        radius?.let { jRadius = it }
        val key = "$jStep/$jWin/$jRadius"
        if (key != lastLoggedJitter) {
            lastLoggedJitter = key
            XposedBridge.log("AYA [$targetId]: jitter → $jStep m / $jWin dtk / R$jRadius m")
        }
    }

    private fun applyState(a: Boolean, la: Double, ln: Double, via: String) {
        val changed = (a != active) || (la != baseLat) || (ln != baseLng)
        active = a
        baseLat = la
        baseLng = ln
        setTransport(via)
        if (changed) jitter.onBaseChanged(la)
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
                    applyJitter(
                        i.getStringExtra("jit_step")?.toFloatOrNull(),
                        i.getStringExtra("jit_win")?.toIntOrNull(),
                        i.getStringExtra("jit_radius")?.toFloatOrNull()
                    )
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
            applyJitter(
                b.getString("jit_step")?.toFloatOrNull(),
                b.getString("jit_win")?.toIntOrNull(),
                b.getString("jit_radius")?.toFloatOrNull()
            )
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
            applyJitter(
                xsp.getString(Keys.jitStepKey(targetId), null)?.toFloatOrNull(),
                xsp.getString(Keys.jitWinKey(targetId), null)?.toIntOrNull(),
                xsp.getString(Keys.jitRadiusKey(targetId), null)?.toFloatOrNull()
            )
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
     * Random-walk GPS dengan CLAMP VEKTOR: magnitude offset dibatasi jRadius
     * (lingkaran sempurna — jangkauan sama di segala arah, sebaran bundar
     * seperti GPS asli). Langkah & jendela dari config dinamis per target.
     */
    private inner class Jitter {
        private val rnd = Random()
        private var oLat = 0.0
        private var oLng = 0.0
        private var windowStart = 0L
        private var baseRef = Double.NaN

        fun onBaseChanged(la: Double) {
            if (la != baseRef) {
                oLat = 0.0
                oLng = 0.0
                baseRef = la
            }
        }

        fun applyTo(baseLat: Double, baseLng: Double): Pair<Double, Double> {
            val now = System.currentTimeMillis()
            if (now - windowStart >= jWin * 1000L) {
                windowStart = now
                val mLat = 111320.0
                val mLng = 111320.0 * cos(Math.toRadians(baseLat))
                oLat += ((rnd.nextDouble() - 0.5) * jStep) / mLat
                oLng += ((rnd.nextDouble() - 0.5) * jStep) / mLng

                // ==== CLAMP VEKTOR (lingkaran sempurna) ====
                val dLatM = oLat * mLat
                val dLngM = oLng * mLng
                val dist = sqrt(dLatM * dLatM + dLngM * dLngM)
                if (dist > jRadius) {
                    val scale = jRadius / dist
                    oLat = (dLatM * scale) / mLat
                    oLng = (dLngM * scale) / mLng
                }
            }
            return (baseLat + oLat) to (baseLng + oLng)
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
