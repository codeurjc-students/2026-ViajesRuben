import { loadEnv } from 'vite';
import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, '.', '');

  return {
    plugins: [react()],
    test: {
      environment: 'jsdom',
      setupFiles: './src/setupTests.ts',
      include: ['src/**/*.int.test.{ts,tsx}'],
      env: {
        VITE_API_URL: env.BACKEND_URL || 'https://localhost',
        // The backend uses a self-signed development certificate, which Node would otherwise reject.
        NODE_TLS_REJECT_UNAUTHORIZED: '0',
      },
    },
  };
});
