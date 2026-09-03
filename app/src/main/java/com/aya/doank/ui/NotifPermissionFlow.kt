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
 * Alur 3-jalur izin notifikasi (Android 13+):
 * granted → langsung; masih bisa dialog sistem → minta;
 * diblokir sistem (2x tolak) → arahkan ke Settings.
 * Prioritas: TIDAK memblokir spoofing — hanya memastikan kesadaran user.
 */
class NotifPermissionFlow(
    private val activity: Activity,
    private val launcher: ActivityResultLauncher<String>
) {
    fun isGranted(): Boolean =
        Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Dipanggil sebelum ▶: kalau izin belum ada, minta/guide — TAPI tetap izinkan play. */
    fun ensureBeforePlay(onContinue: () -> Unit) {
        if (Build.VERSION.SDK_INT < 33) { onContinue(); return }
        if (isGranted()) { onContinue(); return }

        if (canShowSystemDialog()) {
            Toast.makeText(activity,
                "Izinkan notifikasi agar tombol STOP tersedia di status bar",
                Toast.LENGTH_LONG).show()
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            // Play tetap diteruskan — user bisa main tanpa notif, tapi sudah diperingatkan
            onContinue()
        } else {
            // Diblokir sistem → dialog app + Settings, play TETAP jalan
            showBlockedDialog(onContinue)
        }
    }

    /** Dipanggil saat buka app pertama: minta sekali, halus. */
    fun requestAtStartup() {
        if (Build.VERSION.SDK_INT < 33) return
        if (isGranted()) return
        if (canShowSystemDialog()) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        // Diblokir: tidak mengganggu di startup — cukup ditangani saat ▶
    }

    private fun canShowSystemDialog(): Boolean =
        !prefsAskedOnce() ||
                ActivityCompat.shouldShowRequestPermissionRationale(
                    activity, Manifest.permission.POST_NOTIFICATIONS)

    // Notifikasi berbeda dari lokasi: tidak perlu flag "pernah ditanya" —
    // rationale=false + granted=false setelah pernah diminta = blokir permanen.
    // Tapi "belum pernah diminta" juga false → gunakan SharedPreferences sederhana.
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
            }
            .setNegativeButton("Lanjut tanpa notifikasi") { _, _ -> onContinue() }
            .setOnCancelListener { onContinue() }
            .show()
    }
}
