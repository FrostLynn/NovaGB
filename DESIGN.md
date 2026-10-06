# DESIGN.md — NovaGB Design Specification

## 1. Identity & Overview
- **Product**: NovaGB (Game Boy & Game Boy Color Emulator for Android)
- **Design Language**: Modern Retro OLED Handheld
- **Audience**: Retro gaming enthusiasts, homebrew developers, and mobile gamers seeking high-performance, accurate Game Boy emulation.
- **Dial Declaration**: `ENERGY 2 / RHYTHM 2 / MOTION 1`
  - **ENERGY 2 (Balanced)**: Deep OLED black canvas with disciplined Electric Cyan and Neon Magenta accents; immersive and distraction-free during active gameplay.
  - **RHYTHM 2 (Structured with Intentional Breaks)**: Clean grouped cards and filtering in the library and settings dashboards, transitioning into an authentic retro console bezel and ergonomic frosted controls during emulation.
  - **MOTION 1 (Action-Driven)**: Strict interaction-based tactile feedback (instant haptics, tap highlights, smooth sheet modals, button press scale) with zero perpetual or decorative animation loops.

---

## 2. Color System
- **Core Neutral Base (OLED Pure Black Palette)**:
  - `DarkBackground`: `#101114` (Deep OLED black background)
  - `DarkSurface`: `#181A22` (Card and modal container surface)
  - `DarkSurfaceVariant`: `#222632` (Interactive buttons, elevated containers, input fields)
  - `DarkSurfaceSubtle`: `#14161C` (Recessed slot containers and screen bezel)
  - `DarkBorder`: `#2C313E` (Primary boundary border)
  - `DarkBorderSubtle`: `#232733` (Secondary boundary border and dividers)
- **Core Accent Palette**:
  - `AccentPrimary`: `#00E5FF` (Electric Cyan — primary actions, active tabs, focus highlights)
  - `AccentSecondary`: `#FF2A6D` (Neon Magenta — Game Boy Color branding, B button)
  - `AccentSuccess` / `AccentTertiary`: `#05FFA1` (Neon Mint — load state confirmation, success indicators)
  - `AccentDanger`: `#FF4848` (Crimson Red — deletion confirmations, core reset actions)
  - `AccentAmber`: `#FFC077` (Warm retro amber — DMG badges)
- **Hardware Authenticity Palette**:
  - `DmgShellBody`: `#C8CACC` (Classic light gray ABS body)
  - `DmgShellBezel`: `#525660` (DMG bezel gray)
  - `DmgBezelDark`: `#262933` (OLED dark console bezel)
  - `DmgStripeMagenta`: `#882040` (Iconic magenta bezel stripe)
  - `DmgStripeIndigo`: `#202060` (Iconic indigo bezel stripe)
  - `DmgTextBlue`: `#102055` (Iconic navy branding text)
  - `LedRedActive`: `#FF3333` (Active power indicator core)
  - `LedRedGlow`: `rgba(255, 51, 51, 0.4)` (Diffuse 3mm LED halo)
- **Typography & Muted Tones (WCAG AA Compliant)**:
  - `TextPrimary`: `#FFFFFF` / `#F1F3F7` (Contrast > 15:1 against dark surfaces)
  - `TextSecondary`: `#9DA3AF` (Contrast > 7:1 against dark surfaces)
  - `TextMuted`: `#8E95A5` (Contrast > 5.2:1 against dark surfaces)

---

## 3. Typography & Hierarchy
- **Display / Headers**: Bold, sentence-case or subtle tracked micro-labels for retro branding (`DOT MATRIX WITH STEREO SOUND`, `READY TO PLAY`, `DMG • CGB`).
- **Body & Data**: Clean sans-serif with proportional spacing. ROM sizes and timestamps formatted concisely (`KB`, `HH:mm`).
- **Cartridge Labels**: Monogram/initial badges paired with micro console indicators (`GAME BOY` vs `COLOR`).

---

## 4. Components & Physical Authenticity
- **Console Bezel**: Dual-tone magenta (`#882040`) and indigo (`#202060`) racing stripes framing the 160x144 viewport, paired with a glowing battery power LED (`#FF3333`) and diffuse halo.
- **Retro Cartridges**: Authentic top notch, embossed grip ridges, reflective stickers, and directional insertion arrow (`▲`).
- **Touch Controller**: Low-latency 8-direction D-pad, holdable tactile buttons, authentic slanted Start/Select pills (-25° angle), and accessible tap targets ($\ge 48\text{dp}$).
- **Safety Dialogs**: Explicit Material 3 confirmation dialogs before irreversible user actions (ROM library removal, in-game core reset).
- **Grouped Settings**: Settings items grouped into structured elevated cards with subtle dividers (`DarkBorderSubtle`), replacing scattered isolated boxes.
