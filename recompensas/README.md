# Recompensa garantizada (Stellar)

Quien encuentra un celular **perdido** (nunca robado) recibe una recompensa que el dueño dejó bloqueada en un escrow de [Trustless Work](https://docs.trustlesswork.com) sobre Stellar. El dinero no lo tiene nadie mientras tanto: está en un contrato que se puede verificar en la red.

```
Dueño reporta "lo perdí" + monto ──► backend despliega y fondea el escrow (USDC)
Pantalla del celular perdido: monto + QR ──► página de reclamo (/r/:id)
Quien lo encuentra inicia sesión (Pollar) o usa una cuenta de prueba ──► reclamo
Dueño confirma "lo recuperé" ──► disputa + resolución ──► USDC a quien lo devolvió
Dueño cancela ──► los fondos vuelven al dueño
```

**Por qué "resolución" y no "liberación":** en Trustless Work el receptor se fija al crear el escrow y no se puede cambiar una vez fondeado, pero quien encuentra el celular aparece después. La plataforma es el árbitro (`disputeResolver`) y reparte el saldo a la dirección que se conoce al final.

**Etapa actual: testnet.** El servidor custodia las claves de prueba del dueño (una cuenta por dispositivo). En mainnet la custodia pasa a billeteras de Pollar co-firmadas por el dispositivo.

## Partes

- `server/`: API en Node 22 + TypeScript (sin compilar: Node ejecuta `.ts` directamente). También sirve la página de reclamo compilada.
- `web/`: página de reclamo (rama `tarea/pagina-reclamo`).

## Poner en marcha el servidor

```bash
cd recompensas/server
cp .env.example .env      # completar TW_API_KEY (y POLLAR_CLAVE_PUBLICA)
npm install
npm start                 # http://localhost:8787/api/salud
```

- **TW_API_KEY:** en https://dapp.trustlesswork.com, conectar una billetera Stellar (Freighter, en testnet). Completar el perfil con el *use case*, y en **Settings → API Keys → Request API Key**.
- La cuenta de plataforma y las cuentas de los dueños se crean solas en testnet (friendbot), y el USDC de prueba se compra con XLM en el exchange de Stellar. No hace falta ningún faucet.
- Los datos quedan en `server/datos/`, que no se sube al repo porque contiene claves de testnet.

## API

| Método | Ruta | Quién | Qué |
|---|---|---|---|
| GET | `/api/salud` | — | Estado y cuenta de plataforma |
| GET | `/api/config` | — | Configuración pública para la página (clave publicable de Pollar, USDC) |
| POST | `/api/recompensas` | App | `{dispositivoId, nombreDueno, montoBs, mensaje?}` → `202 {recompensa, token}`. El escrow se crea en segundo plano (`estado: creando → activa`) |
| GET | `/api/recompensas/:id` | Todos / dueño | Vista pública; con `Authorization: Bearer <token>` incluye el código, la URL de reclamo y el contacto de quien lo encontró |
| POST | `/api/recompensas/:id/reclamo` | Página | `{codigo, direccion, nombre, contacto}` |
| POST | `/api/recompensas/:id/confirmar` | Dueño | Paga a quien reclamó |
| POST | `/api/recompensas/:id/rechazar` | Dueño | Descarta el reclamo y vuelve a `activa` |
| POST | `/api/recompensas/:id/cancelar` | Dueño | Devuelve los fondos al dueño |

Estados: `creando`, `activa`, `reclamada`, `pagando`, `pagada`, `devolviendo`, `devuelta`, `error`. Cada movimiento trae su enlace a stellar.expert.
