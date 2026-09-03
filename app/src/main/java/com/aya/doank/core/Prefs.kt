package com.aya.doank.core

import android.content.Context
import android.content.SharedPreferences

class Prefs(context: Context) {
    private val sp: SharedPreferences =
        context.getSharedPreferences(Keys.PREFS_NAME, Context.MODE_PRIVATE)

    init {
        cleanupLegacyIds()
    }

    /** Hardening: hapus key orphan dari era ID lama (idempoten). */
    private fun cleanupLegacyIds() {
        sp.edit().apply {
            listOf("grab", "gojek").forEach { old ->
                remove("spoof_${old}_active")
                remove("spoof_${old}_lat")
                remove("spoof_${old}_lng")
                remove("pkg_$old")
            }
        }.apply()
    }

    var isDark: Boolean
        get() = sp.getBoolean(Keys.IS_DARK, false)
        set(value) = sp.edit().putBoolean(Keys.IS_DARK, value).apply()

    var askedLocation: Boolean
        get() = sp.getBoolean(Keys.ASKED_LOCATION, false)
        set(value) = sp.edit().putBoolean(Keys.ASKED_LOCATION, value).apply()

    var askedBackground: Boolean
        get() = sp.getBoolean(Keys.ASKED_BACKGROUND, false)
        set(value) = sp.edit().putBoolean(Keys.ASKED_BACKGROUND, value).apply()

    var notifChainDone: Boolean
        get() = sp.getBoolean(Keys.NOTIF_CHAIN_DONE, false)
        set(value) = sp.edit().putBoolean(Keys.NOTIF_CHAIN_DONE, value).apply()

    var jitterAskAutostart: Boolean
        get() = sp.getBoolean(Keys.ASK_AUTOSTART, true)
        set(value) = sp.edit().putBoolean(Keys.ASK_AUTOSTART, value).apply()

    // ==== Jitter — PER TARGET (3 parameter, default dari kalibrasi per-app) ====
    fun jitterStep(id: String): Float =
        sp.getString(Keys.jitStepKey(id), null)?.toFloatOrNull() ?: 2.5f

    fun setJitterStep(id: String, value: Float) =
        sp.edit().putString(Keys.jitStepKey(id), value.toString()).apply()

    fun jitterWindowSec(id: String): Int =
        sp.getString(Keys.jitWinKey(id), null)?.toIntOrNull() ?: 6

    fun setJitterWindowSec(id: String, value: Int) =
        sp.edit().putString(Keys.jitWinKey(id), value.toString()).apply()

    fun jitterRadius(id: String): Float =
        sp.getString(Keys.jitRadiusKey(id), null)?.toFloatOrNull() ?: 3f

    fun setJitterRadius(id: String, value: Float) =
        sp.edit().putString(Keys.jitRadiusKey(id), value.toString()).apply()

    fun setSpoofActive(id: String, active: Boolean) =
        sp.edit().putBoolean(Keys.spoofActive(id), active).apply()

    fun isSpoofActive(id: String): Boolean = sp.getBoolean(Keys.spoofActive(id), false)

    /** Titik lock per target — String agar presisi double utuh (float bisa lenceng ±1 m). */
    fun setSpoofPoint(id: String, lat: Double, lng: Double) =
        sp.edit()
            .putString(Keys.spoofLat(id), lat.toString())
            .putString(Keys.spoofLng(id), lng.toString())
            .apply()

    fun spoofPoint(id: String): Pair<Double, Double>? {
        val lat = sp.getString(Keys.spoofLat(id), null)?.toDoubleOrNull() ?: return null
        val lng = sp.getString(Keys.spoofLng(id), null)?.toDoubleOrNull() ?: return null
        return lat to lng
    }
}
