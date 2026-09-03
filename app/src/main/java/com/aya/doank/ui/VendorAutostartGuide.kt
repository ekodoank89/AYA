package com.aya.doank.ui

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AlertDialog
import com.aya.doank.R

object VendorAutostartGuide {

    private val VENDORS = listOf("xiaomi","redmi","poco","oppo","vivo","realme",
        "oneplus","huawei","honor","samsung","asus","lenovo","tecno","infinix")

    fun isKnownVendor(): Boolean =
        VENDORS.any { Build.MANUFACTURER.lowercase().contains(it) }

    fun show(activity: Activity) {
        AlertDialog.Builder(activity, R.style.Theme_AYA_Dialog)
            .setTitle("Aktifkan 'Auto-start' / 'Always on'")
            .setMessage(
                "Pabrikan (" + Build.MANUFACTURER + ") kadang mematikan app di latar belakang.\n\n" +
                "Di layar berikutnya, aktifkan 'Auto-start' untuk AYA agar tetap berjalan penuh."
            )
            .setPositiveButton("Buka Pengaturan") { _, _ ->
                val i = buildIntent(activity)
                try { activity.startActivity(i) } catch (t: Throwable) {
                    activity.startActivity(Intent("android.settings.APPLICATION_DETAILS_SETTINGS")
                        .setData(Uri.parse("package:" + activity.packageName)))
                }
            }
            .setNegativeButton("Nanti saja", null)
            .show()
    }

    private fun buildIntent(activity: Activity): Intent {
        val m = Build.MANUFACTURER.lowercase()
        val comps = when {
            m.contains("xiaomi") || m.contains("redmi") || m.contains("poco") -> listOf(
                "com.miui.securitycenter/com.miui.permcenter.autostart.AutoStartManagementActivity")
            m.contains("oppo") || m.contains("realme") -> listOf(
                "com.coloros.safecenter/com.coloros.safecenter.permission.startup.StartupAppListActivity",
                "com.oppo.safe/com.oppo.safe.permission.startup.StartupAppListActivity")
            m.contains("vivo") -> listOf(
                "com.vivo.permissionmanager/com.vivo.permissionmanager.activity.BgStartUpManagerActivity")
            m.contains("oneplus") -> listOf(
                "com.oneplus.security/com.oneplus.security.chainlaunch.ChainLaunchAppListActivity")
            m.contains("huawei") || m.contains("honor") -> listOf(
                "com.huawei.systemmanager/com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")
            m.contains("samsung") -> listOf(
                "com.samsung.android.lool/com.samsung.android.sm.battery.ui.BatteryActivity")
            m.contains("asus") -> listOf(
                "com.asus.mobilemanager/com.asus.mobilemanager.powersaver.PowerSaverSettings")
            else -> emptyList()
        }
        for (c in comps) {
            val i = Intent().setComponent(ComponentName.unflattenFromString(c))
            if (i.resolveActivity(activity.packageManager) != null) return i
        }
        return Intent("android.settings.APPLICATION_DETAILS_SETTINGS")
            .setData(Uri.parse("package:" + activity.packageName))
    }
}
