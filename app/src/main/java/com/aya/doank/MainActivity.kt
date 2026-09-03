package com.aya.doank

import android.Manifest
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
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

    // Launcher izin notifikasi — WAJIB field (terdaftar sebelum onStart).
    private val notifPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        notifPerm.markAsked()
        notifPerm.notifDone?.let { it(); notifPerm.notifDone = null }
    }

    // ===== RANTAI IZIN + DOUBLE CROSS-CHECK =====
    // Urutan: Lokasi → Selalu izinkan → Notifikasi → Baterai → Auto-start guide.
    // Setiap tahap diverifikasi dari STATUS IZIN AKTUAL. Belum granted setelah
    // permintaan → tahap yang sama DIULANG (jeda 0,7 dtk + toast) hingga granted.
    private var lastStage = ""
    private val chainHandler = Handler(Looper.getMainLooper())

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

        // Alur izin lokasi selesai (apapun hasilnya) → evaluasi ulang rantai
        permissionFlow.onSettled = { nextChainStep() }

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

        // ==== v2.3: JITTER — baris keempat panel ====
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

        // Mulai rantai (juga menangani app yang di-clear data)
        nextChainStep()
    }

    /**
     * Mesin status rantai + DOUBLE CROSS-CHECK.
     * Urutan: Lokasi → Selalu izinkan → Notifikasi → Baterai → Auto-start guide.
     * Setiap callback tahap memanggil nextChainStep() lagi — jika tahap itu
     * masih belum granted, beginStage() mengulanginya (toast + jeda) hingga OK.
     */
    private fun nextChainStep() {
        when {
            // 1) LOKASI DASAR
            !permissionFlow.hasPermission() ->
                beginStage("Lokasi") { permissionFlow.requestOrGuide() }

            // 2) SELALU IZINKAN (background) — WAJIB OK sebelum lanjut ke notifikasi
            !permissionFlow.hasBackgroundLocation() ->
                beginStage("Selalu izinkan") {
                    permissionFlow.requestBackgroundLocation { nextChainStep() }
                }

            // 3) NOTIFIKASI — WAJIB OK sebelum lanjut ke baterai
            !notifPerm.isGranted() ->
                beginStage("Notifikasi") {
                    notifPerm.requestInChain { nextChainStep() }
                }

            // 4) BATERAI — WAJIB OK sebelum selesai
            !permissionFlow.isBatteryUnrestricted() ->
                beginStage("Baterai") {
                    permissionFlow.requestBatteryExemption { nextChainStep() }
                }

            // 5) AUTO-START GUIDE (sekali; setting vendor — tak bisa diverifikasi API)
            prefs.jitterAskAutostart && VendorAutostartGuide.isKnownVendor() -> {
                prefs.jitterAskAutostart = false
                VendorAutostartGuide.show(this)
            }
        }
    }

    /**
     * Double cross-check: tahap yang SAMA diminta ulang = belum granted
     * → toast penjelasan + jeda 0,7 dtk sebelum dialog muncul lagi
     * (agar dialog lama sempat tertutup rapi dan user membaca statusnya).
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

        // Kembali dari Settings (jalur "Selalu izinkan" / lokasi diblokir)
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
        val msg = if (active) "${target.label} AKTIF — lock ${map.centerText()} — notif di status bar"
                  else "${target.label} dihentikan — notif hilang"
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun updateThemeIcon() {
        findViewById<ImageButton>(R.id.btn_theme)
            .setImageResource(if (prefs.isDark) R.drawable.ic_sun else R.drawable.ic_moon)
    }
}
