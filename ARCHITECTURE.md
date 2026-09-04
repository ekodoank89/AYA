Arsitektur AYA
Status: v2.7.2 STABIL — modul Xposed + manager peta.Target: DRIVER APPS SAJA (Grab Driver, Gojek Partner).

Komponen
Manager UI: MainActivity (tipis) + ui/ (MapController, PlayPanelController,PermissionFlow, NotifPermissionFlow, NotifController, FavoritesController, JitterController)
Data/contract: core/ (Keys, Prefs, SpoofTarget, ConfigProvider, ConfigPusher, FavoritesStore)
Hook (proses target): xposed/ (HookEntry, SpoofConfig + inner Jitter — clamp vektor)
Transport Config (rantai fallback)
push — broadcast (instan saat toggle/slider/reset + push ulang 1s/3s/6s setelah auto-launch)
remote — ContentProvider (com.aya.doank.config)
xsp — XSharedPreferences via daemon LSPosed (deprecated, lantai terakhir)
Rantai Izin v2.4.2 (double cross-check, mesin nextChainStep())
Lokasi dasar — ulang hingga granted
Selalu izinkan — ulang hingga granted (resumePendingBackground di onResume)
Notifikasi — ulang hingga granted
Baterai — dialog sistem SEKALI (batteryOnceThisSession)
Auto-start guide vendor: DIHAPUS dari UI (v2.4.2)
Fitur Aktif
Spoof per target: lock, toggle live, AUTO-LAUNCH app target (push dulu → open → push ulang)
Jitter per target: langkah/jendela/RADIUS (clamp VEKTOR — lingkaran sempurna),Reset ke Default per target
Default kalibrasi: GOJEK 3m/5dtk/R4m, GRAB 2m/8dtk/R3m (Prefs.defaultJitter — publik)
Notifikasi gabungan: INDIKATOR murni (TANPA tombol — v2.7.1) — ongoing, puncak shade
Favorit: 2 mode input (pin/manual vertikal), edit, hapus berkonfirmasi

Target (v2.7.1: driver apps SAJA)
GRAB = grab-driver → com.grabtaxi.driver2
GOJEK = gojek-driver → com.gojek.partner
Aturan Layer (WAJIB)
Folder	Boleh import	DILARANG
core/	stdlib, android.content	androidx.*, R, ui/, xposed/
ui/	core/, androidx, R	xposed/
xposed/	core/ (Keys, Targets), XposedBridgeApi, android.*	androidx.*, R, ui/
Kontrak Kritis (ubah = dua dunia)
ID target & packages: core/SpoofTarget.kt (SATU sumber)
Schema prefs: core/Keys.kt (spoof_*, jit_step/win/radius, pkg*)
Broadcast: core/ConfigPusher.kt (ACTION, EXTRA_TARGET_ID; extras: active/lat/lng/jit_step/jit_win/jit_radius)
Provider authority: com.aya.doank.config
Target baru WAJIB: manifest + SpoofTarget.packageNames
Dihapus (git history)
Chip koordinat lama (v2.2.2), FAB/floating/bottom bar
Preset Diam/Normal/Aktif → Reset Default per target (v2.6.2)
Auto-start guide vendor dari UI (v2.4.2), VendorAutostartGuide.kt
NotifController per-target + tombol STOP status bar (v2.7.1)
Chip telemetri manager (v2.7.2 — ChipTelemetry.kt; perkiraan, bukan data live hook)
Batasan Diketahui (jujur)
Framework-path Location: field asli (distanceTo real)
Server-side: kecepatan antar ping, rute, cross-check jaringan
HMA: whitelist AYA atau mati saat dev
Android 11+ package visibility: push ke Grab via broadcast (queries menutupnya)
Rotasi ikon navigasi target: diredakan via langkah kecil + jendela panjang
Konvensi
versionCode +1 per rilis; build gagal tidak memakai nomor
Resource: bg_*, ic_*, dot_* — huruf kecil+underscore
MainActivity = wiring; logika di controller; aturan data di Store
File diserahkan UTUH untuk timpa; cek import (View/R/Build/Context) tiap file baru
XML: tidak ada '&' mentah dalam teks atribut
Backlog (tercatat, belum dieksekusi)
IPC hook→manager (chip/telemetri presisi 100% dari proses target)
Jitter heading-aware / smooth transition (anti rotasi ikon)
UI pemilih target dinamis (Tahap 3)
Preset kalibrasi lapangan hasil uji Anda ("Gojek-Natural"/"Grab-Stabil")

Hardening 2 — Tag & Release
Repo → Releases → New release → tag v2.7.2 → judul: "AYA v2.7.2 — Pembersihan UI: hapus chip telemetri" → deskripsi singkat (simplifikasi: fokus panel 4 baris; notif indikator murni; driver apps saja) → Publish → attach APK dari Actions terakhir.

Status Penutup v2.7.2
Kode	Minimal, setiap elemen berfungsi nyata
Repo	Bersih dari file mati, ter-tag, ter-backup
Fungsi	Spoof dual-target + auto-launch + jitter kalibrasi + favorit — semuanya teruji

📌 Catatan Mentor — Penutup Iterasi
Rilis penghapusan adalah rilis yang sama berharganya dengan rilis fitur — v2.7.2 membuktikan disiplin: sesuatu yang pernah dibangun (chip) dihapus begitu terbukti setengah-benar, tanpa ragu, dengan jejak yang rapi di history. Proyek yang sehat tahu kapan menambah DAN kapan memangkas.
Backlog kini berisi empat item yang semuanya "besar tapi jelas" — bukan lagi perbaikan kecil yang menumpuk. Itu tanda fase pembersihan tuntas: dari sini, langkah berikutnya pasti fitur yang sepadan, dan Anda bebas memilih waktunya.
Dan jika nanti di lapangan muncul kebutuhan mendadak ( perilaku app target berubah, ROM baru berperilaku beda), jalur diagnosis kita sudah teruji: log LSPosed filter AYA → cocokkan KUNCI → satu variabel per iterasi.
Proyek AYA v2.7.2: selesai, bersih, ter-tag. Sampai jumpa di iterasi berikutnya. 🚀🏁