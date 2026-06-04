import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { fileURLToPath, URL } from 'node:url';
import { execSync } from 'node:child_process';

// WSL2 下获取 Windows 主机 IP
function getBackendTarget(): string {
  if (process.env.WSL_DISTRO_NAME) {
    try {
      const hostIp = execSync("hostname -I | awk '{print $1}'", { encoding: 'utf-8' }).trim();
      return `http://${hostIp}:8080`;
    } catch {
      return 'http://localhost:8080';
    }
  }
  return 'http://localhost:8080';
}

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    historyApiFallback: true,
    proxy: {
      '/api/v1': {
        target: getBackendTarget(),
        changeOrigin: true,
      },
    },
  },
  build: {
    // 构建输出到 Spring Boot 的 static 目录
    outDir: '../gateway-boot/src/main/resources/static',
    emptyOutDir: true,
    sourcemap: false,
    rollupOptions: {
      output: {
        // 静态资源文件名
        assetFileNames: 'assets/[name]-[hash][extname]',
        chunkFileNames: 'assets/[name]-[hash].js',
        entryFileNames: 'assets/[name]-[hash].js',
      },
    },
  },
});
