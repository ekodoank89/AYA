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

    // State play per target — persist, bertahan walau app ditutup/berotasi
    fun setSpoofActive(id: String, active: Boolean) =
        sp.edit().putBoolean(Keys.spoofActive(id), active).apply()

    fun isSpoofActive(id: String): Boolean = sp.getBoolean(Keys.spoofActive(id), false)

    // Titik pin saat tombol play ditekan (dibaca hook nanti). String = presisi double utuh.
    fun setSpoofPoint(lat: Double, lng: Double) =
        sp.edit().putString(Keys.SPOOF_LAT, lat.toString())
            .putString(Keys.SPOOF_LNG, lng.toString()).apply()

    fun spoofPoint(): Pair<Double, Double>? {
        val lat = sp.getString(Keys.SPOOF_LAT, null)?.toDoubleOrNull() ?: return null
        val lng = sp.getString(Keys.SPOOF_LNG, null)?.toDoubleOrNull() ?: return null
        return lat to lng
    }
}
