Arsitektur AYA

Status: v2.7.1 STABIL — modul Xposed + manager peta.Target: DRIVER APPS SAJA (Grab Driver, Gojek Partner) — keputusan desain v2.7.1(seperti "stop dari notif membuat order hilang": gesekan stop disengaja,posisi tidak boleh melompat saat order aktif).


Komponen

• Manager UI: MainActivity (tipis) + ui/ (MapController, PlayPanelController,PermissionFlow, NotifPermissionFlow, NotifController, FavoritesController, JitterController,ChipTelemetry)
• Data/contract: core/ (Keys, Prefs, SpoofTarget, ConfigProvider, ConfigPusher, FavoritesStore)
• Hook (proses target): xposed/ (HookEntry, SpoofConfig + inner Jitter — clamp vektor)


Transport Config (rantai fallback)

1. push — broadcast (instan saat toggle/slider/reset + push ulang 1s/3s/6s setelah auto-launch)
2. remote — ContentProvider (com.aya.doank.config)
3. xsp — XSharedPreferences via daemon LSPosed (deprecated, lantai terakhir)


Rantai Izin v2.4.2 (double cross-check, mesin nextChainStep())

1. Lokasi dasar — ulang hingga granted
2. Selalu izinkan — ulang hingga granted (resumePendingBackground di onResume)
3. Notifikasi — ulang hingga granted
4. Baterai — dialog sistem SEKALI (batteryOnceThisSession)
• Auto-start guide vendor: DIHAPUS dari UI (v2.4.2 — keputusan)


Fitur Aktif

• Spoof per target: lock, toggle live, AUTO-LAUNCH app target (push dulu → open → push ulang)
• Jitter per target: langkah/jendela/RADIUS (clamp VEKTOR), Reset ke Default per target
• Default kalibrasi: GOJEK 3m/5dtk/R4m, GRAB 2m/8dtk/R3m (Prefs.defaultJitter — publik)
• Notifikasi GABUNGAN: indikator murni (TANPA tombol) — ongoing, puncak shade
• Chip telemetri per target: koordinat/acc/spd/brg/alt (replikasi sisi manager = PERKIRAAN)
• Favorit: 2 mode input (pin/manual vertikal), edit, hapus berkonfirmasi


Target (v2.7.1: driver apps SAJA)

• GRAB = grab-driver → com.grabtaxi.driver2
• GOJEK = gojek-driver → com.gojek.partner
• Customer apps dikeluarkan (token/orderflow belum; bisa kembali via SpoofTarget + )


Aturan Layer (WAJIB)

Folder	Boleh import	DILARANG
core/	stdlib, android.content	androidx.*, R, ui/, xposed/
ui/	core/, androidx, R	xposed/
xposed/	core/ (Keys, Targets), XposedBridgeApi, android.*	androidx.*, R, ui/


Kontrak Kritis (ubah = dua dunia)

• ID target & packages: core/SpoofTarget.kt (SATU sumber)
• Schema prefs: core/Keys.kt (spoof_*, jit_step/win/radius, pkg*)
• Broadcast: core/ConfigPusher.kt (ACTION, EXTRA_TARGET_ID; extras: active/lat/lng/jit_step/jit_win/jit_radius)
• Provider authority: com.aya.doank.config
• Target baru WAJIB: manifest + SpoofTarget.packageNames


Dihapus (git history)

• Chip koordinat lama (v2.2.2), FAB/floating/bottom bar, preset Diam/Normal/Aktif
• Auto-start guide vendor dari UI (v2.4.2), VendorAutostartGuide.kt
• NotifController per-target + tombol STOP status bar (v2.7.1 — pengendalian risiko order)
• TelemetryChipController.kt (versi gagal — digantikan ChipTelemetry)
• JitterSimulator.kt terpisah (jitter internal di ChipTelemetry)


Batasan Diketahui (jujur)

• Framework-path Location: field asli (distanceTo real)
• Server-side: kecepatan antar ping, rute, cross-check jaringan
• HMA: whitelist AYA atau mati saat dev
• Chip manager = PERKIRAAN (IPC hook→manager = backlog)
• Android 11+ package visibility: push ke Grab via broadcast (queries menutupnya)
• Rotasi ikon navigasi target: diredakan via langkah kecil + jendela panjang (opsi heading-aware di backlog)


Konvensi

• versionCode +1 per rilis; build gagal tidak memakai nomor
• Resource: bg_*, ic_*, dot_* — huruf kecil+underscore
• MainActivity = wiring; logika di controller; aturan data di Store
• File diserahkan UTUH untuk timpa; cek import (View/R/Build/Context) tiap file baru
• XML: tidak ada '&' mentah dalam teks atribut


Backlog (tercatat, belum dieksekusi)

• IPC hook→manager (chip presisi 100%)
• Jitter heading-aware / smooth transition (anti rotasi ikon)
• UI pemilih target dinamis (Tahap 3)
• speed/bearing/altitude eksplisit di slider (saat ini turunan otomatis)


Hardening 2 — Tag & Release

Repo → Releases → New release → tag v2.7.1 → target main → judul: "AYA v2.7.1 — Fokus driver apps + indikator notif murni" → deskripsi ringkas (driver-only scope, notif indikator tanpa tombol, jitter per-target 3-param + vektor clamp, auto-launch) → Publish → attach APK dari Actions terakhir.


Hardening 3 — Backup Clone

git clone https://github.com/USERNAME/AYA.git

• aya.jks & password di penyimpanan aman terpisah


Hardening 4 — Audit Secret

github.dev → Ctrl+Shift+F: AIza → 0 hasil; .jks → hanya .gitignore. Bersih → selesai.


Hardening 5 — Bersihkan Sisa Mati

| File | Aksi |
|---|---|
| `ui/VendorAutostartGuide.kt` | **Hapus** (tidak dirujuk sejak v2.4.2) |
| `ic_star.xml` (emas lama) | **Hapus** (dipakai ic_star_white sekarang) |
| `ui/NotifController.kt` | Verifikasi versi v2.7.1 (indikator, tanpa StopReceiver) |


Setelah bersih: Ctrl+Shift+F → VendorAutostartGuide, ic_star (rawan dobel dengan ic_star_white) → 0 referensi mati → commit pembersihan.


Status Penutup

| | |
|---|---|
| **Kode** | v2.7.1 stabil, teruji 6/6 |
| **Repo** | Bersih, ter-tag, ter-backup |
| **Dokumentasi** | Final, jujur, lengkap dengan backlog |
| **Perangkat** | Redmi 10C (MIUI) + Poco M3 (ROM campuran) terverifikasi |


📌 Penutup

Proyek AYA resmi beristirahat dalam kondisi terbaiknya: setiap lapisan teruji, setiap keputusan terdokumentasi beserta alasannya, dan setiap batasan dicatat jujur. Ketika nanti kembali — untuk customer apps, target ketiga, atau apa pun — fondasi v2.7.1 dan tag-nya menunggu sebagai jalan pulang.

Sampai jumpa di iterasi berikutnya. Selamat jalan, dan selamat bekerja dengan aman di lapangan. 🚀🏁
