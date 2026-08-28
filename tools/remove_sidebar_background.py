from collections import deque
from pathlib import Path
from PIL import Image

source = Path('/home/ubuntu/Future/app/src/main/res/drawable-nodpi/home_sidebar_reference_panel.png')
target = source.with_name('home_sidebar_reference_panel_transparent.png')
image = Image.open(source).convert('RGBA')
pixels = image.load()
width, height = image.size

def background_like(rgba):
    r, g, b, a = rgba
    if a == 0:
        return True
    near_white = min(r, g, b) > 150 and max(r, g, b) - min(r, g, b) < 58
    vivid_green = g > 120 and g > r * 1.45 and g > b * 1.12 and r < 150
    return near_white or vivid_green

queue = deque()
seen = bytearray(width * height)
for x in range(width):
    queue.append((x, 0))
    queue.append((x, height - 1))
for y in range(height):
    queue.append((0, y))
    queue.append((width - 1, y))

while queue:
    x, y = queue.popleft()
    index = y * width + x
    if seen[index] or not background_like(pixels[x, y]):
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
print(f'saved={target} size={image.size}')
print('alpha_extrema=', image.getchannel('A').getextrema())
