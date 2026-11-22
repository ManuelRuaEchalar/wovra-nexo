import { useEffect, useState } from 'react'
import { usePollar } from '@pollar/react'
import type { WalletInfo } from '@pollar/core'
import { Asset, Horizon, Keypair, Networks, Operation, TransactionBuilder } from '@stellar/stellar-sdk'
import type { ConfigPublica } from './api'
import type { TextosSitio } from './textosSitio'

/**
 * Dos maneras de tener una cuenta Stellar para cobrar:
 * - Pollar: login con Google o email, billetera sin frase semilla (la recomendada).
 * - Cuenta de prueba: se crea en el navegador, solo para testnet y demos.
 */

export interface PropsCuenta {
  config: ConfigPublica
  onDireccion: (direccion: string | null) => void
  t: TextosSitio['cuentas']
}

const horizon = new Horizon.Server('https://horizon-testnet.stellar.org')

/** Texto de error con el detalle que devuelve Pollar (el SDK solo pone el código en `message`). */
function textoError(e: unknown): string {
  if (!(e instanceof Error)) return String(e)
  const x = e as Error & { body?: unknown; details?: string }
  const cuerpo = x.body && typeof x.body === 'object' && Object.keys(x.body).length ? JSON.stringify(x.body) : ''
  return [x.message, x.details, cuerpo].filter(Boolean).join(' · ')
}

async function tieneUsdc(g: string, usdc: ConfigPublica['usdc']): Promise<boolean | null> {
  try {
    const cuenta = await horizon.loadAccount(g)
    return cuenta.balances.some(
      (b) => 'asset_code' in b && b.asset_code === usdc.codigo && b.asset_issuer === usdc.emisor,
    )
  } catch {
    return null // la cuenta todavía no existe en la red
  }
}

/** La billetera Stellar del usuario, aunque Pollar le haya creado también otras (Solana, Polygon). */
function billeteraStellar(wallet: WalletInfo | null, wallets: WalletInfo[]): WalletInfo | null {
  const esStellar = (w: WalletInfo) => w.chain === 'STELLAR' || (!w.chain && /^G[A-Z2-7]{55}$/.test(w.address))
  return wallets.find(esStellar) ?? (wallet && esStellar(wallet) ? wallet : null)
}

export function CuentaPollar({ config, onDireccion, t }: PropsCuenta) {
  const { isAuthenticated, wallet, wallets, hasWallet, createWallet, login, logout, setTrustline } = usePollar()
  const [email, setEmail] = useState('')
  const [usdcListo, setUsdcListo] = useState<boolean | null>(null)
  const [trabajando, setTrabajando] = useState(false)
  const [error, setError] = useState('')
  const [esperandoDesde] = useState(() => Date.now())
  const [, refrescar] = useState(0)

  const stellar = billeteraStellar(wallet, wallets ?? [])
  const direccion = stellar?.address ?? null
  const creando = stellar?.provisioning === 'CREATING'
  const fallo = stellar?.provisioning === 'FAILED'
  const lista = !!direccion && !creando && !fallo

  // Mientras se espera, refresca el aviso de "tarda más de lo normal".
  useEffect(() => {
    if (lista || !isAuthenticated) return
    const t = setInterval(() => refrescar((n) => n + 1), 5000)
    return () => clearInterval(t)
  }, [lista, isAuthenticated])

  useEffect(() => {
    if (!lista || !direccion) return
    let vivo = true
    const revisar = async () => {
      const ok = await tieneUsdc(direccion, config.usdc)
      if (vivo) setUsdcListo(ok)
    }
    void revisar()
    const t = setInterval(revisar, 5000)
    return () => {
      vivo = false
      clearInterval(t)
    }
  }, [lista, direccion, config.usdc])

  useEffect(() => {
    onDireccion(lista && usdcListo ? direccion : null)
  }, [lista, usdcListo, direccion, onDireccion])

  const crearBilletera = async () => {
    setTrabajando(true)
    setError('')
    try {
      await createWallet()
    } catch (e) {
      setError(textoError(e))
    } finally {
      setTrabajando(false)
    }
  }

  if (!isAuthenticated) {
    return (
      <div className="pila">
        <button className="boton" onClick={() => login({ provider: 'google' })}>
          {t.google}
        </button>
        <form
          className="fila"
          onSubmit={(e) => {
            e.preventDefault()
            if (email.trim()) login({ provider: 'email', email: email.trim() })
          }}
        >
          <input
            type="email"
            placeholder={t.correo}
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
          <button className="boton secundario" type="submit">
            {t.entrar}
          </button>
        </form>
        <p className="nota">{t.sinFrases}</p>
      </div>
    )
  }

  // Sesión iniciada pero sin billetera Stellar: la app de Pollar la crea "a pedido",
  // o el usuario solo tiene billeteras de otras redes.
  if (!stellar || fallo) {
    return (
      <div className="pila">
        <p className="nota">
          {fallo ? t.pollarSinBilletera : hasWallet ? t.sinStellar : t.faltaBilletera}
        </p>
        <button className="boton" disabled={trabajando} onClick={crearBilletera}>
          {trabajando ? t.creando : t.crearStellar}
        </button>
        {error && <p className="error">{error}</p>}
        <p className="nota">
          {t.diagnostico}: {JSON.stringify((wallets ?? []).map((w) => [w.chain ?? '?', w.custody, w.provisioning]))},
          principal {wallet ? `${wallet.chain ?? '?'}/${wallet.provisioning ?? '?'}` : 'ninguna'}
        </p>
        <button className="enlace" onClick={() => logout()}>
          {t.salir}
        </button>
      </div>
    )
  }

  if (creando) {
    const segundos = Math.round((Date.now() - esperandoDesde) / 1000)
    return (
      <div className="pila">
        <p className="nota">{t.creandoStellar}</p>
        {segundos > 45 && (
          <>
            <p className="nota">{t.tarda}</p>
            <button className="enlace" onClick={() => location.reload()}>
              {t.recargar}
            </button>
          </>
        )}
      </div>
    )
  }

  return (
    <div className="pila">
      <p className="cuenta">
        {t.tuBilletera}: <code>{direccion?.slice(0, 6)}…{direccion?.slice(-6)}</code>
        <button className="enlace" onClick={() => logout()}>
          {t.salir}
        </button>
      </p>
      {usdcListo === false && (
        <button
          className="boton secundario"
          disabled={trabajando}
          onClick={async () => {
            setTrabajando(true)
            setError('')
            try {
              await setTrustline({ code: config.usdc.codigo, issuer: config.usdc.emisor })
              setUsdcListo(await tieneUsdc(direccion!, config.usdc))
            } catch (e) {
              setError(textoError(e))
            } finally {
              setTrabajando(false)
            }
          }}
        >
          {trabajando ? t.activando : t.activarUsdc}
        </button>
      )}
      {usdcListo === null && <p className="nota">{t.activandoRed}</p>}
      {error && <p className="error">{error}</p>}
    </div>
  )
}

const CLAVE_LOCAL = 'rc-cuenta-prueba'

export function CuentaPrueba({ config, onDireccion, t }: PropsCuenta) {
  const [kp, setKp] = useState<Keypair | null>(() => {
    const s = localStorage.getItem(CLAVE_LOCAL)
    return s ? Keypair.fromSecret(s) : null
  })
  const [lista, setLista] = useState(false)
  const [trabajando, setTrabajando] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!kp) return
    void tieneUsdc(kp.publicKey(), config.usdc).then((ok) => setLista(!!ok))
  }, [kp, config.usdc])

  useEffect(() => {
    onDireccion(lista && kp ? kp.publicKey() : null)
  }, [lista, kp, onDireccion])

  const crear = async () => {
    setTrabajando(true)
    setError('')
    try {
      const nuevo = kp ?? Keypair.random()
      localStorage.setItem(CLAVE_LOCAL, nuevo.secret())
      setKp(nuevo)
      if ((await tieneUsdc(nuevo.publicKey(), config.usdc)) === null) {
        const r = await fetch(`https://friendbot.stellar.org?addr=${nuevo.publicKey()}`)
        if (!r.ok) throw new Error(t.pruebaFalla)
      }
      if (!(await tieneUsdc(nuevo.publicKey(), config.usdc))) {
        const cuenta = await horizon.loadAccount(nuevo.publicKey())
        const tx = new TransactionBuilder(cuenta, { fee: '10000', networkPassphrase: Networks.TESTNET })
          .addOperation(Operation.changeTrust({ asset: new Asset(config.usdc.codigo, config.usdc.emisor) }))
          .setTimeout(60)
          .build()
        tx.sign(nuevo)
        await horizon.submitTransaction(tx)
      }
      setLista(true)
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setTrabajando(false)
    }
  }

  if (lista && kp) {
    return (
      <div className="pila">
        <p className="cuenta">
          {t.cuentaPrueba}: <code>{kp.publicKey().slice(0, 6)}…{kp.publicKey().slice(-6)}</code>
        </p>
        <p className="nota">{t.soloNavegador}</p>
      </div>
    )
  }
  return (
    <div className="pila">
      <button className="boton" disabled={trabajando} onClick={crear}>
        {trabajando ? t.creandoCuenta : t.crearPrueba}
      </button>
      {error && <p className="error">{error}</p>}
    </div>
  )
}
