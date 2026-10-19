import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    setupFiles: './src/setupTests.ts',
    include: ['src/**/*.int.test.{ts,tsx}'],
    env: { VITE_API_URL: 'http://localhost:8080' },
  },
});
