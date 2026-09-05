package com.aya.doank.core

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Penyimpanan lokasi favorit — v2.8: PER KATEGORI (grab-driver / gojek-driver).
 * Setiap kategori punya list sendiri; nama duplikat diperiksa DALAM kategori.
 * src: "pin" | "manual".
 */
class FavoritesStore(context: Context) {

    private val sp = context.getSharedPreferences(Keys.PREFS_NAME, Context.MODE_PRIVATE)

    data class Fav(val name: String, val lat: Double, val lng: Double, val src: String)

    fun all(catId: String): List<Fav> {
        val raw = sp.getString(Keys.favoritesKey(catId), null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Fav(o.getString("name"), o.getDouble("lat"), o.getDouble("lng"),
                    o.optString("src", "pin"))
            }
        } catch (t: Throwable) { emptyList() }
    }

    /** Tambah ke kategori. Nama duplikat DALAM kategori yang sama ditolak. */
    fun add(catId: String, name: String, lat: Double, lng: Double, src: String): Boolean {
        val list = all(catId).toMutableList()
        if (list.any { it.name.equals(name, ignoreCase = true) }) return false
        list.add(Fav(name, lat, lng, src))
        save(catId, list)
        return true
    }

    /** Update item dalam kategori. Duplikat nama diizinkan untuk item sendiri. */
    fun updateAt(catId: String, index: Int, name: String, lat: Double, lng: Double): Boolean {
        val list = all(catId).toMutableList()
        if (index !in list.indices) return false
        if (list.anyIndexed { j, it -> j != index && it.name.equals(name, ignoreCase = true) }) return false
        list[index] = Fav(name, lat, lng, "manual")
        save(catId, list)
        return true
    }

    fun removeAt(catId: String, index: Int) {
        val list = all(catId).toMutableList()
        if (index in list.indices) { list.removeAt(index); save(catId, list) }
    }

    private fun save(catId: String, list: List<Fav>) {
        val arr = JSONArray()
        list.forEach { f ->
            arr.put(
                JSONObject().put("name", f.name)
                    .put("lat", f.lat).put("lng", f.lng).put("src", f.src)
            )
        }
        sp.edit().putString(Keys.favoritesKey(catId), arr.toString()).apply()
    }

    private inline fun <T> List<T>.anyIndexed(predicate: (Int, T) -> Boolean): Boolean {
        for ((i, v) in this.withIndex()) if (predicate(i, v)) return true
        return false
    }
}
