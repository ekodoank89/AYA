package com.aya.doank.core

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Penyimpanan lokasi tersimpan (list global, dipakai semua target).
 * Format: satu string JSON di prefs — atomic, mudah dimigrasi.
 * Order = urutan penyimpanan (terbaru di bawah).
 */
class FavoritesStore(context: Context) {

    private val sp = context.getSharedPreferences(Keys.PREFS_NAME, Context.MODE_PRIVATE)

    data class Fav(val name: String, val lat: Double, val lng: Double)

    fun all(): List<Fav> {
        val raw = sp.getString(Keys.FAVORITES, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Fav(o.getString("name"), o.getDouble("lat"), o.getDouble("lng"))
            }
        } catch (t: Throwable) { emptyList() }
    }

    fun add(name: String, lat: Double, lng: Double): Boolean {
        val list = all().toMutableList()
        if (list.any { it.name.equals(name, ignoreCase = true) }) return false // nama duplikat ditolak
        list.add(Fav(name, lat, lng))
        save(list); return true
    }

    fun removeAt(index: Int) {
        val list = all().toMutableList()
        if (index in list.indices) { list.removeAt(index); save(list) }
    }

    private fun save(list: List<Fav>) {
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().put("name", it.name).put("lat", it.lat).put("lng", it.lng)) }
        sp.edit().putString(Keys.FAVORITES, arr.toString()).apply()
    }
}
