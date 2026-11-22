import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// En desarrollo, /api va al servidor de recompensas (recompensas/server).
export default defineConfig({
  plugins: [react()],
  server: { proxy: { '/api': 'http://localhost:8787' } },
})
