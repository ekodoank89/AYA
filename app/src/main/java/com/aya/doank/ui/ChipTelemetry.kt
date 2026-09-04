package com.aya.doank.ui

import android.app.Activity
import android.widget.LinearLayout
import android.widget.TextView
import com.aya.doank.R
import com.aya.doank.core.Prefs
import com.aya.doank.core.Targets
import java.util.Locale

/**
 * Chip telemetri per-target di manager — menampilkan posisi fake yang dilihat
 * app target (replikasi jitter di sisi manager, dari titik lock di Prefs).
 */
class ChipTelemetry(
    private val activity: Activity,
    private val prefs: Prefs
) {
    private class Chip(val name: TextView, val coord: TextView, val extra: TextView, val root: LinearLayout)

    private val chips = mutableMapOf<String, Chip>()
    private val sims = mutableMapOf<String, JitterSimulator>()

    fun bind() {
        Targets.all.forEach { t ->
            val rootId = if (t.id == Targets.GRAB.id) R.id.chip_grab else R.id.chip_gojek
            val root = activity.findViewById<LinearLayout>(rootId)
            chips[t.id] = Chip(
                root.findViewById(R.id.tc_name),
                root.findViewById(R.id.tc_coord),
                root.findViewById(R.id.tc_extra),
                root
            )
            sims[t.id] = JitterSimulator(prefs, t.id)
        }
    }

    /** Dipanggil tiap 1 dtk — update semua chip. */
    fun onTick() {
        Targets.all.forEach { t ->
            val chip = chips[t.id] ?: return@forEach
            val sim = sims[t.id] ?: return@forEach
            val active = prefs.isSpoofActive(t.id)
            val lock = prefs.spoofPoint(t.id)

            if (!active || lock == null) {
                chip.root.alpha = 0.5f
                chip.nameView().text = "${t.label} — OFF"
                chip.coordView().text = "—"
                chip.extraView().text = ""
                return@forEach
            }

            chip.root.alpha = 1f
            chip.nameView().text = "${t.label} — LIVE"
            val (oLat, oLng) = sim.applyTo(lock.first, lock.second)
            val lat = lock.first + oLat
            val lng = lock.second + oLng
            chip.coordView().text = String.format(Locale.US, "%.6f, %.6f", lat, lng)

            val acc = (4f + sim.currentOffsetMeters() * 1.2f).coerceIn(4f, 12f)
            val spd = sim.currentSpeedMps()
            val brg = sim.currentBearingDeg()
            val alt = 20.0 + sim.currentOffsetMeters() * 0.5

            chip.extraView().text = String.format(
                Locale.US,
                "acc %.1f m · spd %.2f m/s · brg %s° · alt %.1f m",
                acc, spd, brg?.let { "%.0f".format(it) } ?: "—", alt
            )
        }

        // (helper)
        // Karena kita pakai inner-class sederhana, akses view via id:
        // sudah disederhanakan lewat chip.nameView() dll di bawah.
    }

    // Helper akses TextView dari Chip
    private fun Chip.nameView(): TextView = name
    private fun Chip.coordView(): TextView = coord
    private fun Chip.extraView(): TextView = extra

    // (Deklarasi view id sudah ditangani saat bind di atas — menyimpan langsung objek TextView)
    private inner class ChipViews(
        val name: TextView, val coord: TextView, val extra: TextView, val root: LinearLayout
    )
}
