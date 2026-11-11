from pathlib import Path
import subprocess
import sys
from PIL import Image, ImageDraw, ImageFont
from componer import compose

OUT = Path(__file__).resolve().parent
TONES = [
    ('azul_gris', '#AAB9BE', 'Azul grisáceo'),
    ('gris_calido', '#C4BEB3', 'Gris cálido'),
    ('salvia', '#B4BAAA', 'Salvia tenue'),
]
preview = Image.new('RGB', (1496, 682), '#E7E7E4')
d = ImageDraw.Draw(preview)
font = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf', 22)
small = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf', 17)
for index, (slug, color, title) in enumerate(TONES):
    compose(background_color=color, output_name=f'retrato_fondo_{slug}', crop_right=880)
    subprocess.run([sys.executable, str(OUT / 'crear_flyer.py'),
                    '--photo', f'retrato_fondo_{slug}.png',
                    '--output', f'flyer_fondo_{slug}'], check=True)
    x = 16 + index * 496
    d.text((x + 8, 11), title, font=font, fill='#172C40')
    d.text((x + 8, 39), color, font=small, fill='#52616B')
    flyer = Image.open(OUT / f'flyer_fondo_{slug}.png')
    preview.paste(flyer.resize((480, 600), Image.Resampling.LANCZOS), (x, 70))
preview.save(OUT / 'comparacion_tonos_suaves.jpg', quality=96)
