package com.aya.doank.core

import android.content.Context
import android.content.SharedPreferences

class Prefs(context: Context) {
    private val sp: SharedPreferences =
        context.getSharedPreferences(Keys.PREFS_NAME, Context.MODE_PRIVATE)

    var isDark: Boolean
        get() = sp.getBoolean(Keys.IS_DARK, false)
        set(value) = sp.edit().putBoolean(Keys.IS_DARK, value).apply()

    var askedLocation: Boolean
        get() = sp.getBoolean(Keys.ASKED_LOCATION, false)
        set(value) = sp.edit().putBoolean(Keys.ASKED_LOCATION, value).apply()

    fun setSpoofActive(id: String, active: Boolean) =
        sp.edit().putBoolean(Keys.spoofActive(id), active).apply()

    fun isSpoofActive(id: String): Boolean = sp.getBoolean(Keys.spoofActive(id), false)

    /** Titik lock per target. String agar presisi double utuh (float = bisa lenceng ±1 meter). */
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
