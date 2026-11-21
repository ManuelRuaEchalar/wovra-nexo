import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { PollarProvider } from '@pollar/react'
import '@pollar/react/styles.css'
import './index.css'
import App from './App'
import { api, type ConfigPublica } from './api'
import Panel from './panel/Panel'

const raiz = createRoot(document.getElementById('root')!)

function montar(config: ConfigPublica) {
  const app = <App config={config} />
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

if (location.pathname.startsWith('/panel')) {
  // El panel no usa Pollar: solo descifra en el navegador y habla con el relevo.
  raiz.render(
    <StrictMode>
      <Panel />
    </StrictMode>,
  )
} else {
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
