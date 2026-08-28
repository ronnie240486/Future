from pathlib import Path
from PIL import Image

image = Image.open('/home/ubuntu/Future/app/src/main/res/drawable-nodpi/home_sidebar_reference_panel.png').convert('RGBA')
w, h = image.size
scores = []
for y in range(h):
    score = 0
    for x in range(int(w * 0.10), int(w * 0.92), 8):
        r, g, b, a = image.getpixel((x, y))
        if a > 100 and b > 150 and g > 125 and b > r * 1.35 and g > r * 1.25:
            score += 1
    scores.append(score)
peaks = sorted(range(h), key=lambda y: scores[y], reverse=True)[:80]
clusters = []
for y in sorted(peaks):
    if not clusters or y - clusters[-1][-1] > 12:
        clusters.append([y])
    else:
        clusters[-1].append(y)
print('size=', (w, h))
print('glow_clusters=', [(min(c), max(c), max(scores[y] for y in c)) for c in clusters])
print('normalized_centers=', [round(((min(c)+max(c))/2)/h, 4) for c in clusters])
