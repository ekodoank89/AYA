package com.aya.doank

import android.Manifest
import android.content.Context
import android.content.IntentFilter
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

    // Launcher izin notifikasi — WAJIB field (terdaftar sebelum onStart).
    private val notifPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        notifPerm.markAsked()
        notifPerm.notifDone?.let { it(); notifPerm.notifDone = null }
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

        // v2.6.3: playPanel yang push (push dulu SEBELUM app target dibuka) —
        // MainActivity tidak meng-push ganda di sini.
        playPanel = PlayPanelController(
            this, prefs,
            centerProvider = { map.currentCenter() }
        ) { target, active ->
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

        // Tombol ■ di notifikasi → broadcast → stop target terkait
        NotifController.onNotifStop = { targetId ->
            runOnUiThread { stopFromNotif(targetId) }
        }
        registerReceiver(
            NotifController.StopReceiver(),
            IntentFilter(NotifController.ACTION_STOP),
            if (Build.VERSION.SDK_INT >= 33) Context.RECEIVER_NOT_EXPORTED else 0
        )

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

        playPanel.bind()
        map.attach(supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment)

        if (permissionFlow.hasPermission()) {
            map.ensureBlueDot()
        } else {
            nextChainStep()
        }
    }

    /**
     * Mesin status rantai v2.4.2 + DOUBLE CROSS-CHECK:
     * 1) Lokasi dasar   — ulang hingga granted
     * 2) Selalu izinkan — ulang hingga granted (kembali dari Settings dicek onResume)
     * 3) Notifikasi     — ulang hingga granted
     * 4) Baterai        — dialog sistem SEKALI (batteryOnceThisSession, tidak ditagih)
     * FIX: memakai requestBatteryExemption (API PermissionFlow yang ada),
     * bukan tryBatteryExemptionSilent dari batch lama yang tidak pernah ada di v2.4.2.
     */
    private fun nextChainStep() {
        when {
            // 1) LOKASI DASAR
            !permissionFlow.hasPermission() ->
                beginStage("Lokasi") { permissionFlow.requestOrGuide() }

            // 2) SELALU IZINKAN (background)
            !permissionFlow.hasBackgroundLocation() ->
                beginStage("Selalu izinkan") {
                    permissionFlow.requestBackgroundLocation { nextChainStep() }
                }

            // 3) NOTIFIKASI
            !notifPerm.isGranted() ->
                beginStage("Notifikasi") {
                    notifPerm.requestInChain { nextChainStep() }
                }

            // 4) BATERAI — dialog sistem sekali per sesi, tidak ditagih ulang
            !permissionFlow.isBatteryUnrestricted() -> {
                if (!batteryOnceThisSession) {
                    batteryOnceThisSession = true
                    permissionFlow.requestBatteryExemption { /* selesai — tidak diulang */ }
                }
            }
        }
    }

    /**
     * Double cross-check: tahap yang SAMA diminta ulang = belum granted
     * → toast penjelasan + jeda 0,7 dtk sebelum dialog muncul lagi.
     */
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

    override fun onResume() {
        super.onResume()
        if (permissionFlow.hasPermission()) map.ensureBlueDot()

        // Kembali dari Settings ("Selalu izinkan" / lokasi diblokir)
        // → selesaikan tahap tertunda → rantai mengevaluasi ulang (double check)
        permissionFlow.resumePendingBackground { nextChainStep() }

        // Sinkronkan notifikasi dengan state tersimpan
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
        chainHandler.removeCallbacksAndMessages(null)
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
        val msg = if (active) "${target.label} AKTIF — lock ${map.centerText()} — membuka aplikasi…"
                  else "${target.label} dihentikan — notif hilang"
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun updateThemeIcon() {
        findViewById<ImageButton>(R.id.btn_theme)
            .setImageResource(if (prefs.isDark) R.drawable.ic_sun else R.drawable.ic_moon)
    }
}
