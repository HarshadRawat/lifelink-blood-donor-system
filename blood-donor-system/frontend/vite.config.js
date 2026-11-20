import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const target = env.BACKEND_URL || process.env.BACKEND_URL || 'http://localhost:8080';
  const wsTarget = env.BACKEND_WS_URL || process.env.BACKEND_WS_URL || target.replace(/^http/, 'ws');

  return {
    plugins: [react()],
    server: {
      port: 5173,
      watch: {
        usePolling: true,
      },
      proxy: {
        '/api': {
          target: target,
          changeOrigin: true,
          secure: false,
        },
        '/ws': {
          target: wsTarget,
          ws: true,
          changeOrigin: true,
        },
      },
    },
    test: {
      globals: true,
      environment: 'jsdom',
      setupFiles: './src/test/setup.js',
    },
  };
});
