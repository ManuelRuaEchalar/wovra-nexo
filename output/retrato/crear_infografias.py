"""Render two restrained academic speaker profiles from supplied facts.

Usage: python crear_infografias.py datos_presentacion.json
Required keys: nombre, carrera, experiencia_ia, trabajo, tesis, disertacion.
Optional key: subtitulo.
"""
import json
import sys
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont, ImageOps

OUT = Path(__file__).resolve().parent
REGULAR = '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
BOLD = '/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf'


def font(size, bold=False):
    return ImageFont.truetype(BOLD if bold else REGULAR, size)


def wrap(draw, value, face, width):
    lines = []
    for paragraph in value.split('\n'):
        current = ''
        for word in paragraph.split():
            trial = f'{current} {word}'.strip()
            if current and draw.textlength(trial, font=face) > width:
                lines.append(current)
                current = word
            else:
                current = trial
        lines.append(current)
    return lines


def block(draw, value, box, size=34, bold=False, color='#22323B'):
    x, y, width, height = box
    for chosen in range(size, 23, -1):
        face = font(chosen, bold)
        lines = wrap(draw, value, face, width)
        step = round(chosen * 1.48)
        if len(lines) * step <= height:
            for line in lines:
                draw.text((x, y), line, font=face, fill=color)
                y += step
            return
    raise ValueError('Texto demasiado extenso: resumir antes de exportar.')


def render(data, with_photo):
    accent = '#45616E'
    im = Image.new('RGB', (1400, 1800), '#F7F6F2')
    d = ImageDraw.Draw(im)
    ink = '#22323B'
    d.rectangle((80, 77, 90, 104), fill=accent)
    d.text((109, 73), 'PERFIL DEL EXPOSITOR', font=font(26, True), fill=ink)
    d.line((80, 131, 1320, 131), fill='#BBC1BF', width=2)
    if with_photo:
        photo = Image.open(OUT / 'retrato_fondo_azul_gris.png').convert('RGB')
        photo = ImageOps.fit(photo, (280, 326), method=Image.Resampling.LANCZOS)
        im.paste(photo, (1040, 177))
    block(d, data['nombre'], (80, 180, 910, 180), 60, True)
    block(d, data.get('subtitulo', 'Trayectoria académica y experiencia'),
          (83, 382, 900, 150), 31, color='#5B676C')

    def section(number, title, value, x, y, width, height):
        d.text((x, y), number, font=font(24, True), fill=accent)
        d.text((x + 57, y - 2), title, font=font(29, True), fill=ink)
        d.line((x, y + 51, x + width, y + 51), fill='#CDD1CD', width=2)
        block(d, value, (x, y + 78, width, height - 78), 33)

    section('01', 'FORMACIÓN', data['carrera'], 80, 571, 1240, 215)
    section('02', 'EXPERIENCIA EN IA', data['experiencia_ia'], 80, 816, 580, 420)
    section('03', 'TRABAJO DESTACADO', data['trabajo'], 740, 816, 580, 420)

    # The current thesis gets the strongest emphasis in the visual hierarchy.
    d.rectangle((80, 1285, 1320, 1700), fill=accent)
    d.text((112, 1317), 'ACTUALMENTE · TESIS EN DESARROLLO',
           font=font(25, True), fill='#FFFFFF')
    block(d, data['tesis'], (112, 1374, 1176, 170), 42, True, '#FFFFFF')
    block(d, 'Desarrolla actualmente su tesis en el tema de esta disertación.',
          (112, 1561, 1176, 113), 28, color='#FFFFFF')
    variant = 'con_foto' if with_photo else 'sin_foto'
    im.save(OUT / f'infografia_academica_{variant}.png')
    im.save(OUT / f'infografia_academica_{variant}.jpg', quality=96, subsampling=0)
    im.save(OUT / f'infografia_academica_{variant}.pdf', resolution=180)


if __name__ == '__main__':
    data = json.loads(Path(sys.argv[1]).read_text(encoding='utf-8'))
    for key in ('nombre', 'carrera', 'experiencia_ia', 'trabajo', 'tesis', 'disertacion'):
        if not data.get(key, '').strip():
            raise ValueError(f'Falta el dato: {key}')
    render(data, True)
    render(data, False)
