# Dendra

**En línea:** https://dendra-bo.up.railway.app (Stellar testnet) · `/api/salud` muestra el estado.

Una red de teléfonos que se cuidan entre sí. Si te roban o pierdes el celular, tu
círculo lo sigue por internet, SMS o Bluetooth, y cualquier teléfono con la app que
pase cerca reenvía su señal cifrada. Quien devuelve un celular **perdido** cobra una
recompensa garantizada en un escrow sobre **Stellar**.

Proyecto para la National Startups Hackathon by Stellar 2026 (Bolivia).

## Estructura

```
app-movil/   App Android (Kotlin + Jetpack Compose). Paquete com.dendra.app
backend/     API en Node 22 + TypeScript: escrow (Trustless Work), micropagos y panel
frontend/    Sitio en Vite + React: landing (/), reclamo (/r/:id) y panel web (/panel)
Dockerfile   Imagen de producción: compila el frontend y lo sirve junto con el backend
```

Cada carpeta tiene su README con el detalle.

## Stellar en Dendra

| Qué | Cómo | Dónde |
|---|---|---|
| Recompensa garantizada | El dueño bloquea USDC en un escrow de Trustless Work. Al confirmar la devolución, el escrow paga a quien lo devolvió | `backend/src/recompensas`, `backend/src/stellar` |
| Billetera de quien encuentra el celular | Login con Pollar (Google o correo, sin frase semilla) y trustline de USDC | `frontend/src/paginas/reclamo` |
| Micropagos a los nodos | Un fondo paga en USDC, por lotes, los avisos útiles que entregó cada teléfono | `backend/src/nodos` |
| Recompensa compartida | El dueño puede repartir un % de la recompensa entre los nodos que vieron su celular, en la misma transacción | `backend/src/recompensas` |

Todo corre en **testnet**. Cada movimiento enlaza a stellar.expert.

## Desarrollo local

```bash
# Backend (http://localhost:8787)
cd backend && cp .env.example .env   # completar TW_API_KEY y POLLAR_CLAVE_PUBLICA
npm install && npm run dev

# Frontend (http://localhost:5173, /api va al backend)
cd frontend && npm install && npm run dev

# App Android (JDK 17 y SDK 34)
cd app-movil && ./gradlew installDebug
```

La app usa por defecto el backend desplegado (`https://dendra-bo.up.railway.app`); para desarrollo se cambia en *Ajustes → Recompensa*.

## Despliegue (Railway)

El `Dockerfile` arma un solo servicio con el frontend y la API. Railway lo detecta solo.

1. **New Project → Deploy from GitHub repo** → este repositorio, rama `main`. El Root Directory queda en la raíz.
2. **Volumen**: clic derecho en el servicio → **Attach Volume**, ruta de montaje `/datos`.
   Ahí se guardan las recompensas, las cuentas del panel y las claves de testnet.
3. **Variables**: `TW_API_KEY` y `POLLAR_CLAVE_PUBLICA`. Opcionales: `PLATAFORMA_SECRETA`
   y `FONDO_SECRETA` (si faltan, se generan en el volumen). No hacen falta `PORT` ni
   `URL_PUBLICA`: la URL se arma con `RAILWAY_PUBLIC_DOMAIN`.
4. **Settings → Networking → Generate Domain** (dejar el puerto que Railway detecta) y comprobar `https://<dominio>/api/salud`.
5. Al guardar volumen o variables, Railway muestra **Apply changes**: hay que pulsar **Deploy** para aplicarlos.
6. Agregar el dominio como origen permitido en el panel de Pollar.

## Equipo

Manuel Rua · Freddy Valencia Medina · Andre Claros — Sucre, Bolivia.
