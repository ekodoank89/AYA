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
import com.aya.doank.R
import com.aya.doank.core.Prefs

/**
 * Izin lokasi dasar + background + battery. v2.4.1: semua dialog diberi
 * background kartu solid (bg_dialog_card) — tidak lagi transparan.
 */
class PermissionFlow(
    private val activity: AppCompatActivity,
    private val prefs: Prefs,
    private val onGranted: () -> Unit
) {
    /** Alur izin lokasi dasar selesai (granted/ditolak/batal-settings). */
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
        if (granted) {
            onGranted()
            Toast.makeText(activity, "Izin lokasi diberikan", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(activity, "Izin lokasi ditolak — peta tetap bisa digunakan", Toast.LENGTH_LONG).show()
        }
        onSettled?.invoke()
    }

    // ====== Kartu solid untuk semua dialog builder (anti-transparan) ======
    private fun buildCardDialog(): AlertDialog {
        val d = AlertDialog.Builder(activity, R.style.Theme_AYA_Dialog).create()
        d.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_card)
        return d
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
        val d = buildCardDialog()
        d.setTitle("Izin lokasi diblokir")
        d.setMessage("Izin lokasi telah ditolak berulang kali. Untuk fitur titik biru, nyalakan izin Lokasi di Pengaturan aplikasi.")
        d.setPositiveButton("Buka Pengaturan") { _, _ ->
            activity.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", activity.packageName, null)
                }
            )
        }
        d.setNegativeButton("Batal") { _, _ -> onSettled?.invoke() }
        d.setOnCancelListener { onSettled?.invoke() }
        d.show()
    }

    // ====== 2) LOKASI BACKGROUND ("Selalu izinkan") ======
    fun hasBackgroundLocation(): Boolean =
        Build.VERSION.SDK_INT < 29 ||
                ContextCompat.checkSelfPermission(
                    activity, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

    /** Tahap background — onDone dipanggil saat alur selesai (apapun hasilnya). */
    fun requestBackgroundLocation(onDone: () -> Unit) {
        if (hasBackgroundLocation()) { onDone(); return }
        if (!hasPermission()) { onDone(); return }

        val blocked = Build.VERSION.SDK_INT >= 29 &&
                !activity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION) &&
                prefs.askedBackground

        if (blocked) {
            showBackgroundBlockedDialog(onDone)
        } else {
            val d = buildCardDialog()
            d.setTitle("Izinkan lokasi di latar belakang?")
            d.setMessage(
                "AYA perlu lokasi 'Selalu izinkan' agar titik biru dan status tetap akurat " +
                "walaupun aplikasi sedang tidak dibuka.\n\n" +
                "Di layar berikutnya pilih 'Selalu izinkan' (Allow all the time)."
            )
            d.setPositiveButton("Buka Pengaturan") { _, _ ->
                prefs.askedBackground = true
                activity.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", activity.packageName, null)
                    }
                )
                pendingBackgroundDone = onDone
            }
            d.setNegativeButton("Nanti saja") { _, _ -> onDone() }
            d.setOnCancelListener { onDone() }
            d.show()
        }
    }

    /** Callback tertunda untuk jalur Settings (dipanggil MainActivity.onResume). */
    var pendingBackgroundDone: (() -> Unit)? = null

    private fun showBackgroundBlockedDialog(onDone: () -> Unit) {
        val d = buildCardDialog()
        d.setTitle("Lokasi latar belakang diblokir")
        d.setMessage("Izin 'Selalu izinkan' ditolak sebelumnya. Untuk mengaktifkannya: Pengaturan → Izin → Lokasi → 'Selalu izinkan'.")
        d.setPositiveButton("Buka Pengaturan") { _, _ ->
            activity.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", activity.packageName, null)
                }
            )
            pendingBackgroundDone = onDone
        }
        d.setNegativeButton("Nanti saja") { _, _ -> onDone() }
        d.setOnCancelListener { onDone() }
        d.show()
    }

    /** Dipanggil MainActivity.onResume — selesaikan tahap background jika tertunda. */
    fun resumePendingBackground(onDone: () -> Unit) {
        pendingBackgroundDone?.let {
            pendingBackgroundDone = null
            if (hasBackgroundLocation()) {
                Toast.makeText(activity, "Lokasi latar belakang aktif", Toast.LENGTH_SHORT).show()
            }
            it()   // lanjut rantai apapun hasilnya
        }
    }

    // ====== 3) BATTERY (callback via ActivityResult) ======
    fun isBatteryUnrestricted(): Boolean {
        val pm = activity.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
        return pm.isIgnoringBatteryOptimizations(activity.packageName)
    }

    private val batteryLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        batteryDone?.invoke()
        batteryDone = null
    }

    private var batteryDone: (() -> Unit)? = null

    fun requestBatteryExemption(onDone: () -> Unit) {
        if (isBatteryUnrestricted()) { onDone(); return }
        batteryDone = onDone
        val i = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${activity.packageName}")
        }
        try {
            batteryLauncher.launch(i)
        } catch (t: Throwable) {
            batteryDone = null
            onDone()
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
