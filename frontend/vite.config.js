import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // 개발 서버에서도 운영(Nginx)과 같은 상대 경로 /api/... 로 호출한다 (TS-04)
    proxy: {
      '/api': process.env.API_TARGET || 'http://localhost:8080',
    },
  },
})
