import { resolve } from "node:path";

/** Configuración por variables de entorno. Ver `.env.example`. */
const env = process.env;

export const config = {
  puerto: Number(env.PORT ?? 8787),
  /** URL pública del servidor; con ella se arman los enlaces de reclamo. */
  urlPublica: (env.URL_PUBLICA ?? `http://localhost:${env.PORT ?? 8787}`).replace(/\/$/, ""),

  /** Trustless Work: API de escrow sobre Stellar (testnet por defecto). */
  twApiUrl: (env.TW_API_URL ?? "https://dev.api.trustlesswork.com").replace(/\/$/, ""),
  twApiKey: env.TW_API_KEY ?? "",

  /** Red Stellar. Solo se admite testnet en esta etapa. */
  horizonUrl: env.HORIZON_URL ?? "https://horizon-testnet.stellar.org",
  friendbotUrl: env.FRIENDBOT_URL ?? "https://friendbot.stellar.org",
  usdcEmisor: env.USDC_EMISOR ?? "GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5",

  /** Clave de la plataforma (G… pública, S… secreta). Si falta, se genera una de testnet. */
  plataformaSecreta: env.PLATAFORMA_SECRETA ?? "",

  /** Clave publicable de Pollar (pub_testnet_…) para la página de reclamo. Es pública por diseño. */
  pollarClavePublica: env.POLLAR_CLAVE_PUBLICA ?? "",

  /** Bolivianos por 1 USDC, solo para mostrar montos aproximados. */
  tasaBsPorUsdc: Number(env.TASA_BS_POR_USDC ?? 6.96),
  /** Monto máximo de una recompensa, en USDC. */
  maxRecompensaUsdc: Number(env.MAX_RECOMPENSA_USDC ?? 50),

  directorioDatos: resolve(env.DIRECTORIO_DATOS ?? "datos"),
  /** Carpeta con la página de reclamo ya compilada (recompensas/web/dist). */
  directorioWeb: resolve(env.DIRECTORIO_WEB ?? "../web/dist"),
};

export const explorador = {
  tx: (hash: string) => `https://stellar.expert/explorer/testnet/tx/${hash}`,
  cuenta: (g: string) => `https://stellar.expert/explorer/testnet/account/${g}`,
  contrato: (c: string) => `https://stellar.expert/explorer/testnet/contract/${c}`,
  escrow: (c: string) => `https://viewer.trustlesswork.com/${c}`,
};
