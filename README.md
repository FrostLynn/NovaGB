# NovaGB - Modern Game Boy Emulator for Android

NovaGB adalah emulator Game Boy (DMG) dan Game Boy Color (CGB) modern berkinerja tinggi untuk platform Android yang dibangun dengan **Kotlin murni**, **Jetpack Compose**, dan **Material 3**. Proyek ini juga dilengkapi dengan arsitektur bridge C++/NDK yang siap digunakan untuk adaptasi core native seperti Gambatte atau SameBoy.

---

## Fitur Utama

- **Core Emulation Akurat**:
  - Emulasi CPU Sharp LR35902 (Z80 hybrid) 4.194304 MHz lengkap dengan semua opcode standar (0x00-0xFF) dan opcode CB-prefixed.
  - PPU Scanline Renderer (160×144) dengan dukungan Background tile mapping, Window tile layer, dan 40 OAM sprites (mode 8×8 & 8×16) berprioritas.
  - APU Audio 4-Channel: Pulse 1 (Sweep/Duty), Pulse 2 (Duty), Channel 3 (Wave RAM 32-sample), dan Channel 4 (Noise LFSR) dengan mixer stereo PCM 44.1 kHz.
  - Timer hardware lengkap (DIV, TIMA, TMA, TAC) dan Joypad matrix (0xFF00) dengan interrupt trigger.
  - Cartridge Mapper: ROM Only, MBC1 (16Mbit ROM / 32KByte RAM), MBC2 (512x4 RAM), MBC3 (Timer RTC latched), dan MBC5 (hingga 8MB ROM / 128KB RAM).
  - Battery Save (`.sav`) & Snapshot Save States (`.state`).

- **Antarmuka Modern (Jetpack Compose & Material 3)**:
  - Desain OLED Dark Theme dengan aksen Electric Cyan dan Neon Magenta.
  - Edge-to-edge layout dengan status bar dan navigation bar adaptif.
  - ROM Library Dashboard dengan Card metadata, pencarian cepat, waktu bermain, dan FAB impor berkas menggunakan Storage Access Framework (SAF).
  - Built-in ROM Demo interaktif (`sample.gb`) di folder assets untuk langsung dimainkan tanpa perlu mengunduh ROM terpisah.

- **Display & Retro Shaders**:
  - Mode Aspek Rasio: Original 10:9, Integer Scale 3x, Fit Screen, dan Stretch Full.
  - Shader Grid Dot-Matrix LCD khas Game Boy asli.
  - Filter garis pindaian (CRT Scanlines).
  - Palet Warna Realtime:
    - **DMG Classic**: Nuansa pea-soup green ikonik (`#0F380F` - `#9BBC0F`).
    - **Pocket Gray**: Hitam putih tajam Game Boy Pocket.
    - **Game Boy Light**: Teal backlight indiglo.
    - **Cyberpunk Neon**: Estetika modern berani.
    - **Golden Amber**: Nuansa retro monokrom hangat.

- **Kontrol Sentuh & Gamepad Eksternal**:
  - On-screen touch controller dengan tampilan frosted glass elegan dan opasitas yang dapat diatur.
  - D-pad radial dengan deteksi gestur swipe 8-arah halus.
  - Tombol A, B, Select, Start, dan tombol Turbo.
  - Umpan balik getaran taktil (**Haptic Feedback**) menggunakan Android `VibrationEffect`.
  - Dukungan gamepad nirkabel Bluetooth dan USB (Xbox, PS4/PS5, 8BitDo) secara otomatis melalui key event dan thumbstick joystick.

- **Adaptasi Native Core (C++/NDK)**:
  - Tersedia `app/src/main/cpp/CMakeLists.txt` dan `native-lib.cpp` beserta wrapper Kotlin `NativeGbBridge.kt` bagi yang ingin menghubungkan langsung source code C/C++ Gambatte atau SameBoy.

---

## Struktur Direktori

```text
D:\Projects\GB\
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── assets/games/sample.gb       # Bundled demo Game Boy ROM
│   │   │   ├── cpp/                         # CMake & C++ JNI bridge (Gambatte/SameBoy)
│   │   │   ├── java/com/novagb/emulator/
│   │   │   │   ├── audio/                   # GbAudioPlayer (AudioTrack streaming)
│   │   │   │   ├── core/                    # Cpu, Mmu, Cartridge, Ppu, Apu, Joypad, Timer, GameBoy
│   │   │   │   ├── data/                    # AppSettings, RomMetadata, RomRepository
│   │   │   │   ├── ui/
│   │   │   │   │   ├── components/          # RetroDisplay, TouchController
│   │   │   │   │   ├── screens/             # LibraryScreen, EmulatorScreen, SettingsScreen
│   │   │   │   │   └── theme/               # Color, Theme, Type
│   │   │   │   └── MainActivity.kt          # Compose Navigation & Gamepad Input
│   │   │   ├── res/                         # Strings, colors, vector drawables
│   │   │   └── AndroidManifest.xml
│   │   └── test/java/com/novagb/emulator/   # Unit tests (CpuTest, CartridgeTest)
│   └── build.gradle.kts
├── gradle/
│   ├── libs.versions.toml                   # Gradle Version Catalog
│   └── wrapper/gradle-wrapper.properties
├── build.gradle.kts
├── settings.gradle.kts
├── ARCHITECTURE.md                          # Dokumentasi arsitektur internal
└── README.md
```

---

## Cara Menjalankan & Membangun APK

### 1. Menggunakan Android Studio
1. Buka **Android Studio** (versi Koala / Ladybug atau yang lebih baru).
2. Pilih **Open** dan arahkan ke folder `D:\Projects\GB`.
3. Tunggu Gradle sync selesai.
4. Hubungkan perangkat Android fisik atau jalankan Android Emulator.
5. Klik tombol **Run  app** (`Shift + F10`).

### 2. Menggunakan Command Line (Gradle)
Pastikan `JAVA_HOME` mengarah ke JDK 17 atau yang lebih baru:

```bash
# Menjalankan unit tests
./gradlew test

# Membangun APK Debug
./gradlew assembleDebug

# Output APK tersimpan di:
# app/build/outputs/apk/debug/app-debug.apk

# Menginstal langsung ke HP Android yang terhubung via ADB:
./gradlew installDebug
```

---

## Kontrol Default

| Tombol Game Boy | Layar Sentuh | Keyboard | Gamepad Bluetooth / USB |
| :--- | :--- | :--- | :--- |
| **D-Pad Up** | D-Pad Atas | W / Panah Atas | D-Pad Atas / Analog Kiri |
| **D-Pad Down** | D-Pad Bawah | S / Panah Bawah | D-Pad Bawah / Analog Kiri |
| **D-Pad Left** | D-Pad Kiri | A / Panah Kiri | D-Pad Kiri / Analog Kiri |
| **D-Pad Right** | D-Pad Kanan | D / Panah Kanan | D-Pad Kanan / Analog Kiri |
| **Tombol A** | Tombol Cyan A | K | Tombol A / Cross |
| **Tombol B** | Tombol Magenta B | J | Tombol B / Circle |
| **Select** | Tombol SELECT | Spasi | Tombol Back / Select |
| **Start** | Tombol START | Enter | Tombol Start |
| **Fast Forward** | Tombol Turbo | - | Right Bumper (R1) |
| **Quick Menu** | Tombol MENU | Escape | Home / Guide |

---

## Lisensi
Proyek ini dibuat untuk tujuan edukasi dan emulasi open-source. Bebas dikembangkan dan dimodifikasi lebih lanjut.