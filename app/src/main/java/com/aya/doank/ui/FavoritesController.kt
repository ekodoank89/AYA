package com.aya.doank.ui

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.aya.doank.R
import com.aya.doank.core.FavoritesStore
import com.google.android.gms.maps.model.LatLng
import java.util.Locale

/**
 * Tombol ★ + dialog daftar lokasi tersimpan (list GLOBAL).
 * Tap item → onPick(lat,lng) → manager memindahkan peta (pin mengikuti tengah layar).
 */
class FavoritesController(
    private val activity: Activity,
    private val store: FavoritesStore,
    private val centerProvider: () -> LatLng?,
    private val onPick: (LatLng, String) -> Unit
) {
    // Field kelas — boleh direferensikan local function kapan pun (berbeda dari variabel lokal)
    private var dialog: AlertDialog? = null

    fun bind(btnId: Int) {
        activity.findViewById<View>(btnId).setOnClickListener { show() }
    }

    private fun show() {
        val v = LayoutInflater.from(activity).inflate(R.layout.dialog_favorites, null)
        val nameEt = v.findViewById<EditText>(R.id.fav_name)
        val list   = v.findViewById<LinearLayout>(R.id.fav_list)
        val empty  = v.findViewById<TextView>(R.id.fav_empty)

        fun render() {
            val favs = store.all()
            empty.visibility = if (favs.isEmpty()) View.VISIBLE else View.GONE
            list.visibility  = if (favs.isEmpty()) View.GONE else View.VISIBLE
            list.removeAllViews()
            favs.forEachIndexed { i, f ->
                val item = LayoutInflater.from(activity)
                    .inflate(R.layout.item_favorite, list, false) as LinearLayout
                item.findViewById<TextView>(R.id.if_name).text = f.name
                item.findViewById<TextView>(R.id.if_coord).text =
                    String.format(Locale.US, "%.6f, %.6f", f.lat, f.lng)
                item.findViewById<View>(R.id.if_del).setOnClickListener {
                    store.removeAt(i); render()
                }
                item.setOnClickListener {
                    onPick(LatLng(f.lat, f.lng), f.name)
                    dialog?.dismiss()
                }
                list.addView(item)
            }
        }

        v.findViewById<View>(R.id.fav_add).setOnClickListener {
            val name = nameEt.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(activity, "Beri nama lokasinya dulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val c = centerProvider()
            if (c == null) {
                Toast.makeText(activity, "Peta belum siap", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (store.add(name, c.latitude, c.longitude)) {
                nameEt.text.clear(); render()
            } else {
                Toast.makeText(activity, "Nama sudah dipakai", Toast.LENGTH_SHORT).show()
            }
        }

        dialog = AlertDialog.Builder(activity, R.style.Theme_AYA_Dialog)
            .setView(v)
            .create()
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog?.show()
        render()
    }
}
