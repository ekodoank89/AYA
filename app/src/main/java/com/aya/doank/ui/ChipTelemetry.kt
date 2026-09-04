package com.aya.doank.ui

import android.app.Activity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.aya.doank.R
import com.aya.doank.core.Prefs
import com.aya.doank.core.Targets
import com.aya.doank.ui.JitterSimulator
import java.util.Locale

/**
 * Chip telemetri per-target di manager.
 * Menampilkan posisi fake (replikasi jitter di sisi manager) + accuracy + speed
 * + bearing + altitude — data yang sama dengan yang dilihat app target.
 */
class ChipTelemetry(
    private val activity: Activity,
    private val prefs: Prefs
) {
    private data class ChipViews(
        val root: LinearLayout, val name: TextView,
        val coord: TextView, val extra: TextView
    )

    private val chips = mutableMapOf<String, ChipViews>()
    private val sims = mutableMapOf<String, JitterSimulator>()

    fun bind() {
        Targets.all.forEach { t ->
            val rootId = if (t.id == Targets.GRAB.id) R.id.chip_grab else R.id.chip_gojek
            val root = activity.findViewById<LinearLayout>(rootId)
            chips[t.id] = ChipViews(
                root,
                root.findViewById(R.id.tc_name),
                root.findViewById(R.id.tc_coord),
                root.findViewById(R.id.tc_extra)
            )
            sims[t.id] = JitterSimulator(prefs, t.id)
        }
    }

    /** Dipanggil tiap 1 dtk dari MainActivity. */
    fun onTick() {
        Targets.all.forEach { t ->
            val chip = chips[t.id] ?: return@forEach
            val sim = sims.getOrPut(t.id) { JitterSimulator(prefs, t.id) }
            val active = prefs.isSpoofActive(t.id)
            val lock = prefs.spoofPoint(t.id)

            if (!active || lock == null) {
                chip.root.alpha = 0.5f
                chip.name.text = "${t.label} — OFF"
                chip.coord.text = "—"
                chip.extra.text = ""
                return@forEach
            }

            chip.root.alpha = 1f
            chip.name.text = "${t.label} — LIVE"

            val (oLat, oLng) = sim.applyTo(lock.first, lock.second)
            val lat = lock.first + oLat
            val lng = lock.second + oLng
            chip.coord.text = String.format(Locale.US, "%.6f, %.6f", lat, lng)

            val acc = (4f + sim.currentOffsetMeters() * 1.2f).coerceIn(4f, 12f)
            val spd = sim.currentSpeedMps()
            val brg = sim.currentBearingDeg()
            val alt = 20.0 + sim.currentOffsetMeters() * 0.5

            chip.extra.text = String.format(
                Locale.US,
                "acc %.1fm · spd %.2fm/s · brg %s° · alt %.1fm",
                acc, spd, brg?.let { "%.0f".format(it) } ?: "—", alt
            )
        }
    }
}
