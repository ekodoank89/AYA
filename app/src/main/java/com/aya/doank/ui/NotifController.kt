package com.aya.doank.ui

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.aya.doank.R
import com.aya.doank.core.SpoofTarget
import java.util.Locale

/**
 * SATU notifikasi gabungan — INDIKATOR saja, TANPA tombol.
 * v2.7.1: tombol ■ STOP dihapus dari status bar (keputusan user —
 * stop tak sengaja saat order masuk membuat order hilang).
 * Stop kini HANYA dari app AYA (tombol ■ di panel).
 * Tetap: IMPORTANCE_HIGH + ongoing + category navigation → puncak shade.
 */
class NotifController(private val context: Context) {

    private val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        if (Build.VERSION.SDK_INT >= 26) {
            try {
                nm.deleteNotificationChannel(CHANNEL_ID)
            } catch (_: Throwable) { }
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "AYA Spoofing",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Indikator spoofing AYA"
                    setShowBadge(false)
                }
            )
        }
    }

    fun canNotify(): Boolean =
        Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    /**
     * Update notifikasi indikator — daftar target AKTIF + koordinat lock-nya.
     * Daftar kosong → notifikasi dihapus.
     */
    fun update(activeTargets: List<Pair<SpoofTarget, Pair<Double, Double>>>) {
        if (!canNotify()) return

        if (activeTargets.isEmpty()) {
            nm.cancel(NOTIF_ID)
            return
        }

        val b = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_aya)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .setSortKey("1")
            .setOngoing(true)
            .setOnlyAlertOnce(true)

        if (activeTargets.size == 1) {
            val (t, p) = activeTargets[0]
            b.setContentTitle("AYA — ${t.label} aktif")
            b.setContentText(String.format(Locale.US, "%.6f, %.6f", p.first, p.second))
        } else {
            b.setContentTitle("AYA — ${activeTargets.size} target aktif")
            val sb = StringBuilder()
            activeTargets.forEach { (t, p) ->
                sb.append("${t.label}: ")
                    .append(String.format(Locale.US, "%.6f, %.6f", p.first, p.second))
                    .append("\n")
            }
            b.setStyle(NotificationCompat.BigTextStyle().bigText(sb.toString().trim()))
        }

        nm.notify(NOTIF_ID, b.build())
    }

    fun clear() = nm.cancel(NOTIF_ID)

    companion object {
        private const val CHANNEL_ID = "aya_spoof"
        private const val NOTIF_ID = 100
    }
}
