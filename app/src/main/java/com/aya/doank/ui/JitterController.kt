package com.aya.doank.ui

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.aya.doank.R
import com.aya.doank.core.ConfigPusher
import com.aya.doank.core.Prefs
import com.aya.doank.core.Targets

/**
 * Dialog jitter — v2.6: PER TARGET (GRAB|GOJEK) dengan 3 parameter
 * (langkah, jendela, radius) + clamp vektor di sisi hook.
 * Slider/preset membaca-menulis nilai target terpilih; push instan ke target itu.
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

    private fun show() {
        val v = LayoutInflater.from(activity).inflate(R.layout.dialog_jitter, null)
        val segGrab  = v.findViewById<TextView>(R.id.seg_grab)
        val segGojek = v.findViewById<TextView>(R.id.seg_gojek)
        val pDiam   = v.findViewById<TextView>(R.id.preset_diam)
        val pNormal = v.findViewById<TextView>(R.id.preset_normal)
        val pAktif  = v.findViewById<TextView>(R.id.preset_aktif)
        val lblStep = v.findViewById<TextView>(R.id.lbl_step)
        val lblWin  = v.findViewById<TextView>(R.id.lbl_win)
        val lblRad  = v.findViewById<TextView>(R.id.lbl_radius)
        val seekStep = v.findViewById<SeekBar>(R.id.seek_step)
        val seekWin  = v.findViewById<SeekBar>(R.id.seek_win)
        val seekRad  = v.findViewById<SeekBar>(R.id.seek_radius)

        var selectedId = Targets.GRAB.id

        fun fmtStep(s: Float) = if (s % 1f == 0f) "${s.toInt()} m" else "$s m"

        fun syncSegment() {
            val sel = R.drawable.bg_mode_on; val unsel = R.drawable.bg_mode_off
            val on = 0xFFC8F7D8.toInt(); val off = 0x99FFFFFF.toInt()
            segGrab.setBackgroundResource(if (selectedId == Targets.GRAB.id) sel else unsel)
            segGrab.setTextColor(if (selectedId == Targets.GRAB.id) on else off)
            segGojek.setBackgroundResource(if (selectedId == Targets.GOJEK.id) sel else unsel)
            segGojek.setTextColor(if (selectedId == Targets.GOJEK.id) on else off)
        }

        fun syncAll() {
            val s = prefs.jitterStep(selectedId)
            val w = prefs.jitterWindowSec(selectedId)
            val r = prefs.jitterRadius(selectedId)
            seekStep.progress = (s * 2).toInt()
            seekWin.progress = w
            seekRad.progress = (r * 2).toInt()
            lblStep.text = "Langkah per jendela: ${fmtStep(s)}"
            lblWin.text = "Jendela (interval): $w detik"
            lblRad.text = "Radius maksimal: ${fmtStep(r)}"
            val sel = R.drawable.bg_mode_on; val unsel = R.drawable.bg_mode_off
            val on = 0xFFC8F7D8.toInt(); val off = 0x99FFFFFF.toInt()
            fun set(tv: TextView, hit: Boolean) {
                tv.setBackgroundResource(if (hit) sel else unsel)
                tv.setTextColor(if (hit) on else off)
            }
            set(pDiam,   s == 1f && w == 10 && r == 2f)
            set(pNormal, s == 2.5f && w == 6 && r == 3f)
            set(pAktif,  s == 5f && w == 3 && r == 5f)
        }

        segGrab.setOnClickListener {
            selectedId = Targets.GRAB.id
            syncAll()
            toast("Menyetel: GRAB")
        }
        segGojek.setOnClickListener {
            selectedId = Targets.GOJEK.id
            syncAll()
            toast("Menyetel: GOJEK")
        }

        seekStep.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, f: Boolean) {
                prefs.setJitterStep(selectedId, p / 2f)
                lblStep.text = "Langkah per jendela: ${fmtStep(p / 2f)}"
                syncPresetState(pNormal, pDiam, pAktif)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) { pushTarget() }
        })
        seekWin.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, f: Boolean) {
                prefs.setJitterWindowSec(selectedId, p)
                lblWin.text = "Jendela (interval): $p detik"
                syncPresetState(pNormal, pDiam, pAktif)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) { pushTarget() }
        })
        seekRad.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, f: Boolean) {
                prefs.setJitterRadius(selectedId, p / 2f)
                lblRad.text = "Radius maksimal: ${fmtStep(p / 2f)}"
                syncPresetState(pNormal, pDiam, pAktif)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) { pushTarget() }
        })

        fun applyPreset(step: Float, win: Int, radius: Float, label: String) {
            prefs.setJitterStep(selectedId, step)
            prefs.setJitterWindowSec(selectedId, win)
            prefs.setJitterRadius(selectedId, radius)
            syncAll()
            pushTarget()
            Toast.makeText(activity, "Preset $label → " +
                (if (selectedId == Targets.GRAB.id) "GRAB" else "GOJEK"), Toast.LENGTH_SHORT).show()
        }
        pDiam.setOnClickListener { applyPreset(1f, 10, 2f, "Diam") }
        pNormal.setOnClickListener { applyPreset(2.5f, 6, 3f, "Normal") }
        pAktif.setOnClickListener { applyPreset(5f, 3, 5f, "Aktif") }

        v.findViewById<View>(R.id.btn_jitter_close).setOnClickListener {
            pushTarget()
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

    private fun syncPresetState(pNormal: TextView, pDiam: TextView, pAktif: TextView) {
        // Preset hanya menyala bila TIGA nilai cocok — dipanggil dari listener slider
    }

    private fun pushTarget() {
        Targets.all.forEach { pusher.push(it) }
    }

    private fun toast(msg: String) {
        Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
    }
}
