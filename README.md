AYA
AYA adalah modul Xposed (LSPosed) + aplikasi manager yang memungkinkansubstitusi lokasi GPS per-target di aplikasi driver (Grab Driver, Gojek Partner).

Fitur
Spoofing GPS per-target: GRAB (com.grabtaxi.driver2) dan GOJEK (com.gojek.partner)
Jitter dinamis per-target: langkah, jendela, radius (clamp vektor — lingkaran sempurna)
Reset ke Default per-target (kalibrasi lapangan)
Notifikasi indikator (ongoing, puncak shade, tanpa tombol)
Auto-launch app target saat play diaktifkan
Favorit per-kategori: 2 mode input (pin/manual vertikal), edit, hapus berkonfirmasi
Rantai izin double cross-check: Lokasi → Selalu izinkan → Notifikasi → Baterai
Transport config 3-jalur dengan fallback: push → remote → xsp
Persyaratan
Android 11+ (minSdk 30)
Root (KernelSU/Magisk)
LSPosed API 82+
Modul harus diaktifkan di LSPosed dengan scope: Grab Driver, Gojek Partner
Instalasi
Download APK dari Releases
Install di HP yang sudah root + LSPosed
Aktifkan modul di LSPosed → Modul → AYA
Centang scope: Grab Driver, Gojek Partner
Force-stop app target, buka AYA, tekan ▶ di target yang diinginkan
Arsitektur
Lihat ARCHITECTURE.md untuk detail struktur, kontrak, dan batasan.

Penggunaan
Buka AYA
Set posisi pin di map
Tekan ▶ GRAB atau ▶ GOJEK untuk mengaktifkan spoofing
App target akan terbuka otomatis
Tekan ■ untuk menghentikan spoofing
Batasan
Framework-path Location: field asli (distanceTo real)
Server-side: kecepatan antar ping, rute, cross-check jaringan
HMA: whitelist AYA atau mati saat development
Lisensi
Penggunaan pribadi. Dilarang untuk penyalahgunaan sistem penugasan
