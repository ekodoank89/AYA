package com.aya.doank.core

import android.content.Context
import android.content.Intent

/**
 * Mendorong config spoof ke proses target via broadcast.
 * Arah ini bebas dari dinding package visibility, karena MANAGER yang mendeklarasikan
 * <queries> untuk package target — target tidak perlu tahu keberadaan AYA.
 */
class ConfigPusher(private val context: Context) {

    fun pushAll() = Targets.all.forEach { push(it) }

    fun push(target: SpoofTarget) {
        val sp = context.getSharedPreferences(Keys.PREFS_NAME, Context.MODE_PRIVATE)
        val active = sp.getBoolean(Keys.spoofActive(target.id), false)
        val lat = sp.getString(Keys.spoofLat(target.id), null)
        val lng = sp.getString(Keys.spoofLng(target.id), null)

        target.packageNames.forEach { pkg ->
            try {
                context.sendBroadcast(
                    Intent(ConfigPusher.ACTION).setPackage(pkg)
                        .putExtra(ConfigPusher.EXTRA_TARGET_ID, target.id)
                        .putExtra("active", active)
                        .putExtra("lat", lat)
                        .putExtra("lng", lng)
                )
            } catch (_: Throwable) { /* target tak terlihat / tidak ada — biarkan fallback */ }
        }
    }

    companion object {
        const val ACTION = "com.aya.doank.SPOOF_CONFIG"
        const val EXTRA_TARGET_ID = "target_id"
    }
}
