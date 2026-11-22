# Dendra

**En línea:** https://dendra-bo.up.railway.app (Stellar testnet) · `/api/salud` muestra el estado.

Una red de teléfonos que se cuidan entre sí. Si te roban o pierdes el celular, tu
círculo lo sigue por internet, SMS o Bluetooth, y cualquier teléfono con la app que
pase cerca reenvía su señal cifrada. Quien devuelve un celular **perdido** cobra una
recompensa garantizada en un escrow sobre **Stellar**.

Proyecto para la National Startups Hackathon by Stellar 2026 (Bolivia).

## Probar en 3 minutos

1. **Web:** abrir una recompensa de prueba ya fondeada en testnet:
   https://dendra-bo.up.railway.app/r/NRyVqlvZjFM?c=7JK3ME
   Elegir *Continuar con Google* (Pollar) o *Usar una cuenta de prueba*, dejar nombre y contacto y reclamar.
   La tarjeta enlaza el contrato en el visor de Trustless Work.
2. **App Android 0.4.0 (debug):** https://dendra-bo.up.railway.app/dendra.apk
   Usa por defecto este backend. Con un solo teléfono: *Ajustes → Probar el Escudo* simula robo y pérdida.
   Si Play Protect muestra *«Se bloqueó la app para proteger tu dispositivo»*: lo hace con cualquier app
   descargada de internet que pida recibir SMS o accesibilidad, y Dendra usa ambos (canal SMS y falso apagado).
   Play Store → foto de perfil → *Play Protect* → ⚙ → desactivar *Analizar apps con Play Protect*, instalar
   `dendra.apk` desde Descargas y volver a activarlo. Por USB también funciona: `adb install dendra.apk`.
3. **Estado del servidor:** https://dendra-bo.up.railway.app/api/salud

## Evidencia en Stellar testnet

Recompensa completa ejecutada el 2026-11-22 contra el servidor público.
Escrow (contrato Soroban de Trustless Work): [`CBO5VDB2…KCFW`](https://viewer.trustlesswork.com/CBO5VDB2SY2EHRRNLL3PRVUEPY5WQL5CW7JZHEEWEQKYN6YRTCFUKCFW)

| Paso | Quién firma | Transacción |
|---|---|---|
| Despliegue del escrow | Plataforma | [6f3ba0ea…](https://stellar.expert/explorer/testnet/tx/6f3ba0ea543947d5402dd31e4369735778459340cddcc763ce14ccdf7d30e854) |
| Fondeo en USDC | Dueño (cuenta custodiada en testnet) | [1bfe725f…](https://stellar.expert/explorer/testnet/tx/1bfe725fa7732483f352ad31805d8f4818c39daadc2936669df535ad9cb42112) |
| Disputa (al confirmar la devolución) | Dueño, rol *approver* | [30faffdf…](https://stellar.expert/explorer/testnet/tx/30faffdfafc4cefe4f31c1162b56e299b63dfeae944ca66c41f02f2fa613bb83) |
| Resolución: pago a quien lo devolvió | Plataforma, rol *disputeResolver* | [cb88f6f9…](https://stellar.expert/explorer/testnet/tx/cb88f6f97e50e91b2d877c6b4be145eec88560827da3f6ef3be2de9c77ba8ae7) |
| Pago por lotes a dos nodos (0,01 USDC c/u) | Fondo de la red | [61b88373…](https://stellar.expert/explorer/testnet/tx/61b8837391add024870995d426f1167d2c8f103c86d09aed607211133fa83573) |

**Por qué disputa y resolución:** en Trustless Work el receptor se fija al crear el escrow, pero quien encuentra el celular aparece después. La plataforma es el árbitro y reparte el saldo a la dirección conocida al final (y, si el dueño lo eligió, una parte a los nodos que vieron el celular, en la misma transacción). Si el dueño cancela, la resolución devuelve todo a su cuenta. El dueño puede rechazar un reclamo falso antes de pagar.

## Riesgo: robar para cobrar la recompensa

- La recompensa existe **solo en modo perdido**. Un celular reportado como robado nunca muestra recompensa ni QR.
- El pago sale **solo cuando el dueño confirma** que recuperó el celular; puede rechazar el reclamo.
- Monto máximo por recompensa (50 USDC, `MAX_RECOMPENSA_USDC`) y una sola recompensa activa por equipo.
- Quien reclama deja nombre y contacto, y su dirección Stellar queda registrada en la cadena.
- Si el celular pasa de perdido a robado (apagado falso, retiro de chip), la alerta cambia a robo.

## Economía de la red

Ingreso: suscripción de 10 Bs al mes. Una parte alimenta el **fondo de la red** en USDC, que paga 0,01 USDC por aviso útil confirmado, con topes de 30 avisos por equipo y 100 por nodo al día: el costo máximo por usuario es 0,30 USDC al día solo en los días en que su celular está en alerta. La recompensa la pone el dueño y no es ingreso de la plataforma (comisión del escrow: 0 en esta etapa).

**Fuente de la cifra de robos:** unos 700 celulares robados por día en Bolivia, según [Bloomberg Línea (2022)](https://www.bloomberglinea.com/2022/08/30/robo-de-celular-a-evo-desata-peleas-politicas-y-ciudadanos-se-quejan-de-la-inseguridad/); 781.173 robados y hurtados desde 2010, según [eju.tv](https://eju.tv/?p=580225).

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

| Integrante | Rol |
|---|---|
| Juan Manuel Rua Echalar ([ManuelRuaEchalar](https://github.com/ManuelRuaEchalar)) | App Android, integración con Stellar |
| Freddy Valencia Medina | Backend y frontend |
| Andre Claros | Backend |

Sucre, Bolivia. El historial de este repositorio empieza el 2026-11-22: es la consolidación de un respaldo de trabajo anterior en un solo repositorio ordenado.
