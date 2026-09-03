package com.aya.doank.ui

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import com.aya.doank.R

/**
 * "Always on" / auto-start TIDAK punya API Android umum — ini setting pabrikan.
 * Kita arahkan ke halaman auto-start milik vendor (best-effort, bisa gagal di ROM lain).
 */
object VendorAutostartGuide {

    private val MANUFACTURERS = listOf("xiaomi", "redmi", "poco", "oppo", "vivo",
        "realme", "oneplus", "huawei", "honor", "samsung", "asus", "lenovo", "tecno", "infinix")

    fun isKnownVendor(): Boolean =
        MANUFACTURERS.any { android.os.Build.MANUFACTURER.lowercase().contains(it) }

    fun show(activity: Activity) {
        val intent = autostartIntent()
        AlertDialog.Builder(activity, R.style.Theme_AYA_Dialog)
            .setTitle("Aktifkan 'Auto-start' / 'Always on'")
            .setMessage(
                "Pabrikan HP (" + android.os.Build.MANUFACTURER + ") kadang mematikan aplikasi di latar belakang.\n\n" +
                "Di layar berikutnya, aktifkan sakelar 'Auto-start' / 'Allow autostart' untuk AYA " +
                "agar tetap berjalan penuh di latar belakang & latar depan."
            )
            .setPositiveButton("Buka Pengaturan") { _, _ ->
                try {
                    activity.startActivity(intent)
                } catch (t: Throwable) {
                    // Fallback: halaman app info
                    activity.startActivity(
                        Intent(Settings_ACTION_APP_DETAILS).apply {
                            data = UriParts(activity.packageName)
                        }
                    )
                }
            }
            .setNegativeButton("Nanti saja", null)
            .show()
    }

    private fun autostartIntent(): Intent {
        val m = android.os.Build.MANUFACTURER.lowercase()
        val candidates: List<ComponentName> = when {
            m.contains("xiaomi") || m.contains("redmi") || m.contains("poco") -> listOf(
                ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"))
            m.contains("oppo") || m.contains("realme") -> listOf(
                ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
                ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"))
            m.contains("vivo") -> listOf(
                ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"))
            m.contains("oneplus") -> listOf(
                ComponentName("com.oneplus.security", "com.oneplus.security.chainlaunch.ChainLaunchAppListActivity"))
            m.contains("huawei") || m.contains("honor") -> listOf(
                ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"))
            m.contains("samsung") -> listOf(
                ComponentName("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity"))
            m.contains("asus") -> listOf(
                ComponentName("com.asus.mobilemanager", "com.asus.mobilemanager.powersaver.PowerSaverSettings"))
            else -> emptyList()
        }
        // Coba satu per satu sampai ada yang terpasang
        val out = Intent()
        for (c in candidates) {
            out.setComponent(c)
            if (out.resolveActivity(activity.packageManager) != null) return out
        }
        // Fallback: app info
        return Intent(Settings_ACTION_APP_DETAILS).apply { data = UriParts(activity.packageName) }
    }

    private const val Settings_ACTION_APP_DETAILS = "android.settings.APPLICATION_DETAILS_SETTINGS"

    private fun UriParts(pkg: String): Uri =
        Uri.parse("package:$pkg")

    private fun Intent(data: String): Intent = Intent(data)
    private fun Intent(pkgUri: Uri): Intent = Intent().apply { this.data = pkgUri }
}
