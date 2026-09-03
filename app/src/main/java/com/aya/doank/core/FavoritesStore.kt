package com.aya.doank.core

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Penyimpanan lokasi favorit (list global). Satu string JSON di prefs — atomic.
 * src: "pin" (dari posisi pin) | "manual" (input/diedit manual).
 */
class FavoritesStore(context: Context) {

    private val sp = context.getSharedPreferences(Keys.PREFS_NAME, Context.MODE_PRIVATE)

    data class Fav(val name: String, val lat: Double, val lng: Double, val src: String)

    fun all(): List<Fav> {
        val raw = sp.getString(Keys.FAVORITES, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Fav(o.getString("name"), o.getDouble("lat"), o.getDouble("lng"),
                    o.optString("src", "pin"))
            }
        } catch (t: Throwable) { emptyList() }
    }

    fun add(name: String, lat: Double, lng: Double, src: String): Boolean {
        val list = all().toMutableList()
        if (list.any { it.name.equals(name, ignoreCase = true) }) return false
        list.add(Fav(name, lat, lng, src))
        save(list); return true
    }

    fun updateAt(index: Int, name: String, lat: Double, lng: Double): Boolean {
        val list = all().toMutableList()
        if (index !in list.indices) return false
        // Duplikat nama diizinkan untuk item sendiri (index yang sama)
        if (list.anyIndexed { j, it -> j != index && it.name.equals(name, ignoreCase = true) }) return false
        list[index] = Fav(name, lat, lng, "manual")
        save(list); return true
    }

    fun removeAt(index: Int) {
        val list = all().toMutableList()
        if (index in list.indices) { list.removeAt(index); save(list) }
    }

    private fun save(list: List<Fav>) {
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().put("name", it.name).put("lat", it.lat)
            .put("lng", it.lng).put("src", it.src)) }
        sp.edit().putString(Keys.FAVORITES, arr.toString()).apply()
    }

    private inline fun <T> List<T>.anyIndexed(predicate: (Int, T) -> Boolean): Boolean {
        for ((i, v) in this.withIndex()) if (predicate(i, v)) return true
        return false
    }
}
