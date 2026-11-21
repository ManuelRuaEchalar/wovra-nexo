import { existsSync } from "node:fs";
import { join } from "node:path";
import express, { type NextFunction, type Request, type Response } from "express";
import { config } from "./config.ts";
import {
  cancelar,
  confirmarDevolucion,
  crear,
  ErrorNegocio,
  obtener,
  prepararPlataforma,
  rechazarReclamo,
  reclamar,
  verificarToken,
  vistaDueno,
  vistaPublica,
} from "./recompensas.ts";
import { borrarCuenta, entrar, guardarCuenta, sal } from "./rescate.ts";
import { USDC } from "./stellar.ts";
import { ErrorTrustless } from "./trustless.ts";

const app = express();
app.disable("x-powered-by");
app.use(express.json({ limit: "16kb" }));

// La app Android no necesita CORS; esto es para la página en desarrollo (Vite).
app.use("/api", (req, res, next) => {
  res.setHeader("access-control-allow-origin", "*");
  res.setHeader("access-control-allow-headers", "content-type, authorization");
  res.setHeader("access-control-allow-methods", "GET, POST, OPTIONS");
  if (req.method === "OPTIONS") return void res.sendStatus(204);
  next();
});

const tokenDe = (req: Request) => req.header("authorization")?.replace(/^Bearer\s+/i, "");

/** Solo el dueño (con el token que recibió al crear la recompensa). */
function delDueno(req: Request) {
  const r = obtener(String(req.params.id));
  if (!verificarToken(r, tokenDe(req))) throw new ErrorNegocio("No autorizado", 401);
  return r;
}

let cuentaPlataforma = "";

app.get("/api/salud", (_req, res) => {
  res.json({
    ok: true,
    red: "testnet",
    plataforma: cuentaPlataforma,
    trustlessWork: config.twApiKey ? "configurado" : "falta TW_API_KEY",
    pollar: config.pollarClavePublica ? "configurado" : "sin clave (se usa cuenta de prueba)",
  });
});

/** Configuración pública para la página de reclamo. */
app.get("/api/config", (_req, res) => {
  res.json({
    red: "testnet",
    pollarClavePublica: config.pollarClavePublica || null,
    usdc: { codigo: USDC.code, emisor: USDC.issuer },
    tasaBsPorUsdc: config.tasaBsPorUsdc,
  });
});

app.post("/api/recompensas", (req, res) => {
  const { recompensa, token } = crear(req.body ?? {});
  res.status(202).json({ recompensa: vistaDueno(recompensa), token });
});

app.get("/api/recompensas/:id", (req, res) => {
  const r = obtener(req.params.id);
  res.json(verificarToken(r, tokenDe(req)) ? vistaDueno(r) : vistaPublica(r));
});

app.post("/api/recompensas/:id/reclamo", async (req, res) => {
  const r = await reclamar(req.params.id, req.body ?? {});
  res.json(vistaPublica(r));
});

app.post("/api/recompensas/:id/confirmar", async (req, res) => {
  const r = delDueno(req);
  res.json(vistaDueno(await confirmarDevolucion(r.id)));
});

app.post("/api/recompensas/:id/rechazar", (req, res) => {
  const r = delDueno(req);
  res.json(vistaDueno(rechazarReclamo(r.id)));
});

app.post("/api/recompensas/:id/cancelar", async (req, res) => {
  const r = delDueno(req);
  res.json(vistaDueno(await cancelar(r.id)));
});

// --- Panel web "Mi Escudo": cuentas de rescate (el servidor no puede leerlas) ---

app.get("/api/rescate/sal", (req, res) => {
  res.json(sal(req.query.correo));
});

app.post("/api/rescate", (req, res) => {
  res.json(guardarCuenta(req.body ?? {}));
});

app.post("/api/rescate/borrar", (req, res) => {
  borrarCuenta(req.body ?? {});
  res.json({ ok: true });
});

app.post("/api/rescate/entrar", (req, res) => {
  res.json(entrar(req.body ?? {}));
});

// Página de reclamo (recompensas/web compilada). /r/:id la abre el QR del celular perdido.
if (existsSync(config.directorioWeb)) {
  app.use(express.static(config.directorioWeb, { index: false }));
  app.get(["/", "/r/:id", "/panel"], (_req, res) => res.sendFile(join(config.directorioWeb, "index.html")));
}

app.use((err: unknown, _req: Request, res: Response, _next: NextFunction) => {
  if (err instanceof ErrorNegocio) return void res.status(err.estado).json({ error: err.message });
  if (err instanceof ErrorTrustless) {
    console.error("[trustless]", err.message, JSON.stringify(err.detalle ?? ""));
    return void res.status(err.estado >= 500 ? 502 : err.estado).json({ error: err.message });
  }
  if (err instanceof SyntaxError) return void res.status(400).json({ error: "JSON inválido" });
  console.error(err);
  res.status(500).json({ error: "Error interno" });
});

app.listen(config.puerto, async () => {
  console.log(`[recompensas] escuchando en ${config.urlPublica}`);
  try {
    cuentaPlataforma = await prepararPlataforma();
    console.log(`[recompensas] cuenta de plataforma (testnet): ${cuentaPlataforma}`);
  } catch (e) {
    console.error("[recompensas] no se pudo preparar la cuenta de plataforma:", e);
  }
  if (!config.twApiKey) console.warn("[recompensas] Falta TW_API_KEY: no se podrán crear escrows.");
});
