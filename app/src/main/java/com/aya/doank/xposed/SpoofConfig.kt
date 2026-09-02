package com.aya.doank.xposed

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import com.aya.doank.core.ConfigProvider
import com.aya.doank.core.ConfigPusher
import com.aya.doank.core.Keys
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import java.util.Random
import kotlin.math.abs
import kotlin.math.cos

class SpoofConfig(private val targetId: String) {

    // … SEMUA isi kelas SAMA PERSIS dengan v2.0.1 yang sudah Anda pakai —
    //   (init, latitude/longitude, refresh, ensurePushReceiver, readRemote, readXsp,
    //    applyState, setTransport, currentApplication) — JANGAN diubah.
    // Tambahkan HANYA baris ini di dalam kelas:

    val jitter = Jitter()
