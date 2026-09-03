package com.aya.doank.core

import android.content.Context
import android.content.SharedPreferences

class Prefs(context: Context) {
    private val sp: SharedPreferences =
        context.getSharedPreferences(Keys.PREFS_NAME, Context.MODE_PRIVATE)

    init {
        // Hardening: hapus key orphan dari era ID lama (idempoten — aman dipanggil berulang)
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

    // ==== v2.4: rantai izin ====
    var askedBackground: Boolean
        get() = sp.getBoolean(Keys.ASKED_BACKGROUND, false)
        set(value) = sp.edit().putBoolean(Keys.ASKED_BACKGROUND, value).apply()

    var notifChainDone: Boolean
        get() = sp.getBoolean(Keys.NOTIF_CHAIN_DONE, false)
        set(value) = sp.edit().putBoolean(Keys.NOTIF_CHAIN_DONE, value).apply()

    var jitterAskAutostart: Boolean
        get() = sp.getBoolean(Keys.ASK_AUTOSTART, true)
        set(value) = sp.edit().putBoolean(Keys.ASK_AUTOSTART, value).apply()

    // ==== Jitter settings ====
    var jitterStep: Float
        get() = sp.getString(Keys.JIT_STEP, null)?.toFloatOrNull() ?: 2.5f
        set(value) = sp.edit().putString(Keys.JIT_STEP, value.toString()).apply()

    var jitterWindowSec: Int
        get() = sp.getString(Keys.JIT_WINDOW, null)?.toIntOrNull() ?: 6
        set(value) = sp.edit().putString(Keys.JIT_WINDOW, value.toString()).apply()

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
