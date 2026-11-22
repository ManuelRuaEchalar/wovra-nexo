# Frontend de Dendra

Un solo proyecto Vite + React con tres rutas, cada una cargada por separado:

- `/`: landing (`src/paginas/landing/`). Hero con fondo animado, navbar, la explicación con celulares 3D (Three.js), en español e inglés.
- `/r/:id`: página de reclamo de la recompensa (`src/paginas/reclamo/`, abajo).
- `/panel`: panel web del dueño (`src/paginas/panel/`).

## Landing

- **Fondo del hero:** se cambia en una línea, `FONDO` en `src/paginas/landing/config.ts` (`'dendritas-luz'` o `'sinapsis-cerca'`). Para comparar sin tocar código: `/?fondo=sinapsis-cerca`.
- **Textos:** `src/paginas/landing/textos.ts`, español e inglés con la misma forma. El idioma se elige con el botón de la barra o con `?lang=en`.
- **Celulares 3D:** `src/paginas/landing/senal/escena3d.ts` (poses por paso, señal de partículas). Las capturas reales van en `src/paginas/landing/senal/capturas/` (ver su `LEEME.md`).

## Estructura

```
src/
  main.tsx        elige la página según la ruta y la carga por separado
  index.css       sistema Halo + Nocturno
  compartido/     navbar, pie, logo, idioma (ES/EN), textos y datos del sitio
  paginas/
    landing/      hero, fondos animados, escena 3D, ciudad
    reclamo/      reclamo de la recompensa (Pollar, Stellar) y su cliente de la API
    panel/        panel web del dueño (descifrado en el navegador, relevo, mapa)
```

## Página de reclamo de recompensa

Es la página que abre el QR de la pantalla del celular perdido (`/r/:id?c=CODIGO`). Quien encontró el celular:

1. Crea su billetera Stellar: con **Pollar** (Google o correo, sin frase semilla) o con una **cuenta de prueba** de testnet que se genera en el navegador.
2. Deja su nombre y contacto, y reclama la recompensa.
3. Cuando el dueño confirma que recuperó el celular, el USDC llega a su billetera. La página se actualiza sola y enlaza cada movimiento en stellar.expert.

## Desarrollo

```bash
npm install
npm run dev      # con el backend corriendo en :8787 → http://localhost:5173/r/<id>?c=<codigo>, con /api apuntando a localhost:8787
npm run build    # genera dist/, que sirve el backend
```

Pollar se activa cuando el servidor tiene `POLLAR_CLAVE_PUBLICA` (pub_testnet_…). En el dashboard de Pollar hay que:

- agregar el origen de la página en **Developers → Websites** (y `http://localhost:5173` para desarrollo);
- activar el login con Google y por correo;
- en **Wallets → Tokens**, habilitar USDC de testnet (emisor `GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5`).

Sin esa clave, la página ofrece solo la cuenta de prueba.
