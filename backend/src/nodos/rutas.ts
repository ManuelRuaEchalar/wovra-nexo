import { Router } from "express";
import { confirmarAviso, vistaNodo } from "./servicio.ts";

/** /api/nodos: micropagos a los teléfonos que entregan avisos útiles. */
export const rutasNodos = Router();

rutasNodos.post("/confirmar", (req, res) => {
  res.json(confirmarAviso(req.body ?? {}));
});

rutasNodos.get("/:id", (req, res) => {
  res.json(vistaNodo(req.params.id));
});
