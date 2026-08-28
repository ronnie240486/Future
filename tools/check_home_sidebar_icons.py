from pathlib import Path
from PIL import Image

root = Path('/home/ubuntu/Future/app/src/main/res/drawable-nodpi')
for path in sorted(root.glob('home_sidebar_icon_*.png')):
    image = Image.open(path).convert('RGBA')
    alpha = image.getchannel('A')
    print(path.name, 'size=', image.size, 'alpha=', alpha.getextrema(), 'bbox=', alpha.getbbox(), 'corner=', image.getpixel((0, 0)))
