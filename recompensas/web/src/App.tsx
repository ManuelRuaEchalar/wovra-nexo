import { useCallback, useEffect, useState } from 'react'
import { api, type ConfigPublica, type Recompensa } from './api'
import { CuentaPollar, CuentaPrueba } from './cuentas'

function leerUrl() {
  const id = location.pathname.match(/^\/r\/([\w-]+)/)?.[1] ?? null
  const codigo = new URLSearchParams(location.search).get('c') ?? ''
  return { id, codigo }
}

const bs = (n: number) => n.toLocaleString('es-BO', { maximumFractionDigits: 2 })

export default function App({ config }: { config: ConfigPublica }) {
  const { id, codigo: codigoUrl } = leerUrl()
  const [r, setR] = useState<Recompensa | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!id) return
    let vivo = true
    const cargar = () =>
      api
        .recompensa(id)
        .then((x) => vivo && setR(x))
        .catch((e) => vivo && setError(e.message))
    void cargar()
    const t = setInterval(cargar, 5000)
    return () => {
      vivo = false
      clearInterval(t)
    }
  }, [id])

  if (!id) return <Marco><Inicio /></Marco>
  if (error && !r) return <Marco><p className="error">{error}</p></Marco>
  if (!r) return <Marco><p className="nota">Cargando…</p></Marco>

  return (
    <Marco>
      <Cabecera r={r} />
      <Cuerpo r={r} config={config} codigoUrl={codigoUrl} onCambio={setR} />
      <Verificacion r={r} />
    </Marco>
  )
}

function Marco({ children }: { children: React.ReactNode }) {
  return (
    <main>
      <header className="marca">
        <span className="halo" aria-hidden /> Red Centinela
      </header>
      <section className="tarjeta">{children}</section>
      <footer>
        Recompensa en un escrow de Trustless Work sobre Stellar <span className="etiqueta">testnet</span>
      </footer>
    </main>
  )
}

function Inicio() {
  return (
    <>
      <h1>Recompensa garantizada</h1>
      <p>
        Si encontraste un celular protegido por Red Centinela, escanea el código que aparece en su pantalla
        bloqueada.
      </p>
    </>
  )
}

function Cabecera({ r }: { r: Recompensa }) {
  return (
    <div className="cabecera">
      <p className="sobre">Encontraste el celular de {r.nombreDueno}</p>
      <p className="monto">
        {bs(r.montoBs)} <span>Bs</span>
      </p>
      <p className="nota">
        ≈ {r.montoUsdc.toFixed(2)} USDC bloqueados para quien lo devuelva
      </p>
      {r.mensaje && <blockquote>“{r.mensaje}”</blockquote>}
    </div>
  )
}

function Cuerpo({
  r,
  config,
  codigoUrl,
  onCambio,
}: {
  r: Recompensa
  config: ConfigPublica
  codigoUrl: string
  onCambio: (r: Recompensa) => void
}) {
  switch (r.estado) {
    case 'creando':
      return <Aviso tono="espera">El dueño está bloqueando la recompensa en la red. Tarda unos segundos…</Aviso>
    case 'error':
      return <Aviso tono="alerta">Hubo un problema con la recompensa. El dueño ya fue avisado.</Aviso>
    case 'pagada':
      return <Aviso tono="ok">¡Gracias por devolverlo! La recompensa ya fue pagada{r.reclamante ? ` a ${r.reclamante.nombre}` : ''}.</Aviso>
    case 'devuelta':
      return <Aviso tono="alerta">El dueño retiró esta recompensa.</Aviso>
    case 'reclamada':
    case 'pagando':
    case 'devolviendo':
      return (
        <Aviso tono="espera">
          {r.reclamante?.nombre ?? 'Alguien'} ya reclamó esta recompensa. Cuando {r.nombreDueno} confirme que
          recuperó su celular, se paga a su billetera ({r.reclamante?.direccion}).
          {r.estado === 'pagando' && ' Pagando ahora…'}
        </Aviso>
      )
    case 'activa':
      return <Reclamo r={r} config={config} codigoUrl={codigoUrl} onCambio={onCambio} />
  }
}

function Reclamo({
  r,
  config,
  codigoUrl,
  onCambio,
}: {
  r: Recompensa
  config: ConfigPublica
  codigoUrl: string
  onCambio: (r: Recompensa) => void
}) {
  const [direccion, setDireccion] = useState<string | null>(null)
  const [modo, setModo] = useState<'pollar' | 'prueba'>(config.pollarClavePublica ? 'pollar' : 'prueba')
  const [codigo, setCodigo] = useState(codigoUrl)
  const [nombre, setNombre] = useState('')
  const [contacto, setContacto] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState('')
  const alCambiarDireccion = useCallback((d: string | null) => setDireccion(d), [])

  const enviar = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!direccion) return
    setEnviando(true)
    setError('')
    try {
      onCambio(await api.reclamar(r.id, { codigo, direccion, nombre, contacto }))
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err))
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="pila">
      <ol className="pasos">
        <li>Crea tu billetera para cobrar.</li>
        <li>Deja tu nombre y cómo contactarte.</li>
        <li>Devuelve el celular: cuando {r.nombreDueno} confirme, el dinero llega a tu billetera.</li>
      </ol>

      <h2>1 · Tu billetera</h2>
      {modo === 'pollar' && config.pollarClavePublica ? (
        <CuentaPollar config={config} onDireccion={alCambiarDireccion} />
      ) : (
        <CuentaPrueba config={config} onDireccion={alCambiarDireccion} />
      )}
      {config.pollarClavePublica && !direccion && (
        <button
          className="enlace"
          onClick={() => {
            setDireccion(null)
            setModo(modo === 'pollar' ? 'prueba' : 'pollar')
          }}
        >
          {modo === 'pollar' ? 'Usar una cuenta de prueba' : 'Usar Pollar (Google o correo)'}
        </button>
      )}

      <h2>2 · Tus datos</h2>
      <form className="pila" onSubmit={enviar}>
        <label>
          Tu nombre
          <input value={nombre} onChange={(e) => setNombre(e.target.value)} required maxLength={60} />
        </label>
        <label>
          WhatsApp o teléfono
          <input value={contacto} onChange={(e) => setContacto(e.target.value)} required maxLength={80} inputMode="tel" />
        </label>
        {!codigoUrl && (
          <label>
            Código que aparece en la pantalla del celular
            <input
              value={codigo}
              onChange={(e) => setCodigo(e.target.value.toUpperCase())}
              required
              maxLength={6}
              className="codigo"
            />
          </label>
        )}
        <button className="boton" type="submit" disabled={!direccion || enviando}>
          {enviando ? 'Enviando…' : direccion ? 'Reclamar la recompensa' : 'Primero crea tu billetera'}
        </button>
        {error && <p className="error">{error}</p>}
      </form>
      <p className="nota">Solo {r.nombreDueno} verá tu contacto, para coordinar la devolución en un lugar seguro.</p>
    </div>
  )
}

function Aviso({ tono, children }: { tono: 'ok' | 'espera' | 'alerta'; children: React.ReactNode }) {
  return <div className={`aviso ${tono}`}>{children}</div>
}

const nombreMovimiento: Record<string, string> = {
  despliegue: 'Escrow creado',
  fondeo: 'Recompensa bloqueada',
  disputa: 'Dueño confirmó',
  pago: 'Pagada a quien lo devolvió',
  devolucion: 'Devuelta al dueño',
}

function Verificacion({ r }: { r: Recompensa }) {
  if (!r.contractId && r.movimientos.length === 0) return null
  return (
    <details className="verificacion">
      <summary>Verificar en la red Stellar</summary>
      <ul>
        {r.enlaces && (
          <li>
            <a href={r.enlaces.escrow} target="_blank" rel="noreferrer">
              Ver el escrow en Trustless Work
            </a>
          </li>
        )}
        {r.movimientos.map((m) => (
          <li key={m.hash}>
            <a href={m.enlace} target="_blank" rel="noreferrer">
              {nombreMovimiento[m.tipo] ?? m.tipo}
            </a>{' '}
            <span className="nota">{new Date(m.fecha).toLocaleString('es-BO')}</span>
          </li>
        ))}
      </ul>
    </details>
  )
}
