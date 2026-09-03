package com.aya.doank.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.aya.doank.core.Prefs
import com.aya.doank.R

class PermissionFlow(
    private val activity: AppCompatActivity,
    private val prefs: Prefs,
    private val onGranted: () -> Unit
) {
    /**
     * Dipanggil saat ALUR izin lokasi selesai (apapun hasilnya):
     * granted langsung, granted/ditolak dari dialog, atau dialog blokir ditutup (Batal).
     * SATU PENGECUALIAN: cabang "Buka Pengaturan" tidak memicu di sini —
     * pemanggil (MainActivity) menanganinya via onResume + flag.
     */
    var onSettled: (() -> Unit)? = null

    private val permissions = arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    private val launcher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) onGranted()
        else Toast.makeText(activity, "Izin lokasi ditolak — peta tetap bisa digunakan", Toast.LENGTH_LONG).show()
        onSettled?.invoke()   // alur selesai (granted atau ditolak)
    }

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** 3 jalur: sudah izin → langsung; masih bisa dialog → minta; diblokir → arahkan Settings. */
    fun requestOrGuide() {
        when {
            hasPermission()       -> { onGranted(); onSettled?.invoke() }   // alur instan selesai
            canShowSystemDialog() -> request()
            else                  -> showBlockedDialog()
        }
    }

    private fun request() {
        prefs.askedLocation = true
        launcher.launch(permissions)
    }

    private fun canShowSystemDialog(): Boolean {
        if (!prefs.askedLocation) return true
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION) ||
                ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    private fun showBlockedDialog() {
        AlertDialog.Builder(activity, R.style.Theme_AYA_Dialog)
            .setTitle("Izin lokasi diblokir")
            .setMessage("Izin lokasi telah ditolak berulang kali, sehingga sistem tidak lagi menampilkan dialog. Untuk mengaktifkan fitur titik biru, nyalakan izin Lokasi di Pengaturan aplikasi.")
            .setPositiveButton("Buka Pengaturan") { _, _ ->
                // Buka Pengaturan = alur berlanjut di luar app.
                // onSettled TIDAK dipanggil di sini — MainActivity menanganinya
                // di onResume setelah user kembali (lihat awaitingLocationSettle).
                activity.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", activity.packageName, null)
                    }
                )
            }
            .setNegativeButton("Batal") { _, _ -> onSettled?.invoke() }   // user menunda → alur selesai
            .setOnCancelListener { onSettled?.invoke() }
            .show()
    }

    fun openSettings() {
        activity.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", activity.packageName, null)
            }
        )
    }
}
