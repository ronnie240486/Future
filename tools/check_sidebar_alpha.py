from PIL import Image
from pathlib import Path

path = Path('/home/ubuntu/Future/app/src/main/res/drawable-nodpi/home_sidebar_reference_panel.png')
image = Image.open(path)
print('mode=', image.mode, 'size=', image.size)
if 'A' in image.getbands():
    alpha = image.getchannel('A')
    print('alpha_extrema=', alpha.getextrema(), 'alpha_bbox=', alpha.getbbox())
    corners = [alpha.getpixel(point) for point in [(0, 0), (image.width - 1, 0), (0, image.height - 1), (image.width - 1, image.height - 1)]]
    print('corner_alpha=', corners)
else:
    print('no_alpha_channel')
