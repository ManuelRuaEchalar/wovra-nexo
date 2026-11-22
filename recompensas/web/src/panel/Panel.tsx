import { useEffect, useMemo, useRef, useState } from 'react'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { regional, useIdioma, type Idioma } from '../idioma'
import { Navbar } from '../sitio/Navbar'
import { Pie } from '../sitio/Pie'
import { textosSitio, type TextosSitio } from '../textosSitio'
import { abrirBloque, derivarContrasena, type Identidad } from './cripto'
import { Relevo, vacia, type Punto, type Situacion } from './relevo'

/**
 * Panel web: el dueño sigue y controla su celular desde cualquier navegador,
 * sin depender de nadie del círculo. Todo se descifra aquí, en el navegador.
 */
type T = TextosSitio['panel']

export default function Panel() {
  const [yo, setYo] = useState<Identidad | null>(null)
  const [idioma, cambiarIdioma] = useIdioma()
  const ts = textosSitio[idioma]
  useEffect(() => {
    document.title = ts.titulos.panel
  }, [ts])
  return (
    <div className="pagina-clara">
      <Navbar t={ts.nav} cambiarIdioma={cambiarIdioma} pagina="panel" />
      <main id="contenido" className="panel-web">
        {yo ? <Tablero yo={yo} t={ts.panel} idioma={idioma} onSalir={() => setYo(null)} /> : <Entrar onEntrar={setYo} t={ts.panel} />}
      </main>
      <Pie t={ts} />
    </div>
  )
}

// --- Entrar --------------------------------------------------------------------------

async function pedir<T>(ruta: string, cuerpo?: unknown): Promise<T> {
  const r = await fetch(ruta, cuerpo ? { method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify(cuerpo) } : {})
  const j = await r.json().catch(() => ({}))
  if (!r.ok) throw new Error(j.error ?? `Error ${r.status}`)
  return j as T
}

function Entrar({ onEntrar, t }: { onEntrar: (yo: Identidad) => void; t: T }) {
  const [correo, setCorreo] = useState('')
  const [contrasena, setContrasena] = useState('')
  const [trabajando, setTrabajando] = useState(false)
  const [error, setError] = useState('')

  const enviar = async (e: React.FormEvent) => {
    e.preventDefault()
    setTrabajando(true)
    setError('')
    try {
      const c = correo.trim().toLowerCase()
      const { sal, iteraciones } = await pedir<{ sal: string; iteraciones: number }>(
        `/api/rescate/sal?correo=${encodeURIComponent(c)}`,
      )
      const { clave, verificador } = await derivarContrasena(contrasena, sal, iteraciones)
      const { bloque } = await pedir<{ bloque: string }>('/api/rescate/entrar', { correo: c, verificador })
      const yo = await abrirBloque(clave, bloque)
      if (!yo) throw new Error(t.entrar.noAbre)
      setContrasena('')
      onEntrar(yo)
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err))
    } finally {
      setTrabajando(false)
    }
  }

  return (
    <section className="wrap entrar-grid">
      <div>
        <p className="eyebrow">{t.entrar.eyebrow}</p>
        <h1>{t.entrar.titulo}</h1>
        <p className="bajada">{t.entrar.bajada}</p>
        <ul className="garantias">
          {t.entrar.garantias.map((g) => (
            <li key={g}>{g}</li>
          ))}
        </ul>
      </div>
      <form className="panel pila" onSubmit={enviar}>
        <h3>{t.entrar.formTitulo}</h3>
        <label>
          {t.entrar.correo}
          <input type="email" value={correo} onChange={(e) => setCorreo(e.target.value)} required autoComplete="username" />
        </label>
        <label>
          {t.entrar.contrasena}
          <input
            type="password"
            value={contrasena}
            onChange={(e) => setContrasena(e.target.value)}
            required
            minLength={8}
            autoComplete="current-password"
          />
        </label>
        <button className="boton" type="submit" disabled={trabajando}>
          {trabajando ? t.entrar.abriendo : t.entrar.boton}
        </button>
        {error && <p className="error">{error}</p>}
        <p className="nota">{t.entrar.sinCuenta}</p>
      </form>
    </section>
  )
}

// --- Tablero -------------------------------------------------------------------------

const hora = (ms: number, idioma: Idioma) =>
  new Date(ms).toLocaleString(regional(idioma), { dateStyle: 'short', timeStyle: 'short' })

function hace(ms: number, t: T, ahora = Date.now()) {
  const s = Math.max(0, Math.round((ahora - ms) / 1000))
  if (s < 60) return t.hace.momento
  if (s < 3600) return t.hace.min(Math.round(s / 60))
  if (s < 86400) return t.hace.h(Math.round(s / 3600))
  return t.hace.d(Math.round(s / 86400))
}

const tonoEstado = { ALERTA_ROBO: 'alerta', ALERTA_PERDIDA: 'alerta', ARMADO: 'ok', INACTIVO: 'espera' } as const

function Tablero({ yo, t, idioma, onSalir }: { yo: Identidad; t: T; idioma: Idioma; onSalir: () => void }) {
  const relevo = useMemo(() => new Relevo(yo), [yo])
  const [s, setS] = useState<Situacion>(vacia)
  const [cargado, setCargado] = useState(false)
  const [error, setError] = useState('')
  const [aviso, setAviso] = useState('')
  const [enviando, setEnviando] = useState(false)
  const actual = useRef(s)
  actual.current = s

  useEffect(() => {
    let vivo = true
    const leer = async () => {
      try {
        const n = await relevo.leer(actual.current)
        if (!vivo) return
        setS(n)
        setError('')
      } catch (e) {
        if (vivo) setError(e instanceof Error ? e.message : String(e))
      } finally {
        if (vivo) setCargado(true)
      }
    }
    void leer()
    const t = setInterval(leer, 10_000)
    return () => {
      vivo = false
      clearInterval(t)
    }
  }, [relevo])

  const ordenar = async (codigo: 'ROBO' | 'PERD' | 'OK' | 'RING', texto: string) => {
    setEnviando(true)
    setAviso('')
    try {
      await relevo.ordenar(codigo)
      setAviso(texto)
    } catch (e) {
      setAviso(e instanceof Error ? e.message : String(e))
    } finally {
      setEnviando(false)
    }
  }

  const enAlerta = s.estado === 'ALERTA_ROBO' || s.estado === 'ALERTA_PERDIDA'
  const ultimo = s.puntos.at(-1)
  const titulo = s.estado ? t.estados[s.estado] : t.estados.sinNoticias
  const tono = s.estado ? tonoEstado[s.estado] : 'espera'

  return (
    <section className="wrap tablero">
      <div className="tablero-cabeza">
        <div>
          <p className="eyebrow">{t.tuCelular}</p>
          <h2>{yo.nombre ? t.celularDe(yo.nombre) : t.tuCelular}</h2>
        </div>
        <div className="tablero-estado">
          <p className={`estado-grande ${tono}`}>
            <i aria-hidden /> {cargado ? titulo : t.estados.conectando}
          </p>
          <button className="enlace" onClick={onSalir}>
            {t.salir}
          </button>
        </div>
      </div>

      <div className="tablero-grid">
        <div className="mapa-caja">
          {s.puntos.length > 0 ? (
            <Mapa puntos={s.puntos} t={t} idioma={idioma} />
          ) : (
            <div className="mapa-vacio">
              <span className="orbita o1" aria-hidden />
              <span className="orbita o2" aria-hidden />
              <p>
                {enAlerta ? t.esperando : t.sinCompartir}
              </p>
            </div>
          )}
        </div>

        <aside className="tablero-lado">
          <div className="tarjeta-noche recta">
            <span className="horizonte" aria-hidden />
            <p className="eyebrow">{t.ultima}</p>
            <p className="dato-grande">{ultimo ? hace(ultimo.en, t) : '—'}</p>
            <dl>
              <dt>{t.llegoPor}</dt>
              <dd>
                {!ultimo
                  ? '—'
                  : ultimo.via === 'bluetooth'
                    ? ultimo.tarde
                      ? t.viaBluetoothTarde
                      : t.viaBluetooth
                    : t.viaInternet}
              </dd>
              <dt>{t.puntos}</dt>
              <dd>{s.puntos.length}</dd>
              {s.bateria !== null && (
                <>
                  <dt>{t.bateria}</dt>
                  <dd>{s.bateria} %</dd>
                </>
              )}
              {s.desde && enAlerta && (
                <>
                  <dt>{t.enAlertaDesde}</dt>
                  <dd>{hora(s.desde, idioma)}</dd>
                </>
              )}
            </dl>
            {ultimo && (
              <a
                className="verificar"
                href={`https://www.google.com/maps?q=${ultimo.lat},${ultimo.lng}`}
                target="_blank"
                rel="noreferrer"
              >
                {t.abrirMaps}
              </a>
            )}
          </div>

          {s.encontradoEn && enAlerta && (
            <p className="aviso-encontrado">{t.encontrado(hace(s.encontradoEn, t))}</p>
          )}

          <div className="ordenes">
            {enAlerta ? (
              <>
                <button className="boton" disabled={enviando} onClick={() => ordenar('RING', t.sonarEnviado)}>
                  {t.sonar}
                </button>
                <button
                  className="boton secundario"
                  disabled={enviando}
                  onClick={() => confirm(t.recuperadoPregunta) && ordenar('OK', t.recuperadoEnviado)}
                >
                  {t.recuperado}
                </button>
              </>
            ) : (
              <>
                <button
                  className="boton alerta"
                  disabled={enviando}
                  onClick={() => confirm(t.roboPregunta) && ordenar('ROBO', t.roboEnviado)}
                >
                  {t.robo}
                </button>
                <button
                  className="boton secundario"
                  disabled={enviando}
                  onClick={() => ordenar('PERD', t.perdiEnviado)}
                >
                  {t.perdi}
                </button>
                <button className="enlace" disabled={enviando} onClick={() => ordenar('RING', t.sonarEnviado)}>
                  {t.sonarEnlace}
                </button>
              </>
            )}
            {aviso && <p className="nota">{aviso}</p>}
            {error && <p className="error">{error}</p>}
            <p className="nota">{t.llegan}</p>
          </div>
        </aside>
      </div>

      {s.eventos.length > 0 && (
        <div className="registro">
          <h3>{t.registro}</h3>
          <ol>
            {s.eventos
              .slice(-12)
              .reverse()
              .map((e) => (
                <li key={`${e.en}-${e.titulo}`} className={e.alerta ? 'alerta' : ''}>
                  <span>{hora(e.en, idioma)}</span>
                  <strong>{e.titulo}</strong>
                  <em>{e.detalle}</em>
                </li>
              ))}
          </ol>
        </div>
      )}
    </section>
  )
}

// --- Mapa ----------------------------------------------------------------------------

function Mapa({ puntos, t, idioma }: { puntos: Punto[]; t: T; idioma: Idioma }) {
  const caja = useRef<HTMLDivElement>(null)
  const mapa = useRef<L.Map | null>(null)
  const capa = useRef<L.LayerGroup | null>(null)
  const encuadrado = useRef(false)

  useEffect(() => {
    if (!caja.current || mapa.current) return
    mapa.current = L.map(caja.current, { zoomControl: true, attributionControl: true })
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '© OpenStreetMap',
    }).addTo(mapa.current)
    capa.current = L.layerGroup().addTo(mapa.current)
    return () => {
      mapa.current?.remove()
      mapa.current = null
      encuadrado.current = false
    }
  }, [])

  useEffect(() => {
    const m = mapa.current
    const g = capa.current
    if (!m || !g || puntos.length === 0) return
    g.clearLayers()
    const linea = puntos.map((p) => [p.lat, p.lng] as [number, number])
    L.polyline(linea, { color: '#17684f', weight: 3, opacity: 0.7 }).addTo(g)
    puntos.slice(0, -1).forEach((p) =>
      L.circleMarker([p.lat, p.lng], {
        radius: 4,
        color: '#17684f',
        weight: 1.5,
        fillColor: p.via === 'bluetooth' ? '#ffffff' : '#2bd07e',
        fillOpacity: 1,
        dashArray: p.via === 'bluetooth' ? '2 2' : undefined,
      })
        .bindTooltip(`${hora(p.en, idioma)} · ${p.via === 'bluetooth' ? 'Bluetooth' : t.viaInternet}`)
        .addTo(g),
    )
    const u = puntos.at(-1)!
    L.circle([u.lat, u.lng], { radius: Math.max(u.precision, 15), color: '#2bd07e', weight: 1, fillOpacity: 0.12 }).addTo(g)
    L.circleMarker([u.lat, u.lng], { radius: 9, color: '#0e3b36', weight: 3, fillColor: '#2bd07e', fillOpacity: 1 })
      .bindTooltip(`${t.ultimaTooltip}: ${hora(u.en, idioma)}`, { permanent: false })
      .addTo(g)
    if (!encuadrado.current) {
      m.fitBounds(L.latLngBounds(linea).pad(0.3), { maxZoom: 17 })
      encuadrado.current = true
    }
  }, [puntos, t, idioma])

  return <div ref={caja} className="mapa" />
}
