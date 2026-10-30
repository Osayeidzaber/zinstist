"""Rasterize the Jilaton wordmark used on the startup loading screen. Requires Pillow and a Windows font."""
from pathlib import Path
import xml.etree.ElementTree as ET
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
source = ROOT / "src/main/resources/assets/jilaton/brand/mark.svg"
destination = ROOT / "src/main/resources/assets/jilaton/textures/brand/loading-logo.png"
font_path = Path("C:/Windows/Fonts/bahnschrift.ttf")

WIDTH, HEIGHT = 2048, 512
SUPERSAMPLE = 2
INK = (242, 242, 242, 255)
MUTED = (166, 166, 166, 255)

def tracked_width(draw, text, font, tracking):
    return sum(draw.textlength(char, font=font) for char in text) + tracking * (len(text) - 1)

def tracked_text(draw, xy, text, font, tracking, fill):
    x, y = xy
    for char in text:
        draw.text((x, y), char, font=font, fill=fill)
        x += draw.textlength(char, font=font) + tracking

scale = SUPERSAMPLE
image = Image.new("RGBA", (WIDTH * scale, HEIGHT * scale), (0, 0, 0, 0))
draw = ImageDraw.Draw(image)

word_font = ImageFont.truetype(str(font_path), 270 * scale)
word_font.set_variation_by_name("Bold")
sub_font = ImageFont.truetype(str(font_path), 60 * scale)
sub_font.set_variation_by_name("Regular")

word, subtitle = "JILATON", "CLIENT"
word_tracking, sub_tracking = 28 * scale, 34 * scale
word_width = tracked_width(draw, word, word_font, word_tracking)
sub_width = tracked_width(draw, subtitle, sub_font, sub_tracking)
word_box = draw.textbbox((0, 0), word, font=word_font)
sub_box = draw.textbbox((0, 0), subtitle, font=sub_font)
word_cap = word_box[3] - word_box[1]
sub_cap = sub_box[3] - sub_box[1]

block_gap, mark_text_gap = 54 * scale, 76 * scale
block_height = word_cap + block_gap + sub_cap
mark_height = 290 * scale
unit = mark_height / 24
mark_width = 24 * unit
group_width = mark_width + mark_text_gap + word_width
group_left = (WIDTH * scale - group_width) / 2
block_top = (HEIGHT * scale - block_height) / 2
mark_top = (HEIGHT * scale - mark_height) / 2

svg = ET.parse(source).getroot()
for rect in svg.findall("{http://www.w3.org/2000/svg}rect"):
    x, y, width, height = (float(rect.get(key)) for key in ("x", "y", "width", "height"))
    draw.rectangle((group_left + x * unit, mark_top + y * unit,
                    group_left + (x + width) * unit - 1, mark_top + (y + height) * unit - 1), fill=INK)

text_left = group_left + mark_width + mark_text_gap
tracked_text(draw, (text_left, block_top - word_box[1]), word, word_font, word_tracking, INK)
tracked_text(draw, (text_left, block_top + word_cap + block_gap - sub_box[1]), subtitle, sub_font, sub_tracking, MUTED)

image.resize((WIDTH, HEIGHT), Image.Resampling.LANCZOS).save(destination)
print(destination)
