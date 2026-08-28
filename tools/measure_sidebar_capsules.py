from pathlib import Path
from PIL import Image

path = Path('/home/ubuntu/Future/app/src/main/res/drawable-nodpi/home_sidebar_reference_panel.png')
image = Image.open(path).convert('RGBA')
width, height = image.size
bands = []
inside = False
start = 0
for y in range(height):
    hits = 0
    for x in range(int(width * 0.12), int(width * 0.88), 12):
        r, g, b, a = image.getpixel((x, y))
        if a > 80 and b > 120 and g > 100 and b > r * 1.15:
            hits += 1
    active = hits >= 8
    if active and not inside:
        start = y
        inside = True
    elif inside and not active:
        if y - start >= 12:
            bands.append((start, y - 1))
        inside = False
if inside:
    bands.append((start, height - 1))
print('size', width, height)
print('bands', bands)
print('normalized', [(round(a / height, 4), round(b / height, 4)) for a, b in bands])
