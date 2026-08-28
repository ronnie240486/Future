from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

root = Path('/home/ubuntu/Future')
background = Image.open(root / 'app/src/main/res/drawable-nodpi/future_home_clean_for_native_sidebar.png').convert('RGB')
canvas = background.resize((2048, 921), Image.Resampling.LANCZOS)
draw = ImageDraw.Draw(canvas, 'RGBA')

# Native panel proportions: broad enough for complete one-line labels,
# but still narrower than the approved orbital composition.
panel_w = 365
panel_h = 790
panel_top = 82
panel_left = 0
panel_right = panel_left + panel_w

# White curved panel, matching SidebarWhitePanelDrawable's asymmetrical edge.
mask = Image.new('L', canvas.size, 0)
md = ImageDraw.Draw(mask)
md.rounded_rectangle((panel_left - 12, panel_top - 12, panel_right - 35, panel_top + panel_h + 12), radius=34, fill=255)
# Pull the right edge inward at top/bottom and outward around the middle.
poly = [(0, panel_top), (panel_right - 65, panel_top), (panel_right - 8, panel_top + 120),
        (panel_right - 36, panel_top + panel_h // 2), (panel_right - 8, panel_top + panel_h - 120),
        (panel_right - 65, panel_top + panel_h), (0, panel_top + panel_h)]
md.polygon(poly, fill=255)
white_panel = Image.new('RGBA', canvas.size, (250, 251, 253, 0))
white_panel.putalpha(mask)
canvas = Image.alpha_composite(canvas.convert('RGBA'), white_panel)
draw = ImageDraw.Draw(canvas, 'RGBA')

# Fonts available in the base image environment.
font_regular = '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
font_bold = '/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf'
label_font = ImageFont.truetype(font_bold, 18)
clock_font = ImageFont.truetype(font_bold, 34)
date_font = ImageFont.truetype(font_regular, 17)
name_font = ImageFont.truetype(font_bold, 14)

# Header: avatar left; clock/date immediately in front/right of avatar.
avatar_box = (22, 12, 92, 82)
draw.ellipse(avatar_box, fill=(15, 31, 65, 255), outline=(255, 255, 255, 230), width=2)
draw.ellipse((31, 20, 83, 72), fill=(79, 118, 152, 200))
draw.text((108, 14), '00:43', font=clock_font, fill=(255, 255, 255, 255))
draw.text((110, 53), '28 Ago', font=date_font, fill=(220, 226, 237, 255))

labels = ['DORAMAS', 'NOVELAS TURCAS', 'NOVELAS', 'REELSHORTS', 'ANIMES']
icons = [
    root / 'app/src/main/res/drawable-nodpi/home_sidebar_icon_doramas.png',
    root / 'app/src/main/res/drawable-nodpi/home_sidebar_icon_turkish_novelas.png',
    root / 'app/src/main/res/drawable-nodpi/home_sidebar_icon_novelas.png',
    root / 'app/src/main/res/drawable-nodpi/home_sidebar_icon_reelshorts.png',
    root / 'app/src/main/res/drawable-nodpi/home_sidebar_icon_animes.png',
]
row_x = 24
row_w = 292
row_h = 86
gap = 16
start_y = 126
for i, (label, icon_path) in enumerate(zip(labels, icons)):
    y = start_y + i * (row_h + gap)
    # Dark capsule is a native row background over the white panel.
    draw.rounded_rectangle((row_x, y, row_x + row_w, y + row_h), radius=29,
                           fill=(12, 28, 63, 245), outline=(255, 255, 255, 255), width=2)
    icon = Image.open(icon_path).convert('RGBA')
    icon.thumbnail((55, 55), Image.Resampling.LANCZOS)
    ix = row_x + 16 + (55 - icon.width) // 2
    iy = y + (row_h - icon.height) // 2
    canvas.alpha_composite(icon, (ix, iy))
    # The whole label remains on one line; narrow only the long label.
    f = label_font
    if label == 'NOVELAS TURCAS':
        f = ImageFont.truetype(font_bold, 15)
    draw.text((row_x + 82, y + 30), label, font=f, fill=(255, 255, 255, 255))
    draw.text((row_x + row_w - 30, y + 26), '›', font=ImageFont.truetype(font_regular, 31), fill=(255, 255, 255, 255))

# Clean divider/edge on the white panel; no overlay dialogs.
draw.line((panel_right - 70, panel_top + 14, panel_right - 30, panel_top + 100), fill=(255, 255, 255, 255), width=3)
draw.line((panel_right - 30, panel_top + panel_h - 100, panel_right - 70, panel_top + panel_h - 14), fill=(255, 255, 255, 255), width=3)

out = root / 'preview/home_white_native_panel_approval.png'
canvas.convert('RGB').save(out, quality=95)
print(out)
