# Capturas de la app para "La señal"

Capturas reales de la app (PNG, JPG o WebP; vertical, idealmente 1080 × 2400).
El nombre del archivo decide en qué celular y en qué paso aparece
(ver `pantallas` en `../Senal.tsx`). Mientras falta una, la escena muestra
una pantalla provisional con su nombre.

| Archivo | Celular | Paso |
| --- | --- | --- |
| `inicio` | El tuyo | 1 · Tu celular entra a la red |
| `guardian` | El tuyo | 2 · Si se aleja, lo sabes (vincular el guardián) |
| `perdido` | El tuyo | 3 y 4 · Reportado como robado |
| `reportar` | Tu círculo | 1 a 4 · Reportar el celular de un miembro |
| `nodo` | Un celular cercano | 1 a 6 · Buscar de cerca: lo detecta por Bluetooth |
| `seguimiento` | Tu círculo | 5 · Mapa con la última ubicación |
| `recompensa` | Tu círculo | 6 · Confirmar la devolución y pagar (falta) |

Si falta una captura a mitad del recorrido, ese celular sigue mostrando la anterior.

Las capturas actuales salen de dos celulares de prueba (adb `screencap`), sin la barra de
estado ni la de navegación, reducidas a 720 × 1600 en WebP. Los nombres de calle del mapa
y el número de SMS están desenfocados.
