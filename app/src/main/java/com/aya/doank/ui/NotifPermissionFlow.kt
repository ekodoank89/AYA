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
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.aya.doank.R

/**
 * Izin notifikasi (Android 13+). v2.4: fungsi requestInChain untuk rantai —
 * keputusan berdasarkan STATUS IZIN AKTUAL, bukan flag "pernah ditanya".
 */
class NotifPermissionFlow(
    private val activity: Activity,
    private val launcher: ActivityResultLauncher<String>
) {
    fun isGranted(): Boolean =
        Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Tahap rantai: minta notifikasi (atau arahkan Settings bila diblokir). onDone selalu dipanggil. */
    fun requestInChain(onDone: () -> Unit) {
        if (Build.VERSION.SDK_INT < 33) { onDone(); return }
        if (isGranted()) { onDone(); return }

        val canDialog = !prefsAskedOnce() ||
                ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)

        if (canDialog) {
            markAsked()
            notifDone = onDone
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            // onDone dipanggil dari launcher callback (notifPermLauncher di MainActivity)
        } else {
            AlertDialog.Builder(activity, R.style.Theme_AYA_Dialog)
                .setTitle("Izin notifikasi diblokir")
                .setMessage(
                    "Tombol STOP di status bar tidak akan muncul tanpa izin notifikasi.\n\n" +
                    "Aktifkan izin di Pengaturan untuk kendali jarak jauh penuh."
                )
                .setPositiveButton("Buka Pengaturan") { _, _ ->
                    activity.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", activity.packageName, null)
                        }
                    )
                    onDone()
                }
                .setNegativeButton("Lewati") { _, _ -> onDone() }
                .setOnCancelListener { onDone() }
                .show()
        }
    }

    /** Dipanggil MainActivity dari launcher callback — melanjutkan rantai. */
    var notifDone: (() -> Unit)? = null

    /** Dipanggil sebelum ▶: pastikan user sadar — TIDAK memblokir play. */
    fun ensureBeforePlay(onContinue: () -> Unit) {
        if (Build.VERSION.SDK_INT < 33) { onContinue(); return }
        if (isGranted()) { onContinue(); return }

        val canDialog = !prefsAskedOnce() ||
                ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)

        if (canDialog) {
            Toast.makeText(activity,
                "Izinkan notifikasi agar tombol STOP tersedia di status bar",
                Toast.LENGTH_LONG).show()
            markAsked()
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            onContinue()
        } else {
            showBlockedDialog(onContinue)
        }
    }

    private fun showBlockedDialog(onContinue: () -> Unit) {
        AlertDialog.Builder(activity, R.style.Theme_AYA_Dialog)
            .setTitle("Izin notifikasi diblokir")
            .setMessage(
                "Tombol STOP di status bar tidak akan muncul tanpa izin notifikasi.\n\n" +
                "Anda tetap bisa memakai AYA, tapi tanpa kendali jarak jauh."
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

    private fun prefsAskedOnce(): Boolean =
        activity.getSharedPreferences("aya_prefs", Activity.MODE_PRIVATE)
            .getBoolean("notif_asked", false)

    fun markAsked() {
        activity.getSharedPreferences("aya_prefs", Activity.MODE_PRIVATE)
            .edit().putBoolean("notif_asked", true).apply()
    }
}
