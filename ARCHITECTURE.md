Arsitektur AYA

Aturan Layer (WAJIB)

Folder	Isi	Boleh import	DILARANG import
core/	Model, Prefs, Keys	stdlib, android.content	androidx.*, R, ui/
ui/	Controller tampilan	core/, androidx, R	xposed/
xposed/	Hook (jalan DI DALAM proses target)	core/ (Keys, model), XposedBridgeApi	androidx.*, R, ui/
Alasan: kode di xposed/ diload ke proses app target — ia tidak boleh membawaUI/library app kita. Karena itu semua schema data hidup di core/Keys.kt,dibaca manager (tulis) dan hook (baca) dari file prefs yang sama.


Resep Perubahan Umum

Mau mengubah...	Sentuh file...
Warna/tema tombol	res/values/colors.xml
Tambah target (C, D, ...)	core/SpoofTarget.kt (Targets) + layout + strings
Perilaku peta/kamera	ui/MapController.kt
Alur izin	ui/PermissionFlow.kt
Hook baru	xposed/hooks/*.kt + daftarkan di xposed/HookEntry.kt
Nama key storage	core/Keys.kt (SEKALI, di satu tempat)


Konvensi

versionCode +1 setiap push yang mengubah kode; versionName "x.y"
Resource: bg_*, ic_*, dot_*, huruf kecil+underscore
MainActivity TIDAK menampung logika — hanya wiring controller


Versi

app/build.gradle.kts:
versionCode = 7,
versionName = "1.6"


