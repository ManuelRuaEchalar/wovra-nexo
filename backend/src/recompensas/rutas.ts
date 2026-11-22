import { Router, type Request } from "express";
import { ErrorNegocio } from "../compartido/errores.ts";
import {
  cancelar,
  confirmarDevolucion,
  crear,
  obtener,
  rechazarReclamo,
  reclamar,
  verificarToken,
  vistaDueno,
  vistaPublica,
} from "./servicio.ts";

/** /api/recompensas: recompensa garantizada en escrow por devolver un celular perdido. */
export const rutasRecompensas = Router();

const tokenDe = (req: Request) => req.header("authorization")?.replace(/^Bearer\s+/i, "");

/** Solo el dueño (con el token que recibió al crear la recompensa). */
function delDueno(req: Request) {
  const r = obtener(String(req.params.id));
  if (!verificarToken(r, tokenDe(req))) throw new ErrorNegocio("No autorizado", 401);
  return r;
}

rutasRecompensas.post("/", (req, res) => {
  const { recompensa, token } = crear(req.body ?? {});
  res.status(202).json({ recompensa: vistaDueno(recompensa), token });
});

rutasRecompensas.get("/:id", (req, res) => {
  const r = obtener(req.params.id);
  res.json(verificarToken(r, tokenDe(req)) ? vistaDueno(r) : vistaPublica(r));
});

rutasRecompensas.post("/:id/reclamo", async (req, res) => {
  res.json(vistaPublica(await reclamar(req.params.id, req.body ?? {})));
});

rutasRecompensas.post("/:id/confirmar", async (req, res) => {
  res.json(vistaDueno(await confirmarDevolucion(delDueno(req).id)));
});

rutasRecompensas.post("/:id/rechazar", (req, res) => {
  res.json(vistaDueno(rechazarReclamo(delDueno(req).id)));
});

rutasRecompensas.post("/:id/cancelar", async (req, res) => {
  res.json(vistaDueno(await cancelar(delDueno(req).id)));
});
