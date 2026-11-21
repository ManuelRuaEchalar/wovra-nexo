import { useCallback, useEffect, useState } from 'react'
import { api, type ConfigPublica, type Recompensa } from './api'
import { CuentaPollar, CuentaPrueba } from './cuentas'
import { sitio, tecnologia } from './sitio'

function leerUrl() {
  const id = location.pathname.match(/^\/r\/([\w-]+)/)?.[1] ?? null
  const codigo = new URLSearchParams(location.search).get('c') ?? ''
  return { id, codigo }
}

const bs = (n: number) => n.toLocaleString('es-BO', { maximumFractionDigits: 2 })
const usdc = (n: number) => n.toLocaleString('es-BO', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

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

  return (
    <>
      <Encabezado />
      <main>
        {!id ? (
          <PortadaGeneral />
        ) : !r ? (
          <section className="wrap portada-carga">
            {error ? <p className="error">{error}</p> : <p className="nota">Buscando la recompensa…</p>}
          </section>
        ) : (
          <>
            <Portada r={r} />
            <section id="reclamar" className="wrap seccion-reclamo">
              <div className="panel">
                <Cuerpo r={r} config={config} codigoUrl={codigoUrl} onCambio={setR} />
              </div>
              <Verificacion r={r} />
            </section>
          </>
        )}
        <ComoFunciona />
        <Confianza />
        <Preguntas />
        <Cierre />
      </main>
      <Pie r={r} />
    </>
  )
}

// --- Marca y cabecera ---------------------------------------------------------------

export function Escudo() {
  return (
    <svg viewBox="0 0 60 70" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
      <path d="M30 4 53 13v23c0 14-23 29-23 29S7 50 7 36V13Z" />
      <path d="m19 33 8 8 15-19" />
    </svg>
  )
}

function Encabezado() {
  return (
    <header className="wrap">
      <nav aria-label="Principal">
        <a className="marca" href="#arriba">
          <Escudo /> {sitio.nombre}
        </a>
        <div className="enlaces">
          <a href="#como">Cómo funciona</a>
          <a href="#confianza">Seguridad</a>
          <a href="#preguntas">Preguntas</a>
          <span className="chip-prueba" title="Los montos todavía no son dinero real">
            Red de pruebas
          </span>
        </div>
      </nav>
    </header>
  )
}

// --- Portada ---------------------------------------------------------------------

function Portada({ r }: { r: Recompensa }) {
  return (
    <section className="portada" id="arriba">
      <div className="wrap portada-grid">
        <div className="portada-texto">
          <p className="eyebrow">Recompensa garantizada · {sitio.ciudad}</p>
          <h1>Encontraste el celular de {r.nombreDueno}.</h1>
          <p className="bajada">
            Devuélvelo y cobra la recompensa que dejó bloqueada para ti. No necesitas desbloquearlo ni saber de
            criptomonedas.
          </p>
          <div className="acciones">
            {r.estado === 'activa' && (
              <a className="btn" href="#reclamar">
                Reclamar la recompensa
              </a>
            )}
            <a className="enlace-texto" href="#como">
              Cómo funciona
            </a>
          </div>
        </div>
        <TarjetaNocturna r={r} />
      </div>
      <div className="wrap promesa">
        <span>01 / El dinero ya está bloqueado</span>
        <span>02 / Solo tú puedes cobrarlo</span>
        <span>03 / Se paga al devolverlo</span>
      </div>
    </section>
  )
}

const estadoChip: Record<Recompensa['estado'], [string, 'ok' | 'espera' | 'alerta']> = {
  creando: ['Bloqueándose en Stellar…', 'espera'],
  activa: ['Disponible para quien lo devuelva', 'ok'],
  reclamada: ['Reclamada · esperando la devolución', 'espera'],
  pagando: ['Pagando…', 'espera'],
  pagada: ['Pagada', 'ok'],
  devolviendo: ['Volviendo al dueño…', 'espera'],
  devuelta: ['Retirada por el dueño', 'alerta'],
  error: ['Con un problema', 'alerta'],
}

function TarjetaNocturna({ r }: { r: Recompensa }) {
  const [texto, tono] = estadoChip[r.estado]
  return (
    <div className="escena" aria-label="Recompensa">
      <span className="orbita o1" aria-hidden />
      <span className="orbita o2" aria-hidden />
      <span className="orbita o3" aria-hidden />
      <span className="nodo n1" aria-hidden />
      <span className="nodo n2" aria-hidden />
      <div className="tarjeta-noche">
        <span className="horizonte" aria-hidden />
        <p className="eyebrow">Bloqueados en un escrow</p>
        <p className="monto">
          {bs(r.montoBs)}
          <span>Bs</span>
        </p>
        <p className="equivalente">≈ {usdc(r.montoUsdc)} USDC en la red Stellar</p>
        {r.mensaje && <blockquote>“{r.mensaje}”</blockquote>}
        <p className={`estado ${tono}`}>
          <i aria-hidden /> {texto}
        </p>
        {r.enlaces && (
          <a className="verificar" href={r.enlaces.escrow} target="_blank" rel="noreferrer">
            Verificar el contrato ↗
          </a>
        )}
      </div>
    </div>
  )
}

function PortadaGeneral() {
  return (
    <section className="portada" id="arriba">
      <div className="wrap portada-grid">
        <div className="portada-texto">
          <p className="eyebrow">Recompensa garantizada · {sitio.ciudad}</p>
          <h1>Devolver un celular, por fin, conviene.</h1>
          <p className="bajada">
            Si encontraste un celular protegido por {sitio.nombre}, escanea el código que aparece en su pantalla
            bloqueada. La recompensa ya está bloqueada en la red Stellar y se paga al devolverlo.
          </p>
        </div>
        <div className="escena" aria-hidden>
          <span className="orbita o1" />
          <span className="orbita o2" />
          <span className="orbita o3" />
          <span className="nodo n1" />
          <span className="nodo n2" />
          <div className="tarjeta-noche">
            <span className="horizonte" />
            <p className="eyebrow">Escanea el código</p>
            <p className="monto">
              QR<span>↗</span>
            </p>
            <p className="equivalente">en la pantalla del celular perdido</p>
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
}: {
  r: Recompensa
  config: ConfigPublica
  codigoUrl: string
  onCambio: (r: Recompensa) => void
}) {
  switch (r.estado) {
    case 'creando':
      return <Aviso titulo="Un momento" tono="espera">{r.nombreDueno} está bloqueando la recompensa en la red. Tarda unos segundos.</Aviso>
    case 'error':
      return <Aviso titulo="Hubo un problema" tono="alerta">No se pudo completar la recompensa. {r.nombreDueno} ya fue avisado.</Aviso>
    case 'pagada':
      return (
        <Aviso titulo="¡Gracias por devolverlo!" tono="ok">
          La recompensa ya fue pagada{r.reclamante ? ` a ${r.reclamante.nombre}` : ''}. Gracias a ti, un celular volvió
          con su dueño.
          {r.repartoRed && (
            <>
              {' '}
              Además, {usdc(r.repartoRed.montoUsdc)} USDC se repartieron entre {r.repartoRed.nodos}{' '}
              {r.repartoRed.nodos === 1 ? 'teléfono' : 'teléfonos'} de la red que ayudaron a ubicarlo.
            </>
          )}
        </Aviso>
      )
    case 'devuelta':
      return <Aviso titulo="Recompensa retirada" tono="alerta">{r.nombreDueno} retiró esta recompensa: probablemente ya recuperó su celular.</Aviso>
    case 'reclamada':
    case 'pagando':
    case 'devolviendo':
      return (
        <Aviso titulo="Recompensa reclamada" tono="espera">
          {r.reclamante?.nombre ?? 'Alguien'} ya la reclamó. Cuando {r.nombreDueno} confirme que recuperó su celular, se
          paga a su billetera ({r.reclamante?.direccion}).
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
    <div className="reclamo">
      <div className="reclamo-intro">
        <p className="eyebrow">Reclama en dos pasos</p>
        <h2>Tu billetera y tus datos.</h2>
        <p className="nota">
          Solo {r.nombreDueno} verá tu contacto, para acordar la devolución en un lugar seguro. Nadie más.
        </p>
      </div>

      <ol className="pasos-reclamo">
        <li className={direccion ? 'hecho' : ''}>
          <span className="numero">01</span>
          <div className="pila">
            <h3>Tu billetera</h3>
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
                {modo === 'pollar' ? 'Usar una cuenta de prueba' : 'Usar mi cuenta de Google o mi correo'}
              </button>
            )}
          </div>
        </li>
        <li>
          <span className="numero">02</span>
          <form className="pila" onSubmit={enviar}>
            <h3>Tus datos</h3>
            <label>
              Tu nombre
              <input value={nombre} onChange={(e) => setNombre(e.target.value)} required maxLength={60} autoComplete="name" />
            </label>
            <label>
              WhatsApp o teléfono
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
              {enviando ? 'Enviando…' : direccion ? `Reclamar ${bs(r.montoBs)} Bs` : 'Primero crea tu billetera'}
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

const nombreMovimiento: Record<string, string> = {
  despliegue: 'Escrow creado',
  fondeo: 'Recompensa bloqueada',
  disputa: 'El dueño confirmó',
  pago: 'Pagada a quien lo devolvió',
  devolucion: 'Devuelta al dueño',
}

function Verificacion({ r }: { r: Recompensa }) {
  if (r.movimientos.length === 0) return null
  return (
    <details className="verificacion">
      <summary>Ver cada paso en la red Stellar</summary>
      <ol>
        {r.movimientos.map((m) => (
          <li key={m.hash}>
            <a href={m.enlace} target="_blank" rel="noreferrer">
              {nombreMovimiento[m.tipo] ?? m.tipo} ↗
            </a>
            <span>{new Date(m.fecha).toLocaleString('es-BO', { dateStyle: 'short', timeStyle: 'short' })}</span>
          </li>
        ))}
      </ol>
    </details>
  )
}

// --- Secciones informativas -------------------------------------------------------------

function ComoFunciona() {
  const pasos = [
    ['01', 'Bloqueada', 'Cuando su dueño lo reporta como perdido, la recompensa queda bloqueada en un contrato de la red Stellar.'],
    ['02', 'Reclamas', 'Escaneas el código de la pantalla, inicias sesión con Google o tu correo y dejas tu contacto.'],
    ['03', 'Devuelves', 'Acuerdan un punto seguro y público para entregarlo. No necesitas desbloquear el celular.'],
    ['04', 'Cobras', 'El dueño confirma que lo recuperó y la recompensa llega a tu billetera en segundos.'],
  ]
  return (
    <section id="como" className="canal">
      <div className="wrap">
        <p className="eyebrow">Cómo funciona</p>
        <h2>Una promesa que no depende de la palabra de nadie.</h2>
        <ol className="linea-pasos">
          {pasos.map(([n, t, d]) => (
            <li key={n}>
              <span className="titulo-paso">
                {n} · {t}
              </span>
              <p>{d}</p>
            </li>
          ))}
        </ol>
      </div>
    </section>
  )
}

function Confianza() {
  return (
    <section id="confianza" className="seccion wrap">
      <div className="intro">
        <div>
          <p className="eyebrow">La confianza es parte de la red</p>
          <h2>Nadie puede quedarse con el dinero.</h2>
        </div>
        <p>
          Las recompensas que se ofrecen en redes sociales suelen terminar en estafas, hacia los dos lados. Aquí el
          dinero no lo tiene el dueño ni lo tenemos nosotros: está bloqueado en un contrato que cualquiera puede revisar.
        </p>
      </div>
      <div className="principios">
        <article>
          <h3>Bloqueado de verdad.</h3>
          <p>
            El monto queda en un escrow de Trustless Work sobre Stellar. Puedes verificarlo antes de mover un dedo.
          </p>
        </article>
        <article>
          <h3>Tus datos, solo para el dueño.</h3>
          <p>Tu nombre y tu contacto solo los ve quien perdió el celular. No los publicamos ni los vendemos.</p>
        </article>
        <article>
          <h3>Devolución segura.</h3>
          <p>
            Acuerden un lugar público y concurrido. Solo para celulares perdidos: un robo se denuncia a la policía.
          </p>
        </article>
      </div>
      <div className="estado-proyecto">
        <div>
          <p className="eyebrow">Qué es {sitio.nombre}</p>
          <h3>Una red que se cuida a sí misma.</h3>
          <p>
            Cada celular con la app ayuda a encontrar a los demás, con o sin internet. La recompensa garantizada es la
            primera pieza que conecta esa red con dinero real, para que ayudar también convenga.
          </p>
        </div>
        <div>
          <p className="eyebrow">En qué etapa estamos</p>
          <h3>Probando en la red de pruebas.</h3>
          <p>
            Hoy las recompensas usan la red de pruebas de Stellar: los montos todavía no son dinero real. Es un
            proyecto en desarrollo, presentado en la {sitio.hackathon}.
          </p>
        </div>
      </div>
    </section>
  )
}

function Preguntas() {
  const preguntas: [string, string][] = [
    [
      '¿Necesito saber de criptomonedas?',
      'No. Inicias sesión con Google o con tu correo y se crea una billetera para ti, sin frases secretas ni aplicaciones extra.',
    ],
    [
      '¿Cómo sé que la recompensa existe?',
      'Toca «Verificar el contrato» en la tarjeta de arriba: abre el escrow en un visor público de la red Stellar, con el monto bloqueado.',
    ],
    [
      '¿Qué pasa si el dueño no confirma?',
      'El dinero sigue bloqueado; no vuelve solo al dueño. Si hay un desacuerdo, Red Centinela actúa como árbitro del contrato y decide a quién va.',
    ],
    [
      '¿Puedo recibirlo en bolivianos?',
      'Hoy llega a tu billetera en USDC, un dólar digital. El retiro a bolivianos con Pollar es el siguiente paso del proyecto.',
    ],
    [
      '¿Y si el celular fue robado?',
      'La recompensa es solo para celulares perdidos. Pagar por un celular robado incentivaría robar para «devolverlo». Un robo se denuncia a la policía.',
    ],
  ]
  return (
    <section id="preguntas" className="seccion wrap">
      <div className="faq">
        <p className="eyebrow">Antes de reclamar</p>
        <h2>Preguntas que importan.</h2>
        {preguntas.map(([p, r]) => (
          <details key={p}>
            <summary>{p}</summary>
            <p>{r}</p>
          </details>
        ))}
      </div>
    </section>
  )
}

function Cierre() {
  return (
    <section className="cierre">
      <span className="orbita-cierre" aria-hidden />
      <p className="eyebrow">Gracias por cuidar lo de otros</p>
      <h2>Tu celular también puede ser parte de algo más grande.</h2>
      <p>
        {sitio.nombre} convierte los celulares de una ciudad en una red que se cuida a sí misma. Hoy ayudaste a que uno
        vuelva a casa.
      </p>
    </section>
  )
}

export function Pie({ r }: { r: Recompensa | null }) {
  return (
    <footer className="pie">
      <div className="wrap pie-grid">
        <div className="pie-marca">
          <a className="marca" href="#arriba">
            <Escudo /> {sitio.nombre}
          </a>
          <p>{sitio.lema}</p>
        </div>
        <div>
          <h4>La recompensa</h4>
          <ul>
            <li><a href="#como">Cómo funciona</a></li>
            <li><a href="#confianza">Seguridad y privacidad</a></li>
            <li><a href="#preguntas">Preguntas frecuentes</a></li>
            {r?.enlaces && (
              <li>
                <a href={r.enlaces.escrow} target="_blank" rel="noreferrer">Ver este contrato ↗</a>
              </li>
            )}
          </ul>
        </div>
        <div>
          <h4>Quiénes somos</h4>
          <p>
            Un equipo de {sitio.ciudad}, construyendo {sitio.nombre} para la {sitio.hackathon}.
          </p>
          {sitio.equipo.length > 0 && (
            <ul>
              {sitio.equipo.map((p) => (
                <li key={p.nombre}>
                  {p.nombre}
                  {p.rol && <span> · {p.rol}</span>}
                </li>
              ))}
            </ul>
          )}
          {sitio.contacto && (
            <p>
              Escríbenos: <a href={`mailto:${sitio.contacto}`}>{sitio.contacto}</a>
            </p>
          )}
        </div>
        <div>
          <h4>Construido con</h4>
          <ul>
            {tecnologia.map((t) => (
              <li key={t.nombre}>
                <a href={t.url} target="_blank" rel="noreferrer">{t.nombre} ↗</a>
                <span>{t.nota}</span>
              </li>
            ))}
          </ul>
        </div>
      </div>
      <div className="wrap pie-final">
        <span>© 2026 {sitio.nombre} · {sitio.ciudad}</span>
        <span>Red de pruebas de Stellar: los montos todavía no son dinero real.</span>
      </div>
    </footer>
  )
}
