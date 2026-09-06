package com.aya.doank.xposed

import android.os.Handler
import android.os.Looper

/**
 * Bridge antara hook (proses target) dan Manager UI (proses AYA).
 * v2.9.2: callbacks marker — hook memanggil ini saat state spoofing berubah,
 * Manager menerima dan menampilkan/menghilangkan marker di map-nya.
 *
 * Semua callback dieksekusi di main thread manager via Handler.
 */
object SpoofConfigBridge {
    private val mainHandler = Handler(Looper.getMainLooper())

    var onMarkerState: ((targetId: String, lat: Double, lng: Double) -> Unit)? = null
    var onMarkerRemoved: ((targetId: String) -> Unit)? = null
    var onRelock: ((targetId: String, lat: Double, lng: Double) -> Unit)? = null

    fun postMarkerState(targetId: String, lat: Double, lng: Double) {
        mainHandler.post { onMarkerState?.invoke(targetId, lat, lng) }
    }

    fun postMarkerRemoved(targetId: String) {
        mainHandler.post { onMarkerRemoved?.invoke(targetId) }
    }

    fun postRelock(targetId: String, lat: Double, lng: Double) {
        mainHandler.post { onRelock?.invoke(targetId, lat, lng) }
    }
}
