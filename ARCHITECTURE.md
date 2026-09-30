# NovaGB Internal Architecture & Design Specification

Dokumen ini mendokumentasikan spesifikasi arsitektur internal dari **NovaGB** untuk pengembang yang ingin memahami atau memodifikasi pipeline emulasi.

---

## 1. Diagram Alur Sistem

```text
┌────────────────────────────────────────────────────────┐
│                   Android Activity                     │
│  - Edge-to-Edge UI Layout   - Hardware KeyEvent/Joypad │
│  - SAF File Picker Intent   - Intent View Data         │
└───────────────────────────┬────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│                Jetpack Compose Layer                   │
│  - LibraryScreen (Dashboard, Search, Cover Cards)      │
│  - EmulatorScreen (RetroDisplay Canvas, Modal Sheet)   │
│  - TouchController (Gesture Detection, Haptic Vibrator)│
└───────────────────────────┬────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│              Emulation GameLoop (Coroutine)            │
│  - Target 16.66 ms (60 FPS) / Turbo Divider            │
│  - Step 70,224 Clock Cycles per Frame                  │
└───────────────────────────┬────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│                 GameBoy Master Container               │
│  ┌──────────────┐   ┌──────────────┐   ┌────────────┐  │
│  │     CPU      │◄──┤     MMU      ├──►│ Cartridge  │  │
│  │ (LR35902)    │   │  (Bus/RAM)   │   │  (MBC1-5)  │  │
│  └──────┬───────┘   └──────┬───────┘   └────────────┘  │
│         │                  │                           │
│  ┌──────▼───────┐   ┌──────▼───────┐   ┌────────────┐  │
│  │     PPU      │   │     APU      │   │   Timer    │  │
│  │ (160x144 FB) │   │ (4-Ch Audio) │   │  & Joypad  │  │
│  └──────────────┘   └──────┬───────┘   └────────────┘  │
└────────────────────────────┼───────────────────────────┘
                             │ Audio PCM Samples
┌────────────────────────────▼───────────────────────────┐
│                    GbAudioPlayer                       │
│  - Android AudioTrack (Low-Latency Stream, 44.1 kHz)   │
└────────────────────────────────────────────────────────┘
```

---

## 2. Subsistem Utama

### 2.1. CPU (Sharp LR35902)
- **Clock**: 4.194304 MHz (T-cycles, per instruction 4, 8, 12, 16, 20, atau 24 cycles).
- **Registers**: `A`, `F`, `B`, `C`, `D`, `E`, `H`, `L`, `SP` (0xFFFE), `PC` (0x0100).
- **Interrupts**:
  1. `0x0040`: V-Blank (Prioritas tertinggi, bit 0)
  2. `0x0048`: LCD STAT (bit 1)
  3. `0x0050`: Timer Overflow (bit 2)
  4. `0x0058`: Serial Transfer (bit 3)
  5. `0x0060`: Joypad Press (bit 4)
- **Penanganan HALT**: Saat instruksi `HALT` aktif, CPU menunggu sampai ada bit pending di `(IF & IE & 0x1F)`.

### 2.2. Memory Management Unit (MMU) & Mappers
- **Peta Memori 64KB**:
  - `0x0000 - 0x3FFF`: ROM Bank 0 (16KB)
  - `0x4000 - 0x7FFF`: Switchable ROM Bank (16KB)
  - `0x8000 - 0x9FFF`: Video RAM / VRAM (8KB)
  - `0xA000 - 0xBFFF`: External Cartridge RAM (8KB)
  - `0xC000 - 0xDFFF`: Work RAM / WRAM (8KB)
  - `0xE000 - 0xFDFF`: Echo RAM (Mirror dari WRAM)
  - `0xFE00 - 0xFE9F`: OAM Sprite Table (160 bytes)
  - `0xFF00 - 0xFF7F`: I/O Registers (LCDC, STAT, SCX, SCY, BGP, NRxx)
  - `0xFF80 - 0xFFFE`: High RAM / HRAM (127 bytes)
  - `0xFFFF`: Interrupt Enable Register (IE)
- **OAM DMA (0xFF46)**: Transfer cepat 160 byte dari area ROM/RAM ke OAM.

### 2.3. PPU (Pixel Processing Unit)
- Menghasilkan 154 scanline per frame (144 aktif + 10 V-Blank).
- Setiap scanline memerlukan 456 cycles:
  - **Mode 2 (OAM Search)**: 80 cycles (mencari maksimal 10 sprite pada scanline).
  - **Mode 3 (Pixel Transfer)**: 172 cycles (merender baris background, window, dan sprite ke framebuffer).
  - **Mode 0 (H-Blank)**: 204 cycles (jeda sebelum baris berikutnya).
- Framebuffer dikirim ke layer Jetpack Compose melalui array `IntArray(160 * 144)` berformat ARGB 8888.

### 2.4. APU (Audio Processing Unit)
- Beroperasi pada 4-channel sound synthesizer:
  - **Channel 1**: Square Wave dengan Frekuensi Sweep & Volume Envelope.
  - **Channel 2**: Square Wave dengan Volume Envelope.
  - **Channel 3**: Custom Wave Pattern (32 sample 4-bit di 0xFF30-0xFF3F).
  - **Channel 4**: Pseudo-random White Noise Generator (LFSR 7-bit/15-bit).
- **Frame Sequencer 512 Hz**: Melakukan clocking terhadap length counter (256 Hz), sweep (128 Hz), dan volume envelope (64 Hz).
- Keluaran disample ke 44.1 kHz stereo 16-bit PCM dan dialirkan langsung ke Android `AudioTrack` secara non-blocking.

---

## 3. Integrasi Alternatif Native C++ (Gambatte / SameBoy)

Jika ingin mengganti core Kotlin dengan Gambatte atau SameBoy:
1. Letakkan kode sumber C++ core ke dalam folder `app/src/main/cpp/cores/`.
2. Daftarkan source files ke `app/src/main/cpp/CMakeLists.txt`.
3. Panggil metode native lewat `com.novagb.emulator.core.NativeGbBridge`.