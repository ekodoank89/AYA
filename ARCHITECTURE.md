Arsitektur AYA

Status: v2.0.1 — modul Xposed fungsional di 2 target (Grab Driver2, Gojek Partner).


Komponen

• Manager (UI peta) — core/, ui/, MainActivity
• Modul (hook) — xposed/, dijalankan LSPosed DI DALAM proses target
• ConfigProvider — ContentProvider milik manager (authority: com.aya.doank.config)
• ConfigPusher — broadcast manager→target (jalur push, bebas package-visibility)


Transport Config (rantai fallback, urutan prioritas)

1. push — broadcast dari manager (butuh manager pernah dibuka; tercepat)
2. remote — ContentProvider call (butuh target bisa resolve AYA — bergantung target)
3. xsp — XSharedPreferences via daemon LSPosed (deprecated, disertakan sebagai lantai)


Aturan Layer (WAJIB)

Folder	Isi	Boleh import	DILARANG import
core/	Keys, Prefs, SpoofTarget, ConfigProvider, ConfigPusher	stdlib, android.content	androidx.*, R, ui/
ui/	Controller tampilan	core/, androidx, R	xposed/
xposed/	Hook (jalan di proses target)	core/ (Keys, Targets), XposedBridgeApi, android.*	androidx.*, R, ui/


Kontrak Kritis (mengubah = perubahan di dua dunia)

• ID target: core/SpoofTarget.kt (SATU sumber kebenaran)
• Schema prefs: core/Keys.kt
• Action broadcast: core/ConfigPusher.kt (ACTION + EXTRA_TARGET_ID)


Roadmap

• [✓] Tahap 2: kerangka modul + hook getter + getLastKnown + GMS LocationResult
• [✓] Transport: push/remote/xsp (v2.0.1)
 v2.1: jitter GPS (random-walk di rewriteFields)
• [ ] Tahap 3: pemilih app dinamis (PackageManager + launcher)
• [ ] Tahap 4: perluasan hook (constructor Location, delivery), plausibility (accuracy/speed)
• [ ] Anti-detect: HMA template (sembunyikan root/LSPosed, whitelist AYA)


Konvensi

• versionCode +1 per push yang mengubah kode; build gagal tidak memakai nomor
• Resource: bg_*, ic_*, dot_*, huruf kecil+underscore
• MainActivity tipis — logika di controller


| Chip koordinat | DIHAPUS v2.2.2 — hidup di git history (commit sebelum v2.2.1) bila ingin dihidupkan ulang |




