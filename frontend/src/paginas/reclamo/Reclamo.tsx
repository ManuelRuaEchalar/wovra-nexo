import { useCallback, useEffect, useState } from 'react'
import { api, type ConfigPublica, type Recompensa } from './api'
import { CuentaPollar, CuentaPrueba } from './cuentas'
import { regional, useIdioma, type Idioma } from '../../compartido/idioma'
import { Navbar } from '../../compartido/Navbar'
import { Pie } from '../../compartido/Pie'
import { textosSitio, type TextosSitio } from '../../compartido/textos'

type T = TextosSitio['reclamo']

function leerUrl() {
  const id = location.pathname.match(/^\/r\/([\w-]+)/)?.[1] ?? null
  const codigo = new URLSearchParams(location.search).get('c') ?? ''
  return { id, codigo }
}

const formatos = (idioma: Idioma) => ({
  bs: (n: number) => n.toLocaleString(regional(idioma), { maximumFractionDigits: 2 }),
  usdc: (n: number) => n.toLocaleString(regional(idioma), { minimumFractionDigits: 2, maximumFractionDigits: 2 }),
})

/** Página de reclamo de la recompensa (/r/:id), a la que lleva el QR del celular perdido. */
export default function PaginaReclamo({ config }: { config: ConfigPublica }) {
  const { id, codigo: codigoUrl } = leerUrl()
  const [idioma, cambiarIdioma] = useIdioma()
  const ts = textosSitio[idioma]
  const t = ts.reclamo
  const f = formatos(idioma)
  const [r, setR] = useState<Recompensa | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    document.title = ts.titulos.reclamo
  }, [ts])

  useEffect(() => {
    if (!id) return
    let vivo = true
    const cargar = () =>
      api
        .recompensa(id)
        .then((x) => vivo && setR(x))
        .catch((e) => vivo && setError(e.message))
    void cargar()
    const temporizador = setInterval(cargar, 5000)
    return () => {
      vivo = false
      clearInterval(temporizador)
    }
  }, [id])

  return (
    <div className="pagina-clara">
      <Navbar t={ts.nav} cambiarIdioma={cambiarIdioma} pagina="reclamo" />
      <main id="contenido">
        {!id ? (
          <PortadaGeneral t={t} />
        ) : !r ? (
          <section className="wrap portada-carga">
            {error ? <p className="error">{error}</p> : <p className="nota">{t.buscando}</p>}
          </section>
        ) : (
          <>
            <Portada r={r} t={t} f={f} />
            <section id="reclamar" className="wrap seccion-reclamo">
              <div className="panel">
                <Cuerpo r={r} config={config} codigoUrl={codigoUrl} onCambio={setR} ts={ts} f={f} />
              </div>
              <Verificacion r={r} t={t} idioma={idioma} />
            </section>
          </>
        )}
        <ComoSeCobra t={t} />
        <Confianza t={t} />
        <Preguntas t={t} />
        <Cierre t={t} />
      </main>
      <Pie t={ts} contrato={r?.enlaces?.escrow} />
    </div>
  )
}

type F = ReturnType<typeof formatos>

// --- Portada ---------------------------------------------------------------------

function Portada({ r, t, f }: { r: Recompensa; t: T; f: F }) {
  return (
    <section className="portada" id="arriba">
      <div className="wrap portada-grid">
        <div className="portada-texto">
          <p className="eyebrow">{t.eyebrow}</p>
          <h1>{t.encontraste(r.nombreDueno)}</h1>
          <p className="bajada">{t.bajada}</p>
          <div className="acciones">
            {r.estado === 'activa' && (
              <a className="btn" href="#reclamar">
                {t.reclamar}
              </a>
            )}
            <a className="enlace-texto" href="#como">
              {t.comoSeCobra}
            </a>
          </div>
        </div>
        <TarjetaNocturna r={r} t={t} f={f} />
      </div>
      <div className="wrap promesa">
        {t.promesa.map((p) => (
          <span key={p}>{p}</span>
        ))}
      </div>
    </section>
  )
}

const tonoEstado: Record<Recompensa['estado'], 'ok' | 'espera' | 'alerta'> = {
  creando: 'espera',
  activa: 'ok',
  reclamada: 'espera',
  pagando: 'espera',
  pagada: 'ok',
  devolviendo: 'espera',
  devuelta: 'alerta',
  error: 'alerta',
}

function TarjetaNocturna({ r, t, f }: { r: Recompensa; t: T; f: F }) {
  return (
    <div className="escena" aria-label={t.tarjeta}>
      <span className="orbita o1" aria-hidden />
      <span className="orbita o2" aria-hidden />
      <span className="orbita o3" aria-hidden />
      <span className="nodo n1" aria-hidden />
      <span className="nodo n2" aria-hidden />
      <div className="tarjeta-noche">
        <span className="horizonte" aria-hidden />
        <p className="eyebrow">{t.bloqueados}</p>
        <p className="monto">
          {f.bs(r.montoBs)}
          <span>Bs</span>
        </p>
        <p className="equivalente">{t.equivalente(f.usdc(r.montoUsdc))}</p>
        {r.mensaje && <blockquote>“{r.mensaje}”</blockquote>}
        <p className={`estado ${tonoEstado[r.estado]}`}>
          <i aria-hidden /> {t.estados[r.estado]}
        </p>
        {r.enlaces && (
          <a className="verificar" href={r.enlaces.escrow} target="_blank" rel="noreferrer">
            {t.verificar}
          </a>
        )}
      </div>
    </div>
  )
}

function PortadaGeneral({ t }: { t: T }) {
  return (
    <section className="portada" id="arriba">
      <div className="wrap portada-grid">
        <div className="portada-texto">
          <p className="eyebrow">{t.eyebrow}</p>
          <h1>{t.general.titulo}</h1>
          <p className="bajada">{t.general.bajada}</p>
        </div>
        <div className="escena" aria-hidden>
          <span className="orbita o1" />
          <span className="orbita o2" />
          <span className="orbita o3" />
          <span className="nodo n1" />
          <span className="nodo n2" />
          <div className="tarjeta-noche">
            <span className="horizonte" />
            <p className="eyebrow">{t.general.escanea}</p>
            <p className="monto">
              QR<span>↗</span>
            </p>
            <p className="equivalente">{t.general.donde}</p>
          </div>
        </div>
      </div>
    </section>
  )
}

// --- Reclamo ----------------------------------------------------------------------

function Cuerpo({
  r,
  config,
  codigoUrl,
  onCambio,
  ts,
  f,
}: {
  r: Recompensa
  config: ConfigPublica
  codigoUrl: string
  onCambio: (r: Recompensa) => void
  ts: TextosSitio
  f: F
}) {
  const a = ts.reclamo.avisos
  switch (r.estado) {
    case 'creando':
      return <Aviso titulo={a.creandoT} tono="espera">{a.creando(r.nombreDueno)}</Aviso>
    case 'error':
      return <Aviso titulo={a.errorT} tono="alerta">{a.error(r.nombreDueno)}</Aviso>
    case 'pagada':
      return (
        <Aviso titulo={a.pagadaT} tono="ok">
          {a.pagada(r.reclamante?.nombre ?? null)}
          {r.repartoRed && a.reparto(f.usdc(r.repartoRed.montoUsdc), r.repartoRed.nodos)}
        </Aviso>
      )
    case 'devuelta':
      return <Aviso titulo={a.devueltaT} tono="alerta">{a.devuelta(r.nombreDueno)}</Aviso>
    case 'reclamada':
    case 'pagando':
    case 'devolviendo':
      return (
        <Aviso titulo={a.reclamadaT} tono="espera">
          {a.reclamada(r.reclamante?.nombre ?? null, r.nombreDueno, r.reclamante?.direccion ?? '')}
          {r.estado === 'pagando' && a.pagandoAhora}
        </Aviso>
      )
    case 'activa':
      return <Reclamo r={r} config={config} codigoUrl={codigoUrl} onCambio={onCambio} ts={ts} f={f} />
  }
}

function Reclamo({
  r,
  config,
  codigoUrl,
  onCambio,
  ts,
  f,
}: {
  r: Recompensa
  config: ConfigPublica
  codigoUrl: string
  onCambio: (r: Recompensa) => void
  ts: TextosSitio
  f: F
}) {
  const t = ts.reclamo.form
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
    <div className="reclamo">
      <div className="reclamo-intro">
        <p className="eyebrow">{t.eyebrow}</p>
        <h2>{t.titulo}</h2>
        <p className="nota">{t.privacidad(r.nombreDueno)}</p>
      </div>

      <ol className="pasos-reclamo">
        <li className={direccion ? 'hecho' : ''}>
          <span className="numero">01</span>
          <div className="pila">
            <h3>{t.billetera}</h3>
            {modo === 'pollar' && config.pollarClavePublica ? (
              <CuentaPollar config={config} onDireccion={alCambiarDireccion} t={ts.cuentas} />
            ) : (
              <CuentaPrueba config={config} onDireccion={alCambiarDireccion} t={ts.cuentas} />
            )}
            {config.pollarClavePublica && !direccion && (
              <button
                className="enlace"
                onClick={() => {
                  setDireccion(null)
                  setModo(modo === 'pollar' ? 'prueba' : 'pollar')
                }}
              >
                {modo === 'pollar' ? t.usarPrueba : t.usarPollar}
              </button>
            )}
          </div>
        </li>
        <li>
          <span className="numero">02</span>
          <form className="pila" onSubmit={enviar}>
            <h3>{t.datos}</h3>
            <label>
              {t.nombre}
              <input value={nombre} onChange={(e) => setNombre(e.target.value)} required maxLength={60} autoComplete="name" />
            </label>
            <label>
              {t.contacto}
              <input
                value={contacto}
                onChange={(e) => setContacto(e.target.value)}
                required
                maxLength={80}
                inputMode="tel"
                autoComplete="tel"
                placeholder="+591 7…"
              />
            </label>
            {!codigoUrl && (
              <label>
                {t.codigo}
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
              {enviando ? t.enviando : direccion ? t.reclamar(f.bs(r.montoBs)) : t.primero}
            </button>
            {error && <p className="error">{error}</p>}
          </form>
        </li>
      </ol>
    </div>
  )
}

function Aviso({ titulo, tono, children }: { titulo: string; tono: 'ok' | 'espera' | 'alerta'; children: React.ReactNode }) {
  return (
    <div className={`aviso ${tono}`} role="status">
      <span className="anillo-aviso" aria-hidden />
      <div>
        <h2>{titulo}</h2>
        <p>{children}</p>
      </div>
    </div>
  )
}

function Verificacion({ r, t, idioma }: { r: Recompensa; t: T; idioma: Idioma }) {
  if (r.movimientos.length === 0) return null
  return (
    <details className="verificacion">
      <summary>{t.movimientos.titulo}</summary>
      <ol>
        {r.movimientos.map((m) => (
          <li key={m.hash}>
            <a href={m.enlace} target="_blank" rel="noreferrer">
              {t.movimientos[m.tipo] ?? m.tipo} ↗
            </a>
            <span>{new Date(m.fecha).toLocaleString(regional(idioma), { dateStyle: 'short', timeStyle: 'short' })}</span>
          </li>
        ))}
      </ol>
    </details>
  )
}

// --- Secciones informativas -------------------------------------------------------------

function ComoSeCobra({ t }: { t: T }) {
  return (
    <section id="como" className="canal">
      <div className="wrap">
        <p className="eyebrow">{t.como.eyebrow}</p>
        <h2>{t.como.titulo}</h2>
        <ol className="linea-pasos">
          {t.como.pasos.map(([n, titulo, d]) => (
            <li key={n}>
              <span className="titulo-paso">
                {n} · {titulo}
              </span>
              <p>{d}</p>
            </li>
          ))}
        </ol>
      </div>
    </section>
  )
}

function Confianza({ t }: { t: T }) {
  const c = t.confianza
  return (
    <section id="confianza" className="seccion wrap">
      <div className="intro">
        <div>
          <p className="eyebrow">{c.eyebrow}</p>
          <h2>{c.titulo}</h2>
        </div>
        <p>{c.intro}</p>
      </div>
      <div className="principios">
        {c.principios.map(([titulo, texto]) => (
          <article key={titulo}>
            <h3>{titulo}</h3>
            <p>{texto}</p>
          </article>
        ))}
      </div>
      <div className="estado-proyecto">
        <div>
          <p className="eyebrow">{c.queEs}</p>
          <h3>{c.queEsT}</h3>
          <p>{c.queEsP}</p>
        </div>
        <div>
          <p className="eyebrow">{c.etapa}</p>
          <h3>{c.etapaT}</h3>
          <p>{c.etapaP}</p>
        </div>
      </div>
    </section>
  )
}

function Preguntas({ t }: { t: T }) {
  return (
    <section id="preguntas" className="seccion wrap">
      <div className="faq">
        <p className="eyebrow">{t.preguntas.eyebrow}</p>
        <h2>{t.preguntas.titulo}</h2>
        {t.preguntas.lista.map(([p, r]) => (
          <details key={p}>
            <summary>{p}</summary>
            <p>{r}</p>
          </details>
        ))}
      </div>
    </section>
  )
}

function Cierre({ t }: { t: T }) {
  return (
    <section className="cierre">
      <span className="orbita-cierre" aria-hidden />
      <p className="eyebrow">{t.cierre.eyebrow}</p>
      <h2>{t.cierre.titulo}</h2>
      <p>{t.cierre.texto}</p>
    </section>
  )
}
