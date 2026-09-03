package com.aya.doank.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.aya.doank.R

/**
 * Alur 3-jalur izin notifikasi (Android 13+).
 * Dipanggil SETELAH alur izin lokasi selesai (via onSettled) — bukan paralel dengannya.
 */
class NotifPermissionFlow(
    private val activity: Activity,
    private val launcher: ActivityResultLauncher<String>
) {
    fun isGranted(): Boolean =
        Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /**
     * Permintaan "startup" — dipanggil tepat setelah alur lokasi selesai.
     * Hanya SEKALI seumur hidup app (guard notif_asked); persuasi ulang
     * saat ▶ ditangani ensureBeforePlay().
     */
    fun requestAfterLocation(): Boolean {
        if (Build.VERSION.SDK_INT < 33) return false
        if (isGranted()) return false
        if (prefsAskedOnce()) return false          // sudah pernah → jalur ▶ yang menangani
        if (!canShowSystemDialog()) return false    // diblokir → jalur ▶ yang menangani

        markAsked()
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        return true
    }

    /** Dipanggil sebelum ▶: pastikan user SADAR — tapi TIDAK memblokir play. */
    fun ensureBeforePlay(onContinue: () -> Unit) {
        if (Build.VERSION.SDK_INT < 33) { onContinue(); return }
        if (isGranted()) { onContinue(); return }

        if (canShowSystemDialog()) {
            Toast.makeText(activity,
                "Izinkan notifikasi agar tombol STOP tersedia di status bar",
                Toast.LENGTH_LONG).show()
            markAsked()
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            onContinue()   // play tetap jalan walau belum menjawab
        } else {
            showBlockedDialog(onContinue)
        }
    }

    private fun canShowSystemDialog(): Boolean =
        !prefsAskedOnce() ||
                ActivityCompat.shouldShowRequestPermissionRationale(
                    activity, Manifest.permission.POST_NOTIFICATIONS)

    private fun prefsAskedOnce(): Boolean =
        activity.getSharedPreferences("aya_prefs", Activity.MODE_PRIVATE)
            .getBoolean("notif_asked", false)

    fun markAsked() {
        activity.getSharedPreferences("aya_prefs", Activity.MODE_PRIVATE)
            .edit().putBoolean("notif_asked", true).apply()
    }

    private fun showBlockedDialog(onContinue: () -> Unit) {
        androidx.appcompat.app.AlertDialog.Builder(activity, R.style.Theme_AYA_Dialog)
            .setTitle("Izin notifikasi diblokir")
            .setMessage(
                "Tombol STOP di status bar tidak akan muncul tanpa izin notifikasi.\n\n" +
                "Anda tetap bisa memakai AYA, tapi tanpa kendali jarak jauh — " +
                "matikan spoofing harus dari app ini.\n\n" +
                "Aktifkan izin di Pengaturan untuk pengalaman penuh."
            )
            .setPositiveButton("Buka Pengaturan") { _, _ ->
                activity.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", activity.packageName, null)
                    }
                )
                onContinue()
            }
            .setNegativeButton("Lanjut tanpa notifikasi") { _, _ -> onContinue() }
            .setOnCancelListener { onContinue() }
            .show()
    }
}
