package com.aya.doank.ui

import android.app.Activity
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.aya.doank.R
import com.aya.doank.core.Prefs
import com.aya.doank.core.SpoofTarget
import com.aya.doank.core.Targets
import com.google.android.gms.maps.model.LatLng
import java.util.Locale

class PlayPanelController(
    private val activity: Activity,
    private val prefs: Prefs,
    private val centerProvider: () -> LatLng?,
    private val blueDotProvider: () -> LatLng?,
    private val onToggle: (target: SpoofTarget, active: Boolean) -> Unit
) {
    private class RowData(
        val id: String, val btn: Int, val dot: Int,
        val chip: Int, val lat: Int, val lng: Int
    )

    private class Row(
        val targetId: String, val btn: ImageButton, val dot: View,
        val chip: View, val chipLat: TextView, val chipLng: TextView
    )

    private val rows = mutableListOf<Row>()

    fun bind() {
        listOf(
            RowData("grab",  R.id.btn_grab,  R.id.dot_grab,  R.id.chip_grab,  R.id.chip_grab_lat,  R.id.chip_grab_lng),
            RowData("gojek", R.id.btn_gojek, R.id.dot_gojek, R.id.chip_gojek, R.id.chip_gojek_lat, R.id.chip_gojek_lng)
        ).forEach { d ->
            val row = Row(
                d.id,
                activity.findViewById(d.btn),
                activity.findViewById(d.dot),
                activity.findViewById(d.chip),
                activity.findViewById(d.lat),
                activity.findViewById(d.lng)
            )
            rows.add(row)
            render(row) // restore status dari prefs saat app dibuka lagi
            row.btn.setOnClickListener { toggle(row) }
        }
    }

    private fun toggle(row: Row) {
        val active = !prefs.isSpoofActive(row.targetId)
        prefs.setSpoofActive(row.targetId, active)
        if (active) {
            // Lock koordinat pin SAAT INI untuk target ini
            centerProvider()?.let { prefs.setSpoofPoint(row.targetId, it.latitude, it.longitude) }
        }
        render(row)
        onToggle(Targets.byId(row.targetId), active)
    }

    /** Dipanggil MapController tiap titik biru bergerak — chip target NONAKTIF mengikutinya. */
    fun onBlueDotChanged() {
        rows.forEach { if (!prefs.isSpoofActive(it.targetId)) render(it) }
    }

    private fun render(row: Row) {
        if (prefs.isSpoofActive(row.targetId)) {
            row.btn.setBackgroundResource(R.drawable.bg_play_green_touch)
            row.btn.setImageResource(R.drawable.ic_stop)
            row.dot.setBackgroundResource(R.drawable.bg_dot_green)
            row.chip.setBackgroundResource(R.drawable.bg_coord_chip_active)
            row.chipLat.setTextColor(ContextCompat.getColor(activity, R.color.chip_coord_text_active))
            row.chipLng.setTextColor(ContextCompat.getColor(activity, R.color.chip_coord_text_active))
            val p = prefs.spoofPoint(row.targetId)
            row.chipLat.text = p?.let { fmt(it.first) } ?: "—"
            row.chipLng.text = p?.let { fmt(it.second) } ?: "—"
        } else {
            row.btn.setBackgroundResource(R.drawable.bg_play_red_touch)
            row.btn.setImageResource(R.drawable.ic_play)
            row.dot.setBackgroundResource(R.drawable.bg_dot_red)
            row.chip.setBackgroundResource(R.drawable.bg_coord_chip)
            row.chipLat.setTextColor(ContextCompat.getColor(activity, R.color.chip_coord_text))
            row.chipLng.setTextColor(ContextCompat.getColor(activity, R.color.chip_coord_text))
            val p = blueDotProvider()
            row.chipLat.text = p?.let { fmt(it.latitude) } ?: "—"
            row.chipLng.text = p?.let { fmt(it.longitude) } ?: "—"
        }
    }

    private fun fmt(v: Double) = String.format(Locale.US, "%.6f", v)
}
