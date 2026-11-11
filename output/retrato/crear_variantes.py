from pathlib import Path
import subprocess
import sys
from PIL import Image, ImageStat, ImageDraw, ImageFont
from componer import compose

OUT = Path(__file__).resolve().parent
sky = Image.open(OUT / 'fondo_cielo_arboles.png').convert('RGB').resize((1024, 1024))
celeste = tuple(ImageStat.Stat(sky.crop((450, 50, 650, 200))).median)
cream = '#F3F1E9'
hex_sky = '#%02X%02X%02X' % celeste
for name, color in [('celeste', celeste), ('marfil', cream)]:
    compose(background_color=color, output_name=f'retrato_fondo_{name}', crop_right=880)
    subprocess.run([sys.executable, str(OUT / 'crear_flyer.py'),
                    '--photo', f'retrato_fondo_{name}.png',
                    '--output', f'flyer_fondo_{name}'], check=True)

preview = Image.new('RGB', (1248, 810), '#E1E3E5')
draw = ImageDraw.Draw(preview)
font = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf', 21)
for x, name, label in [(16, 'celeste', f'Celeste cielo · {hex_sky}'),
                        (632, 'marfil', 'Blanco cálido / marfil · #F3F1E9')]:
    draw.text((x + 8, 17), label, fill='#172C40', font=font)
    flyer = Image.open(OUT / f'flyer_fondo_{name}.png')
    preview.paste(flyer.resize((600, 750), Image.Resampling.LANCZOS), (x, 52))
preview.save(OUT / 'comparacion_fondos_flyer.jpg', quality=95)
(OUT / 'colores_fondos.txt').write_text(f'Celeste cielo: {hex_sky}\nBlanco calido / marfil: #F3F1E9\n', encoding='utf-8')
print(f'Celeste: {hex_sky}; marfil: {cream}')
