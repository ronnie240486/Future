from collections import deque
from pathlib import Path
from PIL import Image

source = Path('/home/ubuntu/Future/preview/home_sidebar_white_compact_raw.png')
target = Path('/home/ubuntu/Future/app/src/main/res/drawable-nodpi/home_sidebar_reference_panel_white_compact.png')
image = Image.open(source).convert('RGBA')
pixels = image.load()
width, height = image.size

def checkerboard_or_background(rgba):
    r, g, b, a = rgba
    if a == 0:
        return True
    # Removes the gray checkerboard and very bright neutral matte from the
    # outside, while preserving cyan/white line art inside the panel.
    neutral = max(r, g, b) - min(r, g, b) < 35
    gray_mat = neutral and min(r, g, b) > 115
    return gray_mat

queue = deque()
seen = bytearray(width * height)
for x in range(width):
    queue.extend(((x, 0), (x, height - 1)))
for y in range(height):
    queue.extend(((0, y), (width - 1, y)))

while queue:
    x, y = queue.popleft()
    index = y * width + x
    if seen[index] or not checkerboard_or_background(pixels[x, y]):
        continue
    seen[index] = 1
    r, g, b, _ = pixels[x, y]
    pixels[x, y] = (r, g, b, 0)
    if x > 0:
        queue.append((x - 1, y))
    if x + 1 < width:
        queue.append((x + 1, y))
    if y > 0:
        queue.append((x, y - 1))
    if y + 1 < height:
        queue.append((x, y + 1))

image.save(target)
print('saved=', target)
print('alpha=', image.getchannel('A').getextrema())
print('bbox=', image.getchannel('A').getbbox())
