package com.aya.doank.core

import android.content.Context
import android.content.Intent

/**
 * Mendorong config spoof ke proses target via broadcast.
 * v2.3: + setting jitter (step/window) ikut terkirim — hook membaca tanpa restart.
 */
class ConfigPusher(private val context: Context) {

    fun pushAll() = Targets.all.forEach { push(it) }

    fun push(target: SpoofTarget) {
        val sp = context.getSharedPreferences(Keys.PREFS_NAME, Context.MODE_PRIVATE)
        val active = sp.getBoolean(Keys.spoofActive(target.id), false)
        val lat = sp.getString(Keys.spoofLat(target.id), null)
        val lng = sp.getString(Keys.spoofLng(target.id), null)
        val jStep = sp.getString(Keys.JIT_STEP, null)
        val jWin = sp.getString(Keys.JIT_WINDOW, null)

        target.packageNames.forEach { pkg ->
            try {
                context.sendBroadcast(
                    Intent(ConfigPusher.ACTION).setPackage(pkg)
                        .putExtra(ConfigPusher.EXTRA_TARGET_ID, target.id)
                        .putExtra("active", active)
                        .putExtra("lat", lat)
                        .putExtra("lng", lng)
                        .putExtra("jit_step", jStep)
                        .putExtra("jit_win", jWin)
                )
            } catch (_: Throwable) { /* target tak terlihat — biarkan fallback */ }
        }
    }

    companion object {
        const val ACTION = "com.aya.doank.SPOOF_CONFIG"
        const val EXTRA_TARGET_ID = "target_id"
    }
}
