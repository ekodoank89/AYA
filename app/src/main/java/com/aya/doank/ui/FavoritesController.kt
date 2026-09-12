package com.aya.doank.ui

import android.app.Activity
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
import com.aya.doank.core.Targets
import com.google.android.gms.maps.model.LatLng
import java.util.Locale

/**
 * Dialog favorit — v2.9: PER KATEGORI (GRAB | GOJEK).
 * Tap nama favorit → onFavoritePlay(cat, lat, lng, name) → MainActivity memicu play.
 * Semua dialog dikartukan solid + dilebarkan 92% via helper.
 */
class FavoritesController(
    private val activity: Activity,
    private val store: FavoritesStore,
    private val centerProvider: () -> LatLng?,
    private val onFavoritePlay: (catId: String, lat: Double, lng: Double, name: String) -> Unit
) {
    private var dialog: AlertDialog? = null

    fun bind(btnId: Int) {
        activity.findViewById<View>(btnId).setOnClickListener { show() }
    }

    /** Dialog default sempit — lebarkan ke 92% lebar layar. */
    private fun lebarkan(d: AlertDialog) {
        d.setOnShowListener {
            d.window?.setLayout(
                (activity.resources.displayMetrics.widthPixels * 0.92).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }
    }

    /** Dialog builder tanpa layout custom — beri kartu solid + lebar 92%. */
    private fun kartu(builder: AlertDialog.Builder): AlertDialog {
        val d = builder.create()
        d.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_card)
        d.setOnShowListener {
            d.window?.setLayout(
                (activity.resources.displayMetrics.widthPixels * 0.92).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }
        return d
    }

    private fun show() {
        val v = LayoutInflater.from(activity).inflate(R.layout.dialog_favorites, null)
        val catGrab   = v.findViewById<TextView>(R.id.cat_grab)
        val catGojek  = v.findViewById<TextView>(R.id.cat_gojek)
        val modePin   = v.findViewById<TextView>(R.id.mode_pin)
        val modeManual = v.findViewById<TextView>(R.id.mode_manual)
        val nameEt    = v.findViewById<EditText>(R.id.fav_name)
        val latlngRow = v.findViewById<View>(R.id.latlng_row)
        val latEt     = v.findViewById<EditText>(R.id.in_lat)
        val lngEt     = v.findViewById<EditText>(R.id.in_lng)
        val errTv     = v.findViewById<TextView>(R.id.fav_err)
        val list      = v.findViewById<LinearLayout>(R.id.fav_list)
        val empty     = v.findViewById<TextView>(R.id.fav_empty)

        var cat = Targets.GRAB.id
        var mode = "pin"

        fun clearErr() {
            errTv.visibility = View.GONE
            latEt.error = null
            lngEt.error = null
        }

        fun render() {
            val favs = store.all(cat)
            empty.visibility = if (favs.isEmpty()) View.VISIBLE else View.GONE
            list.visibility = if (favs.isEmpty()) View.GONE else View.VISIBLE
            list.removeAllViews()
            favs.forEachIndexed { i, f ->
                val item = LayoutInflater.from(activity)
                    .inflate(R.layout.item_favorite, list, false) as LinearLayout
                item.findViewById<TextView>(R.id.if_name).text = f.name
                item.findViewById<TextView>(R.id.if_coord).text =
                    String.format(Locale.US, "%.6f, %.6f", f.lat, f.lng)
                item.findViewById<View>(R.id.if_edit).setOnClickListener { showEdit(cat, i) }
                item.findViewById<View>(R.id.if_del).setOnClickListener { askDelete(cat, i) }
                // Tap nama favorit → langsung play sesuai kategori
                item.setOnClickListener {
                    dialog?.dismiss()
                    onFavoritePlay(cat, f.lat, f.lng, f.name)
                }
                list.addView(item)
            }
        }

        fun setCat(c: String) {
            cat = c
            val sel = R.drawable.bg_mode_on
            val unsel = R.drawable.bg_mode_off
            val on = 0xFFC8F7D8.toInt()
            val off = 0x99FFFFFF.toInt()
            catGrab.setBackgroundResource(if (c == Targets.GRAB.id) sel else unsel)
            catGrab.setTextColor(if (c == Targets.GRAB.id) on else off)
            catGojek.setBackgroundResource(if (c == Targets.GOJEK.id) sel else unsel)
            catGojek.setTextColor(if (c == Targets.GOJEK.id) on else off)
            clearErr()
            render()
        }
        catGrab.setOnClickListener { setCat(Targets.GRAB.id) }
        catGojek.setOnClickListener { setCat(Targets.GOJEK.id) }

        fun setMode(m: String) {
            mode = m
            val sel = R.drawable.bg_mode_on
            val unsel = R.drawable.bg_mode_off
            val on = 0xFFC8F7D8.toInt()
            val off = 0x99FFFFFF.toInt()
            modePin.setBackgroundResource(if (m == "pin") sel else unsel)
            modePin.setTextColor(if (m == "pin") on else off)
            modeManual.setBackgroundResource(if (m == "manual") sel else unsel)
            modeManual.setTextColor(if (m == "manual") on else off)
            latlngRow.visibility = if (m == "manual") View.VISIBLE else View.GONE
            clearErr()
        }
        modePin.setOnClickListener { setMode("pin") }
        modeManual.setOnClickListener { setMode("manual") }

        fun validate(la: EditText, ln: EditText): Pair<Double, Double>? {
            clearErr()
            val lat = la.text.toString().replace(',', '.').toDoubleOrNull()
            val lng = ln.text.toString().replace(',', '.').toDoubleOrNull()
            if (lat == null || lng == null) {
                errTv.text = "Latitude dan Longitude wajib angka desimal."
                errTv.visibility = View.VISIBLE
                if (lat == null) la.error = " "
                if (lng == null) ln.error = " "
                return null
            }
            if (lat < -90 || lat > 90) {
                errTv.text = "Latitude harus antara -90 sampai 90."
                errTv.visibility = View.VISIBLE
                la.error = " "
                return null
            }
            if (lng < -180 || lng > 180) {
                errTv.text = "Longitude harus antara -180 sampai 180."
                errTv.visibility = View.VISIBLE
                ln.error = " "
                return null
            }
            return lat to lng
        }

        v.findViewById<View>(R.id.fav_add).setOnClickListener {
            val name = nameEt.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(activity, "Beri nama lokasinya dulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val lat: Double
            val lng: Double
            val src: String
            if (mode == "pin") {
                val c = centerProvider()
                if (c == null) {
                    Toast.makeText(activity, "Peta belum siap", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                lat = c.latitude
                lng = c.longitude
                src = "pin"
            } else {
                val p = validate(latEt, lngEt) ?: return@setOnClickListener
                lat = p.first
                lng = p.second
                src = "manual"
            }
            if (store.add(cat, name, lat, lng, src)) {
                nameEt.text.clear()
                latEt.text.clear()
                lngEt.text.clear()
                render()
            } else {
                Toast.makeText(activity, "Nama sudah dipakai di kategori ini", Toast.LENGTH_SHORT).show()
            }
        }

        dialog = AlertDialog.Builder(activity, R.style.Theme_AYA_Dialog)
            .setView(v)
            .create()
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        lebarkan(dialog!!)
        dialog?.show()
        render()
    }

    // ===== EDIT: nama + koordinat (kartu solid + 92% lebar) =====
    private fun showEdit(cat: String, i: Int) {
        val f = store.all(cat).getOrNull(i) ?: return
        val v = LayoutInflater.from(activity).inflate(R.layout.dialog_edit_fav, null)
        val nameEt = v.findViewById<EditText>(R.id.e_name)
        val latEt = v.findViewById<EditText>(R.id.e_lat)
        val lngEt = v.findViewById<EditText>(R.id.e_lng)
        val errTv = v.findViewById<TextView>(R.id.e_err)
        nameEt.setText(f.name)
        latEt.setText(f.lat.toString())
        lngEt.setText(f.lng.toString())

        val d = AlertDialog.Builder(activity, R.style.Theme_AYA_Dialog)
            .setView(v)
            .create()
        d.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_card)
        lebarkan(d)   // ← dialog edit 92% lebar layar — sama dengan dialog utama

        v.findViewById<View>(R.id.e_cancel).setOnClickListener { d.dismiss() }
        v.findViewById<View>(R.id.e_save).setOnClickListener {
            val name = nameEt.text.toString().trim()
            if (name.isEmpty()) {
                errTv.text = "Nama tidak boleh kosong."
                errTv.visibility = View.VISIBLE
                return@setOnClickListener
            }
            val lat = latEt.text.toString().replace(',', '.').toDoubleOrNull()
            val lng = lngEt.text.toString().replace(',', '.').toDoubleOrNull()
            if (lat == null || lng == null || lat < -90 || lat > 90 || lng < -180 || lng > 180) {
                errTv.text = "Koordinat tidak valid (lat -90..90, lng -180..180)."
                errTv.visibility = View.VISIBLE
                return@setOnClickListener
            }
            if (store.updateAt(cat, i, name, lat, lng)) {
                d.dismiss()
                refreshDialogIfOpen()
                Toast.makeText(activity, "\"$name\" diperbarui", Toast.LENGTH_SHORT).show()
            } else {
                errTv.text = "Nama sudah dipakai lokasi lain di kategori ini."
                errTv.visibility = View.VISIBLE
            }
        }
        d.show()
    }

    // ===== HAPUS: konfirmasi =====
    private fun askDelete(cat: String, i: Int) {
        val f = store.all(cat).getOrNull(i) ?: return
        val d = AlertDialog.Builder(activity, R.style.Theme_AYA_Dialog)
            .setTitle("Hapus lokasi?")
            .setMessage("\"${f.name}\" akan dihapus permanen dari kategori ini.")
            .setPositiveButton("Hapus") { _, _ ->
                store.removeAt(cat, i)
                refreshDialogIfOpen()
                Toast.makeText(activity, "\"${f.name}\" dihapus", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun refreshDialogIfOpen() {
        if (dialog?.isShowing == true) {
            dialog?.dismiss()
            show()
        }
    }
}
