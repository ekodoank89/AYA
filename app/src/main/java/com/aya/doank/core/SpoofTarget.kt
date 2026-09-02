package com.aya.doank.core

data class SpoofTarget(
    val id: String,                  // kunci di prefs — SATU sumber kebenaran (dibaca manager & hook)
    val label: String,               // teks UI manager
    val packageNames: Set<String>    // SEMUA package yang menerima config target ini
)

object Targets {
    // Satu tombol = satu brand = driver + customer menerima koordinat yang sama.
    // ID TIDAK diubah (grab-driver/gojek-driver) agar state tersimpan & hook tetap cocok.
    val GRAB = SpoofTarget(
        "grab-driver", "GRAB",
        setOf("com.grabtaxi.driver2", "com.grabtaxi.passenger")
    )
    val GOJEK = SpoofTarget(
        "gojek-driver", "GOJEK",
        setOf("com.gojek.partner", "com.gojek.app")
    )

    val all = listOf(GRAB, GOJEK)
    fun byId(id: String): SpoofTarget = all.first { it.id == id }
}
