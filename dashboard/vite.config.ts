/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import path from 'node:path'

// En développement (npm run dev), les appels API sont relayés vers erp-core.
// En production, c'est nginx (dashboard) qui relaie vers erp-core:8000.
const backend = process.env.ERP_BACKEND ?? 'http://localhost:8000'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: { alias: { '@': path.resolve(__dirname, 'src') } },
  server: {
    port: 5173,
    proxy: {
      '/api': backend,
      '/health': backend,
      '/events': backend,
      '/status': backend,
    },
  },
})
