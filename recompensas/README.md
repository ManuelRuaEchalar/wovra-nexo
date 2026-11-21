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

## Panel web (`/panel`)

Para quien no tiene círculo o no puede comunicarse con él: el dueño entra
desde cualquier navegador, ve la ubicación y la ruta en un mapa, y puede
reportar el celular (robado o perdido), hacerlo sonar o terminar la alerta.

- En la app, *Ajustes → Panel web*, el dueño elige correo y contraseña. El
  teléfono deriva 64 bytes con PBKDF2-SHA256 (210 000 iteraciones): 32 cifran
  con AES-GCM su identidad (id y secreto del equipo, relevo), y los otros 32
  son el *verificador*. Sube solo el bloque cifrado, la sal y el verificador.
- El servidor guarda el hash del verificador y el bloque, que no puede abrir.
  Solo el mismo celular (con su token) puede cambiar o borrar la cuenta.
- El navegador repite la derivación, entra con el verificador, descifra el
  bloque y desde ahí habla directamente con el relevo cifrado, como un
  miembro más del círculo. Las órdenes van firmadas con el id del propio
  equipo; la app solo acepta de ese origen reportar, sonar y terminar la alerta.

| Método | Ruta | Uso |
| --- | --- | --- |
| GET | `/api/rescate/sal?correo=` | Sal e iteraciones (para un correo sin cuenta devuelve una sal falsa estable) |
| POST | `/api/rescate` | Crear o actualizar la cuenta (`correo, idEquipo, sal, iteraciones, verificador, tokenEquipo, bloque`) |
| POST | `/api/rescate/entrar` | `correo, verificador` → `bloque` (máximo 8 fallos cada 15 min) |
| POST | `/api/rescate/borrar` | `tokenEquipo` |
