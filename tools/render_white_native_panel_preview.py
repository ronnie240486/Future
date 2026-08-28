from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

root = Path('/home/ubuntu/Future')
background = Image.open(root / 'app/src/main/res/drawable-nodpi/future_home_clean_for_native_sidebar.png').convert('RGB')
canvas = background.resize((2048, 921), Image.Resampling.LANCZOS).convert('RGBA')
draw = ImageDraw.Draw(canvas, 'RGBA')

panel_w = int(2048 * 0.19)
panel_h = 921
panel_left = 0
panel_top = 0
# Blue panel with the same asymmetrical white curve as SidebarWhitePanelDrawable.
mask = Image.new('L', canvas.size, 0)
md = ImageDraw.Draw(mask)
path = [(0, 0), (int(panel_w * .78), 0), (int(panel_w * .93), 55), (panel_w, 175),
        (int(panel_w * .83), 313), (int(panel_w * .70), 424),
        (int(panel_w * .70), 497), (int(panel_w * .83), 608),
        (panel_w, 746), (int(panel_w * .93), 866), (int(panel_w * .78), 921), (0, 921)]
md.polygon(path, fill=255)
white = Image.new('RGBA', canvas.size, (250, 251, 253, 255))
white.putalpha(mask)
canvas = Image.alpha_composite(canvas, white)
draw = ImageDraw.Draw(canvas, 'RGBA')
# White outer and inner contours.
draw.line(path[1:-1], fill=(255, 255, 255, 235), width=3, joint='curve')
inner = [(int(panel_w * .72), 5), (int(panel_w * .85), 83), (int(panel_w * .91), 185),
         (int(panel_w * .77), 313), (int(panel_w * .65), 424),
         (int(panel_w * .65), 497), (int(panel_w * .77), 608),
         (int(panel_w * .91), 736), (int(panel_w * .85), 838),
         (int(panel_w * .72), 916)]
draw.line(inner, fill=(255, 255, 255, 190), width=2, joint='curve')

font_path = '/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf'
regular_path = '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
label_font = ImageFont.truetype(font_path, 16)
clock_font = ImageFont.truetype(font_path, 32)
date_font = ImageFont.truetype(regular_path, 16)

# Header: complete avatar followed by clock and date immediately to its right.
draw.ellipse((14, 10, 84, 80), fill=(54, 94, 128, 255), outline=(255, 255, 255, 255), width=2)
draw.ellipse((26, 22, 72, 68), outline=(255, 255, 255, 230), width=2)
draw.text((96, 10), '00:57', font=clock_font, fill=(255, 255, 255, 255))
draw.text((98, 48), '28 Ago', font=date_font, fill=(226, 232, 242, 255))

labels = ['DORAMAS', 'NOVELAS TURCAS', 'NOVELAS', 'REELSHORTS', 'ANIMES']
icons = [
    root / 'app/src/main/res/drawable-nodpi/home_sidebar_icon_doramas.png',
    root / 'app/src/main/res/drawable-nodpi/home_sidebar_icon_turkish_novelas.png',
    root / 'app/src/main/res/drawable-nodpi/home_sidebar_icon_novelas.png',
    root / 'app/src/main/res/drawable-nodpi/home_sidebar_icon_reelshorts.png',
    root / 'app/src/main/res/drawable-nodpi/home_sidebar_icon_animes.png',
]
row_x = int(panel_w * .055)
row_w = panel_w - 2 * row_x
row_h = int(panel_h * .095)
gap = int(panel_h * .018)
start_y = int(panel_h * .14)
for i, (label, icon_path) in enumerate(zip(labels, icons)):
    y = start_y + i * (row_h + gap)
    draw.rounded_rectangle((row_x, y, row_x + row_w, y + row_h), radius=20,
                           fill=(11, 27, 62, 255), outline=(255, 255, 255, 255), width=2)
    icon = Image.open(icon_path).convert('RGBA')
    icon.thumbnail((42, 42), Image.Resampling.LANCZOS)
    icon_alpha = icon.getchannel('A')
    visible_icon = Image.new('RGBA', icon.size, (255, 255, 255, 0))
    visible_icon.putalpha(icon_alpha)
    icon = visible_icon
    ix = row_x + 8 + (42 - icon.width) // 2
    iy = y + (row_h - icon.height) // 2
    canvas.alpha_composite(icon, (ix, iy))
    f = label_font
    if label == 'NOVELAS TURCAS':
        f = ImageFont.truetype(font_path, 13)
    draw.text((row_x + 61, y + (row_h - f.getbbox(label)[3]) // 2 - 2), label, font=f, fill=(255, 255, 255, 255))
    draw.text((row_x + row_w - 24, y + 23), '›', font=ImageFont.truetype(regular_path, 28), fill=(255, 255, 255, 255))

out = root / 'preview/home_white_native_panel_approval_v2.png'
canvas.convert('RGB').save(out, quality=95)
print(out)
