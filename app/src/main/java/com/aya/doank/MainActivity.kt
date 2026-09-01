package com.aya.doank

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aya.doank.core.Prefs
import com.aya.doank.core.SpoofTarget
import com.aya.doank.ui.MapController
import com.aya.doank.ui.PermissionFlow
import com.aya.doank.ui.PlayPanelController

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var map: MapController
    private lateinit var permissionFlow: PermissionFlow
    private lateinit var playPanel: PlayPanelController
    private lateinit var tvCenter: TextView
    private lateinit var btnTheme: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvCenter = findViewById(R.id.tv_center)
        btnTheme = findViewById(R.id.btn_theme)

        prefs = Prefs(this)
        map = MapController(this, prefs)
        permissionFlow = PermissionFlow(this, prefs) { map.ensureBlueDot() }
        playPanel = PlayPanelController(this, prefs) { target, active ->
            if (active) {
                map.currentCenter()?.let { prefs.setSpoofPoint(it.latitude, it.longitude) }
            }
            announce(target, active)
        }

        map.onCenterChanged = { _, _ -> tvCenter.text = map.centerText() }

        findViewById<Button>(R.id.btn_zoom_in).setOnClickListener { map.zoomMax() }
        findViewById<Button>(R.id.btn_zoom_out).setOnClickListener { map.zoomOut() }
        findViewById<ImageButton>(R.id.btn_my_location).setOnClickListener { permissionFlow.requestOrGuide() }

        btnTheme.setOnClickListener {
            prefs.isDark = !prefs.isDark
            map.applyStyle(prefs.isDark)
            updateThemeIcon()
        }
        updateThemeIcon()

        playPanel.bind()
        map.attach(supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment)
    }

    override fun onResume() {
        super.onResume()
        if (permissionFlow.hasPermission()) map.ensureBlueDot()
    }

    private fun announce(target: SpoofTarget, active: Boolean) {
        val msg = if (active) "${target.label} AKTIF — target membaca lokasi pin: ${map.centerText()}"
                  else "${target.label} dihentikan"
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun updateThemeIcon() {
        btnTheme.setImageResource(if (prefs.isDark) R.drawable.ic_sun else R.drawable.ic_moon)
    }
}
