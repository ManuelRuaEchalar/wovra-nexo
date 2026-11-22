import type { Keypair } from "@stellar/stellar-sdk";
import { config } from "./config.ts";
import { firmarXdr, USDC } from "./stellar.ts";

/**
 * Cliente mínimo de la API REST de Trustless Work (escrow sobre Soroban).
 * Cada operación devuelve un XDR sin firmar: lo firmamos con la clave del rol
 * correspondiente y lo enviamos con /helper/send-transaction.
 * Docs: https://docs.trustlesswork.com/trustless-work/api-rest/introduction
 */

export class ErrorTrustless extends Error {
  readonly estado: number;
  readonly detalle: unknown;
  constructor(mensaje: string, estado: number, detalle?: unknown) {
    super(mensaje);
    this.estado = estado;
    this.detalle = detalle;
  }
}

async function llamar(metodo: "GET" | "POST", ruta: string, cuerpo?: unknown): Promise<any> {
  if (!config.twApiKey) {
    throw new ErrorTrustless("Falta TW_API_KEY: pide una clave en https://dapp.trustlesswork.com", 503);
  }
  const r = await fetch(`${config.twApiUrl}${ruta}`, {
    method: metodo,
    headers: { "content-type": "application/json", "x-api-key": config.twApiKey },
    body: cuerpo === undefined ? undefined : JSON.stringify(cuerpo),
  });
  const texto = await r.text();
  let datos: any;
  try {
    datos = texto ? JSON.parse(texto) : {};
  } catch {
    datos = { message: texto };
  }
  if (!r.ok) {
    const msg = Array.isArray(datos?.message) ? datos.message.join("; ") : (datos?.message ?? r.statusText);
    throw new ErrorTrustless(`Trustless Work ${ruta}: ${msg}`, r.status, datos);
  }
  return datos;
}

export interface ResultadoTx {
  hash: string;
  respuesta: any;
}

/** Pide una operación, la firma con `firmante` y la envía. */
async function operar(ruta: string, cuerpo: unknown, firmante: Keypair): Promise<ResultadoTx> {
  const pedido = await llamar("POST", ruta, cuerpo);
  const xdr: string | undefined = pedido.unsignedTransaction;
  if (!xdr) throw new ErrorTrustless(`Trustless Work ${ruta}: no devolvió transacción`, 502, pedido);
  const { xdrFirmado, hash } = firmarXdr(xdr, firmante);
  const respuesta = await llamar("POST", "/helper/send-transaction", { signedXdr: xdrFirmado });
  if (respuesta?.status && String(respuesta.status).toUpperCase() !== "SUCCESS") {
    throw new ErrorTrustless(`Trustless Work ${ruta}: ${respuesta.message ?? "transacción rechazada"}`, 502, respuesta);
  }
  return { hash, respuesta };
}

export interface RolesEscrow {
  approver: string;
  serviceProvider: string;
  platformAddress: string;
  releaseSigner: string;
  disputeResolver: string;
  receiver: string;
}

export async function desplegarEscrow(
  p: { engagementId: string; titulo: string; descripcion: string; monto: number; roles: RolesEscrow; hito: string },
  plataforma: Keypair,
): Promise<ResultadoTx & { contractId: string }> {
  const r = await operar(
    "/deployer/single-release",
    {
      signer: plataforma.publicKey(),
      engagementId: p.engagementId,
      title: p.titulo,
      description: p.descripcion,
      roles: p.roles,
      amount: p.monto,
      platformFee: 0,
      milestones: [{ description: p.hito }],
      trustline: { address: USDC.issuer, symbol: USDC.code },
    },
    plataforma,
  );
  const contractId: string | undefined = r.respuesta?.contractId ?? r.respuesta?.escrow?.contractId;
  if (!contractId) throw new ErrorTrustless("El despliegue no devolvió contractId", 502, r.respuesta);
  return { ...r, contractId };
}

export const fondearEscrow = (contractId: string, monto: number, depositante: Keypair) =>
  operar("/escrow/single-release/fund-escrow", { contractId, signer: depositante.publicKey(), amount: monto }, depositante);

/** Abre disputa: la puede abrir el aprobador o el proveedor del servicio. */
export const abrirDisputa = (contractId: string, firmante: Keypair) =>
  operar("/escrow/single-release/dispute-escrow", { contractId, signer: firmante.publicKey() }, firmante);

/** Reparte el saldo del escrow en disputa. La suma debe ser igual al saldo actual. */
export const resolverDisputa = (
  contractId: string,
  reparto: { address: string; amount: number }[],
  arbitro: Keypair,
) =>
  operar(
    "/escrow/single-release/resolve-dispute",
    { contractId, disputeResolver: arbitro.publicKey(), distributions: reparto },
    arbitro,
  );

/** Saldo actual del escrow según Trustless Work, o `null` si no se pudo leer. */
export async function saldoEscrow(contractId: string): Promise<number | null> {
  try {
    const r = await llamar("GET", `/helper/get-multiple-escrow-balance?addresses=${encodeURIComponent(contractId)}`);
    const lista = Array.isArray(r) ? r : (r?.data ?? r?.balances ?? []);
    const item = lista.find?.((x: any) => x.address === contractId) ?? lista[0];
    const saldo = Number(item?.balance);
    return Number.isFinite(saldo) ? saldo : null;
  } catch {
    return null;
  }
}
