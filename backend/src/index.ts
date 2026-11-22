import { existsSync } from "node:fs";
import { join } from "node:path";
import express, { type NextFunction, type Request, type Response } from "express";
import { ErrorNegocio } from "./compartido/errores.ts";
import { config } from "./config.ts";
import { rutasNodos } from "./nodos/rutas.ts";
import { cuentaFondo, pagarNodos } from "./nodos/servicio.ts";
import { rutasRecompensas } from "./recompensas/rutas.ts";
import { prepararPlataforma } from "./recompensas/servicio.ts";
import { rutasRescate } from "./rescate/rutas.ts";
import { USDC } from "./stellar/red.ts";
import { ErrorTrustless } from "./stellar/trustless.ts";

/**
 * Servidor de Dendra: la API (recompensas, nodos y panel) y el frontend
 * compilado (landing, página de reclamo y panel web) en un solo proceso.
 */
const app = express();
app.disable("x-powered-by");
app.use(express.json({ limit: "16kb" }));

// La app Android no necesita CORS; esto es para el frontend en desarrollo (Vite).
app.use("/api", (req, res, next) => {
  res.setHeader("access-control-allow-origin", "*");
  res.setHeader("access-control-allow-headers", "content-type, authorization");
  res.setHeader("access-control-allow-methods", "GET, POST, OPTIONS");
  if (req.method === "OPTIONS") return void res.sendStatus(204);
  next();
});

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

app.use("/api/recompensas", rutasRecompensas);
app.use("/api/nodos", rutasNodos);
app.use("/api/rescate", rutasRescate);

app.use("/api", (_req, res) => void res.status(404).json({ error: "Ruta no encontrada" }));

// Frontend compilado. /r/:id lo abre el QR del celular perdido.
if (existsSync(config.directorioWeb)) {
  app.use(express.static(config.directorioWeb, { index: false }));
  app.get(["/", "/r/:id", "/panel"], (_req, res) => res.sendFile(join(config.directorioWeb, "index.html")));
} else {
  console.warn(`[web] No está compilado el frontend (${config.directorioWeb}): solo se sirve la API.`);
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
  console.log(`[servidor] escuchando en ${config.urlPublica}`);
  try {
    cuentaPlataforma = await prepararPlataforma();
    console.log(`[recompensas] cuenta de plataforma (testnet): ${cuentaPlataforma}`);
  } catch (e) {
    console.error("[recompensas] no se pudo preparar la cuenta de plataforma:", e);
  }
  console.log(`[nodos] fondo de la red (testnet): ${cuentaFondo()}`);
  setInterval(() => {
    pagarNodos().catch((e) => console.error("[nodos] no se pudo pagar:", e?.response?.data?.extras?.result_codes ?? e));
  }, config.intervaloPagoNodosMs);
  if (!config.twApiKey) console.warn("[recompensas] Falta TW_API_KEY: no se podrán crear escrows.");
});
