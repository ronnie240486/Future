from collections import deque
from pathlib import Path
from PIL import Image

root = Path('/home/ubuntu/Future/app/src/main/res/drawable-nodpi')
for path in sorted(root.glob('home_sidebar_icon_*.png')):
    if path.name.endswith('_original.png'):
        continue
    image = Image.open(path).convert('RGBA')
    px = image.load()
    width, height = image.size
    queue = deque()
    seen = bytearray(width * height)

    def matte(x, y):
        r, g, b, a = px[x, y]
        neutral = max(r, g, b) - min(r, g, b) < 28
        return a > 0 and neutral and min(r, g, b) > 180

    for x in range(width):
        queue.append((x, 0))
        queue.append((x, height - 1))
    for y in range(height):
        queue.append((0, y))
        queue.append((width - 1, y))

    while queue:
        x, y = queue.popleft()
        index = y * width + x
        if seen[index] or not matte(x, y):
            continue
        seen[index] = 1
        r, g, b, _ = px[x, y]
        px[x, y] = (r, g, b, 0)
        if x:
            queue.append((x - 1, y))
        if x + 1 < width:
            queue.append((x + 1, y))
        if y:
            queue.append((x, y - 1))
        if y + 1 < height:
            queue.append((x, y + 1))

    image.save(path)
    print(path.name, 'alpha_bbox=', image.getchannel('A').getbbox())
