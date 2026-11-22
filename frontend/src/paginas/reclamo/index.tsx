import { StrictMode } from 'react'
import type { Root } from 'react-dom/client'
import { PollarProvider } from '@pollar/react'
import '@pollar/react/styles.css'
import PaginaReclamo from './Reclamo'
import { api, type ConfigPublica } from './api'

/** Página de reclamo (/r/:id): pide la configuración pública y monta Pollar si hay clave. */
export function iniciarReclamo(raiz: Root) {
  const montar = (config: ConfigPublica) => {
    const app = <PaginaReclamo config={config} />
    raiz.render(
      <StrictMode>
        {config.pollarClavePublica ? (
          <PollarProvider client={{ apiKey: config.pollarClavePublica, stellarNetwork: 'testnet' }}>
            {app}
          </PollarProvider>
        ) : (
          app
        )}
      </StrictMode>,
    )
  }
  api
    .config()
    .then(montar)
    .catch(() =>
      montar({
        red: 'testnet',
        pollarClavePublica: null,
        usdc: { codigo: 'USDC', emisor: 'GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5' },
        tasaBsPorUsdc: 6.96,
      }),
    )
}
