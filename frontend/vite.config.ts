import { fileURLToPath, URL } from 'node:url';
import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: process.env.VITE_BACKEND_TARGET || 'http://localhost:8081',
        changeOrigin: true
      },
      '/ws': {
        target: (process.env.VITE_BACKEND_TARGET || 'http://localhost:8081').replace(/^http/, 'ws'),
        changeOrigin: true,
        ws: true
      },
      '/files': {
        target: process.env.VITE_BACKEND_TARGET || 'http://localhost:8081',
        changeOrigin: true
      }
    }
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) return;
          if (id.includes('echarts') || id.includes('zrender')) return 'vendor-echarts';
          if (id.includes('element-plus') || id.includes('@element-plus')) return 'vendor-element';
          if (id.includes('markdown-it') || id.includes('highlight.js') || id.includes('dompurify')) return 'vendor-markdown';
          if (id.includes('/vue/') || id.includes('vue-router') || id.includes('pinia') || id.includes('vue-i18n')) return 'vendor-vue';
          return 'vendor';
        }
      }
    }
  }
});
