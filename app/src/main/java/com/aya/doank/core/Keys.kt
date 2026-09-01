package com.aya.doank.core

object Keys {
    const val PREFS_NAME = "aya_prefs"
    const val IS_DARK = "is_dark"
    const val ASKED_LOCATION = "asked_location"

    // ==== Schema spoof — DIBACA HOOK di proses target (Tahap 2). Jangan diubah sembarangan! ====
    // Satu titik lock PER TARGET (GRAB & GOJEK bisa lock di koordinat berbeda).
    private const val KEY_ACTIVE_TEMPLATE = "spoof_%s_active"
    private const val KEY_LAT_TEMPLATE = "spoof_%s_lat"
    private const val KEY_LNG_TEMPLATE = "spoof_%s_lng"

    fun spoofActive(id: String) = KEY_ACTIVE_TEMPLATE.format(id)
    fun spoofLat(id: String) = KEY_LAT_TEMPLATE.format(id)
    fun spoofLng(id: String) = KEY_LNG_TEMPLATE.format(id)
}
