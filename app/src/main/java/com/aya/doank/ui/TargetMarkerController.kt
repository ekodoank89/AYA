package com.aya.doank.ui

import android.app.Activity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

/**
 * Marker per-target di peta AYA.
 * v2.9: saat ▶ di panel → marker muncul di koordinat lock dengan label target.
 * Saat ■ → marker hilang. Saat re-lock → marker pindah.
 */
class TargetMarkerController(private val activity: Activity) {

    private var googleMap: GoogleMap? = null
    private val markers = mutableMapOf<String, Marker>()

    fun attach(map: GoogleMap?) {
        googleMap = map
    }

    /** Tampilkan / perbarui posisi marker untuk target tertentu. */
    fun show(targetId: String, label: String, colorHue: Float, lat: Double, lng: Double) {
        val map = googleMap ?: return
        val pos = LatLng(lat, lng)
        val existing = markers[targetId]
        if (existing != null) {
            existing.position = pos
            existing.title = label
        } else {
            markers[targetId] = map.addMarker(
                MarkerOptions()
                    .position(pos)
                    .title(label)
                    .icon(BitmapDescriptorFactory.defaultMarker(colorHue))
            )
        }
    }

    /** Hapus marker target tertentu. */
    fun remove(targetId: String) {
        markers.remove(targetId)?.remove()
    }

    /** Hapus semua marker (dipanggil saat app ditutup). */
    fun clearAll() {
        markers.values.forEach { it.remove() }
        markers.clear()
    }
}
