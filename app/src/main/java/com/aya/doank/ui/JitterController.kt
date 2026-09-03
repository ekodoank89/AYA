package com.aya.doank.ui

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.aya.doank.R
import com.aya.doank.core.ConfigPusher
import com.aya.doank.core.Prefs
import com.aya.doank.core.Targets

/**
 * Dialog jitter — v2.6.2: PER TARGET (GRAB|GOJEK).
 * FIX: tap segmen kini selalu memanggil syncSegment() — highlight
 * (focus visual) berpindah sesuai target yang di-tap.
 */
class JitterController(
    private val activity: Activity,
    private val prefs: Prefs,
    private val pusher: ConfigPusher
) {
    private var dialog: AlertDialog? = null

    fun bind(btnId: Int) {
        activity.findViewById<View>(btnId).setOnClickListener { show() }
    }

    /** Dialog default sempit — lebarkan ke 92% lebar layar. */
    private fun widen(d: AlertDialog) {
        d.setOnShowListener {
            d.window?.setLayout(
                (activity.resources.displayMetrics.widthPixels * 0.92).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun targetLabel(id: String) =
        if (id == Targets.GRAB.id) "GRAB" else "GOJEK"

    private fun show() {
        val v = LayoutInflater.from(activity).inflate(R.layout.dialog_jitter, null)
        val segGrab  = v.findViewById<TextView>(R.id.seg_grab)
        val segGojek = v.findViewById<TextView>(R.id.seg_gojek)
        val btnReset = v.findViewById<Button>(R.id.btn_reset_default)
        val lblStep  = v.findViewById<TextView>(R.id.lbl_step)
        val lblWin   = v.findViewById<TextView>(R.id.lbl_win)
        val lblRad   = v.findViewById<TextView>(R.id.lbl_radius)
        val seekStep = v.findViewById<SeekBar>(R.id.seek_step)
        val seekWin  = v.findViewById<SeekBar>(R.id.seek_win)
        val seekRad  = v.findViewById<SeekBar>(R.id.seek_radius)

        var selectedId = Targets.GRAB.id

        fun fmtStep(s: Float) = if (s % 1f == 0f) "${s.toInt()} m" else "$s m"

        /** Highlight segmen mengikuti selectedId — dipanggil DI SETIAP perubahan. */
        fun syncSegment() {
            val sel = R.drawable.bg_mode_on
            val unsel = R.drawable.bg_mode_off
            val on = 0xFFC8F7D8.toInt()
            val off = 0x99FFFFFF.toInt()
            segGrab.setBackgroundResource(if (selectedId == Targets.GRAB.id) sel else unsel)
            segGrab.setTextColor(if (selectedId == Targets.GRAB.id) on else off)
            segGojek.setBackgroundResource(if (selectedId == Targets.GOJEK.id) sel else unsel)
            segGojek.setTextColor(if (selectedId == Targets.GOJEK.id) on else off)
        }

        fun syncAll() {
            syncSegment()   // ← FIX: highlight ikut di-refresh setiap kali syncAll
            val s = prefs.jitterStep(selectedId)
            val w = prefs.jitterWindowSec(selectedId)
            val r = prefs.jitterRadius(selectedId)
            seekStep.progress = (s * 2).toInt()
            seekWin.progress = w
            seekRad.progress = (r * 2).toInt()
            lblStep.text = "Langkah per jendela: ${fmtStep(s)}"
            lblWin.text = "Jendela (interval): $w detik"
            lblRad.text = "Radius maksimal: ${fmtStep(r)}"
            val d = prefs.defaultJitter(selectedId)
            btnReset.text = "↺ Reset ke Default ${targetLabel(selectedId)}\n" +
                "${fmtStep(d.first)} / ${d.second} dtk / R${fmtStep(d.third)}"
        }

        segGrab.setOnClickListener {
            if (selectedId == Targets.GRAB.id) return@setOnClickListener  // sudah aktif → abaikan
            selectedId = Targets.GRAB.id
            syncAll()
            toast("Menyetel: GRAB")
        }
        segGojek.setOnClickListener {
            if (selectedId == Targets.GOJEK.id) return@setOnClickListener
            selectedId = Targets.GOJEK.id
            syncAll()
            toast("Menyetel: GOJEK")
        }

        seekStep.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, f: Boolean) {
                prefs.setJitterStep(selectedId, p / 2f)
                lblStep.text = "Langkah per jendela: ${fmtStep(p / 2f)}"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) { pusher.pushAll() }
        })
        seekWin.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, f: Boolean) {
                prefs.setJitterWindowSec(selectedId, p)
                lblWin.text = "Jendela (interval): $p detik"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) { pusher.pushAll() }
        })
        seekRad.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, f: Boolean) {
                prefs.setJitterRadius(selectedId, p / 2f)
                lblRad.text = "Radius maksimal: ${fmtStep(p / 2f)}"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) { pusher.pushAll() }
        })

        // ==== RESET KE DEFAULT (target terpilih) ====
        btnReset.setOnClickListener {
            val d = prefs.defaultJitter(selectedId)
            prefs.setJitterStep(selectedId, d.first)
            prefs.setJitterWindowSec(selectedId, d.second)
            prefs.setJitterRadius(selectedId, d.third)
            syncAll()
            pusher.pushAll()
            Toast.makeText(activity,
                "Default ${targetLabel(selectedId)} diterapkan — " +
                "${fmtStep(d.first)} / ${d.second} dtk / R${fmtStep(d.third)}",
                Toast.LENGTH_SHORT).show()
        }

        v.findViewById<View>(R.id.btn_jitter_close).setOnClickListener {
            pusher.pushAll()
            dialog?.dismiss()
        }

        dialog = AlertDialog.Builder(activity, R.style.Theme_AYA_Dialog)
            .setView(v)
            .create()
        dialog?.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_card)
        widen(dialog!!)
        dialog?.show()
        syncAll()
    }

    private fun toast(msg: String) {
        Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
    }
}
