package com.aya.doank.core

object Keys {
    const val PREFS_NAME = "aya_prefs"
    const val IS_DARK = "is_dark"
    const val ASKED_LOCATION = "asked_location"

    // ==== v2.4: rantai izin ====
    const val ASKED_BACKGROUND = "asked_background"
    const val NOTIF_CHAIN_DONE = "notif_chain_done"
    const val ASK_AUTOSTART = "ask_autostart"

    // ==== Jitter (pergerakan titik) — bisa diubah live via push ====
    const val JIT_STEP = "jit_step"
    const val JIT_WINDOW = "jit_window"

    // ==== Favorit ====
    const val FAVORITES = "favorites_json"

    // ==== Schema spoof — DIBACA HOOK di proses target. Jangan diubah sembarangan! ====
    private const val KEY_ACTIVE_TEMPLATE = "spoof_%s_active"
    private const val KEY_LAT_TEMPLATE = "spoof_%s_lat"
    private const val KEY_LNG_TEMPLATE = "spoof_%s_lng"
    private const val KEY_PKG_TEMPLATE = "pkg_%s"

    fun spoofActive(id: String) = KEY_ACTIVE_TEMPLATE.format(id)
    fun spoofLat(id: String) = KEY_LAT_TEMPLATE.format(id)
    fun spoofLng(id: String) = KEY_LNG_TEMPLATE.format(id)

    /** Nama package override per target — ditulis manager (fitur pemilih target). */
    fun targetPkgKey(id: String) = KEY_PKG_TEMPLATE.format(id)
}
