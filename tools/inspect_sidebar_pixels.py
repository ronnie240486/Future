from PIL import Image

image = Image.open('/home/ubuntu/Future/app/src/main/res/drawable-nodpi/home_sidebar_reference_panel.png').convert('RGBA')
points = [(0, 0), (1695, 0), (1600, 300), (1650, 1000), (100, 100), (850, 100), (1500, 1500)]
for point in points:
    print(point, image.getpixel(point))
