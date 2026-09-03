package com.aya.doank.ui

import android.app.Activity
import android.view.View
import android.widget.ImageButton
import com.aya.doank.R
import com.aya.doank.core.Prefs
import com.aya.doank.core.SpoofTarget
import com.aya.doank.core.Targets
import com.google.android.gms.maps.model.LatLng

/**
 * Panel play: GRAB & GOJEK (tombol + dot status).
 * v2.2.2: chip koordinat dihapus dari layout — controller dirapikan mengikuti.
 * Lock koordinat tetap via centerProvider saat ▶ ditekan.
 */
class PlayPanelController(
    private val activity: Activity,
    private val prefs: Prefs,
    private val centerProvider: () -> LatLng?,
    private val onToggle: (target: SpoofTarget, active: Boolean) -> Unit
) {
    private class Row(
        val targetId: String, val btn: ImageButton, val dot: View
    )

    private val rows = mutableListOf<Row>()

    fun bind() {
        listOf(
            Triple(Targets.GRAB.id,  R.id.btn_grab,  R.id.dot_grab),
            Triple(Targets.GOJEK.id, R.id.btn_gojek, R.id.dot_gojek)
        ).forEach { (targetId, btnId, dotId) ->
            val row = Row(
                targetId,
                activity.findViewById(btnId),
                activity.findViewById(dotId)
            )
            rows.add(row)
            render(row)
            row.btn.setOnClickListener { toggle(row) }
        }
    }

    private fun toggle(row: Row) {
        val active = !prefs.isSpoofActive(row.targetId)
        prefs.setSpoofActive(row.targetId, active)
        if (active) {
            centerProvider()?.let { prefs.setSpoofPoint(row.targetId, it.latitude, it.longitude) }
        }
        render(row)
        onToggle(Targets.byId(row.targetId), active)
    }

    /** Render ulang satu target (dipakai stop dari notifikasi). */
    fun refresh(targetId: String) {
        rows.firstOrNull { it.targetId == targetId }?.let { render(it) }
    }

    private fun render(row: Row) {
        val active = prefs.isSpoofActive(row.targetId)
        row.btn.setBackgroundResource(
            if (active) R.drawable.bg_play_green_touch else R.drawable.bg_play_red_touch)
        row.btn.setImageResource(
            if (active) R.drawable.ic_stop else R.drawable.ic_play)
        row.dot.setBackgroundResource(
            if (active) R.drawable.bg_dot_green else R.drawable.bg_dot_red)
    }
}
