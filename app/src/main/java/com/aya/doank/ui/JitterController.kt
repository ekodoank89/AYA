package com.aya.doank.ui

import android.app.Activity
import android.view.LayoutInflater
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.aya.doank.R
import com.aya.doank.core.ConfigPusher
import com.aya.doank.core.Prefs

/** Dialog pengaturan jitter: preset + 2 slider, tersimpan & langsung di-push. */
class JitterController(
    private val activity: Activity,
    private val prefs: Prefs,
    private val pusher: ConfigPusher
) {
    fun bind(btnId: Int) {
        activity.findViewById<View>(btnId).setOnClickListener { show() }
    }

    private fun show() {
        val v = LayoutInflater.from(activity).inflate(R.layout.dialog_jitter, null)
        val pDiam   = v.findViewById<TextView>(R.id.preset_diam)
        val pNormal = v.findViewById<TextView>(R.id.preset_normal)
        val pAktif  = v.findViewById<TextView>(R.id.preset_aktif)
        val lblStep = v.findViewById<TextView>(R.id.lbl_step)
        val lblWin  = v.findViewById<TextView>(R.id.lbl_win)
        val seekStep = v.findViewById<SeekBar>(R.id.seek_step)
        val seekWin  = v.findViewById<SeekBar>(R.id.seek_win)

        fun fmtStep(s: Float) = if (s % 1f == 0f) "${s.toInt()} m" else "${s} m"

        fun syncPresets() {
            val s = prefs.jitterStep; val w = prefs.jitterWindowSec
            val sel = R.drawable.bg_mode_on; val unsel = R.drawable.bg_mode_off
            val on = 0xFFC8F7D8.toInt(); val off = 0x99FFFFFF.toInt()

            fun set(tv: TextView, hit: Boolean) {
                tv.setBackgroundResource(if (hit) sel else unsel)
                tv.setTextColor(if (hit) on else off)
            }
            set(pDiam,   s == 1f  && w == 10)
            set(pNormal, s == 2.5f && w == 6)
            set(pAktif,  s == 5f  && w == 3)
        }

        fun syncLabels() {
            lblStep.text = "Langkah per jendela: ${fmtStep(prefs.jitterStep)}"
            lblWin.text  = "Jendela (interval): ${prefs.jitterWindowSec} detik"
        }

        fun pushAll() {
            // Push ulang config ke semua target — jitter baru terbaca hook saat tick berikut
            pusher.pushAll()
        }

        seekStep.progress = (prefs.jitterStep * 2).toInt()   // 0.5 langkah
        seekWin.progress  = prefs.jitterWindowSec
        syncLabels(); syncPresets()

        seekStep.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                prefs.jitterStep = p / 2f
                syncLabels(); syncPresets()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) { pushAll() }
        })
        seekWin.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                prefs.jitterWindowSec = p
                syncLabels(); syncPresets()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) { pushAll() }
        })

        pDiam.setOnClickListener {
            prefs.jitterStep = 1f; prefs.jitterWindowSec = 10
            seekStep.progress = 2; seekWin.progress = 10
            syncLabels(); syncPresets(); pushAll()
            Toast.makeText(activity, "Preset Diam", Toast.LENGTH_SHORT).show()
        }
        pNormal.setOnClickListener {
            prefs.jitterStep = 2.5f; prefs.jitterWindowSec = 6
            seekStep.progress = 5; seekWin.progress = 6
            syncLabels(); syncPresets(); pushAll()
            Toast.makeText(activity, "Preset Normal", Toast.LENGTH_SHORT).show()
        }
        pAktif.setOnClickListener {
            prefs.jitterStep = 5f; prefs.jitterWindowSec = 3
            seekStep.progress = 10; seekWin.progress = 3
            syncLabels(); syncPresets(); pushAll()
            Toast.makeText(activity, "Preset Aktif", Toast.LENGTH_SHORT).show()
        }

        v.findViewById<View>(R.id.btn_jitter_close).setOnClickListener { pushAll() }

        val d = AlertDialog.Builder(activity, R.style.Theme_AYA_Dialog)
            .setView(v).create()
        d.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_card)
        d.window?.setLayout(
            (activity.resources.displayMetrics.widthPixels * 0.92).toInt(),
            android.view.WindowManager.LayoutParams.WRAP_CONTENT
        )
        d.show()
    }
}
