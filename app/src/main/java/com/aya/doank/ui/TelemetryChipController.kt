package com.aya.doank.ui

import android.app.Activity
import android.widget.LinearLayout
import android.widget.TextView
import com.aya.doank.R
import com.aya.doank.core.Targets
import com.aya.doank.xposed.SpoofConfig
import java.util.Locale

/**
 * Chip telemetri per-target (GRAB & GOJEK) di kiri-atas layar.
 * Menampilkan: koordinat fake, accuracy, speed, bearing, altitude.
 * Update tiap 1 dtk via onTick() dari pemanggil.
 */
class TelemetryChipController(
    private val activity: Activity,
    private val configs: Map<String, SpoofConfig>
) {
    private data class Chip(val name: TextView, val coord: TextView, val extra: TextView, val root: View)

    private val chips = mutableMapOf<String, Chip>()

    fun bind() {
        Targets.all.forEach { t ->
            val root = activity.findViewById<LinearLayout>(
                if (t.id == Targets.GRAB.id) R.id.chip_grab else R.id.chip_gojek
            )
            chips[t.id] = Chip(
                root.findViewById(R.id.tc_name),
                root.findViewById(R.id.tc_coord),
                root.findViewById(R.id.tc_extra),
                root
            )
        }
    }

    /** Dipanggil tiap 1 dtk dari MainActivity. */
    fun onTick() {
        Targets.all.forEach { t ->
            val c = chips[t.id] ?: return@forEach
            val cfg = configs[t.id] ?: return@forEach
            if (!prefs.isActive(t.id)) {
                c.name.text = "${t.label} — OFF"
                c.coord.text = "—"
                c.extra.text = ""
                c.root.alpha = 0.5f
                return@forEach
            }
            c.root.alpha = 1f
            c.name.text = "${t.label} — LIVE"
            val lat = cfg.latitude()
            val lng = cfg.longitude()
            if (lat != null && lng != null) {
                c.coord.text = String.format(Locale.US, "%.6f, %.6f", lat, lng)
                val acc = cfg.accuracy()
                val spd = cfg.speedMps()
                val brg = cfg.bearingDeg()
                val alt = cfg.altitudeM()
                c.extra.text = String.format(
                    Locale.US,
                    "acc %.1fm · spd %.2fm/s · brg %s° · alt %.1fm",
                    acc, spd, brg?.let { "%.0f".format(it) } ?: "—", alt
                )
            } else {
                c.extra.text = "menunggu config…"
            }
        }
    }

    private val prefs: com.aya.doank.core.Prefs by lazy {
        com.aya.doank.core.Prefs(activity)
    }
}
