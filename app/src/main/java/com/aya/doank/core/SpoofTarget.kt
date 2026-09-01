package com.aya.doank.core

data class SpoofTarget(
    val id: String,           // "grab" / "gojek" — kunci di storage
    val label: String,        // untuk teks/toast
    val packageName: String?  // diisi Tahap 2, mis. "com.grab.driver"
)

object Targets {
    val GRAB = SpoofTarget("grab", "GRAB", null)
    val GOJEK = SpoofTarget("gojek", "GOJEK", null)
    val all = listOf(GRAB, GOJEK)
    fun byId(id: String): SpoofTarget = all.first { it.id == id }
}
