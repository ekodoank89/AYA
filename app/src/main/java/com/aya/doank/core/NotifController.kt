package com.aya.doank.core

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

/**
 * Satu notifikasi per target aktif (id stabil per target).
 * Ongoing (tak bisa di-swipe) — notif hilang = spoofing memang berhenti.
 * Tombol STOP mengirim broadcast ke dalam app sendiri → MainActivity menangani stop.
 */
class NotifController(private val context: Context) {

    private val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "AYA Spoofing", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    fun canNotify(): Boolean =
        Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun show(target: SpoofTarget, lat: Double, lng: Double) {
        if (!canNotify()) return
        val stopIntent = PendingIntent.getBroadcast(
            context, target.id.hashCode(),
            Intent(ACTION_STOP).setPackage(context.packageName).putExtra("target_id", target.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n: Notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_aya)      // dibuat di File 6
            .setContentTitle("AYA — ${target.label} aktif")
            .setContentText(String.format(java.util.Locale.US, "%.6f, %.6f", lat, lng))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(0, "■ STOP", stopIntent)
            .build()
        nm.notify(notifId(target), n)
    }

    fun hide(target: SpoofTarget) = nm.cancel(notifId(target))

    private fun notifId(t: SpoofTarget) = NOTIF_ID_BASE + t.id.hashCode().mod(1000)

    /** Callback statis: di-set MainActivity, dipanggil receiver. */
    companion object {
        private const val CHANNEL_ID = "aya_spoof"
        private const val NOTIF_ID_BASE = 100
        const val ACTION_STOP = "com.aya.doank.NOTIF_STOP"

        var onNotifStop: ((targetId: String) -> Unit)? = null

        class StopReceiver : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                i.getStringExtra("target_id")?.let { onNotifStop?.invoke(it) }
            }
        }
    }
}
