# DESIGN.md — NovaGB Design Specification

## 1. Identity & Overview
- **Product**: NovaGB (Game Boy & Game Boy Color Emulator for Android)
- **Design Language**: Modern Retro OLED Handheld
- **Audience**: Retro gaming enthusiasts, homebrew developers, and mobile gamers seeking high-performance, accurate Game Boy emulation.
- **Dial Declaration**: `ENERGY 2 / RHYTHM 2 / MOTION 1`
  - **ENERGY 2 (Balanced)**: Deep OLED black canvas with disciplined Electric Cyan and Neon Magenta accents; immersive and distraction-free during active gameplay.
  - **RHYTHM 2 (Structured with Intentional Breaks)**: Clean cards and filtering in the library dashboard, transitioning into an authentic retro console bezel and ergonomic frosted controls during emulation.
  - **MOTION 1 (Action-Driven)**: Strict interaction-based feedback (instant haptics, tap highlights, smooth sheet modals) with zero perpetual or decorative animation loops.

---

## 2. Color System
- **Core Neutral Base**:
  - `DarkBackground`: `#101114` (Deep OLED black)
  - `DarkSurface`: `#181A22` (Card and modal container surface)
  - `DarkSurfaceVariant`: `#222632` (Interactive buttons and inputs)
  - `DarkBorder`: `#2C313E` (Subtle boundary borders)
- **Core Accent Palette**:
  - `AccentPrimary`: `#00E5FF` (Electric Cyan — primary actions, active tabs, focus)
  - `AccentSecondary`: `#FF2A6D` (Neon Magenta — Game Boy Color branding, B button)
  - `AccentSuccess`: `#05FFA1` (Neon Mint — load state confirmation, success indicators)
  - `AccentAmber`: `#FFC077` (Warm retro amber)
- **Typography & Muted Tones (WCAG AA Compliant)**:
  - `TextPrimary`: `#FFFFFF` / `#F1F3F7` (Contrast > 15:1 against surfaces)
  - `TextSecondary`: `#9DA3AF` (Contrast > 7:1 against surfaces)
  - `TextMuted`: `#8E95A5` (Contrast > 5.2:1 against dark surfaces)

---

## 3. Typography & Hierarchy
- **Display / Headers**: Bold, sentence-case or subtle tracked micro-labels for retro branding (`DOT MATRIX WITH STEREO SOUND`, `READY TO PLAY`).
- **Body & Data**: Clean sans-serif with proportional spacing. ROM sizes and timestamps formatted concisely (`KB`, `HH:mm`).
- **Cartridge Labels**: Monogram/initial badges paired with micro console indicators (`GAME BOY` vs `COLOR`).

---

## 4. Components & Physical Authenticity
- **Console Bezel**: Dual-tone magenta (`#882040`) and indigo (`#202060`) racing stripes framing the 160x144 viewport, paired with a glowing battery power LED (`#FF3333`).
- **Retro Cartridges**: Authentic top notch, side grip grooves, and reflective stickers for library items.
- **Touch Controller**: Low-latency 8-direction D-pad, holdable tactile buttons, and accessible tap targets ($\ge 46\text{dp}$).
