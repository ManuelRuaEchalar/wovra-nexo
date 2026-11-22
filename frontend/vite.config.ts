import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// En desarrollo, /api va al backend (carpeta backend/, puerto 8787).
export default defineConfig({
  plugins: [react()],
  server: { proxy: { '/api': 'http://localhost:8787' } },
})
