package com.aya.doank

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.Locale

class MainActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnCameraIdleListener {

    private lateinit var map: GoogleMap
    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var tvCenter: TextView
    private lateinit var btnTheme: ImageButton

    private val prefs by lazy { getSharedPreferences("aya_prefs", MODE_PRIVATE) }
    private var isDark = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            if (::map.isInitialized) enableMyLocation()
        } else {
            Toast.makeText(this, "Izin lokasi ditolak — peta tetap bisa digunakan", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Muat preferensi tema yang tersimpan
        isDark = prefs.getBoolean("is_dark", false)

        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        tvCenter = findViewById(R.id.tv_center)
        btnTheme = findViewById(R.id.btn_theme)

        // Zoom IN: langsung lompat ke zoom MAKSIMAL
        findViewById<Button>(R.id.btn_zoom_in).setOnClickListener {
            if (::map.isInitialized) {
                map.animateCamera(CameraUpdateFactory.zoomTo(map.maxZoomLevel))
            }
        }

        // Zoom OUT: satu tingkat (bisa diubah ke minZoomLevel jika ingin simetris)
        findViewById<Button>(R.id.btn_zoom_out).setOnClickListener {
            if (::map.isInitialized) map.animateCamera(CameraUpdateFactory.zoomOut())
        }

        // Fokus ke titik biru (lokasi saya)
        findViewById<ImageButton>(R.id.btn_my_location).setOnClickListener {
            focusMyLocation()
        }

        // Toggle tema gelap/terang
        btnTheme.setOnClickListener {
            isDark = !isDark
            prefs.edit().putBoolean("is_dark", isDark).apply()
            if (::map.isInitialized) applyMapStyle()
            updateThemeIcon()
        }

        updateThemeIcon()

        (supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment)
            .getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        map.uiSettings.isZoomControlsEnabled = false
        map.uiSettings.isMyLocationButtonEnabled = false
        map.setOnCameraIdleListener(this)

        applyMapStyle()

        // Posisi awal; akan pindah ke lokasi user jika izin sudah diberikan
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(-6.2088, 106.8456), 12f))

        if (hasLocationPermission()) {
            enableMyLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }

    override fun onCameraIdle() {
        val target = map.cameraPosition?.target ?: return
        tvCenter.text = String.format(Locale.US, "%.6f, %.6f", target.latitude, target.longitude)
    }

    @SuppressLint("MissingPermission")
    private fun focusMyLocation() {
        if (!hasLocationPermission()) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
            return
        }
        if (!::map.isInitialized) return

        Toast.makeText(this, "Mencari posisi Anda…", Toast.LENGTH_SHORT).show()

        // getCurrentLocation = ambil posisi FRESH dari GPS (lebih akurat dari lastLocation)
        fusedClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            CancellationTokenSource().token
        ).addOnSuccessListener { loc ->
            if (loc != null) {
                map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 17f)
                )
            } else {
                // Fallback: posisi terakhir yang diketahui
                fusedClient.lastLocation.addOnSuccessListener { last ->
                    if (last != null) {
                        map.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(LatLng(last.latitude, last.longitude), 17f)
                        )
                    } else {
                        Toast.makeText(this, "Lokasi belum tersedia — pastikan GPS aktif", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Gagal mengambil lokasi", Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyMapStyle() {
        try {
            val ok = if (isDark) {
                map.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style_dark))
            } else {
                map.setMapStyle(null) // kembali ke gaya bawaan (terang)
            }
            if (!ok) Log.w("AYA", "Gagal menerapkan gaya peta")
        } catch (e: Exception) {
            Log.w("AYA", "Gaya peta gagal: ${e.message}")
        }
    }

    private fun updateThemeIcon() {
        // Saat terang tampil ikon bulan (klik = ke gelap), dan sebaliknya
        btnTheme.setImageResource(if (isDark) R.drawable.ic_sun else R.drawable.ic_moon)
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        map.isMyLocationEnabled = true

        fusedClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 16f))
            }
        }
    }
}
