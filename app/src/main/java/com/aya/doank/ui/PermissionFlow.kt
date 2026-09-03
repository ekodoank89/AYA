package com.aya.doank.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.aya.doank.core.Prefs
import com.aya.doank.R

/**
 * Rantai izin (urutan): Lokasi dasar → Lokasi BACKGROUND → Battery optimization → selesai.
 * Notifikasi & auto-start ditangani NotifPermissionFlow / vendor guide (MainActivity).
 * Sinyal: onSettled (alur lokasi dasar selesai), onBackgroundSettled, onBatterySettled.
 */
class PermissionFlow(
    private val activity: AppCompatActivity,
    private val prefs: Prefs,
    private val onGranted: () -> Unit
) {
    /** Alur izin lokasi dasar selesai (granted/ditolak/batal-settings). */
    var onSettled: (() -> Unit)? = null
    /** Alur background location selesai (apapun hasilnya). */
    var onBackgroundSettled: (() -> Unit)? = null
    /** Alur battery selesai. */
    var onBatterySettled: (() -> Unit)? = null

    private val permissions = arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    private val launcher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            onGranted()
            Toast.makeText(activity, "Izin lokasi diberikan", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(activity, "Izin lokasi ditolak — peta tetap bisa digunakan", Toast.LENGTH_LONG).show()
        }
        onSettled?.invoke()
    }

    // ====== 1) LOKASI DASAR ======
    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun requestOrGuide() {
        when {
            hasPermission()       -> { onGranted(); onSettled?.invoke() }
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
            .setMessage("Izin lokasi telah ditolak berulang kali. Untuk fitur titik biru, nyalakan izin Lokasi di Pengaturan aplikasi.")
            .setPositiveButton("Buka Pengaturan") { _, _ ->
                activity.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", activity.packageName, null)
                    }
                )
            }
            .setNegativeButton("Batal") { _, _ -> onSettled?.invoke() }
            .setOnCancelListener { onSettled?.invoke() }
            .show()
    }

    // ====== 2) LOKASI BACKGROUND ("Selalu izinkan") ======
    fun hasBackgroundLocation(): Boolean =
        Build.VERSION.SDK_INT < 29 ||
                ContextCompat.checkSelfPermission(
                    activity, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

    /**
     * Tahap 2 — dipanggil SETELAH lokasi dasar granted.
     * Android 10+: dialog "Allow all the time" tidak bisa dipicu launcher biasa;
     * kita tunjukkan dialog penjelasan → tombol Settings (halaman izin app).
     */
    fun requestBackgroundLocation() {
        if (hasBackgroundLocation()) {
            onBackgroundSettled?.invoke()
            return
        }
        if (!hasPermission()) {   // dasar belum ada — hentikan rantai di sini
            onBackgroundSettled?.invoke()
            return
        }
        if (Build.VERSION.SDK_INT >= 29 &&
            !activity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION) &&
            prefs.askedBackground
        ) {
            // Diblokir / ditolak permanen → langsung Settings
            showBackgroundSettingsDialog()
        } else {
            // Dialog penjelasan dulu (WAJIB di Android 11+: tanpa ini, opsi "Always" tidak muncul)
            AlertDialog.Builder(activity, R.style.Theme_AYA_Dialog)
                .setTitle("Izinkan lokasi di latar belakang?")
                .setMessage(
                    "AYA perlu lokasi 'Selalu izinkan' agar titik biru dan status tetap akurat\n" +
                    "walaupun aplikasi sedang tidak dibuka.\n\n" +
                    "Di layar berikutnya: pilih 'Selalu izinkan' (Allow all the time)."
                )
                .setPositiveButton("Buka Pengaturan") { _, _ ->
                    prefs.askedBackground = true
                    activity.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", activity.packageName, null)
                        }
                    )
                }
                .setNegativeButton("Nanti saja") { _, _ -> onBackgroundSettled?.invoke() }
                .setOnCancelListener { onBackgroundSettled?.invoke() }
                .show()
        }
    }

    private fun showBackgroundSettingsDialog() {
        AlertDialog.Builder(activity, R.style.Theme_AYA_Dialog)
            .setTitle("Lokasi latar belakang diblokir")
            .setMessage("Izin 'Selalu izinkan' ditolak sebelumnya. Untuk mengaktifkannya: Pengaturan → Izin → Lokasi → 'Selalu izinkan'.")
            .setPositiveButton("Buka Pengaturan") { _, _ ->
                activity.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", activity.packageName, null)
                    }
                )
            }
            .setNegativeButton("Nanti saja") { _, _ -> onBackgroundSettled?.invoke() }
            .setOnCancelListener { onBackgroundSettled?.invoke() }
            .show()
    }

    // ====== 3) BATTERY OPTIMIZATION ======
    fun isBatteryUnrestricted(): Boolean {
        val pm = activity.getSystemService(android.content.Context.POWER_SERVICE)
                as android.os.PowerManager
        return pm.isIgnoringBatteryOptimizations(activity.packageName)
    }

    /** Dialog sistem — BUKAN runtime permission biasa. */
    fun requestBatteryExemption() {
        if (isBatteryUnrestricted()) {
            onBatterySettled?.invoke()
            return
        }
        val i = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${activity.packageName}")
        }
        try {
            activity.startActivity(i)
        } catch (t: Throwable) {
            // Perangkat tanpa aktivitas ini (jarang) — lewati tahap
            onBatterySettled?.invoke()
        }
    }

    fun openSettings() {
        activity.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", activity.packageName, null)
            }
        )
    }
}
