package com.aya.doank.ui

import android.app.Activity
import android.view.View
import android.widget.ImageButton
import com.aya.doank.R
import com.aya.doank.core.Prefs
import com.aya.doank.core.SpoofTarget
import com.aya.doank.core.Targets

class PlayPanelController(
    private val activity: Activity,
    private val prefs: Prefs,
    private val onToggle: (target: SpoofTarget, active: Boolean) -> Unit
) {
    private data class Row(val targetId: String, val btn: ImageButton, val dot: View)

    fun bind() {
        listOf(
            Row("grab",  activity.findViewById(R.id.btn_grab),  activity.findViewById(R.id.dot_grab)),
            Row("gojek", activity.findViewById(R.id.btn_gojek), activity.findViewById(R.id.dot_gojek))
        ).forEach { row ->
            render(row, prefs.isSpoofActive(row.targetId)) // restore state saat app dibuka lagi
            row.btn.setOnClickListener {
                val active = !prefs.isSpoofActive(row.targetId)
                prefs.setSpoofActive(row.targetId, active)
                render(row, active)
                onToggle(Targets.byId(row.targetId), active)
            }
        }
    }

    private fun render(row: Row, active: Boolean) {
        row.btn.setBackgroundResource(
            if (active) R.drawable.bg_play_green_touch else R.drawable.bg_play_red_touch
        )
        row.btn.setImageResource(if (active) R.drawable.ic_stop else R.drawable.ic_play)
        row.dot.setBackgroundResource(if (active) R.drawable.bg_dot_green else R.drawable.bg_dot_red)
    }
}
