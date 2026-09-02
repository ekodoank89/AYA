package com.aya.doank.xposed

/**
 * Peta bawaan: package aplikasi target → id target di manager.
 *
 * ⚠️ WAJIB DISAMAKAN dengan aplikasi yang benar-benar terinstall di HP Anda!
 * Cara mengecek nama package:
 *   - LSPosed → Modul → AYA → cakupan/scope → nama package tampil di bawah nama aplikasi
 *   - atau via terminal root: pm list packages | grep -i grab
 *
 * Override dinamis: jika prefs berisi key "pkg_<id>" (fitur Tahap 3),
 * nilai tersebut yang menang atas peta bawaan ini.
 */
object TargetMap {
    private val BUILTIN = mapOf(
        "com.grabtaxi.passenger" to "grab",   // Grab - penumpang
        "com.grabtaxi.driver2" to "grab",          // Grab - driver
        "com.gojek.app" to "gojek",           // Gojek - customer
        "com.gojek.partner" to "gojek"         // Gojek - driver (verifikasi!)
    )

    fun lookup(pkg: String): String? = BUILTIN[pkg]
}
