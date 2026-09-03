package com.aya.doank.ui

import android.app.Activity
import android.content.Intent
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import com.aya.doank.R
import com.aya.doank.core.Prefs
import com.aya.doank.core.SpoofTarget
import com.aya.doank.core.Targets

/**
 * Panel play: GRAB & GOJEK.
 * v2.6.3: saat ▶ (AKTIFKAN), selain lock+push, langsung membuka aplikasi target.
 * Saat ■ (stop) tidak membuka apa pun. Fallback terukur bila app tak ditemukan.
 */
class PlayPanelController(
    private val activity: Activity,
    private val prefs: Prefs,
    private val centerProvider: () -> com.google.android.gms.maps.model.LatLng?,
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
            // 1) Lock koordinat pin saat ini
            centerProvider()?.let { prefs.setSpoofPoint(row.targetId, it.latitude, it.longitude) }
        }
        render(row)

        // 2) Push dulu — agar config sudah benar SEBELUM app target dibuka
        val target = Targets.byId(row.targetId)
        onToggle(target, active)

        // 3) ▶ = buka aplikasi target (bukan saat ■)
        if (active) launchTarget(target)
    }

    /** Buka launcher activity target; coba tiap package di daftar, fallback toast. */
    private fun launchTarget(target: SpoofTarget) {
        val pm = activity.packageManager

        // Jalur 1: launcher activity (cara paling andal membuka app lain)
        for (pkg in target.packageNames) {
            val launch = pm.getLaunchIntentForPackage(pkg)
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                activity.startActivity(launch)
                return
            }
        }

        // Jalur 2 (fallback): coba explicit activity utama via resolve — sudah dicakup
        // queries di manifest; kalau semua gagal → toast, spoofing tetap aktif.
        Toast.makeText(
            activity,
            "${target.label} tidak dapat dibuka — buka manual. Spoofing tetap aktif.",
            Toast.LENGTH_LONG
        ).show()
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
