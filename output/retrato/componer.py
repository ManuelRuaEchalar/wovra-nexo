from pathlib import Path
from PIL import Image, ImageEnhance, ImageFilter

ROOT = Path(__file__).resolve().parents[2]
OUT = Path(__file__).resolve().parent


def compose(background_color=None, output_name='retrato_final_izquierda', crop_right=1024):
    background = Image.open(OUT / 'fondo_cielo_arboles.png').convert('RGB')
    background = background.resize((1024, 1024), Image.Resampling.LANCZOS)
    if background_color is not None:
        background = Image.new('RGB', (1024, 1024), background_color)
    source = Image.open(ROOT / 'yo_sin_fondo.png').convert('RGBA')
    source = source.crop(source.getbbox())
    alpha = source.getchannel('A')
    rgb = source.convert('RGB')
    # Tonal corrections only: no synthesis, retouching, or facial reshaping.
    curve = [round(255 * (max(0, min(1, (v - 48) / 204)) ** 0.97)) for v in range(256)]
    rgb = rgb.point(curve * 3)
    red, green, blue = rgb.split()
    red = red.point(lambda v: round(v * 0.985))
    blue = blue.point(lambda v: min(255, round(v * 1.045 + 2)))
    rgb = Image.merge('RGB', (red, green, blue))
    rgb = ImageEnhance.Color(rgb).enhance(0.95)
    rgb.putalpha(alpha)
    # Match reference framing and keep the cutout's lower edge outside the frame.
    width = 940
    height = round(rgb.height * width / rgb.width)
    layer = rgb.resize((width, height), Image.Resampling.LANCZOS)
    mask = layer.getchannel('A')
    # A subpixel edge contraction reduces the original warm fringe.
    inner = mask.filter(ImageFilter.MinFilter(3))
    mask = Image.blend(mask, inner, 0.65).filter(ImageFilter.GaussianBlur(0.35))
    layer.putalpha(mask)
    final = background.convert('RGBA')
    final.alpha_composite(layer, (-35, 62))
    final = final.crop((0, 0, crop_right, 1024)).convert('RGB')
    final.save(OUT / f'{output_name}.png')
    final.save(OUT / f'{output_name}.jpg', quality=96, subsampling=0)


if __name__ == '__main__':
    compose()
