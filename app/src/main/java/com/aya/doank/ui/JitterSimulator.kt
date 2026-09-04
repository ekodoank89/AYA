package com.aya.doank.ui

import com.aya.doank.core.Prefs
import java.util.Random
import kotlin.math.cos
import kotlin.math.sqrt

/** Simulasi jitter di sisi MANAGER — sama rumus dengan Jitter di sisi hook. */
class JitterSimulator(private val prefs: Prefs, private val targetId: String) {
    private val rnd = Random()
    private var oLat = 0.0
    private var oLng = 0.0
    private var windowStart = 0L
    private var baseRef = Double.NaN

    private var prevOLat = 0.0
    private var prevOLng = 0.0
    private var lastSpeed = 0f
    private var lastBearing: Float? = null

    fun onBaseChanged(la: Double) {
        if (la != baseRef) {
            oLat = 0.0; oLng = 0.0
            prevOLat = 0.0; prevOLng = 0.0
            baseRef = la
            lastSpeed = 0f; lastBearing = null
        }
    }

    fun currentOffsetMeters(): Float {
        val mLat = 111320.0
        val mLng = 111320.0 * cos(Math.toRadians(baseRef.takeIf { !it.isNaN() } ?: 0.0))
        val dLatM = oLat * mLat
        val dLngM = oLng * mLng
        return sqrt(dLatM * dLatM + dLngM * dLngM).toFloat()
    }

    fun currentSpeedMps(): Float = lastSpeed
    fun currentBearingDeg(): Float? = lastBearing

    fun applyTo(baseLat: Double, baseLng: Double): Pair<Double, Double> {
        val jStep = prefs.jitterStep(targetId)
        val jWin = prefs.jitterWindowSec(targetId)
        val jRadius = prefs.jitterRadius(targetId)
        val now = System.currentTimeMillis()
        if (now - windowStart >= jWin * 1000L) {
            val dt = (now - windowStart) / 1000.0
            windowStart = now
            val mLat = 111320.0
            val mLng = 111320.0 * cos(Math.toRadians(baseLat))
            prevOLat = oLat; prevOLng = oLng
            oLat += ((rnd.nextDouble() - 0.5) * jStep) / mLat
            oLng += ((rnd.nextDouble() - 0.5) * jStep) / mLng
            val dLatM = oLat * mLat
            val dLngM = oLng * mLng
            val dist = sqrt(dLatM * dLatM + dLngM * dLngM)
            if (dist > jRadius) {
                val scale = jRadius / dist
                oLat = (dLatM * scale) / mLat
                oLng = (dLngM * scale) / mLng
            }
            val moveLatM = (oLat - prevOLat) * mLat
            val moveLngM = (oLng - prevOLng) * mLng
            val moveDist = sqrt(moveLatM * moveLatM + moveLngM * moveLngM)
            lastSpeed = (moveDist / jWin).toFloat()
            lastBearing = if (moveDist > 0.2) {
                ((Math.toDegrees(Math.atan2(moveLngM, moveLatM)) + 360.0) % 360.0).toFloat()
            } else null
        }
        return (baseLat + oLat) to (baseLng + oLng)
    }
}
