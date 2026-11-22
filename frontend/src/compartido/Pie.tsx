import { Logo } from './Logo'
import { sitio } from './datosSitio'
import type { TextosSitio } from './textos'
import './sitio.css'

const SECCIONES = ['senal', 'red', 'recompensa', 'privacidad', 'equipo'] as const

/** Pie de todo el sitio: el mismo en la landing, el panel y la página de reclamo. */
export function Pie({
  t,
  enLanding = false,
  contrato,
}: {
  t: TextosSitio
  enLanding?: boolean
  /** En la página de reclamo: enlace al escrow de esa recompensa. */
  contrato?: string
}) {
  const base = enLanding ? '' : '/'
  const tecnologia = [
    { nombre: 'Stellar', url: 'https://stellar.org', nota: t.tecnologia.stellar },
    { nombre: 'Trustless Work', url: 'https://www.trustlesswork.com', nota: t.tecnologia.trustless },
    { nombre: 'Pollar', url: 'https://pollar.xyz', nota: t.tecnologia.pollar },
  ]
  return (
    <footer className="sitio-pie">
      <div className="sitio-pie-grid">
        <div className="sitio-pie-marca">
          <a className="nav-marca" href={enLanding ? '#arriba' : '/'}>
            <Logo /> Dendra
          </a>
          <p>{t.pie.lema}</p>
        </div>
        <div>
          <h4>{t.pie.sitio}</h4>
          <ul>
            {SECCIONES.map((id) => (
              <li key={id}>
                <a href={`${base}#${id}`}>{t.nav[id]}</a>
              </li>
            ))}
          </ul>
        </div>
        <div>
          <h4>{t.pie.ayuda}</h4>
          <ul>
            <li>
              <a href="/panel">{t.pie.panel}</a>
              <span>{t.pie.panelNota}</span>
            </li>
            {contrato && (
              <li>
                <a href={contrato} target="_blank" rel="noreferrer">
                  {t.pie.contrato} ↗
                </a>
              </li>
            )}
          </ul>
          <h4 className="sitio-pie-sub">{t.pie.quienes}</h4>
          <p>{t.pie.equipo}</p>
          {sitio.contacto && (
            <p>
              {t.pie.escribenos}: <a href={`mailto:${sitio.contacto}`}>{sitio.contacto}</a>
            </p>
          )}
        </div>
        <div>
          <h4>{t.pie.construido}</h4>
          <ul>
            {tecnologia.map((x) => (
              <li key={x.nombre}>
                <a href={x.url} target="_blank" rel="noreferrer">
                  {x.nombre} ↗
                </a>
                <span>{x.nota}</span>
              </li>
            ))}
          </ul>
        </div>
      </div>
      <div className="sitio-pie-final">
        <span>{t.pie.derechos}</span>
        <span className="sitio-pie-prueba">{t.pie.prueba}</span>
      </div>
    </footer>
  )
}
