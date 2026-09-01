package com.aya.doank.ui

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.aya.doank.R
import com.aya.doank.core.Prefs
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.Locale

class MapController(private val context: Context, private val prefs: Prefs) {

    private var map: GoogleMap? = null
    private lateinit var fused: FusedLocationProviderClient
    var onCenterChanged: ((Double, Double) -> Unit)? = null

    fun attach(fragment: SupportMapFragment) {
        fused = LocationServices.getFusedLocationProviderClient(context)
        fragment.getMapAsync(::onReady)
    }

    private fun onReady(g: GoogleMap) {
        map = g
        g.uiSettings.isZoomControlsEnabled = false
        g.uiSettings.isMyLocationButtonEnabled = false
        g.setOnCameraIdleListener {
            val t = g.cameraPosition?.target ?: return@setOnCameraIdleListener
            onCenterChanged?.invoke(t.latitude, t.longitude)
        }
        applyStyle(prefs.isDark)
        g.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(-6.2088, 106.8456), 12f))
    }

    val isReady: Boolean get() = map != null

    fun centerText(): String {
        val t = map?.cameraPosition?.target ?: return "…"
        return String.format(Locale.US, "%.6f, %.6f", t.latitude, t.longitude)
    }

    fun currentCenter(): LatLng? = map?.cameraPosition?.target

    fun zoomMax() { map?.let { m -> m.animateCamera(CameraUpdateFactory.zoomTo(m.maxZoomLevel)) } }
    fun zoomOut() { map?.animateCamera(CameraUpdateFactory.zoomOut()) }

    fun applyStyle(dark: Boolean) {
        val m = map ?: return
        try {
            val ok = if (dark) m.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark))
                     else m.setMapStyle(null)
            if (!ok) Log.w("AYA", "Gagal menerapkan gaya peta")
        } catch (e: Exception) {
            Log.w("AYA", "Gaya peta gagal: ${e.message}")
        }
    }

    /** Titik biru + lompat ke posisi terakhir. Aman dipanggil berulang (idempotent). */
    @SuppressLint("MissingPermission")
    fun ensureBlueDot() {
        val m = map ?: return
        if (m.isMyLocationEnabled) return
        m.isMyLocationEnabled = true
        fused.lastLocation.addOnSuccessListener { loc ->
            loc?.let {
                m.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 16f))
            }
        }
    }

    /** Fix GPS segar saat tombol fokus ditekan, dengan fallback lastLocation. */
    fun focusFresh() {
        val m = map ?: return
        Toast.makeText(context, "Mencari posisi Anda…", Toast.LENGTH_SHORT).show()
        fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    m.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 17f))
                } else {
                    fused.lastLocation.addOnSuccessListener { last ->
                        if (last != null) {
                            m.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(last.latitude, last.longitude), 17f))
                        } else {
                            Toast.makeText(context, "Lokasi belum tersedia — pastikan GPS aktif", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Gagal mengambil lokasi", Toast.LENGTH_SHORT).show()
            }
    }
}
