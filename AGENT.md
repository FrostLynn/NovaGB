# AGENTS.md - Developer & Agent Guidelines for NovaGB

Dokumen ini memuat panduan struktur proyek, konvensi penulisan kode, alur kerja pengujian, serta instruksi khusus bagi agent atau pengembang yang bekerja pada repositori **NovaGB**.

---

## 1. Ringkasan Proyek

NovaGB adalah emulator Game Boy (DMG) dan Game Boy Color (CGB) modern berkinerja tinggi untuk platform Android.
- **Bahasa Utama**: Kotlin (Pure Kotlin Core Engine + Jetpack Compose UI).
- **NDK / Native Option**: C++ via Android NDK (JNI bridge untuk integrasi Gambatte / SameBoy).
- **Target Platform**: Android Min SDK 26 (Android 8.0) hingga Target SDK 35 (Android 15).
- **UI Toolkit**: Jetpack Compose + Material 3 (Material You dynamic & OLED dark theme).

---

## 2. Struktur Direktori & Tanggung Jawab Modul

```text
D:\Projects\GB\
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── cpp/                          # CMakeLists.txt & native JNI bridge
│   │   │   ├── java/com/novagb/emulator/
│   │   │   │   ├── audio/                    # AudioTrack PCM streaming buffer
│   │   │   │   ├── core/                     # Sharp LR35902 CPU, MMU, PPU, APU, Cartridge, Joypad, Timer
│   │   │   │   ├── data/                     # RomRepository, RomMetadata, AppSettings
│   │   │   │   ├── ui/
│   │   │   │   │   ├── components/           # RetroDisplay (Canvas/Shaders), TouchController (Haptics)
│   │   │   │   │   ├── screens/              # LibraryScreen, EmulatorScreen, SettingsScreen
│   │   │   │   │   └── theme/                # Color, Theme, Type
│   │   │   │   └── MainActivity.kt           # NavHost, Gamepad key/motion events, SAF Intent handler
│   │   │   └── res/                          # XML resources, launcher icons, drawables
│   │   └── test/java/com/novagb/emulator/    # Unit tests CPU, Cartridge MBC, Timer
│   └── build.gradle.kts                      # Dependencies & Compose configuration
├── gradle/
│   ├── libs.versions.toml                    # Gradle Version Catalog
│   └── wrapper/gradle-wrapper.properties     # Gradle 8.7 wrapper
├── build.gradle.kts
├── settings.gradle.kts
├── ARCHITECTURE.md                           # Dokumentasi arsitektur internal
└── README.md
```

---

## 3. Konvensi Kode & Best Practices

1. **Efisiensi Core Emulasi**:
   - Hindari alokasi objek baru (`new` / GC allocations) di dalam loop panas emulasi (`step()`, `stepFrame()`, `renderScanline()`).
   - Gunakan tipe primitif (`Int`, `ByteArray`, `IntArray`, `ShortArray`) untuk buffer memori, register, dan framebuffer.
   - PPU merender langsung ke `IntArray(160 * 144)` berformat ARGB 8888 yang di-blit ke `Bitmap` tanpa konversi intermediate.

2. **UI & Jetpack Compose**:
   - Gunakan pola *Unidirectional Data Flow* (UDF): *state down, events up*.
   - Pisahkan logika berat emulasi ke thread terpisah (`Dispatchers.Default` atau dedicated thread), jangan pernah memblokir thread UI Compose.
   - Pertahankan estetika modern: OLED pure black (`#101114`), glowing accent cyan (`#00E5FF`), frosted glass touch buttons, dan respons taktil haptic feedback.

3. **Kompatibilitas Gamepad**:
   - Setiap tombol baru pada kontroler harus dipetakan ke `mapKeyCodeToJoypad` di `MainActivity.kt` untuk memastikan controller Bluetooth/USB fisik (Xbox, PS4/PS5, 8BitDo) langsung berfungsi.

---

## 4. Konvensi Git Commit

- Selalu gunakan konvensi **Conventional Commits**:
  - `feat(...)`: Fitur baru.
  - `fix(...)`: Perbaikan bug.
  - `refactor(...)`: Perubahan struktur kode tanpa mengubah fungsionalitas.
  - `test(...)`: Menambah atau memperbarui unit test.
  - `docs(...)`: Dokumentasi (README, ARCHITECTURE, AGENTS).
  - `chore(...)`: Perubahan konfigurasi, build script, dependensi.
- **PENTING**: Jangan pernah mencantumkan nama agen, bot, atau AI di dalam pesan commit. Gunakan author default Git lokal pengguna (`Akhdan Rafif Nugraha`).

---

## 5. Perintah Pengujian & Pembangunan

```bash
# Menjalankan seluruh Unit Tests
./gradlew test

# Membangun APK Debug
./gradlew assembleDebug

# Memasang APK ke HP / Emulator Android
./gradlew installDebug

# Membersihkan direktori build
./gradlew clean
```

---

## 6. Saluran Notifikasi Proyek

Ketika menjalankan tugas otomatis yang panjang atau butuh konfirmasi:
- Kirim notifikasi HTTP POST ke: `https://ntfy.sh/home_cdx_gb`
- Cantumkan status tugas yang telah selesai serta sisa tugas yang perlu dikerjakan.