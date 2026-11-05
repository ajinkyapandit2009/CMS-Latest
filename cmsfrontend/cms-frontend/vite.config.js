import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'
import fs from 'fs'

export default defineConfig({
  plugins: [react()],

  server: {
    host: '0.0.0.0',
    port: 5173,
    strictPort: true,

    https: {
      cert: fs.readFileSync('./cms-local.pem'),
      key: fs.readFileSync('./cms-local-key.pem'),
    },
  },
})