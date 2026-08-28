from pathlib import Path
from PIL import Image

path = Path('/home/ubuntu/Future/app/src/main/res/drawable-nodpi/home_sidebar_reference_panel_white.png')
im = Image.open(path).convert('RGBA')
w, h = im.size
scores = []
for y in range(h):
    score = 0
    for x in range(int(w * 0.12), int(w * 0.86), 4):
        r, g, b, a = im.getpixel((x, y))
        if a > 100 and min(r, g, b) > 175 and max(r, g, b) - min(r, g, b) < 70:
            score += 1
    scores.append(score)
# Report local maxima and merge close rows into visual bands.
peaks = [y for y in range(2, h - 2) if scores[y] >= scores[y - 1] and scores[y] >= scores[y + 1] and scores[y] > 30]
bands = []
for y in peaks:
    if not bands or y - bands[-1][-1] > 18:
        bands.append([y])
    else:
        bands[-1].append(y)
print('size=', (w, h))
print('bands=', [(min(b), max(b), max(scores[y] for y in b)) for b in bands])
print('normalized=', [(round(min(b)/h, 4), round(max(b)/h, 4)) for b in bands])
