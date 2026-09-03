Arsitektur AYA
Status: v2.4.2 STABIL — modul Xposed + manager peta, 2 target terverifikasi lapangan.

Komponen
Manager UI: MainActivity (tipis) + ui/ (MapController, PlayPanelController,PermissionFlow, NotifPermissionFlow, NotifController, FavoritesController, JitterController)
Data/contract: core/ (Keys, Prefs, SpoofTarget, ConfigProvider, ConfigPusher, FavoritesStore)
Hook (jalan DI DALAM proses target): xposed/ (HookEntry, SpoofConfig + inner class Jitter)
Transport Config (rantai fallback, urutan prioritas)
push — broadcast dari manager (instan saat toggle/slider/preset + berkala 10 dtk selama app terbuka)
remote — ContentProvider (authority: com.aya.doank.config)
xsp — XSharedPreferences via daemon LSPosed (deprecated, lantai terakhir)
Rantai Izin v2.4.2 (double cross-check, mesin status nextChainStep())
Lokasi dasar — ulang hingga granted
Selalu izinkan — ulang hingga granted (kembali dari Settings dicek di onResume)
Notifikasi — ulang hingga granted
Baterai — dialog sistem SEKALI (batteryOnceThisSession, tidak ditagih)
Auto-start guide vendor: DIHAPUS dari UI (v2.4.2) — kode di git history
Battery exemption tetap di-deklarasikan di manifest (dialog sistem sekali)
Fitur Aktif
Pin overlay tengah, tema gelap/terang persist, titik biru + izin berantai
Spoof per target (GRAB/GOJEK), toggle live <1 dtk, lock per target
Jitter dinamis (step/window via slider+preset, push live tanpa restart) — radius 5 m terkunci
Notifikasi per target (ongoing) + tombol STOP remote
Favorit: 2 mode input (pin/manual vertikal), edit, hapus berkonfirmasi, kartu solid

Aturan Layer (WAJIB)
Folder	Boleh import	DILARANG
core/	stdlib, android.content	androidx.*, R, ui/, xposed/
ui/	core/, androidx, R	xposed/
xposed/	core/ (Keys, Targets), XposedBridgeApi, android.*	androidx.*, R, ui/
Kontrak Kritis (ubah = perubahan dua dunia)
ID target & packages: core/SpoofTarget.kt (SATU sumber kebenaran)
Schema prefs: core/Keys.kt (termasuk JIT_STEP/JIT_WINDOW)
Broadcast action: core/ConfigPusher.kt (ACTION, EXTRA_TARGET_ID)
Provider authority: com.aya.doank.config
Dihapus (hidup di git history)
Chip koordinat (v2.2.2)
FAB / floating stop / bottom bar (digantikan notifikasi)
Auto-start guide vendor dari UI (v2.4.2) — VendorAutostartGuide.kt masih ada, tak dirujuk
Batasan Diketahui (jujur)
Location dari jalur framework: field asli (distanceTo menghitung real)
Server-side: kecepatan antar ping, kelogisanan rute, cross-check jaringan
HMA harus whitelist AYA atau dimatikan saat development
Android 11+ package visibility: Push ke Grab lewat broadcast (queries di manifest)

Konvensi
versionCode +1 per rilis; build gagal tidak memakai nomor
Resource: bg_*, ic_*, dot_* — huruf kecil+underscore
MainActivity = wiring saja; logika di controller; aturan data di Store
File diserahkan UTUH untuk timpa (tanpa instruksi potongan)
