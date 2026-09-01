package com.aya.doank.core

object Keys {
    const val PREFS_NAME = "aya_prefs"
    const val IS_DARK = "is_dark"
    const val ASKED_LOCATION = "asked_location"

    // ==== Schema spoof — DIBACA HOOK di proses target (Tahap 2). Jangan diubah sembarangan! ====
    const val SPOOF_LAT = "spoof_lat"
    const val SPOOF_LNG = "spoof_lng"
    private const val KEY_ACTIVE_TEMPLATE = "spoof_%s_active"
    fun spoofActive(id: String) = KEY_ACTIVE_TEMPLATE.format(id)
}
