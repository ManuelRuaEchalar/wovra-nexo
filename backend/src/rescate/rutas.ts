import { Router } from "express";
import { borrarCuenta, entrar, guardarCuenta, sal } from "./servicio.ts";

/** /api/rescate: cuentas del panel web (el servidor guarda solo datos cifrados). */
export const rutasRescate = Router();

rutasRescate.get("/sal", (req, res) => {
  res.json(sal(req.query.correo));
});

rutasRescate.post("/", (req, res) => {
  res.json(guardarCuenta(req.body ?? {}));
});

rutasRescate.post("/borrar", (req, res) => {
  borrarCuenta(req.body ?? {});
  res.json({ ok: true });
});

rutasRescate.post("/entrar", (req, res) => {
  res.json(entrar(req.body ?? {}));
});
