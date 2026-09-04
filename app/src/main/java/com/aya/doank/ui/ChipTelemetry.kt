package com.aya.doank.ui

import android.app.Activity
import android.widget.LinearLayout
import android.widget.TextView
import com.aya.doank.R
import com.aya.doank.core.Prefs
import com.aya.doank.core.Targets
import java.util.Locale
import java.util.Random
import kotlin.math.cos
import kotlin.math.sqrt

/**
 * Chip telemetri per-target di manager (kiri-atas).
 * Menampilkan replikasi posisi fake + accuracy + speed + bearing + altitude,
 * dihitung dari titik lock (Prefs) dengan rumus jitter yang sama.
 * Perkiraan di sisi manager — data asli di proses target bisa sedikit berbeda.
 */
class ChipTelemetry(
    private val activity: Activity,
    private val prefs: Prefs
) {
    private data class ChipViews(
        val root: LinearLayout, val name: TextView,
        val coord: TextView, val extra: TextView
    )

    /** Simulasi jitter — rumus identik dengan Jitter.kt (clamp vektor). */
    private class JitterSim(
        private val prefs: Prefs, private val id: String
    ) {
        private val rnd = Random()
        private var oLat = 0.0; private var oLng = 0.0
        private var windowStart = 0L
        private var prevOLat = 0.0; private var prevOLng = 0.0
        private var lastSpeed = 0f; private var lastBearing: Float? = null

        fun advance(baseLat: Double, baseLng: Double): Pair<Double, Double> {
            val step = prefs.jitterStep(id)
            val win = prefs.jitterWindowSec(id)
            val radius = prefs.jitterRadius(id)
            val now = System.currentTimeMillis()
            if (now - windowStart >= win * 1000L) {
                windowStart = now
                val mLat = 111320.0
                val mLng = 111320.0 * cos(Math.toRadians(baseLat))
                prevOLat = oLat; prevOLng = oLng
                oLat += ((rnd.nextDouble() - 0.5) * step) / mLat
                oLng += ((rnd.nextDouble() - 0.5) * step) / mLng
                val dLatM = oLat * mLat; val dLngM = oLng * mLng
                val dist = sqrt(dLatM * dLatM + dLngM * dLngM)
                if (dist > radius) {
                    val s = radius / dist
                    oLat = (dLatM * s) / mLat
                    oLng = (dLngM * s) / mLng
                }
                val mvLat = (oLat - prevOLat) * mLat
                val mvLng = (oLng - prevOLng) * mLng
                val mvDist = sqrt(mvLat * mvLat + mvLng * mvLng)
                lastSpeed = (mvDist / win).toFloat()
                lastBearing = if (mvDist > 0.2)
                    ((Math.toDegrees(Math.atan2(mvLng, mvLat)) + 360.0) % 360.0).toFloat()
                else null
            }
            return (baseLat + oLat) to (baseLng + oLng)
        }

        fun currentOffsetMeters(baseLat: Double): Float {
            val mLat = 111320.0
            val mLng = 111320.0 * cos(Math.toRadians(baseLat))
            val dLatM = oLat * mLat; val dLngM = oLng * mLng
            return sqrt(dLatM * dLatM + dLngM * dLngM).toFloat()
        }
        fun currentSpeed(): Float = lastSpeed
        fun currentBearing(): Float? = lastBearing
    }

    private data class ChipViews(
        val root: LinearLayout, val name: TextView,
        val coord: TextView, val extra: TextView
    )

    private data class Sim(val jit: JitterSim)

    private val chips = mutableMapOf<String, ChipViews>()
    private val sims = mutableMapOf<String, Sim>()

    fun bind() {
        fun register(id: String, rootId: Int, nameId: Int, coordId: Int, extraId: Int) {
            val root = activity.findViewById<LinearLayout>(rootId)
            chips[id] = ChipViews(
                root,
                root.findViewById(nameId),
                root.findViewById(coordId),
                root.findViewById(extraId)
            )
            sims[id] = Sim(JitterSim(prefs, id))
        }
        register(Targets.GRAB.id,  R.id.chip_grab,  R.id.tc_name, R.id.tc_coord, R.id.tc_extra)
        register(Targets.GOJEK.id, R.id.chip_gojek, R.id.tc_name_gojek, R.id.tc_coord_gojek, R.id.tc_extra_gojek)
    }

    /** Dipanggil tiap 1 dtk dari MainActivity — update seluruh chip. */
    fun onTick() {
        Targets.all.forEach { t ->
            val chip = chips[t.id] ?: return@forEach
            val sim = sims[t.id] ?: return@forEach
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

            val (oLat, oLng) = sim.jit.advance(lock.first, lock.second)
            val lat = lock.first + oLat
            val lng = lock.second + oLng
            chip.coord.text = String.format(Locale.US, "%.6f, %.6f", lat, lng)

            val acc = (4f + sim.jit.currentOffsetMeters(lock.first) * 1.2f).coerceIn(4f, 12f)
            val spd = sim.jit.currentSpeed()
            val brg = sim.jit.currentBearing()
            val alt = 20.0 + sim.jit.currentOffsetMeters(lock.first) * 0.5

            chip.extra.text = String.format(
                Locale.US,
                "acc %.1fm · spd %.2fm/s · brg %s° · alt %.1fm",
                acc, spd, brg?.let { "%.0f".format(it) } ?: "—", alt
            )
        }
    }
}
