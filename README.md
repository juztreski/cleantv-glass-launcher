# Glass TV Launcher

Launcher Android TV bergaya Apple TV untuk Hisense Android TV. UI memakai latar hitam, jam/tanggal, jadwal sholat, pill kaca, grid aplikasi, dock aplikasi favorit, baris **Lanjutkan menonton**, preview dock, dan widget ayat/hadis harian. Proyek diuji pada Android 11 (API 30), resolusi panel 1280x720, ABI `armeabi-v7a`; tidak ada pustaka native tambahan.

## Kondisi perangkat yang diuji

- TV: Hisense HiSmart TV, Android 11 / API 30, `armeabi-v7a`.
- Backup sebelum perubahan TV disimpan di `backup/`: `getprop`, ABI, daftar paket, tiga namespace Settings, `dumpsys package`, HOME awal, ukuran layar, dan density. File dump berasal dari perangkat rumah dan sebaiknya tidak dibagikan.
- Launcher sudah dipasang; HOME role diarahkan ke `io.tvhome.launcher`. Aplikasi TV bawaan dan layanan TV tidak di-uninstall.
- Browser bawaan (`com.internet.tvbrowser`), rekomendasi Android TV (`com.google.android.tvrecommendations`), dan launcher bawaan (`com.google.android.tvlauncher`) dinonaktifkan dengan `pm disable-user`. Nama paket yang benar-benar dinonaktifkan tersimpan di `backup/disabled-packages.txt`, agar `tools/restore.sh` dapat mengaktifkannya.
- Lokasi jadwal sholat adalah Kabupaten Soppeng, Sulawesi Selatan, memakai koordinat `-4.35, 119.89`, zona waktu `Asia/Makassar`, dan metode Kementerian Agama (Aladhan method 20). Data diambil paling banyak sekali per tanggal/lokasi dan disimpan lokal; jam hitung mundur diperbarui tiap menit.
- Baris **Lanjutkan menonton** membaca Watch Next dari aplikasi yang terpasang (maksimal 5 item), mengutamakan item `CONTINUE` dengan progres valid, lalu mengurutkan berdasarkan aktivitas terbaru. Klik kartu membuka aplikasi pemilik konten (halaman utama yang memuat baris Continue Watching) agar aplikasi menentukan posisi resume; posisi terakhir tetap ditentukan aplikasi TV masing-masing. Preview dock memakai video/poster Preview Programs lalu poster Watch Next; pada firmware ini provider Preview Programs umumnya kosong, jadi preview hanya muncul jika aplikasi TV benar-benar menaruh datanya. Tidak ada widget cuaca.
- Pill Inputs membuka dropdown ringkas input TV yang disediakan TV Input Framework; Wi-Fi menampilkan SSID bila Android mengizinkannya (izin lokasi yang di-grant melalui ADB hanya dipakai untuk nama jaringan). Jika Location TV dimatikan, Android 11 menyamarkan SSID menjadi `<unknown ssid>`; launcher tidak mengubah saklar privasi itu. Setelan membuka Settings. `READ_TV_LISTINGS` dipakai oleh TV Input Framework untuk enumerasi channel/input.
- Outfit adalah font open-source berlisensi SIL Open Font License, disertakan bersama berkas lisensinya.

## Tata letak proyek

- `app/src/main/java/io/tvhome/launcher/`: launcher, akses TV Input Framework, jadwal sholat, katalog wallpaper preset, dan katalog ayat/hadis.
- `app/src/main/assets/fonts/`: font Outfit serta lisensi SIL OFL.
- `app/src/main/res/values/config.xml`: dock, aplikasi tersembunyi, preset lokasi, lokasi/zona waktu sholat, wallpaper fallback, screensaver, dan saklar fitur.
- `tools/build-install.sh`: build, install, grant permission yang diminta, set role HOME, lalu jalankan launcher.
- `tools/restore.sh`: aktifkan kembali paket yang tercatat, cabut permission tambahan launcher, dan pulihkan HOME bawaan dari backup.
- `tools/debloat.sh` dan `tools/debloat-candidates.txt`: disable kandidat yang ditinjau; daftar aktual yang diubah ditulis ke `backup/disabled-packages.txt`.
- `tools/screenshot.sh`: simpan screenshot PNG TV ke `screenshots/` atau ke lokasi yang diberikan.
- `backup/`: inventaris pra-perubahan, termasuk dump Settings dan Package Manager. Folder ini tidak disertakan di repo publik karena berisi data perangkat.

## Build, install, dan screenshot

Pastikan ADB TV diotorisasi dan komputer berada pada jaringan yang sama. Serial bawaan skrip adalah `192.168.100.17:5555`; untuk TV lain:

```sh
DEVICE_SERIAL=alamat-tv:5555 ./tools/build-install.sh
./tools/screenshot.sh
```

`build-install.sh` sengaja menolak berjalan bila backup pra-perubahan penting tidak ada. Repo publik ini tidak menyertakan `backup/`; buat backup perangkat sendiri lebih dulu (lihat daftar berkas yang diminta skrip). Jika input TV yang dipilih memberi tangkapan layar kosong, itu berasal dari jalur video TV; kembali ke launcher untuk mengambil screenshot UI.

## Mengubah konfigurasi

Edit `app/src/main/res/values/config.xml`, lalu jalankan `./tools/build-install.sh`.

- **Dock/grid:** `dock_packages` menjadi favorit awal. Pill **Atur → Aplikasi favorit** memilih maksimal enam aplikasi (urutan centang menjadi urutan dock) dan **Atur → Jumlah dock** mengatur 1–6 ikon. Pilihan disimpan di SharedPreferences. Jika jumlah dock lebih besar daripada favorit terpilih, hanya favorit yang tersedia yang tampil; aplikasi yang tidak tampil di dock masuk grid. Grid memakai `<integer name="grid_columns">4</integer>`.
- **Nama sapaan:** pill **Atur → Nama sapaan** mengisi nama setelah sapaan, misalnya “Selamat siang, Budi”. Kosongkan untuk kembali ke sapaan biasa.
- **Grid:** otomatis memuat aplikasi berkategori `LEANBACK_LAUNCHER`. Masukkan ID paket ke `hidden_packages` untuk mengecualikannya dari grid.
- **Jadwal sholat:** ubah `prayer_location`, `prayer_latitude`, `prayer_longitude`, dan `prayer_timezone`. Soppeng memakai Aladhan method 20; waktu berikutnya dan hitung mundur diperbarui tiap menit. Izin lokasi Android tidak dipakai untuk menentukan lokasi sholat.
- **Lokasi runtime:** pill **Atur → Lokasi** memilih kota dari `location_presets` atau memakai lokasi perangkat. Pilihan disimpan di SharedPreferences dan menggantikan nilai default di `config.xml`.
- **Wallpaper:** `wallpaper_uri` kosong berarti memakai preset **Senja**. Pill **Atur → Wallpaper** menyediakan sembilan preset prosedural (Senja, Malam, Samudra, Hutan, Aurora, Hitam, Pemandangan, Perkotaan, Abstrak) dalam galeri thumbnail, pemilih gambar dari penyimpanan TV, dan URL gambar HTTPS. Pilihan disimpan di SharedPreferences; `wallpaper_uri` hanya menjadi fallback awal.
- **Screensaver:** pill **Atur → Screensaver** dapat memulai screensaver, mengaktifkan/menjeda otomatis, dan memilih waktu tunggu 1/2/5/10 menit. Screensaver menampilkan jam, tanggal, lokasi, jadwal sholat berikutnya, hitung mundur per detik, dan kelima waktu sholat. `screensaver_timeout_seconds` mengatur default 120 detik; `screensaver=true` mengaktifkan fitur.
- **Ayat & hadis:** pill **Atur → Ayat & Hadis** memilih mode otomatis harian (berganti mengikuti hari) atau manual dari 12 entri (6 ayat Al-Qur'an dan 6 hadis) beserta teks Arab, terjemahan Indonesia, dan sumber. Pilihan disimpan di SharedPreferences.
- **Preview dock:** pill **Atur → Preview dock** mengaktifkan/mematikan video atau poster latar saat tile dock difokuskan. Preview berhenti saat fokus pindah atau launcher dijeda.
- **Saklar fitur:** `system_pills`, `prayer_times`, `watch_next`, `preview_programs`, `dock_preview_video`, `verse_widget`, dan `screensaver` mengatur fitur terkait di `config.xml`.

Skrip build juga menonaktifkan launcher HOME bawaan yang tercatat dalam backup agar tombol HOME benar-benar membuka launcher baru pada firmware ini; paket itu tetap terpasang dan otomatis dicatat untuk pemulihan. Ini tidak menonaktifkan layanan tuner atau input TV.

## Navigasi dan fitur

- D-pad kiri/kanan memilih tile dan berhenti di tepi; grid empat kolom tidak melompat ke baris lain. Atas/bawah bergerak antarbaris/area; dari dock turun ke grid yang bergulir. Posisi fokus terakhir disimpan di SharedPreferences.
- Highlight kaca tunggal bergerak dengan animasi, tile fokus membesar sedikit, dan nama aplikasi tampil. Dock menampilkan nama aplikasi fokus dengan glow berwarna yang diambil dari banner.
- Jam, tanggal, dan sapaan tampil kiri atas. Jadwal sholat menampilkan waktu berikutnya dan kelima waktu harian tanpa kartu. Widget di kolom kanan menampilkan ayat atau hadis harian dengan teks Arab, terjemahan, dan sumber.
- Inputs membuka dropdown kaca untuk sumber TV; SSID dan Settings ada di pill kanan atas. Pill **Atur** membuka pengaturan cepat Lokasi, Aplikasi favorit, Jumlah dock, Wallpaper, Screensaver, Ayat & Hadis, Nama sapaan, dan Preview dock.
- Baris **Lanjutkan menonton** muncul di antara dock dan grid bila aplikasi TV menyediakan data Watch Next, dengan video terbaru di depan dan progress bar posisi terakhir. Klik kartu membuka aplikasi pemilik konten agar baris Continue Watching milik aplikasi yang melanjutkan pemutaran. Fokus pada tile dock menampilkan nama aplikasi dan, jika tersedia, video/poster preview di latar belakang; scrim diperkuat dan teks diberi bayangan agar tetap terbaca; preview dihentikan saat fokus berpindah.
- Screensaver launcher aktif setelah tidak ada input dan menampilkan jam, tanggal, lokasi, jadwal sholat berikutnya, hitung mundur per detik, serta kelima waktu sholat. Tombol apa pun menutup screensaver. Screensaver sistem/Daydream tetap terpisah.
- Tidak ada cuaca atau polling widget berkala. Jaringan hanya dipakai untuk memperbarui jadwal sholat sekali per hari dan memuat wallpaper URL saat launcher dibuka jika dikonfigurasi. Watch Next dan preview dibaca dari provider lokal, bukan dari jaringan launcher.

## Debloat dan pemulihan

`tools/debloat.sh` hanya menjalankan `pm disable-user --user 0`, tidak meng-uninstall apa pun, dan memerlukan backup serta launcher sebagai HOME preference. Kandidat yang disertakan dan alasannya:

| Paket | Alasan |
| --- | --- |
| `com.internet.tvbrowser` | Browser TV vendor, bukan komponen tuner atau HOME. |
| `com.google.android.tvrecommendations` | Feed/rekomendasi layar utama bawaan yang digantikan launcher. |
| `com.google.android.tvlauncher` | HOME bawaan, diganti launcher ini; tetap terpasang dan bisa dipulihkan. |

Setup wizard sengaja **tidak** dinonaktifkan: dapat diperlukan untuk setup awal/pemulihan pabrik. Tidak ada kandidat store vendor yang dapat diidentifikasi dengan aman dari inventaris ini; Play Store, Google Play Services, tuner/TV Input, HDMI, Bluetooth remote, dan layanan terkait tidak disentuh.

Pulihkan launcher bawaan dan paket yang skrip debloat ubah:

```sh
./tools/restore.sh
```

Perintah hanya mengaktifkan paket pada `backup/disabled-packages.txt` lalu memulihkan HOME dari `backup/default-home.txt`. APK launcher tidak dihapus; jika ingin menghapusnya sendiri setelah HOME dipulihkan, lakukan terpisah lewat ADB.
