from pathlib import Path
import argparse
from PIL import Image, ImageDraw, ImageFont

OUT = Path(__file__).resolve().parent
parser = argparse.ArgumentParser()
parser.add_argument('--photo', default='retrato_final_recortado.png')
parser.add_argument('--output', default='flyer_expositor_generico')
args = parser.parse_args()
W, H = 1200, 1500
CREAM = '#F3F1E9'
NAVY = '#172C40'
MUTED = '#5B6972'
LIME = '#D7F36A'
FONT = '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
BOLD = '/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf'
im = Image.new('RGB', (W, H), CREAM)
d = ImageDraw.Draw(im)


def text(x, y, value, size, color=NAVY, bold=False):
    d.text((x, y), value, font=ImageFont.truetype(BOLD if bold else FONT, size), fill=color)


# Editorial event header.
d.rectangle((64, 66, 78, 92), fill=NAVY)
text(94, 61, 'CONEXIONES', 25, bold=True)
text(729, 66, 'CICLO DE CONFERENCIAS', 20)
d.line((64, 120, 1136, 120), fill=NAVY, width=2)
text(56, 153, 'EXPOSITORES', 119, bold=True)
text(66, 297, 'Ideas que inspiran. Conversaciones que conectan.', 26, MUTED)

# Insert the existing photograph without generative edits or warping.
photo = Image.open(OUT / args.photo).convert('RGB')
photo = photo.resize((620, 721), Image.Resampling.LANCZOS)
im.paste(photo, (64, 365))
d = ImageDraw.Draw(im)
d.rectangle((64, 365, 257, 410), fill=LIME)
text(82, 373, 'CONOCE A', 21, bold=True)

# Speaker information, deliberately generic.
d.ellipse((738, 367, 831, 460), fill=LIME)
text(760, 384, '01', 37, bold=True)
text(738, 501, 'EXPOSITOR', 19, MUTED, True)
text(738, 529, 'INVITADO', 19, MUTED, True)
text(732, 586, 'NOMBRE', 48, bold=True)
text(732, 647, 'APELLIDO', 48, bold=True)
d.line((738, 746, 1136, 746), fill=NAVY, width=2)
text(738, 781, 'Una mirada diferente', 23)
text(738, 819, 'para compartir ideas,', 23)
text(738, 857, 'experiencias y nuevas', 23)
text(738, 895, 'perspectivas.', 23)
text(738, 1010, 'CHARLA + PREGUNTAS', 19, bold=True)

# Session card.
d.rectangle((64, 1126, 1136, 1335), fill=NAVY)
text(96, 1150, 'TEMA DE LA CONFERENCIA', 18, LIME, True)
text(92, 1191, 'Ideas para transformar', 47, CREAM, True)
text(92, 1250, 'el futuro', 47, CREAM, True)
d.ellipse((1031, 1200, 1102, 1271), outline=LIME, width=2)
d.line((1052, 1250, 1081, 1221), fill=LIME, width=3)
d.line((1062, 1221, 1081, 1221, 1081, 1240), fill=LIME, width=3)

text(64, 1363, 'FECHA POR CONFIRMAR', 20, bold=True)
text(738, 1363, 'SEDE DEL EVENTO', 20, bold=True)
d.line((64, 1415, 1136, 1415), fill='#BCC2BE', width=1)
text(64, 1439, 'DISEÑO DE MUESTRA · DATOS DE EJEMPLO', 16, MUTED)
text(940, 1435, 'CONEXIONES', 19, bold=True)

im.save(OUT / f'{args.output}.png')
im.save(OUT / f'{args.output}.jpg', quality=96, subsampling=0)
