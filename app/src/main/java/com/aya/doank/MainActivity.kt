package com.aya.doank

import android.Manifest
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.aya.doank.core.ConfigPusher
import com.aya.doank.core.FavoritesStore
import com.aya.doank.core.Prefs
import com.aya.doank.core.SpoofTarget
import com.aya.doank.core.Targets
import com.aya.doank.ui.FavoritesController
import com.aya.doank.ui.JitterController
import com.aya.doank.ui.MapController
import com.aya.doank.ui.NotifController
import com.aya.doank.ui.NotifPermissionFlow
import com.aya.doank.ui.PermissionFlow
import com.aya.doank.ui.PlayPanelController
import com.aya.doank.ui.VendorAutostartGuide
import com.google.android.gms.maps.SupportMapFragment

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var map: MapController
    private lateinit var permissionFlow: PermissionFlow
    private lateinit var playPanel: PlayPanelController
    private lateinit var pusher: ConfigPusher
    private lateinit var notifs: NotifController
    private lateinit var favorites: FavoritesController
    private lateinit var notifPerm: NotifPermissionFlow
    private lateinit var jitter: JitterController

    private val notifPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        notifPerm.markAsked()
        notifPermNext()   // lanjutkan rantai setelah notifikasi dijawab
    }

    // Urutan izin: LOKASI → BACKGROUND → BATTERY → NOTIFIKASI
    private var awaitingLocationSettle = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = Prefs(this)
        map = MapController(this, prefs)
        pusher = ConfigPusher(this)
        notifs = NotifController(this)
        notifPerm = NotifPermissionFlow(this, notifPermLauncher)
        favorites = FavoritesController(
            this,
            FavoritesStore(this),
            centerProvider = { map.currentCenter() }
        ) { latLng, name ->
            map.flyTo(latLng)
            Toast.makeText(this, "Pin → $name. Tekan ▶ untuk lock.", Toast.LENGTH_SHORT).show()
        }

        permissionFlow = PermissionFlow(this, prefs) {
            map.ensureBlueDot()
            map.focusFresh()
        }

        // 1) Lokasi selesai → 2) Background location
        permissionFlow.onSettled = {
            if (awaitingLocationSettle) {
                awaitingLocationSettle = false
                if (permissionFlow.hasPermission()) {
                    permissionFlow.requestBackgroundLocation()
                } else {
                    // Lokasi ditolak → lompat ke battery (tetap jalan)
                    permissionFlow.requestBatteryExemption()
                }
            }
        }
        // 2) Background selesai → 3) Battery
        permissionFlow.onBackgroundSettled = {
            permissionFlow.requestBatteryExemption()
        }
        // 3) Battery selesai → 4) Notifikasi
        permissionFlow.onBatterySettled = {
            notifPerm.requestAfterLocation()   // nama fungsi tetap — jalannya setelah battery
        }

        playPanel = PlayPanelController(
            this, prefs,
            centerProvider = { map.currentCenter() }
        ) { target, active ->
            pusher.push(target)
            if (active) {
                val p = prefs.spoofPoint(target.id)
                if (p != null) notifs.show(target, p.first, p.second)
                if (!notifs.canNotify()) {
                    notifPerm.ensureBeforePlay { }
                }
            } else {
                notifs.hide(target)
            }
            announce(target, active)
        }

        NotifController.onNotifStop = { targetId ->
            runOnUiThread { stopFromNotif(targetId) }
        }
        registerReceiver(
            NotifController.StopReceiver(),
            IntentFilter(NotifController.ACTION_STOP),
            if (Build.VERSION.SDK_INT >= 33) Context.RECEIVER_NOT_EXPORTED else 0
        )

        favorites.bind(R.id.btn_fav)
        jitter = JitterController(this, prefs, pusher)
        jitter.bind(R.id.btn_jitter)

        findViewById<Button>(R.id.btn_zoom_in).setOnClickListener { map.zoomMax() }
        findViewById<Button>(R.id.btn_zoom_out).setOnClickListener { map.zoomOut() }
        findViewById<ImageButton>(R.id.btn_my_location).setOnClickListener {
            permissionFlow.requestOrGuide()
        }
        findViewById<ImageButton>(R.id.btn_theme).setOnClickListener {
            prefs.isDark = !prefs.isDark
            map.applyStyle(prefs.isDark)
            updateThemeIcon()
        }
        updateThemeIcon()

        playPanel.bind()
        map.attach(supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment)

        if (permissionFlow.hasPermission()) {
            map.ensureBlueDot()
            startChainFromBackground()   // lokasi sudah granted → mulai dari tahap 2
        } else {
            awaitingLocationSettle = true
            permissionFlow.requestOrGuide()
        }
    }

    private fun startChainFromBackground() {
        if (permissionFlow.hasBackgroundLocation()) {
            if (permissionFlow.isBatteryUnrestricted()) {
                notifPerm.requestAfterLocation()
            } else {
                permissionFlow.requestBatteryExemption()
            }
        } else {
            permissionFlow.requestBackgroundLocation()
        }
    }

    private fun notifPermNext() {
        // Setelah notifikasi: panduan auto-start (sekali, hanya jika vendor dikenal & belum pernah)
        if (prefs.jitterAskAutostart) {
            prefs.jitterAskAutostart = false
            VendorAutostartGuide.show(this)
        }
    }

    override fun onResume() {
        super.onResume()
        if (permissionFlow.hasPermission()) map.ensureBlueDot()

        // Jalur "Buka Pengaturan" — user kembali: sinkronkan rantai
        if (awaitingLocationSettle && permissionFlow.hasPermission()) {
            awaitingLocationSettle = false
            startChainFromBackground()
        }

        Targets.all.forEach { t ->
            if (prefs.isSpoofActive(t.id)) {
                prefs.spoofPoint(t.id)?.let { notifs.show(t, it.first, it.second) }
            } else {
                notifs.hide(t)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::map.isInitialized) map.stop()
    }

    private fun stopFromNotif(targetId: String) {
        val t = Targets.byId(targetId)
        prefs.setSpoofActive(t.id, false)
        pusher.push(t)
        playPanel.refresh(t.id)
        notifs.hide(t)
        Toast.makeText(this, "${t.label} dihentikan dari notifikasi", Toast.LENGTH_SHORT).show()
    }

    private fun announce(target: SpoofTarget, active: Boolean) {
        val msg = if (active) "${target.label} AKTIF — lock ${map.centerText()} — notif di status bar"
                  else "${target.label} dihentikan — notif hilang"
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun updateThemeIcon() {
        findViewById<ImageButton>(R.id.btn_theme)
            .setImageResource(if (prefs.isDark) R.drawable.ic_sun else R.drawable.ic_moon)
    }
}
