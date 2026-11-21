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

## Micropagos a los nodos de la red

Un *nodo* es cualquier teléfono con la app que captó por Bluetooth un celular
ajeno en alerta y entregó su aviso cifrado (ver "guardar y reenviar" en la app).

1. El nodo agrega a cada aviso reenviado su `idNodo`: un número al azar,
   distinto del id de su equipo, así reenviar no revela quién es.
2. La app de alguien del círculo del dueño descifra el aviso. Si trae una
   ubicación válida que no había llegado ya por internet, lo confirma con
   `POST /api/nodos/confirmar` (`idEquipo, idNodo, en`).
3. Cada aviso confirmado suma `PAGO_POR_AVISO_USDC` (0,01). Topes contra
   abusos: un aviso por minuto por par equipo–nodo, 30 por equipo y 100 por
   nodo al día.
4. Cada `INTERVALO_PAGO_NODOS_MS` (2 min) el **fondo de la red** paga lo
   pendiente en **una sola transacción** de Stellar, con hasta 50 pagos. En el
   modelo de negocio, ese fondo se alimenta con una parte de cada suscripción.
5. **Recompensa compartida:** si el dueño lo eligió (`porcentajeRed`, 10 % por
   defecto en la app), al confirmar la devolución la resolución del escrow
   reparte ese porcentaje en partes iguales entre los nodos que vieron el
   celular durante la pérdida, y el resto va a quien lo devolvió. Todo ocurre
   en la misma transacción.

`GET /api/nodos/:idNodo` devuelve lo ganado, lo pagado, los últimos pagos y
la cuenta Stellar del nodo, que en la demo custodia el servidor.

Pendiente para producción: solo deberían poder confirmar avisos los
dispositivos con una suscripción activa, la cuenta del nodo debería ser su
billetera Pollar, y las cuentas nuevas deberían ser patrocinadas por el fondo.
