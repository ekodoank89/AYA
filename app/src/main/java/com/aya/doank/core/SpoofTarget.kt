package com.aya.doank.core

data class SpoofTarget(
    val id: String,                  // kunci di prefs — HARUS konsisten karena dipakai kedua dunia
    val label: String,               // teks UI manager
    val packageNames: Set<String>    // package aplikasi target (bisa lebih dari satu)
)

object Targets {
    // ⚠️ Package di bawah diambil dari log LSPosed Anda — sudah terverifikasi.
    // Tambah package lain? Cukup tambahkan ke set di sini.
    val GRAB = SpoofTarget("grab-driver", "GRAB", setOf("com.grabtaxi.driver2"))
    val GOJEK = SpoofTarget("gojek-driver", "GOJEK", setOf("com.gojek.partner"))

    val all = listOf(GRAB, GOJEK)
    fun byId(id: String): SpoofTarget = all.first { it.id == id }
}
