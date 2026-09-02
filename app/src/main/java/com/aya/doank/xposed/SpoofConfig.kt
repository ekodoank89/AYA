package com.aya.doank.xposed

import android.app.Application
import android.net.Uri
import com.aya.doank.core.ConfigProvider
import com.aya.doank.core.Keys
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge

/**
 * Pembaca config SATU target. Rantai jalur: remote (Provider) → XSP (fallback).
 * Indikator kebenaran = log "transport config" dan "spoof AKTIF", bukan baris init.
 */
class SpoofConfig(private val targetId: String) {

    private var lastReload = 0L
    private var active = false
    private var lat = Double.NaN
    private var lng = Double.NaN
    private var lastLoggedActive = false
    private var transport = TRANSPORT_NONE
    private var loggedRemoteFail = false

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

        val got = readRemote() || readXsp()
        if (!got) return

        if (active != lastLoggedActive) {
            lastLoggedActive = active
            if (active) XposedBridge.log("AYA [$targetId]: spoof AKTIF via $transport → $lat, $lng")
            else XposedBridge.log("AYA [$targetId]: spoof dimatikan (transport: $transport)")
        }
    }

    /**
     * Application proses target via refleksi ActivityThread —
     * pengganti AndroidAppHelper yang tidak tersedia di stub api-82.
     * Bisa null di awal umur proses → caller menangani (retry di tick berikutnya).
     */
    private fun currentApplication(): Application? = try {
        Class.forName("android.app.ActivityThread")
            .getMethod("currentApplication")
            .invoke(null) as? Application
    } catch (t: Throwable) {
        null
    }

    /** Jalur utama: ContentProvider milik manager — IPC Binder standar, tanpa library. */
    private fun readRemote(): Boolean {
        return try {
            val app = currentApplication() ?: return false
            val b = app.contentResolver.call(
                Uri.parse("content://${ConfigProvider.AUTHORITY}"),
                ConfigProvider.METHOD_SPOOF, targetId, null
            ) ?: return false
            active = b.getBoolean("active", false)
            lat = b.getString("lat")?.toDoubleOrNull() ?: Double.NaN
            lng = b.getString("lng")?.toDoubleOrNull() ?: Double.NaN
            setTransport(TRANSPORT_REMOTE)
            true
        } catch (t: Throwable) {
            if (!loggedRemoteFail) {
                loggedRemoteFail = true
                XposedBridge.log("AYA [$targetId]: jalur remote gagal → fallback XSP. Penyebab: $t")
            }
            false
        }
    }

    /** Fallback: XSharedPreferences — deprecated, tapi disertakan sebagai jaring pengaman. */
    private fun readXsp(): Boolean {
        return try {
            xsp.reload()
            active = xsp.getBoolean(Keys.spoofActive(targetId), false)
            lat = xsp.getString(Keys.spoofLat(targetId), null)?.toDoubleOrNull() ?: Double.NaN
            lng = xsp.getString(Keys.spoofLng(targetId), null)?.toDoubleOrNull() ?: Double.NaN
            setTransport(TRANSPORT_XSP)
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
    }
}
