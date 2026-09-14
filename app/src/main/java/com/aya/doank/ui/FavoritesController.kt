package com.aya.doank.ui

import android.content.Context
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.json.JSONArray
import org.json.JSONObject

data class FavItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val lat: Double,
    val lng: Double
)

data class FavCategory(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val items: MutableList<FavItem> = mutableListOf()
)

class FavoritesController(
    private val context: Context,
    private val onSelectLocation: (lat: Double, lng: Double, name: String) -> Unit
) {

    private val categories = mutableListOf<FavCategory>()
    private var activeDialog: AlertDialog? = null

    init {
        loadFavorites()
    }

    fun showFavoritesDialog() {
        val builder = MaterialAlertDialogBuilder(context)
        builder.setTitle("Lokasi Favorit")

        val catNames = categories.map { it.name }.toTypedArray()
        if (catNames.isEmpty()) {
            builder.setMessage("Belum ada kategori favorit.")
            builder.setPositiveButton("Tambah Kategori") { _, _ -> showAddCategoryDialog() }
            builder.setNegativeButton("Tutup", null)
        } else {
            builder.setItems(catNames) { _, which ->
                showCategoryItemsDialog(categories[which])
            }
            builder.setPositiveButton("Tambah Kategori") { _, _ -> showAddCategoryDialog() }
            builder.setNegativeButton("Tutup", null)
        }

        activeDialog = builder.show()
    }

    fun showCategoryItemsDialog(cat: FavCategory) {
        val itemLabels = cat.items.map { "${it.name}\n(${it.lat}, ${it.lng})" }.toTypedArray()

        val builder = MaterialAlertDialogBuilder(context)
        builder.setTitle("Kategori: ${cat.name}")

        if (cat.items.isEmpty()) {
            builder.setMessage("Belum ada lokasi tersimpan di kategori ini.")
        } else {
            builder.setItems(itemLabels) { _, which ->
                val item = cat.items[which]
                showItemOptionsDialog(cat, item)
            }
        }

        builder.setNeutralButton("Hapus Kategori") { _, _ ->
            askDeleteCategory(cat)
        }
        builder.setNegativeButton("Kembali") { _, _ ->
            showFavoritesDialog()
        }

        activeDialog = builder.show()
    }

    private fun showItemOptionsDialog(cat: FavCategory, item: FavItem) {
        val options = arrayOf("Gunakan Lokasi Ini", "Hapus Lokasi")
        MaterialAlertDialogBuilder(context)
            .setTitle(item.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> onSelectLocation(item.lat, item.lng, item.name)
                    1 -> askDelete(cat, item)
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    fun addFavoriteItem(categoryName: String, name: String, lat: Double, lng: Double) {
        var cat = categories.find { it.name.equals(categoryName, ignoreCase = true) }
        if (cat == null) {
            cat = FavCategory(name = categoryName)
            categories.add(cat)
        }
        val newItem = FavItem(name = name, lat = lat, lng = lng)
        cat.items.add(newItem)
        saveFavorites()
        refreshDialogIfOpen()
    }

    private fun showAddCategoryDialog() {
        val input = EditText(context)
        input.hint = "Masukkan nama kategori"

        MaterialAlertDialogBuilder(context)
            .setTitle("Tambah Kategori Baru")
            .setView(input)
            .setPositiveButton("Simpan") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    if (categories.none { it.name.equals(name, ignoreCase = true) }) {
                        categories.add(FavCategory(name = name))
                        saveFavorites()
                        refreshDialogIfOpen()
                    } else {
                        Toast.makeText(context, "Kategori '$name' sudah ada.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    /**
     * Konfirmasi hapus item favorit tertentu
     */
    private fun askDelete(cat: FavCategory, item: FavItem) {
        MaterialAlertDialogBuilder(context)
            .setTitle("Hapus Favorit")
            .setMessage("Apakah Anda yakin ingin menghapus '${item.name}' dari kategori '${cat.name}'?")
            .setPositiveButton("Hapus") { _, _ ->
                cat.items.remove(item)
                saveFavorites()
                refreshDialogIfOpen()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    /**
     * Konfirmasi hapus seluruh kategori
     */
    private fun askDeleteCategory(cat: FavCategory) {
        MaterialAlertDialogBuilder(context)
            .setTitle("Hapus Kategori")
            .setMessage("Apakah Anda yakin ingin menghapus kategori '${cat.name}' beserta seluruh falls lokasi di dalamnya?")
            .setPositiveButton("Hapus") { _, _ ->
                categories.remove(cat)
                saveFavorites()
                refreshDialogIfOpen()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    /**
     * Memperbarui/merefresh tampilan dialog jika dialog favorit sedang dalam keadaan terbuka
     */
    private fun refreshDialogIfOpen() {
        if (activeDialog?.isShowing == true) {
            activeDialog?.dismiss()
            showFavoritesDialog()
        }
    }

    private fun loadFavorites() {
        try {
            val prefs = context.getSharedPreferences("aya_fav_prefs", Context.MODE_PRIVATE)
            val jsonStr = prefs.getString("favorites_json", null) ?: return
            
            categories.clear()
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val catObj = array.getJSONObject(i)
                val catName = catObj.getString("name")
                val itemsArray = catObj.getJSONArray("items")
                
                val itemsList = mutableListOf<FavItem>()
                for (j in 0 until itemsArray.length()) {
                    val itemObj = itemsArray.getJSONObject(j)
                    itemsList.add(
                        FavItem(
                            id = itemObj.optString("id", java.util.UUID.randomUUID().toString()),
                            name = itemObj.getString("name"),
                            lat = itemObj.getDouble("lat"),
                            lng = itemObj.getDouble("lng")
                        )
                    )
                }
                categories.add(FavCategory(name = catName, items = itemsList))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveFavorites() {
        try {
            val array = JSONArray()
            for (cat in categories) {
                val catObj = JSONObject()
                catObj.put("name", cat.name)
                
                val itemsArray = JSONArray()
                for (item in cat.items) {
                    val itemObj = JSONObject()
                    itemObj.put("id", item.id)
                    itemObj.put("name", item.name)
                    itemObj.put("lat", item.lat)
                    itemObj.put("lng", item.lng)
                    itemsArray.put(itemObj)
                }
                catObj.put("items", itemsArray)
                array.put(catObj)
            }
            
            val prefs = context.getSharedPreferences("aya_fav_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("favorites_json", array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
