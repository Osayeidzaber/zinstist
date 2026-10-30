"""Rasterize the original SVG rectangles into the icon sizes Minecraft asks for. Requires Pillow."""
from pathlib import Path
import xml.etree.ElementTree as ET
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
source = ROOT / "src/main/resources/assets/jilaton/brand/mark.svg"
svg = ET.parse(source).getroot()

# Supersample each size to roughly 512 px before downscaling, matching the original icon-128 render.
for size, factor in ((16, 32), (32, 16), (48, 10), (128, 4), (256, 2)):
    canvas = size * factor
    image = Image.new("RGBA", (canvas, canvas))
    draw = ImageDraw.Draw(image)
    draw.rounded_rectangle((0, 0, canvas - 1, canvas - 1), radius=canvas * 3 // 16, fill="#101116")
    scale = canvas // 32
    offset = canvas // 8
    for rect in svg.findall("{http://www.w3.org/2000/svg}rect"):
        x, y, width, height = (float(rect.get(key)) for key in ("x", "y", "width", "height"))
        draw.rectangle((offset + x * scale, offset + y * scale,
                        offset + (x + width) * scale - 1, offset + (y + height) * scale - 1), fill=svg.get("fill"))
    destination = source.parent / f"icon-{size}.png"
    image.resize((size, size), Image.Resampling.LANCZOS).save(destination)
    print(destination)
