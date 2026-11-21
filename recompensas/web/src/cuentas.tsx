import { useEffect, useState } from 'react'
import { usePollar } from '@pollar/react'
import { Asset, Horizon, Keypair, Networks, Operation, TransactionBuilder } from '@stellar/stellar-sdk'
import type { ConfigPublica } from './api'

/**
 * Dos maneras de tener una cuenta Stellar para cobrar:
 * - Pollar: login con Google o email, billetera sin frase semilla (la recomendada).
 * - Cuenta de prueba: se crea en el navegador, solo para testnet y demos.
 */

export interface PropsCuenta {
  config: ConfigPublica
  onDireccion: (direccion: string | null) => void
}

const horizon = new Horizon.Server('https://horizon-testnet.stellar.org')

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

export function CuentaPollar({ config, onDireccion }: PropsCuenta) {
  const { isAuthenticated, wallet, login, logout, setTrustline } = usePollar()
  const [email, setEmail] = useState('')
  const [usdcListo, setUsdcListo] = useState<boolean | null>(null)
  const [trabajando, setTrabajando] = useState(false)
  const [error, setError] = useState('')

  const direccion = wallet?.chain && wallet.chain !== 'STELLAR' ? null : (wallet?.address ?? null)
  const lista = !!direccion && wallet?.provisioning !== 'CREATING'

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

  if (!isAuthenticated) {
    return (
      <div className="pila">
        <button className="boton" onClick={() => login({ provider: 'google' })}>
          Continuar con Google
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
            placeholder="o con tu correo"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
          <button className="boton secundario" type="submit">
            Entrar
          </button>
        </form>
        <p className="nota">Sin frases secretas ni apps extra: Pollar crea tu billetera al iniciar sesión.</p>
      </div>
    )
  }

  if (!lista) return <p className="nota">Creando tu billetera en Stellar…</p>

  return (
    <div className="pila">
      <p className="cuenta">
        Tu billetera: <code>{direccion?.slice(0, 6)}…{direccion?.slice(-6)}</code>
        <button className="enlace" onClick={() => logout()}>
          salir
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
              setError(e instanceof Error ? e.message : String(e))
            } finally {
              setTrabajando(false)
            }
          }}
        >
          {trabajando ? 'Activando…' : 'Activar USDC en mi billetera'}
        </button>
      )}
      {usdcListo === null && <p className="nota">Activando tu cuenta en la red…</p>}
      {error && <p className="error">{error}</p>}
    </div>
  )
}

const CLAVE_LOCAL = 'rc-cuenta-prueba'

export function CuentaPrueba({ config, onDireccion }: PropsCuenta) {
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
        if (!r.ok) throw new Error('No se pudo crear la cuenta de prueba')
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
          Cuenta de prueba: <code>{kp.publicKey().slice(0, 6)}…{kp.publicKey().slice(-6)}</code>
        </p>
        <p className="nota">Se guarda solo en este navegador. Es de testnet: no vale dinero real.</p>
      </div>
    )
  }
  return (
    <div className="pila">
      <button className="boton" disabled={trabajando} onClick={crear}>
        {trabajando ? 'Creando cuenta…' : 'Crear cuenta de prueba (testnet)'}
      </button>
      {error && <p className="error">{error}</p>}
    </div>
  )
}
