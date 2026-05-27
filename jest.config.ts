/** @type {import('ts-jest').JestConfigWithTsJest} */
export default {
  preset: 'ts-jest',
  testEnvironment: 'jsdom',
  roots: ['<rootDir>/__tests__'],
  moduleNameMapper: {
    '^@/(.*)$': '<rootDir>/src/$1',
    '^\\.(css|less|scss|sass)$': 'identity-obj-proxy',
    // Mock services that use import.meta (Vite-specific syntax)
    '^../services/apiService$': '<rootDir>/__tests__/mocks/apiService.mock.ts',
    '^../services/api$': '<rootDir>/__tests__/mocks/apiService.mock.ts',
  },
  transform: {
    '^.+\\.tsx?$': ['ts-jest', {
      tsconfig: {
        jsx: 'react-jsx',
        esModuleInterop: true,
        module: 'commonjs',
        target: 'ES2020',
        strict: true,
        skipLibCheck: true
      }
    }]
  },
  setupFilesAfterFramework: ['jest-framework-setup'],
  setupFiles: ['<rootDir>/__tests__/setup.ts'],
  collectCoverageFrom: [
    'hooks/**/*.ts',
    'stores/**/*.ts',
    'utils/helpers/**/*.ts',
    '!**/*.d.ts',
    '!**/node_modules/**'
  ],
  coverageDirectory: 'coverage',
  coverageReporters: ['text', 'lcov', 'html'],
  testMatch: [
    '**/__tests__/**/*.test.ts',
    '**/__tests__/**/*.test.tsx'
  ],
  verbose: true,
  clearMocks: true,
  restoreMocks: true,
  forceExit: true,
  detectOpenHandles: false,
  testTimeout: 10000
};
