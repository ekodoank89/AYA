package com.aya.doank

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aya.doank.core.ConfigPusher
import com.aya.doank.core.Prefs
import com.aya.doank.core.SpoofTarget
import com.aya.doank.ui.MapController
import com.aya.doank.ui.PermissionFlow
import com.aya.doank.ui.PlayPanelController
import com.google.android.gms.maps.SupportMapFragment

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var map: MapController
    private lateinit var permissionFlow: PermissionFlow
    private lateinit var playPanel: PlayPanelController
    private lateinit var pusher: ConfigPusher
    private lateinit var tvCenter: TextView
    private lateinit var btnTheme: ImageButton

    private val pushHandler = Handler(Looper.getMainLooper())
    private val pushRunnable = object : Runnable {
        override fun run() {
            pusher.pushAll()
            pushHandler.postDelayed(this, PUSH_INTERVAL_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvCenter = findViewById(R.id.tv_center)
        btnTheme = findViewById(R.id.btn_theme)

        prefs = Prefs(this)
        map = MapController(this, prefs)
        pusher = ConfigPusher(this)

        permissionFlow = PermissionFlow(this, prefs) {
            map.ensureBlueDot()
            map.focusFresh()
        }

        playPanel = PlayPanelController(
            this, prefs,
            centerProvider = { map.currentCenter() },
            blueDotProvider = { map.blueDot }
        ) { target, active ->
            pusher.push(target)   // toggle langsung didorong ke target (efek real-time)
            announce(target, active)
        }

        map.onCenterChanged = { _, _ -> tvCenter.text = map.centerText() }
        map.onBlueDotChanged = { _, _ -> playPanel.onBlueDotChanged() }

        findViewById<Button>(R.id.btn_zoom_in).setOnClickListener { map.zoomMax() }
        findViewById<Button>(R.id.btn_zoom_out).setOnClickListener { map.zoomOut() }
        findViewById<ImageButton>(R.id.btn_my_location).setOnClickListener {
            permissionFlow.requestOrGuide()
        }

        btnTheme.setOnClickListener {
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
            permissionFlow.requestOrGuide()
        }
    }

    override fun onResume() {
        super.onResume()
        if (permissionFlow.hasPermission()) map.ensureBlueDot()
        // Push berkala selama AYA terbuka: menangkap target yang baru saja start
        pusher.pushAll()
        pushHandler.postDelayed(pushRunnable, PUSH_INTERVAL_MS)
    }

    override fun onPause() {
        super.onPause()
        pushHandler.removeCallbacks(pushRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::map.isInitialized) map.stop()
    }

    private fun announce(target: SpoofTarget, active: Boolean) {
        val msg = if (active) "${target.label} AKTIF — lock ${map.centerText()}"
                  else "${target.label} dihentikan — mengikuti titik biru"
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun updateThemeIcon() {
        btnTheme.setImageResource(if (prefs.isDark) R.drawable.ic_sun else R.drawable.ic_moon)
    }

    companion object {
        private const val PUSH_INTERVAL_MS = 10_000L
    }
}
