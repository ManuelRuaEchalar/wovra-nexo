import { Keypair, Memo, Operation, TransactionBuilder } from "@stellar/stellar-sdk";
import { Almacen } from "../compartido/almacen.ts";
import { config, explorador } from "../config.ts";
import { ErrorNegocio } from "../compartido/errores.ts";
import { asegurarTrustline, asegurarUsdc, horizon, redPassphrase, USDC } from "../stellar/red.ts";

/**
 * Micropagos a los nodos de la red.
 *
 * Un "nodo" es cualquier teléfono con la app que captó por Bluetooth un
 * celular ajeno en alerta y entregó el aviso (cifrado, sin poder leerlo).
 * Cuando la app de alguien del círculo del dueño lo descifra y comprueba que
 * es válido y nuevo, lo confirma aquí y el nodo gana un pago fijo en USDC.
 *
 * De dónde sale el dinero: un fondo de la red en Stellar que la startup
 * alimenta con una parte de cada suscripción. Los pagos se agrupan en una
 * sola transacción cada pocos minutos (hasta 50 pagos por transacción).
 *
 * Además, el dueño puede compartir un porcentaje de su recompensa con los
 * nodos que vieron su celular durante la pérdida (ver `nodosQueVieron`).
 *
 * Etapa de demo (testnet): el servidor custodia la cuenta de cada nodo.
 */

type Nodo = {
  id: string;
  secreta: string;
  avisos: number;
  ganado: number;
  pagado: number;
  pagos: { hash: string; monto: number; fecha: string; motivo: "aviso" | "recompensa" }[];
  creado: string;
};

type Aviso = { idEquipo: string; idNodo: string; en: number; fecha: string };

const nodos = new Almacen<Nodo>("nodos");
const avisos = new Almacen<Aviso>("avisos");
const claves = new Almacen<string>("fondo");

const DIA = 24 * 3_600_000;
const redondear = (n: number) => Math.floor(n * 1e7) / 1e7;

function fondo(): Keypair {
  if (config.fondoSecreta) return Keypair.fromSecret(config.fondoSecreta);
  const s = claves.obtener("fondo") ?? claves.guardar("fondo", Keypair.random().secret());
  return Keypair.fromSecret(s);
}

function nodo(id: string): Nodo {
  return (
    nodos.obtener(id) ??
    nodos.guardar(id, {
      id,
      secreta: Keypair.random().secret(),
      avisos: 0,
      ganado: 0,
      pagado: 0,
      pagos: [],
      creado: new Date().toISOString(),
    })
  );
}

const hex = (v: unknown, nombre: string, largo: number) => {
  const s = String(v ?? "");
  if (!new RegExp(`^[0-9a-f]{${largo}}$`).test(s)) throw new ErrorNegocio(`${nombre} inválido`);
  return s;
};

/**
 * Alguien del círculo confirma que el aviso que entregó `idNodo` sobre el
 * equipo `idEquipo` era válido y nuevo. Devuelve si genera pago.
 */
export function confirmarAviso(cuerpo: Record<string, unknown>): { pagado: boolean; motivo?: string } {
  const idEquipo = hex(cuerpo.idEquipo, "Id del equipo", 8);
  const idNodo = hex(cuerpo.idNodo, "Id del nodo", 16);
  const en = Number(cuerpo.en);
  const ahora = Date.now();
  if (!Number.isFinite(en) || en > ahora + 10 * 60_000 || en < ahora - 13 * 3_600_000) {
    throw new ErrorNegocio("Hora del aviso inválida");
  }
  const clave = `${idEquipo}:${idNodo}:${Math.floor(en / 1000)}`;
  if (avisos.obtener(clave)) return { pagado: false, motivo: "ya confirmado" };

  const recientes = avisos.todos().filter((a) => a.en > ahora - DIA);
  const delPar = recientes.filter((a) => a.idEquipo === idEquipo && a.idNodo === idNodo);
  // Límites contra abusos: un nodo cobra a lo sumo un aviso por minuto de un
  // mismo equipo, el equipo paga a lo sumo 30 avisos por día y el nodo 100.
  if (delPar.some((a) => Math.abs(a.en - en) < 60_000)) return { pagado: false, motivo: "muy seguido" };
  if (recientes.filter((a) => a.idEquipo === idEquipo).length >= config.avisosMaxPorEquipoDia) {
    return { pagado: false, motivo: "tope del equipo" };
  }
  if (recientes.filter((a) => a.idNodo === idNodo).length >= config.avisosMaxPorNodoDia) {
    return { pagado: false, motivo: "tope del nodo" };
  }

  avisos.guardar(clave, { idEquipo, idNodo, en, fecha: new Date().toISOString() });
  const n = nodo(idNodo);
  nodos.guardar(idNodo, { ...n, avisos: n.avisos + 1, ganado: redondear(n.ganado + config.pagoPorAvisoUsdc) });
  return { pagado: true };
}

/** Nodos que entregaron avisos válidos de este equipo desde `desde` (para compartir la recompensa). */
export function nodosQueVieron(idEquipo: string, desde: number): { id: string; direccion: string; kp: Keypair }[] {
  const ids = [...new Set(avisos.todos().filter((a) => a.idEquipo === idEquipo && a.en >= desde).map((a) => a.idNodo))];
  return ids.map((id) => {
    const kp = Keypair.fromSecret(nodo(id).secreta);
    return { id, direccion: kp.publicKey(), kp };
  });
}

/** Anota en el nodo lo que recibió de una recompensa (el pago ya ocurrió en el escrow). */
export function anotarParteRecompensa(idNodo: string, monto: number, hash: string) {
  const n = nodo(idNodo);
  nodos.guardar(idNodo, {
    ...n,
    pagos: [...n.pagos, { hash, monto, fecha: new Date().toISOString(), motivo: "recompensa" }],
  });
}

export function vistaNodo(id: string) {
  const idNodo = hex(id, "Id del nodo", 16);
  const n = nodos.obtener(idNodo);
  if (!n) return { avisos: 0, ganadoUsdc: 0, pagadoUsdc: 0, pendienteUsdc: 0, recompensasUsdc: 0, pagos: [] };
  const direccion = Keypair.fromSecret(n.secreta).publicKey();
  const deRecompensas = n.pagos.filter((p) => p.motivo === "recompensa").reduce((s, p) => s + p.monto, 0);
  return {
    avisos: n.avisos,
    ganadoUsdc: n.ganado,
    pagadoUsdc: n.pagado,
    pendienteUsdc: redondear(n.ganado - n.pagado),
    recompensasUsdc: redondear(deRecompensas),
    direccion,
    enlaceCuenta: explorador.cuenta(direccion),
    pagos: n.pagos.slice(-10).map((p) => ({ ...p, enlace: explorador.tx(p.hash) })),
  };
}

let pagando = false;

/** Paga en una sola transacción lo pendiente de cada nodo (hasta 50 nodos por vez). */
export async function pagarNodos(): Promise<string | null> {
  if (pagando) return null;
  pagando = true;
  try {
    const pendientes = nodos
      .todos()
      .map((n) => ({ n, monto: redondear(n.ganado - n.pagado) }))
      .filter((x) => x.monto >= config.pagoPorAvisoUsdc)
      .slice(0, 50);
    if (pendientes.length === 0) return null;

    const f = fondo();
    const total = pendientes.reduce((s, x) => s + x.monto, 0);
    await asegurarUsdc(f, total);
    // Cada nodo necesita su cuenta y la trustline de USDC (en mainnet, patrocinadas por el fondo).
    for (const x of pendientes) await asegurarTrustline(Keypair.fromSecret(x.n.secreta));

    const cuenta = await horizon.loadAccount(f.publicKey());
    const tx = new TransactionBuilder(cuenta, { fee: "10000", networkPassphrase: redPassphrase });
    for (const x of pendientes) {
      tx.addOperation(
        Operation.payment({
          destination: Keypair.fromSecret(x.n.secreta).publicKey(),
          asset: USDC,
          amount: x.monto.toFixed(7),
        }),
      );
    }
    const firmada = tx.addMemo(Memo.text("Red: avisos utiles")).setTimeout(60).build();
    firmada.sign(f);
    const r = await horizon.submitTransaction(firmada);
    const fecha = new Date().toISOString();
    for (const x of pendientes) {
      const n = nodos.obtener(x.n.id)!;
      nodos.guardar(n.id, {
        ...n,
        pagado: redondear(n.pagado + x.monto),
        pagos: [...n.pagos, { hash: r.hash, monto: x.monto, fecha, motivo: "aviso" }],
      });
    }
    console.log(`[nodos] pagados ${pendientes.length} nodos (${total.toFixed(2)} USDC): ${r.hash}`);
    return r.hash;
  } finally {
    pagando = false;
  }
}

export function cuentaFondo(): string {
  return fondo().publicKey();
}
