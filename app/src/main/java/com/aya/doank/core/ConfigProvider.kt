package com.aya.doank.core

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle

/**
 * Jembatan config manager → hook di proses target (pengganti XSharedPreferences).
 * Hanya melayani data spoof untuk id target yang dikenal — bukan pembaca prefs bebas.
 * exported=true disengaja: proses target HARUS bisa memanggil ini via Binder.
 */
class ConfigProvider : ContentProvider() {

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if (method != METHOD_SPOOF) return null
        val target = Targets.all.firstOrNull { it.id == arg } ?: return null
        val sp = context?.getSharedPreferences(Keys.PREFS_NAME, Context.MODE_PRIVATE) ?: return null

        return Bundle().apply {
            putBoolean("active", sp.getBoolean(Keys.spoofActive(target.id), false))
            putString("lat", sp.getString(Keys.spoofLat(target.id), null))
            putString("lng", sp.getString(Keys.spoofLng(target.id), null))
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
