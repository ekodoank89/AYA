package com.aya.doank.xposed

import com.aya.doank.core.Keys
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge

/**
 * Pembaca config spoof untuk SATU target di dalam proses app target.
 * Dibaca lewat daemon LSPosed (XSharedPreferences), throttle 1 detik.
 */
class SpoofConfig(private val targetId: String) {

    private val sp = XSharedPreferences(MODULE_PACKAGE, Keys.PREFS_NAME)
    private var lastReload = 0L
    private var active = false
    private var lat = Double.NaN
    private var lng = Double.NaN
    private var lastLoggedActive = false

    init {
        refresh(now = System.currentTimeMillis(), force = true)
        // Catatan: file.exists() dari proses target SELALU false (SELinux) — bukan indikasi masalah.
        XposedBridge.log(
            "AYA [$targetId]: config awal → active=$active, lat=$lat, lng=$lng " +
            "(prefs: ${MODULE_PACKAGE}/${Keys.PREFS_NAME})"
        )
    }

    fun latitude(): Double? { refresh(System.currentTimeMillis()); return value(lat) }
    fun longitude(): Double? { refresh(System.currentTimeMillis()); return value(lng) }

    private fun value(v: Double): Double? = if (active && !v.isNaN()) v else null

    private fun refresh(now: Long, force: Boolean = false) {
        if (!force && now - lastReload < RELOAD_INTERVAL_MS) return
        lastReload = now
        try {
            sp.reload()
            active = sp.getBoolean(Keys.spoofActive(targetId), false)
            lat = sp.getString(Keys.spoofLat(targetId), null)?.toDoubleOrNull() ?: Double.NaN
            lng = sp.getString(Keys.spoofLng(targetId), null)?.toDoubleOrNull() ?: Double.NaN
            if (active != lastLoggedActive) {
                lastLoggedActive = active
                if (active) XposedBridge.log("AYA [$targetId]: spoof AKTIF → $lat, $lng")
                else XposedBridge.log("AYA [$targetId]: spoof dimatikan")
            }
        } catch (t: Throwable) {
            XposedBridge.log("AYA [$targetId]: gagal baca config: $t")
        }
    }

    companion object {
        private const val MODULE_PACKAGE = "com.aya.doank"
        private const val RELOAD_INTERVAL_MS = 1000L
    }
}
