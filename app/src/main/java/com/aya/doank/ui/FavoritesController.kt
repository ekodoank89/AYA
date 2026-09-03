package com.aya.doank.ui

import android.app.Activity
import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
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
 * Tombol ★ (di panel) + dialog favorit:
 * 2 mode input (pin/manual, kolom vertikal), edit nama+koordinat, hapus berkonfirmasi.
 * v2.2.3: dialog dilebarkan ke 92% lebar layar (helper widen) — lat/lng terbaca jelas.
 */
class FavoritesController(
    private val activity: Activity,
    private val store: FavoritesStore,
    private val centerProvider: () -> LatLng?,
    private val onPick: (LatLng, String) -> Unit
) {
    private var dialog: AlertDialog? = null

    fun bind(btnId: Int) {
        activity.findViewById<View>(btnId).setOnClickListener { show() }
    }

    /** AlertDialog default sempit — lebarkan ke 92% lebar layar. */
    private fun widen(d: AlertDialog) {
        d.setOnShowListener {
            d.window?.setLayout(
                (activity.resources.displayMetrics.widthPixels * 0.92).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun show() {
        val v = LayoutInflater.from(activity).inflate(R.layout.dialog_favorites, null)
        val modePin    = v.findViewById<TextView>(R.id.mode_pin)
        val modeManual = v.findViewById<TextView>(R.id.mode_manual)
        val nameEt     = v.findViewById<EditText>(R.id.fav_name)
        val latlngRow  = v.findViewById<View>(R.id.latlng_row)
        val latEt      = v.findViewById<EditText>(R.id.in_lat)
        val lngEt      = v.findViewById<EditText>(R.id.in_lng)
        val errTv      = v.findViewById<TextView>(R.id.fav_err)
        val list       = v.findViewById<LinearLayout>(R.id.fav_list)
        val empty      = v.findViewById<TextView>(R.id.fav_empty)
        var mode = "pin"

        fun clearErr() {
            errTv.visibility = View.GONE
            latEt.error = null; lngEt.error = null
        }

        fun setMode(m: String) {
            mode = m
            modePin.setBackgroundResource(
                if (m == "pin") R.drawable.bg_mode_on else R.drawable.bg_mode_off)
            modePin.setTextColor(
                if (m == "pin") 0xFFC8F7D8.toInt() else 0x99FFFFFF.toInt())
            modeManual.setBackgroundResource(
                if (m == "manual") R.drawable.bg_mode_on else R.drawable.bg_mode_off)
            modeManual.setTextColor(
                if (m == "manual") 0xFFC8F7D8.toInt() else 0x99FFFFFF.toInt())
            latlngRow.visibility = if (m == "manual") View.VISIBLE else View.GONE
            clearErr()
        }
        modePin.setOnClickListener { setMode("pin") }
        modeManual.setOnClickListener { setMode("manual") }

        /** Validasi manual; koma→titik; null = invalid. */
        fun validate(la: EditText, ln: EditText): Pair<Double, Double>? {
            clearErr()
            val lat = la.text.toString().replace(',', '.').toDoubleOrNull()
            val lng = ln.text.toString().replace(',', '.').toDoubleOrNull()
            if (lat == null || lng == null) {
                errTv.text = "Latitude & Longitude wajib angka desimal."
                errTv.visibility = View.VISIBLE
                if (lat == null) la.error = " "
                if (lng == null) ln.error = " "
                return null
            }
            if (lat < -90 || lat > 90) {
                errTv.text = "Latitude harus antara -90 sampai 90."
                errTv.visibility = View.VISIBLE; la.error = " "
                return null
            }
            if (lng < -180 || lng > 180) {
                errTv.text = "Longitude harus antara -180 sampai 180."
                errTv.visibility = View.VISIBLE; ln.error = " "
                return null
            }
            return lat to lng
        }

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
                item.findViewById<View>(R.id.if_edit).setOnClickListener { showEdit(i) }
                item.findViewById<View>(R.id.if_del).setOnClickListener { askDelete(i) }
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
            if (mode == "pin") {
                val c = centerProvider()
                if (c == null) {
                    Toast.makeText(activity, "Peta belum siap", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (store.add(name, c.latitude, c.longitude, "pin")) {
                    nameEt.text.clear(); render()
                } else Toast.makeText(activity, "Nama sudah dipakai", Toast.LENGTH_SHORT).show()
            } else {
                val p = validate(latEt, lngEt) ?: return@setOnClickListener
                if (store.add(name, p.first, p.second, "manual")) {
                    nameEt.text.clear(); latEt.text.clear(); lngEt.text.clear(); render()
                } else Toast.makeText(activity, "Nama sudah dipakai", Toast.LENGTH_SHORT).show()
            }
        }

        dialog = androidx.appcompat.app.AlertDialog.Builder(activity, R.style.Theme_AYA_Dialog)
            .setView(v).create()
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        widen(dialog!!)
        dialog?.show()
        render()
    }

    // ===== EDIT: nama + koordinat =====
    private fun showEdit(i: Int) {
        val f = store.all().getOrNull(i) ?: return
        val v = LayoutInflater.from(activity).inflate(R.layout.dialog_edit_fav, null)
        val nameEt = v.findViewById<EditText>(R.id.e_name)
        val latEt  = v.findViewById<EditText>(R.id.e_lat)
        val lngEt  = v.findViewById<EditText>(R.id.e_lng)
        val errTv  = v.findViewById<TextView>(R.id.e_err)
        nameEt.setText(f.name)
        latEt.setText(f.lat.toString())
        lngEt.setText(f.lng.toString())

        val d = androidx.appcompat.app.AlertDialog.Builder(activity, R.style.Theme_AYA_Dialog)
            .setView(v).create()
        d.window?.setBackgroundDrawableResource(android.R.color.transparent)
        widen(d)

        v.findViewById<View>(R.id.e_cancel).setOnClickListener { d.dismiss() }
        v.findViewById<View>(R.id.e_save).setOnClickListener {
            val name = nameEt.text.toString().trim()
            if (name.isEmpty()) {
                errTv.text = "Nama tidak boleh kosong."; errTv.visibility = View.VISIBLE
                return@setOnClickListener
            }
            val lat = latEt.text.toString().replace(',', '.').toDoubleOrNull()
            val lng = lngEt.text.toString().replace(',', '.').toDoubleOrNull()
            if (lat == null || lng == null || lat < -90 || lat > 90 || lng < -180 || lng > 180) {
                errTv.text = "Koordinat tidak valid (lat -90..90, lng -180..180)."
                errTv.visibility = View.VISIBLE
                return@setOnClickListener
            }
            if (store.updateAt(i, name, lat, lng)) {
                d.dismiss()
                refreshDialogIfOpen()   // render ulang dialog utama yang terbuka di bawah
                Toast.makeText(activity, "\"$name\" diperbarui", Toast.LENGTH_SHORT).show()
            } else {
                errTv.text = "Nama sudah dipakai lokasi lain."; errTv.visibility = View.VISIBLE
            }
        }
        d.show()
    }

    // ===== HAPUS: konfirmasi =====
    private fun askDelete(i: Int) {
        val f = store.all().getOrNull(i) ?: return
        val d = androidx.appcompat.app.AlertDialog.Builder(activity, R.style.Theme_AYA_Dialog)
            .setTitle("Hapus lokasi?")
            .setMessage("\"${f.name}\" akan dihapus permanen dari daftar favorit.")
            .setPositiveButton("Hapus") { _, _ ->
                store.removeAt(i)
                refreshDialogIfOpen()
                Toast.makeText(activity, "\"${f.name}\" dihapus", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    /** Render ulang dialog utama bila sedang terbuka — tutup yang lama, buka baru. */
    private fun refreshDialogIfOpen() {
        if (dialog?.isShowing == true) {
            dialog?.dismiss()
            show()
        }
    }
}
