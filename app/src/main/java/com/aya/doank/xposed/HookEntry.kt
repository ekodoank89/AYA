package com.aya.doank.xposed

import com.aya.doank.core.Keys
import com.aya.doank.core.Targets
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * Entry point modul — di-load LSPosed DI DALAM proses setiap app yang masuk scope.
 * Per proses target dibuat satu instance HookEntry sendiri.
 */
class HookEntry : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // Jangan pernah hook proses manager sendiri
        if (lpparam.packageName == MODULE_PACKAGE) return

        // App ini target siapa? (override prefs dulu, baru peta bawaan)
        val targetId = resolveTargetId(lpparam.packageName) ?: return

        XposedBridge.log("AYA: hook terpasang di '${lpparam.packageName}' (target: $targetId)")

        val config = SpoofConfig(targetId)

        // Intercept pembacaan koordinat — pola yang dipakai modul fake-GPS umum:
        // semua Location di proses ini (LocationManager maupun GMS FusedLocation)
        // pada akhirnya dibaca lewat getter-geter ini.
        val latitudeHook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                config.latitude()?.let { param.result = it }
            }
        }
        val longitudeHook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                config.longitude()?.let { param.result = it }
            }
        }

        XposedHelpers.findAndHookMethod(
            "android.location.Location", lpparam.classLoader,
            "getLatitude", latitudeHook
        )
        XposedHelpers.findAndHookMethod(
            "android.location.Location", lpparam.classLoader,
            "getLongitude", longitudeHook
        )
    }

    private fun resolveTargetId(pkg: String): String? {
        val sp = try {
            XSharedPreferences(MODULE_PACKAGE, Keys.PREFS_NAME)
        } catch (t: Throwable) {
            XposedBridge.log("AYA: XSharedPreferences gagal dibuka: $t")
            return TargetMap.lookup(pkg)
        }
        return try {
            sp.reload()
            for (t in Targets.all) {
                if (sp.getString(Keys.targetPkgKey(t.id), null) == pkg) return t.id
            }
            TargetMap.lookup(pkg)
        } catch (t: Throwable) {
            TargetMap.lookup(pkg)
        }
    }

    companion object {
        private const val MODULE_PACKAGE = "com.aya.doank"
    }
}
