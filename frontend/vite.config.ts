import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

const apiBase = process.env.VITE_API_BASE_URL ?? '/api'
const proxyTarget = process.env.VITE_PROXY_TARGET ?? 'http://localhost:8090'

export default defineConfig({
  plugins: [react()],
  server: {
    host: '0.0.0.0',
    port: 5173,
    strictPort: true,
    watch: { usePolling: true },
    proxy: {
      [apiBase]: { target: proxyTarget, changeOrigin: true },
    },
  },
})
