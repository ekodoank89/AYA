package com.aya.doank.ui

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.aya.doank.R
import com.aya.doank.core.Prefs
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
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
    private var updatesStarted = false
    private var firstFixJumpDone = false
    private var pendingEnsure = false

    private var _blueDot: LatLng? = null
    val blueDot: LatLng? get() = _blueDot

    var onCenterChanged: ((Double, Double) -> Unit)? = null
    var onBlueDotChanged: ((Double, Double) -> Unit)? = null

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc = result.lastLocation ?: return
            _blueDot = LatLng(loc.latitude, loc.longitude)
            onBlueDotChanged?.invoke(loc.latitude, loc.longitude)
            if (!firstFixJumpDone) {
                firstFixJumpDone = true
                _blueDot?.let { map?.animateCamera(CameraUpdateFactory.newLatLngZoom(it, 16f)) }
            }
        }
    }

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
        if (pendingEnsure) ensureBlueDot()
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

    /** Titik biru + pembaruan lokasi real-time. Aman dipanggil berulang; juga aman saat peta belum siap. */
    @SuppressLint("MissingPermission")
    fun ensureBlueDot() {
        val m = map
        if (m == null) { pendingEnsure = true; return }
        if (!m.isMyLocationEnabled) m.isMyLocationEnabled = true
        if (!updatesStarted) {
            updatesStarted = true
            val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
                .setMinUpdateIntervalMillis(2000L)
                .build()
            fused.requestLocationUpdates(req, locationCallback, Looper.getMainLooper())
        }
    }

    fun stop() {
        if (updatesStarted) {
            fused.removeLocationUpdates(locationCallback)
            updatesStarted = false
        }
    }

    fun flyTo(target: LatLng) {
        map?.animateCamera(CameraUpdateFactory.newLatLngZoom(target, 17f))
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
