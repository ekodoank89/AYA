package com.aya.doank.core

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle

/** v2.6: menyajikan config + 3 parameter jitter per target. */
class ConfigProvider : ContentProvider() {

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if (method != METHOD_SPOOF) return null
        val target = Targets.all.firstOrNull { it.id == arg } ?: return null
        val sp = context?.getSharedPreferences(Keys.PREFS_NAME, Context.MODE_PRIVATE) ?: return null

        return Bundle().apply {
            putBoolean("active", sp.getBoolean(Keys.spoofActive(target.id), false))
            putString("lat", sp.getString(Keys.spoofLat(target.id), null))
            putString("lng", sp.getString(Keys.spoofLng(target.id), null))
            putString("jit_step", sp.getString(Keys.jitStepKey(target.id), null))
            putString("jit_win", sp.getString(Keys.jitWinKey(target.id), null))
            putString("jit_radius", sp.getString(Keys.jitRadiusKey(target.id), null))
        }
    }

    override fun onCreate() = true
    override fun getType(uri: Uri): String? = null
    override fun query(uri: Uri, p: Array<String>?, s: String?, a: Array<String>?, o: String?): Cursor? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, s: String?, a: Array<String>?): Int = 0
    override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<String>?): Int = 0

    companion object {
        const val AUTHORITY = "com.aya.doank.config"
        const val METHOD_SPOOF = "spoof"
    }
}
