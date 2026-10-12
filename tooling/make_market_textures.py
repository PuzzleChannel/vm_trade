from PIL import Image, ImageDraw
from pathlib import Path

OUT = Path('/mnt/data/vmtrade_work/src/main/resources/assets/vmtrade/textures/block')
OUT.mkdir(parents=True, exist_ok=True)

# Opaque Minecraft-style palette.
WOOD_D = (57, 32, 22)
WOOD = (104, 62, 31)
WOOD_L = (143, 88, 42)
WOOD_H = (185, 123, 57)
IRON_D = (33, 36, 40)
IRON = (75, 78, 82)
IRON_L = (111, 114, 117)
RED_D = (120, 19, 20)
RED = (184, 31, 30)
RED_L = (222, 55, 46)
CREAM_D = (181, 159, 121)
CREAM = (238, 220, 176)
CREAM_L = (255, 241, 204)
GOLD_D = (116, 70, 16)
GOLD = (218, 157, 35)
GOLD_L = (255, 214, 76)
GLASS_D = (24, 44, 43)
GLASS = (46, 78, 67)
WHITE = (242, 242, 225)
BLACK = (15, 14, 13)


def base():
    im = Image.new('RGB', (32, 32), WOOD_D)
    return im, ImageDraw.Draw(im)


def cart(draw, x, y, scale=2, c=CREAM_L):
    # 7x6 pixel cart glyph, scaled.
    pts = [
        (0,0),(1,0),(1,1),(2,1),(6,1),(6,2),(5,2),(5,4),(2,4),(2,3),(6,3),
        (2,5),(3,5),(3,6),(4,6),(4,5),(5,5),(5,6),(6,6),(6,5)
    ]
    for px, py in pts:
        draw.rectangle((x+px*scale, y+py*scale, x+(px+1)*scale-1, y+(py+1)*scale-1), fill=c)


def coin(draw, x, y, s=1):
    draw.rectangle((x+2*s, y, x+6*s-1, y+7*s-1), fill=GOLD_D)
    draw.rectangle((x+s, y+2*s, x+7*s-1, y+5*s-1), fill=GOLD_D)
    draw.rectangle((x+2*s, y+s, x+5*s-1, y+6*s-1), fill=GOLD)
    draw.rectangle((x+3*s, y+2*s, x+4*s, y+5*s-1), fill=GOLD_L)


def awning(draw, y=3, h=8):
    # Dark trim.
    draw.rectangle((2, y, 29, y+h), fill=IRON_D)
    cols = [RED, CREAM_L, RED, CREAM_L, RED]
    x = 3
    width = 5
    for i, col in enumerate(cols):
        x0 = x + i*width
        x1 = min(x0+width-1, 28)
        draw.rectangle((x0, y+1, x1, y+h-2), fill=col)
        # slight lower shadow to give the canopy depth
        draw.rectangle((x0, y+h-2, x1, y+h-1), fill=RED_D if col == RED else CREAM_D)
    draw.rectangle((3, y, 28, y), fill=IRON_L)


def front():
    im, d = base()
    # Outer frame and iron corners.
    d.rectangle((0,0,31,31), fill=IRON_D)
    d.rectangle((2,2,29,29), fill=WOOD)
    d.rectangle((3,12,28,28), fill=WOOD_D)
    # Signboard.
    d.rectangle((8,1,23,6), fill=IRON_D)
    d.rectangle((9,2,22,5), fill=(62, 43, 41))
    cart(d, 12, 2, scale=1, c=CREAM_L)
    # Awning.
    awning(d, y=6, h=8)
    # Shop window.
    d.rectangle((5,14,26,24), fill=IRON_D)
    d.rectangle((6,15,25,23), fill=GLASS_D)
    d.rectangle((7,16,24,22), fill=GLASS)
    # Shelves/stock.
    d.rectangle((8,17,11,19), fill=WOOD_H)
    d.rectangle((20,17,23,19), fill=WOOD_H)
    d.rectangle((8,20,23,21), fill=WOOD_L)
    # Coin and little crate in the shop.
    coin(d, 13, 16, s=1)
    d.rectangle((12,21,17,22), fill=GOLD_D)
    d.rectangle((13,20,16,20), fill=GOLD)
    # Counter.
    d.rectangle((4,23,27,25), fill=WOOD_H)
    d.rectangle((5,23,26,23), fill=WOOD_L)
    # Lower branded panel.
    d.rectangle((8,26,23,29), fill=IRON_D)
    d.rectangle((9,27,22,28), fill=(55, 39, 34))
    cart(d, 13, 26, scale=1, c=GOLD_L)
    # Corner bolts/highlights.
    for x in (3, 28):
        for y in (15, 26):
            d.rectangle((x,y,x+1,y+1), fill=IRON_L)
    return im


def side():
    im, d = base()
    d.rectangle((0,0,31,31), fill=IRON_D)
    d.rectangle((2,2,29,29), fill=WOOD)
    # Planks.
    for y in range(3,29,5):
        d.rectangle((3,y,28,y+3), fill=WOOD_L if (y//5)%2 else WOOD)
        d.line((3,y,28,y), fill=WOOD_H)
        d.line((3,y+3,28,y+3), fill=WOOD_D)
    # Canopy edge.
    d.rectangle((3,3,28,9), fill=IRON_D)
    for i,x in enumerate(range(4,28,5)):
        col=RED if i%2==0 else CREAM_L
        d.rectangle((x,4,min(x+4,27),8), fill=col)
    # Side sign with coin/shop emblem.
    d.rectangle((7,12,24,25), fill=IRON_D)
    d.rectangle((8,13,23,24), fill=(53, 37, 30))
    coin(d, 12, 15, s=2)
    d.rectangle((10,21,21,22), fill=WOOD_H)
    d.rectangle((11,23,20,23), fill=WOOD_D)
    # vertical iron brackets.
    d.rectangle((2,10,3,28), fill=IRON)
    d.rectangle((28,10,29,28), fill=IRON)
    return im


def top():
    im = Image.new('RGB',(32,32),IRON_D); d=ImageDraw.Draw(im)
    d.rectangle((2,2,29,29), fill=RED_D)
    # Checker/striped awning roof.
    for x in range(3,28,6):
        d.rectangle((x,3,min(x+2,28),28), fill=RED)
        if x + 3 <= 28:
            d.rectangle((x+3,3,min(x+5,28),28), fill=CREAM_L)
    d.rectangle((3,3,28,4), fill=RED_L)
    d.rectangle((3,27,28,28), fill=RED_D)
    # Center shop marker.
    d.rectangle((11,11,20,20), fill=IRON_D)
    d.rectangle((12,12,19,19), fill=(53,37,30))
    cart(d, 14, 13, scale=1, c=GOLD_L)
    return im


def bottom():
    im=Image.new('RGB',(32,32),IRON_D); d=ImageDraw.Draw(im)
    d.rectangle((2,2,29,29), fill=WOOD_D)
    for y in range(4,28,5):
        d.rectangle((3,y,28,y+3), fill=WOOD if (y//5)%2 else WOOD_L)
        d.line((3,y,28,y), fill=WOOD_H)
        d.line((3,y+3,28,y+3), fill=WOOD_D)
    return im

for name, im in {
    'market_front.png': front(),
    'market_side.png': side(),
    'market_top.png': top(),
    'market_bottom.png': bottom(),
}.items():
    # Explicitly opaque RGB; no transparent texels at all.
    im.convert('RGB').save(OUT/name)
    print(name, im.size, im.mode, im.getbbox())
