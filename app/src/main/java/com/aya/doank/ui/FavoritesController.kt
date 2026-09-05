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
 * Dialog favorit — v2.8: PER KATEGORI (GRAB | GOJEK).
 * v2.8.2: tap nama favorit → dialog konfirmasi "Mulai Spoofing?" →
 * PLAY = langsung aktif di koordinat favorit sesuai kategorinya.
 * Semua dialog dikartukan solid + dilebarkan 92% via helper.
 */
class FavoritesController(
    private val activity: Activity,
    private val store: FavoritesStore,
    private val centerProvider: () -> LatLng?,
    private val onPlay: (catId: String, lat: Double, lng: Double, name: String) -> Unit,
    private val onPick: (catId: String, lat: Double, lng: Double, name: String) -> Unit
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
                item.setOnClickListener {
                    showPlayConfirm(cat, i, f)
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
        modePin.setOnClickListener { setMode
