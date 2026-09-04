package com.aya.doank.ui

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.aya.doank.R
import com.aya.doank.core.SpoofTarget
import com.aya.doank.core.Targets
import java.util.Locale

/**
 * SATU notifikasi gabungan untuk semua target aktif.
 * v2.6.9: menggantikan notif per-target — menyelesaikan masalah ROM (MIUI)
 * yang menyembunyikan tombol pada notif kedua. Kini keduanya selalu tampil
 * penuh dengan tombol STOP masing-masing, di satu notifikasi puncak.
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
                    description = "Kendali spoofing AYA"
                    setShowBadge(false)
                }
            )
        }
    }

    fun canNotify(): Boolean =
        Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /**
     * Update notifikasi gabungan — dipanggil dengan daftar target aktif + posisinya.
     * Bila daftar kosong → notif dihapus.
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

        when (activeTargets.size) {
            1 -> {
                val (t, p) = activeTargets[0]
                b.setContentTitle("AYA — ${t.label} aktif")
                b.setContentText(String.format(Locale.US, "%.6f, %.6f", p.first, p.second))
                b.addAction(
                    0, "■ STOP ${t.label}",
                    stopIntent(t)
                )
            }
            else -> {
                b.setContentTitle("AYA — ${activeTargets.size} target aktif")
                // BigTextStyle: daftar semua target + koordinat masing-masing
                val sb = StringBuilder()
                activeTargets.forEach { (t, p) ->
                    sb.append("${t.label}: ")
                        .append(String.format(Locale.US, "%.6f, %.6f", p.first, p.second))
                        .append("\n")
                }
                b.setStyle(NotificationCompat.BigTextStyle().bigText(sb.toString().trim()))
                // Tombol STOP per target (maks 3 tombol di Android)
                activeTargets.take(3).forEach { (t, _) ->
                    b.addAction(0, "■ STOP ${t.label}", stopIntent(t))
                }
            }
        }

        nm.notify(NOTIF_ID, b.build())
    }

    fun clear() = nm.cancel(NOTIF_ID)

    private fun stopIntent(t: SpoofTarget): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            slotIndex(t) + 1,                      // 1 utk GRAB, 2 utk GOJEK — deterministik
            Intent(ACTION_STOP).setPackage(context.packageName)
                .putExtra("target_id", t.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun slotIndex(t: SpoofTarget): Int =
        Targets.all.indexOfFirst { it.id == t.id }

    class StopReceiver : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            i.getStringExtra("target_id")?.let { onNotifStop?.invoke(it) }
        }
    }

    companion object {
        private const val CHANNEL_ID = "aya_spoof"
        private const val NOTIF_ID = 100
        const val ACTION_STOP = "com.aya.doank.NOTIF_STOP"

        var onNotifStop: ((targetId: String) -> Unit)? = null
    }
}
