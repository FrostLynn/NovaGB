import math
from PIL import Image, ImageDraw, ImageFont, ImageFilter

W, H = 585, 1280

FONT_REG = "/mnt/c/Windows/Fonts/segoeui.ttf"
FONT_BOLD = "/mnt/c/Windows/Fonts/segoeuib.ttf"
FONT_ARIAL_BOLD = "/mnt/c/Windows/Fonts/arialbd.ttf"
FONT_ARIAL_BOLD_ITALIC = "/mnt/c/Windows/Fonts/arialbi.ttf"
FONT_ARIAL_ITALIC = "/mnt/c/Windows/Fonts/ariali.ttf"

def get_font(path, size):
    try:
        return ImageFont.truetype(path, size)
    except Exception:
        return ImageFont.load_default()

# Preload fonts
f_title = get_font(FONT_BOLD, 22)
f_badge = get_font(FONT_BOLD, 17)
f_bezel_top = get_font(FONT_BOLD, 12)
f_led = get_font(FONT_BOLD, 11)
f_gameboy = get_font(FONT_ARIAL_BOLD_ITALIC, 24)
f_tm = get_font(FONT_ARIAL_BOLD_ITALIC, 13)
f_pill = get_font(FONT_BOLD, 17)
f_arrow = get_font(FONT_BOLD, 24)
f_action = get_font(FONT_BOLD, 28)
f_turbo = get_font(FONT_BOLD, 17)
f_dmg_label = get_font(FONT_ARIAL_BOLD_ITALIC, 21)
f_dmg_pill_label = get_font(FONT_ARIAL_BOLD_ITALIC, 16)

# Load game screen
try:
    game_screen = Image.open("/tmp/extracted_game_screen.png").convert("RGBA")
except Exception:
    game_screen = Image.new("RGBA", (430, 387), (155, 188, 15, 255))

# Scale game screen to fit neatly in bezel (width ~420, height ~378)
GW, GH = 420, 378
game_screen_scaled = game_screen.resize((GW, GH), Image.Resampling.LANCZOS)

# ----------------------------------------------------
# 1. GENERATE MODERN DARK THEME SCREENSHOT
# ----------------------------------------------------
def generate_modern():
    img = Image.new("RGBA", (W, H), (12, 13, 16, 255))
    draw = ImageDraw.Draw(img)

    # Top App Header
    # Left: POKEMON RED
    draw.text((28, 38), "POKEMON RED", font=f_title, fill=(208, 213, 224, 255))
    # Right: 2X FAST (pink) and 119 FPS (cyan)
    draw.text((400, 42), "2X FAST", font=f_badge, fill=(255, 42, 109, 255))
    draw.text((485, 42), "119 FPS", font=f_badge, fill=(0, 229, 255, 255))

    # RetroDisplay Bezel
    bx, by, bw, bh = 24, 85, 537, 490
    # Outer Bezel
    draw.rounded_rectangle([bx, by, bx + bw, by + bh], radius=24, fill=(26, 29, 36, 255), outline=(42, 46, 56, 255), width=2)

    # Bezel Top Line: Accent stripes and "DOT MATRIX WITH STEREO SOUND"
    top_y = by + 22
    # Left red stripe
    draw.line([(bx + 30, top_y), (bx + 115, top_y)], fill=(195, 30, 60, 255), width=3)
    # Center text
    draw.text((bx + 130, top_y - 8), "DOT MATRIX WITH STEREO SOUND", font=f_bezel_top, fill=(142, 149, 165, 255))
    # Right blue stripe
    draw.line([(bx + 380, top_y), (bx + 505, top_y)], fill=(30, 80, 180, 255), width=3)

    # Battery LED
    led_x, led_y = bx + 36, by + 180
    draw.ellipse([led_x - 6, led_y - 6, led_x + 6, led_y + 6], fill=(240, 30, 30, 255), outline=(120, 0, 0, 255), width=1)
    draw.text((led_x - 18, led_y + 12), "BATTERY", font=f_led, fill=(142, 149, 165, 255))

    # Place LCD Game Screen
    gx = bx + (bw - GW) // 2 + 10
    gy = by + 52
    # Draw screen border / shadow
    draw.rectangle([gx - 2, gy - 2, gx + GW + 2, gy + GH + 2], fill=(10, 15, 10, 255))
    img.paste(game_screen_scaled, (gx, gy), game_screen_scaled)

    # Bezel Bottom Text: clean GAME BOY™ (NO Nintendo!)
    bot_y = by + bh - 38
    draw.text((bx + 38, bot_y), "GAME BOY", font=f_gameboy, fill=(208, 213, 224, 255))
    draw.text((bx + 185, bot_y + 2), "TM", font=f_tm, fill=(208, 213, 224, 255))

    # Touch Controller Section
    ctrl_y = 610

    # Top Row: MENU pill & 2X TURBO pill
    # MENU Pill
    draw.rounded_rectangle([32, ctrl_y, 140, ctrl_y + 44], radius=22, fill=(36, 39, 49, 255), outline=(59, 64, 78, 255), width=2)
    draw.text((60, ctrl_y + 10), "MENU", font=f_pill, fill=(208, 213, 224, 255))

    # 2X TURBO Pill (Active Cyan)
    draw.rounded_rectangle([W - 176, ctrl_y, W - 32, ctrl_y + 44], radius=22, fill=(0, 229, 255, 255), outline=(0, 229, 255, 255), width=2)
    draw.text((W - 160, ctrl_y + 10), "2X TURBO", font=f_pill, fill=(0, 0, 0, 255))

    # Middle Row: D-Pad & Action Buttons
    mid_y = ctrl_y + 80

    # Modern Radial D-Pad
    dpad_cx, dpad_cy = 135, mid_y + 130
    dpad_r = 105
    # Outer circle
    draw.ellipse([dpad_cx - dpad_r, dpad_cy - dpad_r, dpad_cx + dpad_r, dpad_cy + dpad_r], fill=(28, 30, 38, 255), outline=(59, 64, 78, 255), width=3)
    # Center circle
    draw.ellipse([dpad_cx - 35, dpad_cy - 35, dpad_cx + 35, dpad_cy + 35], fill=(20, 22, 28, 255), outline=(50, 55, 68, 255), width=2)
    # Arrows
    draw.text((dpad_cx - 10, dpad_cy - 85), "▲", font=f_arrow, fill=(142, 149, 165, 255))
    draw.text((dpad_cx - 10, dpad_cy + 55), "▼", font=f_arrow, fill=(142, 149, 165, 255))
    draw.text((dpad_cx - 80, dpad_cy - 16), "◀", font=f_arrow, fill=(142, 149, 165, 255))
    draw.text((dpad_cx + 60, dpad_cy - 16), "▶", font=f_arrow, fill=(142, 149, 165, 255))

    # Right: Action Buttons Group
    act_cx, act_cy = 445, mid_y + 130

    # Turbo Buttons Row (TB, TA)
    tb_cx, tb_cy = act_cx - 50, act_cy - 85
    ta_cx, ta_cy = act_cx + 35, act_cy - 85
    # TB
    draw.ellipse([tb_cx - 28, tb_cy - 28, tb_cx + 28, tb_cy + 28], fill=(30, 33, 41, 255), outline=(255, 42, 109, 180), width=2)
    draw.text((tb_cx - 12, tb_cy - 11), "TB", font=f_turbo, fill=(255, 92, 138, 255))
    # TA
    draw.ellipse([ta_cx - 28, ta_cy - 28, ta_cx + 28, ta_cy + 28], fill=(30, 33, 41, 255), outline=(0, 229, 255, 180), width=2)
    draw.text((ta_cx - 12, ta_cy - 11), "TA", font=f_turbo, fill=(0, 229, 255, 255))

    # Main Action Buttons (B, A) in diagonal offset
    b_cx, b_cy = act_cx - 55, act_cy + 25
    a_cx, a_cy = act_cx + 38, act_cy - 15
    # Button B (Magenta)
    draw.ellipse([b_cx - 36, b_cy - 36, b_cx + 36, b_cy + 36], fill=(34, 37, 46, 255), outline=(255, 42, 109, 220), width=3)
    draw.text((b_cx - 10, b_cy - 18), "B", font=f_action, fill=(255, 42, 109, 255))
    # Button A (Cyan)
    draw.ellipse([a_cx - 36, a_cy - 36, a_cx + 36, a_cy + 36], fill=(34, 37, 46, 255), outline=(0, 229, 255, 220), width=3)
    draw.text((a_cx - 10, a_cy - 18), "A", font=f_action, fill=(0, 229, 255, 255))

    # Bottom Row: SELECT & START Pills
    bot_pill_y = ctrl_y + 440
    # SELECT
    draw.rounded_rectangle([155, bot_pill_y, 255, bot_pill_y + 42], radius=21, fill=(36, 39, 49, 255), outline=(59, 64, 78, 255), width=2)
    draw.text((178, bot_pill_y + 9), "SELECT", font=f_pill, fill=(208, 213, 224, 255))
    # START
    draw.rounded_rectangle([325, bot_pill_y, 425, bot_pill_y + 42], radius=21, fill=(36, 39, 49, 255), outline=(59, 64, 78, 255), width=2)
    draw.text((352, bot_pill_y + 9), "START", font=f_pill, fill=(208, 213, 224, 255))

    return img.convert("RGB")

# ----------------------------------------------------
# 2. GENERATE CLASSIC DMG-01 THEME SCREENSHOT
# ----------------------------------------------------
def generate_dmg():
    # Warm gray ABS plastic background
    img = Image.new("RGBA", (W, H), (200, 202, 204, 255))
    draw = ImageDraw.Draw(img)

    # Top groove accent line
    draw.rectangle([0, 20, W, 24], fill=(176, 179, 186, 255))
    draw.line([(0, 25), (W, 25)], fill=(225, 228, 232, 255), width=1)

    # Top Header
    draw.text((28, 42), "POKEMON RED", font=f_title, fill=(30, 36, 56, 255))
    draw.text((495, 46), "60 FPS", font=f_badge, fill=(15, 32, 90, 255))

    # RetroDisplay Bezel (Slate Gray #525660)
    bx, by, bw, bh = 24, 85, 537, 490
    draw.rounded_rectangle([bx, by, bx + bw, by + bh], radius=24, fill=(82, 86, 96, 255), outline=(62, 65, 72, 255), width=2)

    # Bezel Top Line: Accent stripes and "DOT MATRIX WITH STEREO SOUND"
    top_y = by + 22
    draw.line([(bx + 30, top_y), (bx + 115, top_y)], fill=(180, 25, 50, 255), width=3)
    draw.text((bx + 130, top_y - 8), "DOT MATRIX WITH STEREO SOUND", font=f_bezel_top, fill=(190, 195, 205, 255))
    draw.line([(bx + 380, top_y), (bx + 505, top_y)], fill=(25, 60, 140, 255), width=3)

    # Battery LED
    led_x, led_y = bx + 36, by + 180
    draw.ellipse([led_x - 6, led_y - 6, led_x + 6, led_y + 6], fill=(220, 30, 30, 255), outline=(100, 0, 0, 255), width=1)
    draw.text((led_x - 18, led_y + 12), "BATTERY", font=f_led, fill=(190, 195, 205, 255))

    # Place LCD Game Screen
    gx = bx + (bw - GW) // 2 + 10
    gy = by + 52
    draw.rectangle([gx - 2, gy - 2, gx + GW + 2, gy + GH + 2], fill=(30, 35, 30, 255))
    img.paste(game_screen_scaled, (gx, gy), game_screen_scaled)

    # Bezel Bottom Text: Clean "GAME BOY™" in dark navy (NO Nintendo badge!)
    bot_y = by + bh - 38
    draw.text((bx + 38, bot_y), "GAME BOY", font=f_gameboy, fill=(16, 32, 85, 255))
    draw.text((bx + 185, bot_y + 2), "TM", font=f_tm, fill=(16, 32, 85, 255))

    # Touch Controller Section (Classic DMG-01)
    ctrl_y = 610

    # Top Row: Utility buttons (MENU and 1X PLAY)
    # MENU
    draw.rounded_rectangle([32, ctrl_y, 125, ctrl_y + 40], radius=10, fill=(86, 88, 96, 255), outline=(62, 64, 72, 255), width=2)
    draw.text((54, ctrl_y + 9), "MENU", font=f_pill, fill=(226, 228, 232, 255))

    # 1X PLAY
    draw.rounded_rectangle([W - 145, ctrl_y, W - 32, ctrl_y + 40], radius=10, fill=(86, 88, 96, 255), outline=(62, 64, 72, 255), width=2)
    draw.text((W - 130, ctrl_y + 9), "1X PLAY", font=f_pill, fill=(226, 228, 232, 255))

    # Middle Row: Black Cross D-Pad on Left & Action Buttons on Right
    mid_y = ctrl_y + 80

    # Authentic Classic DMG Black Cross D-Pad
    dpad_cx, dpad_cy = 135, mid_y + 130
    # Outer recessed circular well
    draw.ellipse([dpad_cx - 105, dpad_cy - 105, dpad_cx + 105, dpad_cy + 105], fill=(184, 186, 192, 255), outline=(160, 163, 171, 255), width=3)
    # Horizontal cross bar
    hw, hh = 90, 30
    draw.rounded_rectangle([dpad_cx - hw, dpad_cy - hh, dpad_cx + hw, dpad_cy + hh], radius=6, fill=(24, 25, 30, 255), outline=(46, 49, 58, 255), width=2)
    # Vertical cross bar
    draw.rounded_rectangle([dpad_cx - hh, dpad_cy - hw, dpad_cx + hh, dpad_cy + hw], radius=6, fill=(24, 25, 30, 255), outline=(46, 49, 58, 255), width=2)
    # Center concave circular thumb rest
    draw.ellipse([dpad_cx - 28, dpad_cy - 28, dpad_cx + 28, dpad_cy + 28], fill=(16, 17, 20, 255), outline=(40, 42, 51, 255), width=2)
    # Embossed arrow lines
    draw.text((dpad_cx - 8, dpad_cy - 78), "▲", font=f_arrow, fill=(62, 65, 75, 255))
    draw.text((dpad_cx - 8, dpad_cy + 50), "▼", font=f_arrow, fill=(62, 65, 75, 255))
    draw.text((dpad_cx - 75, dpad_cy - 16), "◀", font=f_arrow, fill=(62, 65, 75, 255))
    draw.text((dpad_cx + 56, dpad_cy - 16), "▶", font=f_arrow, fill=(62, 65, 75, 255))

    # Right: Action Buttons (Angled Capsule Recess + Magenta Buttons + Turbo TB/TA)
    act_cx, act_cy = 445, mid_y + 130

    # Turbo Buttons (TB, TA) above capsule
    tb_cx, tb_cy = act_cx - 45, act_cy - 92
    ta_cx, ta_cy = act_cx + 35, act_cy - 92
    draw.ellipse([tb_cx - 26, tb_cy - 26, tb_cx + 26, tb_cy + 26], fill=(136, 21, 58, 255), outline=(100, 10, 38, 255), width=2)
    draw.text((tb_cx - 11, tb_cy - 10), "TB", font=f_turbo, fill=(255, 255, 255, 255))
    draw.ellipse([ta_cx - 26, ta_cy - 26, ta_cx + 26, ta_cy + 26], fill=(136, 21, 58, 255), outline=(100, 10, 38, 255), width=2)
    draw.text((ta_cx - 11, ta_cy - 10), "TA", font=f_turbo, fill=(255, 255, 255, 255))

    # Angled Capsule Recess (Rotated -26 degrees)
    # We create a layer for the capsule, rotate it, and paste
    capsule_layer = Image.new("RGBA", (220, 120), (0, 0, 0, 0))
    c_draw = ImageDraw.Draw(capsule_layer)
    c_draw.rounded_rectangle([20, 20, 200, 100], radius=40, fill=(184, 186, 192, 255), outline=(160, 163, 171, 255), width=2)
    rotated_capsule = capsule_layer.rotate(26, resample=Image.Resampling.BICUBIC)
    img.paste(rotated_capsule, (act_cx - 110, act_cy - 48), rotated_capsule)

    # Magenta Action Buttons B and A (Calibrated offsets: B at -46x, +32y; A at +46x, -12y)
    b_cx, b_cy = act_cx - 46, act_cy + 20
    a_cx, a_cy = act_cx + 42, act_cy - 20
    # Button B
    draw.ellipse([b_cx - 34, b_cy - 34, b_cx + 34, b_cy + 34], fill=(158, 22, 68, 255), outline=(110, 10, 44, 255), width=2)
    # Button A
    draw.ellipse([a_cx - 34, a_cy - 34, a_cx + 34, a_cy + 34], fill=(158, 22, 68, 255), outline=(110, 10, 44, 255), width=2)

    # Labels B and A in dark navy, italic bold, shifted +10px to the right!
    draw.text((b_cx + 8, b_cy + 42), "B", font=f_dmg_label, fill=(15, 32, 90, 255))
    draw.text((a_cx + 8, a_cy + 42), "A", font=f_dmg_label, fill=(15, 32, 90, 255))

    # Bottom Row: SELECT & START (Angled Rubber Pills)
    bot_pill_y = ctrl_y + 440
    # Create layer for slanted pills
    pill_layer = Image.new("RGBA", (90, 50), (0, 0, 0, 0))
    p_draw = ImageDraw.Draw(pill_layer)
    p_draw.rounded_rectangle([10, 15, 80, 35], radius=10, fill=(90, 92, 100, 255), outline=(66, 68, 76, 255), width=2)
    slanted_pill = pill_layer.rotate(26, resample=Image.Resampling.BICUBIC)

    # Paste SELECT pill
    img.paste(slanted_pill, (170, bot_pill_y - 10), slanted_pill)
    draw.text((180, bot_pill_y + 36), "SELECT", font=f_dmg_pill_label, fill=(15, 32, 90, 255))

    # Paste START pill
    img.paste(slanted_pill, (300, bot_pill_y - 10), slanted_pill)
    draw.text((315, bot_pill_y + 36), "START", font=f_dmg_pill_label, fill=(15, 32, 90, 255))

    # Speaker Grille (6 diagonal slots at bottom-right)
    grille_x, grille_y = W - 110, H - 90
    for i in range(6):
        gx = grille_x + i * 14
        gy = grille_y
        draw.line([(gx, gy + 35), (gx + 18, gy)], fill=(40, 43, 51, 255), width=4)

    return img.convert("RGB")

# Generate and save
print("Generating modern theme screenshot...")
modern_img = generate_modern()
modern_img.save("/home/akhdan/Projects/NovaGB/docs/screenshots/gameplay_pokemon.jpg", quality=95)

print("Generating classic DMG theme screenshot...")
dmg_img = generate_dmg()
dmg_img.save("/home/akhdan/Projects/NovaGB/docs/screenshots/classic_dmg_theme.jpg", quality=95)

print("Screenshots successfully generated and saved to docs/screenshots/")
