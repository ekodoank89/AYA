package com.aya.doank

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.aya.doank.core.ConfigPusher
import com.aya.doank.core.FavoritesStore
import com.aya.doank.core.Prefs
import com.aya.doank.core.SpoofTarget
import com.aya.doank.core.Targets
import com.aya.doank.ui.ChipTelemetry
import com.aya.doank.ui.FavoritesController
import com.aya.doank.ui.JitterController
import com.aya.doank.ui.MapController
import com.aya.doank.ui.NotifController
import com.aya.doank.ui.NotifPermissionFlow
import com.aya.doank.ui.PermissionFlow
import com.aya.doank.ui.PlayPanelController
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
    private lateinit var chipTelemetry: ChipTelemetry

    // Launcher izin notifikasi — WAJIB field (terdaftar sebelum onStart).
    private val notifPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        notifPerm.markAsked()
        notifPerm.notifDone?.let { it(); notifPerm.notifDone = null }
    }

    // Handler chip telemetri (tick 1 dtk)
    private val chipHandler = Handler(Looper.getMainLooper())
    private val chipTick = object : Runnable {
        override fun run() {
            if (::chipTelemetry.isInitialized) chipTelemetry.onTick()
            chipHandler.postDelayed(this, 1000L)
        }
    }

    // ===== RANTAI IZIN + DOUBLE CROSS-CHECK (v2.4.2) =====
    private var lastStage = ""
    private val chainHandler = Handler(Looper.getMainLooper())
    private var batteryOnceThisSession = false

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

        permissionFlow.onSettled = { nextChainStep() }

        // v2.6.4: PlayPanel mem-push sendiri saat toggle
        // (lock → push → buka app target + push ulang terjadwal)
        playPanel = PlayPanelController(
            this, prefs,
            pusher = pusher,
            centerProvider = { map.currentCenter() }
        ) { target, active ->
            if (active) {
                if (!notifs.canNotify()) {
                    notifPerm.ensureBeforePlay { }
                }
            }
            refreshNotif()
            announce(target, active)
        }

        favorites.bind(R.id.btn_fav)

        // ==== JITTER ====
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

        // ==== CHIP TELEMETRI ====
        chipTelemetry = ChipTelemetry(this, prefs)
        chipTelemetry.bind()

        playPanel.bind()
        map.attach(supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment)

        if (permissionFlow.hasPermission()) {
            map.ensureBlueDot()
        } else {
            nextChainStep()
        }

        chipHandler.post(chipTick)
    }

    /**
     * Satu pintu update notifikasi indikator:
     * kumpulkan semua target AKTIF + titik lock-nya → NotifController.update().
     * Dipanggil dari toggle, stop (panel), dan onResume.
     */
    private fun refreshNotif() {
        val activeList = Targets.all.mapNotNull { t ->
            if (prefs.isSpoofActive(t.id)) {
                prefs.spoofPoint(t.id)?.let { t to it }
            } else null
        }
        notifs.update(activeList)
    }

    override fun onResume() {
        super.onResume()
        if (permissionFlow.hasPermission()) map.ensureBlueDot()
        chipHandler.post(chipTick)

        // Kembali dari Settings → selesaikan tahap tertunda → rantai evaluasi ulang
        permissionFlow.resumePendingBackground { nextChainStep() }

        // Notifikasi indikator sinkron dengan state tersimpan
        refreshNotif()
    }

    override fun onPause() {
        super.onPause()
        chipHandler.removeCallbacks(chipTick)
    }

    override fun onDestroy() {
        super.onDestroy()
        chainHandler.removeCallbacksAndMessages(null)
        chipHandler.removeCallbacks(chipTick)
        if (::map.isInitialized) map.stop()
    }

    /**
     * Mesin status rantai v2.4.2 + DOUBLE CROSS-CHECK:
     * 1) Lokasi dasar   — ulang hingga granted
     * 2) Selalu izinkan — ulang hingga granted (kembali dari Settings dicek onResume)
     * 3) Notifikasi     — ulang hingga granted
     * 4) Baterai        — dialog sistem SEKALI per sesi (tidak ditagih ulang)
     */
    private fun nextChainStep() {
        when {
            !permissionFlow.hasPermission() ->
                beginStage("Lokasi") { permissionFlow.requestOrGuide() }

            !permissionFlow.hasBackgroundLocation() ->
                beginStage("Selalu izinkan") {
                    permissionFlow.requestBackgroundLocation { nextChainStep() }
                }

            !notifPerm.isGranted() ->
                beginStage("Notifikasi") {
                    notifPerm.requestInChain { nextChainStep() }
                }

            !permissionFlow.isBatteryUnrestricted() -> {
                if (!batteryOnceThisSession) {
                    batteryOnceThisSession = true
                    permissionFlow.requestBatteryExemption { }
                }
            }
        }
    }

    private fun beginStage(name: String, request: () -> Unit) {
        if (name == lastStage) {
            Toast.makeText(
                this,
                "Izin \"$name\" belum aktif — mengulangi permintaan",
                Toast.LENGTH_SHORT
            ).show()
            chainHandler.postDelayed({ request() }, 700)
        } else {
            lastStage = name
            request()
        }
    }

    private fun announce(target: SpoofTarget, active: Boolean) {
        val msg = if (active) "${target.label} AKTIF — lock ${map.centerText()} — membuka aplikasi…"
                  else "${target.label} dihentikan — notif hilang"
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun updateThemeIcon() {
        findViewById<ImageButton>(R.id.btn_theme)
            .setImageResource(if (prefs.isDark) R.drawable.ic_sun else R.drawable.ic_moon)
    }
}
