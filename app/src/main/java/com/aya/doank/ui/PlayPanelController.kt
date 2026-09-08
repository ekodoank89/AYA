package com.aya.doank.ui

import android.app.Activity
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import com.aya.doank.R
import com.aya.doank.core.ConfigPusher
import com.aya.doank.core.Prefs
import com.aya.doank.core.SpoofTarget
import com.aya.doank.core.Targets

/**
 * Controller play/stop per target — v2.9.
 * Layout horizontal bawah: btn_grab | btn_gojek berdampingan.
 * Saat aktivasi: lock → push → callback → auto-launch app target
 * → push ulang terjadwal (1s/3s/6s) menangkap receiver target yang baru start.
 */
class PlayPanelController(
    private val activity: Activity,
    private val prefs: Prefs,
    private val pusher: ConfigPusher,
    private val centerProvider: () -> com.google.android.gms.maps.model.LatLng?,
    private val onToggle: (target: SpoofTarget, active: Boolean) -> Unit
) {
    private data class Row(
        val targetId: String, val btn: ImageButton, val dot: View
    )

    private val rows = mutableListOf<Row>()
    private val handler = Handler(Looper.getMainLooper())

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
            // 1) Lock koordinat pin saat ini
            centerProvider()?.let { prefs.setSpoofPoint(row.targetId, it.latitude, it.longitude) }
        }
        render(row)

        val target = Targets.byId(row.targetId)

        // 2) Push config ke target
        pusher.push(target)

        // 3) Callback UI (notif, dsb.)
        onToggle(target, active)

        // 4) Aktivasi → buka app target + push ulang terjadwal
        if (active) launchTarget(target)
    }

    /** Buka launcher activity target. Setelah launch, push diulang pada 1s/3s/6s —
     *  menangkap receiver target yang baru saja terpasang setelah proses start. */
    private fun launchTarget(target: SpoofTarget) {
        val pm = activity.packageManager

        var launched = false
        for (pkg in target.packageNames) {
            val launch = pm.getLaunchIntentForPackage(pkg)
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                activity.startActivity(launch)
                launched = true
                break
            }
        }

        if (launched) {
            // Push ulang terjadwal: menangkap receiver target yang baru saja terpasang
            listOf(1000L, 3000L, 6000L).forEach { delay ->
                handler.postDelayed({ pusher.push(target) }, delay)
            }
        } else {
            Toast.makeText(
                activity,
                "${target.label} tidak dapat dibuka — buka manual. Spoofing tetap aktif.",
                Toast.LENGTH_LONG
            ).show()
        }
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
