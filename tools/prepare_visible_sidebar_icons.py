from pathlib import Path
from PIL import Image

root = Path('/home/ubuntu/Future/app/src/main/res/drawable-nodpi')
for path in sorted(root.glob('home_sidebar_icon_*.png')):
    if path.name.endswith('_original.png'):
        continue
    image = Image.open(path).convert('RGBA')
    px = image.load()
    w, h = image.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            # Chroma-key residue from generated assets: remove green/cyan
            # background but retain neutral white line art.
            if g > 120 and g > r * 1.12 and g > b * 1.12:
                px[x, y] = (r, g, b, 0)
    bbox = image.getchannel('A').getbbox()
    if bbox is None:
        raise RuntimeError(f'empty icon: {path}')
    left, top, right, bottom = bbox
    pad = max(12, int(max(right - left, bottom - top) * 0.06))
    left = max(0, left - pad)
    top = max(0, top - pad)
    right = min(w, right + pad)
    bottom = min(h, bottom + pad)
    crop = image.crop((left, top, right, bottom))
    size = max(crop.size)
    square = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    square.alpha_composite(crop, ((size - crop.width) // 2, (size - crop.height) // 2))
    square.save(path)
    print(path.name, 'bbox=', bbox, 'saved=', square.size)
