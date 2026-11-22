# Capturas de la app para "La señal"

Capturas reales de la app (PNG, JPG o WebP; vertical, idealmente 1080 × 2400).
El nombre del archivo decide en qué celular y en qué paso aparece
(ver `pantallasDeseadas` en `../Senal.tsx`). Mientras falta una, la escena muestra
una pantalla provisional con su nombre.

| Archivo | Celular | Escena |
| --- | --- | --- |
| `inicio` | El tuyo | 0 y 2 · Escudo activo |
| `armar` | El tuyo | 1 · Arma tu círculo (emulador) |
| `reportar` | Tu círculo | 2 · Reportar como perdido |
| `perdido` | El tuyo | 3 y 7 · Pantalla de perdido con la recompensa y el QR |
| `seguimiento` | Tu círculo | 0 y 4 · Mapa con la ubicación |
| `brujula` | Cercano / tu círculo | 0, 4 y 5 · Buscar de cerca (≈ 3,6 m) |
| `sonando` | El tuyo | 6 · Hacer sonar: pantalla blanca (emulador) |
| `reclamo` | Quien lo encuentra | 7 · Página de reclamo con el código |
| `devuelto` | El tuyo | 8 · El dueño confirma y paga |
| `recompensa` | Quien lo encuentra | 8 · Página de reclamo ya pagada |

Si falta una captura a mitad del recorrido, ese celular sigue mostrando la anterior.

Las capturas salen de dos celulares de prueba y del emulador `centinela34` (adb `screencap`), sin la barra de
estado ni la de navegación, reducidas a 720 × 1600 en WebP. Los nombres de calle del mapa
y los números de teléfono están desenfocados.
