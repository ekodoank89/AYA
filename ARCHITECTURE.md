Keputusan Storage (v2.7.2 — FINAL)
Favorit: JSON di SharedPreferences (dengan backup ganda + fallback otomatis).Dipilih karena: kurasi manual, puluhan item, akses load-all.
Pemicu migrasi ke Room (objektif, bukan dogma):
favorit > 1000 item, ATAU
field per favorit > 6, ATAU
pertanyaan "query X" mulai terasa dipaksa di memori
Data time-series (statistik order, log pergerakan): Room SEJAK AWAL saat dibangun.
Backlog (tercatat, belum dieksekusi)
IPC hook→manager (chip/telemetri presisi 100% dari proses target)
Jitter heading-aware / smooth transition (anti rotasi ikon)
UI pemilih target dinamis (Tahap 3)
Preset kalibrasi lapangan hasil uji ("Gojek-Natural"/"Grab-Stabil")
Room untuk statistik/time-series (jika fitur statistik dibangun)
Status v2.7.2 Setelah Keputusan Ini
Aspek
Kondisi
Kode	v2.7.2 stabil, 6/6 teruji — tidak berubah
Favorit	JSON + backup ganda + findNear siap pakai (di FavoritesStore)
Dokumentasi	Keputusan storage tercatat + pemicu migrasi objektif
Backlog	5 item, semuanya besar-dan-jelas

Ringkasan Penutup Iterasi
Iterasi ini menutup dua hal:

Keputusan storage — JSON terkonfirmasi dengan pemicu migrasi yang objektif (bukan dogma)
Bukti lapangan — sistem end-to-end bekerja penuh di kedua target (log LSPosed terverifikasi: lock per-target, jitter per-target, push/rantai, auto-launch)
📌 Catatan Mentor
Keputusan "tidak berubah" adalah keputusan yang sama berharganya dengan keputusan "berubah" — selama ia diambil dengan kriteria objektif dan dicatat. Tanpa catatan ini, tiga bulan lagi pertanyaan yang sama akan muncul lagi dan debatnya diulang dari nol. Dengan catatan, jawabannya tinggal dibaca.
findNear yang baru ditambahkan diam-diam membuka pintu fitur menarik kelak: "favorit terdekat dari posisi saya" — tombol yang menyaring favorit dalam radius X meter. Kalau suatu hari dibutuhkan, fungsi datanya sudah jadi.
Proyek AYA kini berada di titik yang paling sehat sepanjang sejarahnya: stabil, minimal, terdokumentasi, ter-backup, ter-audit — dan roadmap ke depan berisi item besar yang jelas, bukan perbaikan menumpuk.
Dengan ARCHITECTURE.md ter-update, iterasi ini resmi tuntas. Kapan pun siap untuk Tahap 3 (pemilih target dinamis) atau item backlog lain — fondasi menunggu. 🚀🏁
