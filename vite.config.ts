import path from 'path';
import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
    const env = loadEnv(mode, '.', '');
    return {
      server: {
        port: 3000,
        host: '0.0.0.0',
      },
      plugins: [react()],
      resolve: {
        alias: {
          '@': path.resolve(__dirname, '.'),
        }
      },
      build: {
        rollupOptions: {
          output: {
            manualChunks(id) {
              // React 核心
              if (id.includes('node_modules/react') || id.includes('node_modules/react-dom')) {
                return 'vendor-react';
              }
              
              // Recharts (图表库 - 懒加载)
              if (id.includes('node_modules/recharts')) {
                return 'vendor-recharts';
              }
              
              // Supabase
              if (id.includes('node_modules/@supabase')) {
                return 'vendor-supabase';
              }
              
              // Zustand (状态管理)
              if (id.includes('node_modules/zustand')) {
                return 'vendor-zustand';
              }

              // 业务逻辑拆分 - 避免循环依赖
              if (id.includes('/stores/')) {
                return 'stores';
              }
              
              if (id.includes('/hooks/')) {
                return 'hooks';
              }
              
              if (id.includes('/services/')) {
                return 'services';
              }
              
              // Pages 和 UI Components 合并为一个 chunk（避免循环）
              if (id.includes('/components/pages/') || id.includes('/components/ui/')) {
                return 'components';
              }
              
              if (id.includes('/utils/')) {
                return 'utils';
              }

              return undefined;
            },
            
            // Chunk 文件名格式
            chunkFileNames: 'assets/[name]-[hash].js',
            entryFileNames: 'assets/[name]-[hash].js',
          }
        },
        
        // 增大 chunk 大小警告阈值
        chunkSizeWarningLimit: 350,
        
        // 启用 CSS code split
        cssCodeSplit: true,
        
        // 目标浏览器
        target: 'es2020',
        
        // 压缩配置
        minify: 'terser',
        terserOptions: {
          compress: {
            drop_console: mode === 'production',
            drop_debugger: mode === 'production'
          },
          format: {
            comments: false
          }
        }
      },
      
      // 优化依赖预构建
      optimizeDeps: {
        include: [
          'react',
          'react-dom',
          'zustand',
          '@supabase/supabase-js'
          // recharts 不预构建，按需加载
        ]
      }
    };
});
