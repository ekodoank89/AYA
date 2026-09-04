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
import com.aya.doank.core.Targets
import com.aya.doank.core.SpoofTarget
import java.util.Locale

/**
 * Satu notifikasi per target aktif (ongoing, puncak shade).
 * v2.6.8: notifId & requestCode DETERMINISTIK per target (index di Targets.all)
 * — menjamin kedua tombol ■ STOP unik & berfungsi terpisah.
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

    fun show(target: SpoofTarget, lat: Double, lng: Double) {
        if (!canNotify()) return
        val stopIntent = PendingIntent.getBroadcast(
            context,
            requestCode(target),                    // ← deterministik: 1 utk GRAB, 2 utk GOJEK
            Intent(ACTION_STOP).setPackage(context.packageName)
                .putExtra("target_id", target.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n: Notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_aya)
            .setContentTitle("AYA — ${target.label} aktif")
            .setContentText(String.format(Locale.US, "%.6f, %.6f", lat, lng))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .setSortKey("1")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(0, "■ STOP", stopIntent)
            .build()
        nm.notify(notifId(target), n)
    }

    fun hide(target: SpoofTarget) = nm.cancel(notifId(target))

    private fun notifId(t: SpoofTarget): Int = NOTIF_ID_BASE + slotIndex(t)

    private fun requestCode(t: SpoofTarget): Int = slotIndex(t) + 1

    /** Slot deterministik per target dari urutan di Targets.all (0,1,2,...) — tanpa hash. */
    private fun slotIndex(t: SpoofTarget): Int =
        Targets.all.indexOfFirst { it.id == t.id }

    class StopReceiver : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            i.getStringExtra("target_id")?.let { onNotifStop?.invoke(it) }
        }
    }

    companion object {
        private const val CHANNEL_ID = "aya_spoof"
        private const val NOTIF_ID_BASE = 100
        const val ACTION_STOP = "com.aya.doank.NOTIF_STOP"

        var onNotifStop: ((targetId: String) -> Unit)? = null
    }
}
