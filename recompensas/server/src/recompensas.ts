import { createHash, randomBytes, timingSafeEqual } from "node:crypto";
import { Keypair } from "@stellar/stellar-sdk";
import { Almacen } from "./almacen.ts";
import { config, explorador } from "./config.ts";
import { asegurarTrustline, asegurarUsdc, esDireccionValida, saldoUsdc } from "./stellar.ts";
import { abrirDisputa, desplegarEscrow, fondearEscrow, resolverDisputa, saldoEscrow } from "./trustless.ts";

/**
 * Recompensa garantizada por devolver un celular perdido.
 *
 * El dueño bloquea la recompensa en un escrow de Trustless Work. Quien encuentra
 * el celular la reclama desde la página web, y cuando el dueño confirma que lo
 * recuperó, la plataforma (árbitro del escrow) entrega los fondos a quien lo
 * devolvió. Si el dueño cancela, los fondos vuelven a él.
 *
 * Por qué el pago va por "resolución" y no por "liberación": en Trustless Work el
 * receptor se fija al crear el escrow y no puede cambiarse después de fondearlo,
 * pero quien encuentra el celular aparece después. La resolución permite repartir
 * el saldo a una dirección que se conoce recién al final.
 *
 * Solo para pérdidas: pagar por celulares robados incentivaría robar para "devolver".
 * Etapa de demo (testnet): el servidor custodia las claves del dueño. En mainnet,
 * la custodia pasa a billeteras de Pollar co-firmadas por el dispositivo.
 */

export type Estado =
  | "creando"
  | "activa"
  | "reclamada"
  | "pagando"
  | "pagada"
  | "devolviendo"
  | "devuelta"
  | "error";

export interface Movimiento {
  tipo: "despliegue" | "fondeo" | "disputa" | "pago" | "devolucion";
  hash: string;
  fecha: string;
}

export interface Recompensa {
  id: string;
  codigo: string;
  dispositivoId: string;
  nombreDueno: string;
  mensaje: string;
  montoBs: number;
  montoUsdc: number;
  estado: Estado;
  dueno: string;
  contractId?: string;
  reclamo?: { direccion: string; nombre: string; contacto: string; fecha: string };
  movimientos: Movimiento[];
  error?: string;
  tokenHash: string;
  creada: string;
  actualizada: string;
}

interface CuentaCustodiada {
  dispositivoId: string;
  secreta: string;
}

const recompensas = new Almacen<Recompensa>("recompensas");
const cuentas = new Almacen<CuentaCustodiada>("cuentas");
const claves = new Almacen<string>("claves");

export class ErrorNegocio extends Error {
  readonly estado: number;
  constructor(mensaje: string, estado = 400) {
    super(mensaje);
    this.estado = estado;
  }
}

// ---------- claves ----------

function plataforma(): Keypair {
  if (config.plataformaSecreta) return Keypair.fromSecret(config.plataformaSecreta);
  let secreta = claves.obtener("plataforma");
  if (!secreta) {
    secreta = Keypair.random().secret();
    claves.guardar("plataforma", secreta);
    console.warn("[recompensas] Clave de plataforma de testnet generada en datos/claves.json");
  }
  return Keypair.fromSecret(secreta);
}

/** Cuenta custodiada del dueño (una por dispositivo). Solo testnet. */
function cuentaDe(dispositivoId: string): Keypair {
  let c = cuentas.obtener(dispositivoId);
  if (!c) c = cuentas.guardar(dispositivoId, { dispositivoId, secreta: Keypair.random().secret() });
  return Keypair.fromSecret(c.secreta);
}

export async function prepararPlataforma(): Promise<string> {
  const kp = plataforma();
  await asegurarTrustline(kp);
  return kp.publicKey();
}

// ---------- utilidades ----------

const ahora = () => new Date().toISOString();
const hashToken = (t: string) => createHash("sha256").update(t).digest("hex");
const idCorto = (bytes: number) => randomBytes(bytes).toString("base64url");
const codigoDevolucion = () =>
  Array.from(randomBytes(6), (b) => "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"[b % 32]).join("");

function guardar(r: Recompensa, cambios: Partial<Recompensa>): Recompensa {
  return recompensas.guardar(r.id, { ...r, ...cambios, actualizada: ahora() });
}

function exigir(id: string): Recompensa {
  const r = recompensas.obtener(id);
  if (!r) throw new ErrorNegocio("Recompensa no encontrada", 404);
  return r;
}

export function verificarToken(r: Recompensa, token: string | undefined): boolean {
  if (!token) return false;
  const a = Buffer.from(hashToken(token), "hex");
  const b = Buffer.from(r.tokenHash, "hex");
  return a.length === b.length && timingSafeEqual(a, b);
}

/** Evita que dos pedidos operen a la vez sobre la misma recompensa. */
const enCurso = new Set<string>();
async function conCandado<T>(id: string, fn: () => Promise<T>): Promise<T> {
  if (enCurso.has(id)) throw new ErrorNegocio("Hay una operación en curso; intenta en unos segundos", 409);
  enCurso.add(id);
  try {
    return await fn();
  } finally {
    enCurso.delete(id);
  }
}

// ---------- vistas ----------

export const urlReclamo = (r: Recompensa) => `${config.urlPublica}/r/${r.id}?c=${r.codigo}`;

const enmascarar = (g: string) => `${g.slice(0, 4)}…${g.slice(-4)}`;

/** Lo que ve cualquiera (página de reclamo): sin datos personales del reclamante. */
export function vistaPublica(r: Recompensa) {
  return {
    id: r.id,
    nombreDueno: r.nombreDueno,
    mensaje: r.mensaje,
    montoBs: r.montoBs,
    montoUsdc: r.montoUsdc,
    estado: r.estado,
    contractId: r.contractId,
    enlaces: r.contractId
      ? { escrow: explorador.escrow(r.contractId), contrato: explorador.contrato(r.contractId) }
      : undefined,
    reclamada: !!r.reclamo,
    reclamante: r.reclamo ? { nombre: r.reclamo.nombre, direccion: enmascarar(r.reclamo.direccion) } : undefined,
    movimientos: r.movimientos.map((m) => ({ ...m, enlace: explorador.tx(m.hash) })),
    error: r.estado === "error" ? r.error : undefined,
    creada: r.creada,
    actualizada: r.actualizada,
  };
}

/** Lo que ve el dueño (con su token): incluye el contacto de quien lo encontró. */
export function vistaDueno(r: Recompensa) {
  return {
    ...vistaPublica(r),
    codigo: r.codigo,
    urlReclamo: urlReclamo(r),
    cuentaDueno: r.dueno,
    reclamo: r.reclamo,
    error: r.error,
  };
}

// ---------- operaciones ----------

export function crear(p: { dispositivoId: string; nombreDueno: string; mensaje?: string; montoBs: number }) {
  if (!p.dispositivoId || p.dispositivoId.length > 128) throw new ErrorNegocio("dispositivoId inválido");
  if (!p.nombreDueno?.trim()) throw new ErrorNegocio("Falta el nombre del dueño");
  const montoBs = Math.round(Number(p.montoBs) * 100) / 100;
  const montoUsdc = Math.round((montoBs / config.tasaBsPorUsdc) * 100) / 100;
  if (!(montoUsdc >= 0.1)) throw new ErrorNegocio("El monto es demasiado bajo");
  if (montoUsdc > config.maxRecompensaUsdc) {
    throw new ErrorNegocio(`El máximo es ${Math.floor(config.maxRecompensaUsdc * config.tasaBsPorUsdc)} Bs`);
  }

  const activa = recompensas
    .todos()
    .find((r) => r.dispositivoId === p.dispositivoId && ["creando", "activa", "reclamada"].includes(r.estado));
  if (activa) throw new ErrorNegocio("Este celular ya tiene una recompensa activa", 409);

  const token = idCorto(24);
  const dueno = cuentaDe(p.dispositivoId);
  const id = idCorto(8);
  const creada = recompensas.guardar(id, {
    id,
    codigo: codigoDevolucion(),
    dispositivoId: p.dispositivoId,
    nombreDueno: p.nombreDueno.trim().slice(0, 60),
    mensaje: (p.mensaje ?? "").trim().slice(0, 200),
    montoBs,
    montoUsdc,
    estado: "creando",
    dueno: dueno.publicKey(),
    movimientos: [],
    tokenHash: hashToken(token),
    creada: ahora(),
    actualizada: ahora(),
  });

  // Desplegar y fondear lleva varias transacciones: se hace en segundo plano.
  void desplegarYFondear(id, dueno).catch((e) => {
    console.error(`[recompensas] ${id}:`, e);
    guardar(exigir(id), { estado: "error", error: String(e?.message ?? e) });
  });

  return { recompensa: creada, token };
}

async function desplegarYFondear(id: string, dueno: Keypair) {
  const plat = plataforma();
  let r = exigir(id);
  await asegurarTrustline(plat);
  await asegurarUsdc(dueno, r.montoUsdc);

  const desp = await desplegarEscrow(
    {
      engagementId: `recompensa-${r.id}`,
      titulo: `Recompensa por devolver el celular de ${r.nombreDueno}`,
      descripcion: "Red Centinela · recompensa garantizada por devolver un celular perdido",
      monto: r.montoUsdc,
      hito: "Celular devuelto a su dueño",
      roles: {
        approver: dueno.publicKey(),
        serviceProvider: plat.publicKey(),
        platformAddress: plat.publicKey(),
        releaseSigner: plat.publicKey(),
        disputeResolver: plat.publicKey(),
        receiver: dueno.publicKey(),
      },
    },
    plat,
  );
  r = guardar(r, {
    contractId: desp.contractId,
    movimientos: [...r.movimientos, { tipo: "despliegue", hash: desp.hash, fecha: ahora() }],
  });

  const fondeo = await fondearEscrow(desp.contractId, r.montoUsdc, dueno);
  r = exigir(id);
  guardar(r, {
    estado: r.reclamo ? "reclamada" : "activa",
    movimientos: [...r.movimientos, { tipo: "fondeo", hash: fondeo.hash, fecha: ahora() }],
  });
}

export async function reclamar(id: string, p: { codigo: string; direccion: string; nombre: string; contacto: string }) {
  const r = exigir(id);
  if (p.codigo?.toUpperCase() !== r.codigo) throw new ErrorNegocio("El código no coincide con el del celular", 403);
  if (!["creando", "activa"].includes(r.estado)) {
    throw new ErrorNegocio(r.reclamo ? "Esta recompensa ya fue reclamada" : "Esta recompensa ya no está activa", 409);
  }
  const direccion = p.direccion?.trim();
  if (!esDireccionValida(direccion)) throw new ErrorNegocio("Dirección Stellar inválida");
  if ((await saldoUsdc(direccion)) === null) {
    throw new ErrorNegocio("La cuenta no existe o no acepta USDC todavía (falta la trustline)");
  }
  if (!p.nombre?.trim() || !p.contacto?.trim()) throw new ErrorNegocio("Deja tu nombre y un contacto");
  return guardar(r, {
    estado: r.estado === "creando" ? "creando" : "reclamada",
    reclamo: { direccion, nombre: p.nombre.trim().slice(0, 60), contacto: p.contacto.trim().slice(0, 80), fecha: ahora() },
  });
}

/** El dueño rechaza un reclamo (por ejemplo, no fue esa persona quien lo devolvió). */
export function rechazarReclamo(id: string) {
  const r = exigir(id);
  if (r.estado !== "reclamada") throw new ErrorNegocio("No hay un reclamo pendiente", 409);
  return guardar(r, { estado: "activa", reclamo: undefined });
}

/** Reparte todo el saldo del escrow a `destino` mediante disputa + resolución. */
async function repartir(r: Recompensa, destino: string, tipo: "pago" | "devolucion") {
  const contrato = r.contractId;
  if (!contrato) throw new ErrorNegocio("El escrow todavía no está listo", 409);
  const dueno = cuentaDe(r.dispositivoId);
  const plat = plataforma();

  if (!r.movimientos.some((m) => m.tipo === "disputa")) {
    const d = await abrirDisputa(contrato, dueno);
    r = guardar(r, { movimientos: [...r.movimientos, { tipo: "disputa", hash: d.hash, fecha: ahora() }] });
  }
  const saldo = (await saldoEscrow(contrato)) ?? r.montoUsdc;
  const res = await resolverDisputa(contrato, [{ address: destino, amount: saldo }], plat);
  return guardar(r, {
    estado: tipo === "pago" ? "pagada" : "devuelta",
    error: undefined,
    movimientos: [...r.movimientos, { tipo, hash: res.hash, fecha: ahora() }],
  });
}

/** El dueño confirma que recuperó el celular: se paga a quien lo devolvió. */
export function confirmarDevolucion(id: string) {
  return conCandado(id, async () => {
    const r = exigir(id);
    if (r.estado === "pagada") return r;
    if (!r.reclamo) throw new ErrorNegocio("Nadie reclamó la recompensa todavía", 409);
    if (!["reclamada", "pagando", "error"].includes(r.estado)) throw new ErrorNegocio("Estado no válido para pagar", 409);
    const enPago = guardar(r, { estado: "pagando" });
    try {
      return await repartir(enPago, r.reclamo.direccion, "pago");
    } catch (e: any) {
      guardar(exigir(id), { estado: "error", error: String(e?.message ?? e) });
      throw e;
    }
  });
}

/** El dueño cancela (lo encontró solo, o ya no quiere ofrecerla): los fondos vuelven a él. */
export function cancelar(id: string) {
  return conCandado(id, async () => {
    const r = exigir(id);
    if (r.estado === "devuelta") return r;
    if (["pagada", "pagando", "creando"].includes(r.estado)) throw new ErrorNegocio("No se puede cancelar ahora", 409);
    const enDevolucion = guardar(r, { estado: "devolviendo" });
    try {
      return await repartir(enDevolucion, r.dueno, "devolucion");
    } catch (e: any) {
      guardar(exigir(id), { estado: "error", error: String(e?.message ?? e) });
      throw e;
    }
  });
}

export const obtener = exigir;
