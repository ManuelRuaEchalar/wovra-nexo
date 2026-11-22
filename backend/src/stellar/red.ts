import { Asset, Horizon, Keypair, Networks, Operation, StrKey, TransactionBuilder } from "@stellar/stellar-sdk";
import { config } from "../config.ts";

export const horizon = new Horizon.Server(config.horizonUrl);
export const redPassphrase = Networks.TESTNET;
export const USDC = new Asset("USDC", config.usdcEmisor);

export const esDireccionValida = (g: string) => StrKey.isValidEd25519PublicKey(g);

export async function existeCuenta(g: string): Promise<boolean> {
  try {
    await horizon.loadAccount(g);
    return true;
  } catch {
    return false;
  }
}

/** Saldo de USDC de una cuenta, o `null` si no tiene la trustline (o la cuenta no existe). */
export async function saldoUsdc(g: string): Promise<number | null> {
  try {
    const cuenta = await horizon.loadAccount(g);
    const linea = cuenta.balances.find(
      (b) => "asset_code" in b && b.asset_code === USDC.code && b.asset_issuer === USDC.issuer,
    );
    return linea ? Number(linea.balance) : null;
  } catch {
    return null;
  }
}

/** Crea la cuenta con friendbot (solo testnet) si todavía no existe. */
export async function asegurarCuenta(kp: Keypair): Promise<void> {
  if (await existeCuenta(kp.publicKey())) return;
  const r = await fetch(`${config.friendbotUrl}?addr=${kp.publicKey()}`);
  if (!r.ok && !(await existeCuenta(kp.publicKey()))) {
    throw new Error(`friendbot no pudo crear la cuenta (${r.status})`);
  }
}

/** Crea la cuenta (testnet) y su trustline de USDC si faltan. Trustless Work exige la trustline en cada rol. */
export async function asegurarTrustline(kp: Keypair): Promise<void> {
  await asegurarCuenta(kp);
  if ((await saldoUsdc(kp.publicKey())) !== null) return;
  const cuenta = await horizon.loadAccount(kp.publicKey());
  const tx = new TransactionBuilder(cuenta, { fee: "10000", networkPassphrase: redPassphrase })
    .addOperation(Operation.changeTrust({ asset: USDC }))
    .setTimeout(60)
    .build();
  tx.sign(kp);
  await horizon.submitTransaction(tx);
}

/**
 * Deja la cuenta con al menos `minimo` USDC de testnet: crea la trustline si falta
 * y compra USDC con XLM en el exchange de Stellar (path payment).
 */
export async function asegurarUsdc(kp: Keypair, minimo: number): Promise<number> {
  await asegurarCuenta(kp);
  const saldo = await saldoUsdc(kp.publicKey());
  if (saldo !== null && saldo >= minimo) return saldo;

  const falta = Math.max(minimo - (saldo ?? 0), 0) + 1;
  const caminos = await horizon.strictReceivePaths([Asset.native()], USDC, falta.toFixed(7)).call();
  if (caminos.records.length === 0) throw new Error("No hay liquidez XLM→USDC en testnet ahora mismo");
  const xlmNecesario = Number(caminos.records[0].source_amount) * 1.05;

  const cuenta = await horizon.loadAccount(kp.publicKey());
  const tx = new TransactionBuilder(cuenta, { fee: "10000", networkPassphrase: redPassphrase });
  if (saldo === null) tx.addOperation(Operation.changeTrust({ asset: USDC }));
  tx.addOperation(
    Operation.pathPaymentStrictReceive({
      sendAsset: Asset.native(),
      sendMax: xlmNecesario.toFixed(7),
      destination: kp.publicKey(),
      destAsset: USDC,
      destAmount: falta.toFixed(7),
      path: [],
    }),
  );
  const firmada = tx.setTimeout(60).build();
  firmada.sign(kp);
  await horizon.submitTransaction(firmada);
  return (await saldoUsdc(kp.publicKey())) ?? 0;
}

/** Firma un XDR sin firmar devuelto por Trustless Work y devuelve el XDR firmado y su hash. */
export function firmarXdr(xdr: string, kp: Keypair): { xdrFirmado: string; hash: string } {
  const tx = TransactionBuilder.fromXDR(xdr, redPassphrase);
  tx.sign(kp);
  return { xdrFirmado: tx.toXDR(), hash: Buffer.from(tx.hash()).toString("hex") };
}
