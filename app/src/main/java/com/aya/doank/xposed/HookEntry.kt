package com.aya.doank.xposed

import android.location.Location
import com.aya.doank.core.Keys
import com.aya.doank.core.Targets
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.util.concurrent.atomic.AtomicBoolean

class HookEntry : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName == MODULE_PACKAGE) return

        val targetId = resolveTargetId(lpparam.packageName) ?: return
        XposedBridge.log("AYA: hook terpasang di '${lpparam.packageName}' (target: $targetId)")

        val config = SpoofConfig(targetId)

        hookLocationGetters(lpparam.classLoader, config)
        hookLastKnownLocation(lpparam.classLoader, config)
        hookGmsLocationResult(lpparam.classLoader, config)
    }

    // ===== 1) Getter: membaca koordinat dari objek Location mana pun =====
    private fun hookLocationGetters(classLoader: ClassLoader, config: SpoofConfig) {
        val loggedFirst = AtomicBoolean(false)
        try {
            XposedHelpers.findAndHookMethod("android.location.Location", classLoader, "getLatitude",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        config.latitude()?.let {
                            logFirst(loggedFirst, "getLatitude")
                            param.result = it
                        }
                    }
                })
            XposedHelpers.findAndHookMethod("android.location.Location", classLoader, "getLongitude",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        config.longitude()?.let {
                            logFirst(loggedFirst, "getLongitude")
                            param.result = it
                        }
                    }
                })
        } catch (t: Throwable) {
            XposedBridge.log("AYA: getter Location gagal di-hook: $t")
        }
    }

    // ===== 2) getLastKnownLocation: tulis fake ke FIELD objek (konsistensi penuh) =====
    private fun hookLastKnownLocation(classLoader: ClassLoader, config: SpoofConfig) {
        try {
            XposedHelpers.findAndHookMethod(
                "android.location.LocationManager", classLoader,
                "getLastKnownLocation", String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        (param.result as? Location)?.let { rewriteFields(it, config, "getLastKnownLocation") }
                    }
                })
        } catch (t: Throwable) {
            XposedBridge.log("AYA: getLastKnownLocation gagal di-hook: $t")
        }
    }

    // ===== 3) GMS LocationResult: jalur FusedLocation milik app modern =====
    private fun hookGmsLocationResult(classLoader: ClassLoader, config: SpoofConfig) {
        try {
            val cls = XposedHelpers.findClassIfExists(GMS_LOCATION_RESULT, classLoader)
            if (cls == null) {
                XposedBridge.log("AYA: GMS LocationResult tidak ditemukan (app tidak pakai GMS location?)")
                return
            }
            XposedBridge.hookAllMethods(cls, "getLastLocation", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    (param.result as? Location)?.let { rewriteFields(it, config, "GMS.getLastLocation") }
                }
            })
            XposedBridge.hookAllMethods(cls, "getLocations", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    @Suppress("UNCHECKED_CAST")
                    (param.result as? List<Location>)?.forEach { rewriteFields(it, config, "GMS.getLocations") }
                }
            })
            XposedBridge.log("AYA: GMS LocationResult di-hook")
        } catch (t: Throwable) {
            XposedBridge.log("AYA: GMS LocationResult gagal: $t")
        }
    }

    private val loggedRewrite = AtomicBoolean(false)

    /** Tulis fake langsung ke field — getter, distanceTo, toString ikut konsisten. */
    private fun rewriteFields(loc: Location, config: SpoofConfig, source: String) {
        val lat = config.latitude() ?: return
        val lng = config.longitude() ?: return
        try {
            loc.latitude = lat
            loc.longitude = lng
            if (loggedRewrite.compareAndSet(false, true)) {
                XposedBridge.log("AYA: field Location ditulis fake via $source")
            }
        } catch (t: Throwable) {
            XposedBridge.log("AYA: gagal menulis field: $t")
        }
    }

    private fun logFirst(flag: AtomicBoolean, what: String) {
        if (flag.compareAndSet(false, true)) {
            XposedBridge.log("AYA: $what terpanggil di app target — mengirim koordinat fake")
        }
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
        private const val GMS_LOCATION_RESULT = "com.google.android.gms.location.LocationResult"
    }
}
