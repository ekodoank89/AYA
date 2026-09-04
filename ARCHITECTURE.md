Arsitektur AYA

Status: v2.6.9 STABIL — modul Xposed + manager peta, 2 target terverifikasi lapangan.


Komponen

• Manager UI: MainActivity (tipis) + ui/ (MapController, PlayPanelController,PermissionFlow, NotifPermissionFlow, NotifController, FavoritesController, JitterController,ChipTelemetry)
• Data/contract: core/ (Keys, Prefs, SpoofTarget, ConfigProvider, ConfigPusher, FavoritesStore)
• Hook (proses target): xposed/ (HookEntry, SpoofConfig + inner Jitter — clamp vektor)


Transport Config (rantai fallback)

1. push — broadcast (instan saat toggle/slider/reset + push ulang 1s/3s/6s setelah auto-launch)
2. remote — ContentProvider (com.aya.doank.config)
3. xsp — XSharedPreferences (deprecated, lantai terakhir)


Fitur Aktif

• Spoof per target: lock, toggle live, auto-launch app target (push dulu → open → push ulang)
• Jitter per target: langkah/jendela/RADIUS (clamp VEKTOR — lingkaran sempurna), reset-default per target
• Notifikasi GABUNGAN: satu notif untuk semua target aktif, tombol ■ STOP per target
• Chip telemetri per target (koordinat/acc/spd/brg/alt — replikasi sisi manager, perkiraan)
• Favorit: 2 mode input (pin/manual vertikal), edit, hapus berkonfirmasi
• Rantai izin double cross-check: Lokasi → Selalu izinkan → Notifikasi → Baterai (sekali)


Aturan Layer (WAJIB)

Folder	Boleh import	DILARANG
core/	stdlib, android.content	androidx.*, R, ui/, xposed/
ui/	core/, androidx, R	xposed/
xposed/	core/ (Keys, Targets), XposedBridgeApi, android.*	androidx.*, R, ui/


Kontrak Kritis (ubah = dua dunia)

• ID target & packages: core/SpoofTarget.kt
• Schema prefs: core/Keys.kt (spoof_*, jit_*, pkg*)
• Broadcast: core/ConfigPusher.kt (ACTION, EXTRA_TARGET_ID, extra keys: active/lat/lng/jit_*)
• Provider authority: com.aya.doank.config
• Target baru WAJIB: manifest + SpoofTarget.packageNames


Dihapus (git history)

• Chip koordinat lama, FAB/floating stop/bottom bar, preset Diam/Normal/Aktif (→ Reset Default),auto-start guide vendor dari UI, NotifController per-target terpisah (→ gabungan), JitterSimulator terpisah


Batasan Diketahui (jujur)

• Framework-path Location: field asli (distanceTo real)
• Server-side: kecepatan antar ping, rute, cross-check jaringan
• HMA: whitelist AYA atau mati saat dev
• MIUI: tombol aksi notif kedua collapsed → diselesaikan notif gabungan
• Chip manager = PERKIRAAN (bukan data live dari proses target; IPC hook→manager = backlog)


Konvensi

• versionCode +1 per rilis; file diserahkan UTUH untuk timpa (tanpa instruksi potongan)
• MainActivity = wiring; logika di controller; aturan data di Store
• Cek import (View/R/Build/Context) setiap file baru


Hardening 2 — Tag & Release

Repo → Releases → Draft a new release → tag: v2.6.9 → judul: "AYA v2.6.9 — Jitter per-target + Notifikasi gabungan + Chip telemetri" → deskripsi ringkas fitur → Publish → attach APK dari Actions terakhir.


Hardening 3 — Backup Clone (di komputer)

git clone https://github.com/USERNAME/AYA.git

• aya.jks & password di penyimpanan aman terpisah (keystore TIDAK ada di repo).


Hardening 4 — Audit Secret

github.dev → Ctrl+Shift+F: AIza → harus 0 hasil; .jks → hanya di .gitignore.


Hardening 5 — Bersihkan Sisa (opsional, direkomendasikan)

File
Status
ui/TelemetryChipController.kt	Hapus (file gagal generasi lama — ChipTelemetry yang dipakai)
ui/JitterSimulator.kt	Hapus jika ada (jitter kini internal ChipTelemetry)
ic_star.xml (emas)	Hapus — sudah diganti ic_star_white

Setelah bersih: Ctrl+Shift+F → TelemetryChipController & JitterSimulator → wajib 0 hasil → commit pembersihan.


Roadmap Selanjutnya (kandidat, setelah hardening)

1. Backlog penting: jalur IPC hook→manager (chip presisi 100%, bukan perkiraan)
2. Target dinamis (Tahap 3 tertunda — preview sudah pernah dibuat)
3. Grup favorit per label
4. Preset kalibrasi hasil lapangan Anda (Gojek 3/5/R4, Grab 2/8/R3 → dibakukan sebagai "Gojek-Natural"/"Grab-Stabil")