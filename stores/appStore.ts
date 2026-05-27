import { create } from 'zustand';
import { AppSettings, SmokeLog, ViewState } from '../types';
import { DEFAULT_SETTINGS } from '../constants';

interface AppState {
  view: ViewState;
  settings: AppSettings;
  isLocalMode: boolean;
  isInitialized: boolean;
  isLoading: boolean;
  isConnecting: boolean;
  isRecording: boolean;
  isRestoring: boolean;
  showSystemLog: boolean;
  showStorageErrorDialog: boolean;
  storageError: string | null;
}

interface AppActions {
  setView: (view: ViewState) => void;
  updateSettings: (settings: Partial<AppSettings>) => void;
  setSettings: (settings: AppSettings) => void;
  setIsLocalMode: (isLocalMode: boolean) => void;
  setIsInitialized: (isInitialized: boolean) => void;
  setIsLoading: (isLoading: boolean) => void;
  setIsConnecting: (isConnecting: boolean) => void;
  setIsRecording: (isRecording: boolean) => void;
  setIsRestoring: (isRestoring: boolean) => void;
  setShowSystemLog: (show: boolean) => void;
  setShowStorageErrorDialog: (show: boolean) => void;
  setStorageError: (error: string | null) => void;
  resetToDefaults: () => void;
}

export const useAppStore = create<AppState & AppActions>((set) => ({
  // 初始状态
  view: ViewState.DASHBOARD,
  settings: DEFAULT_SETTINGS,
  isLocalMode: true,
  isInitialized: false,
  isLoading: true,
  isConnecting: false,
  isRecording: false,
  isRestoring: false,
  showSystemLog: false,
  showStorageErrorDialog: false,
  storageError: null,

  // 操作方法
  setView: (view) => set({ view }),
  
  updateSettings: (newSettings) => set((state) => ({
    settings: { ...state.settings, ...newSettings }
  })),
  
  setSettings: (settings) => set({ settings }),
  
  setIsLocalMode: (isLocalMode) => set({ isLocalMode }),
  setIsInitialized: (isInitialized) => set({ isInitialized }),
  setIsLoading: (isLoading) => set({ isLoading }),
  setIsConnecting: (isConnecting) => set({ isConnecting }),
  setIsRecording: (isRecording) => set({ isRecording }),
  setIsRestoring: (isRestoring) => set({ isRestoring }),
  setShowSystemLog: (showSystemLog) => set({ showSystemLog }),
  setShowStorageErrorDialog: (showStorageErrorDialog) => set({ showStorageErrorDialog }),
  setStorageError: (storageError) => set({ storageError }),
  
  resetToDefaults: () => set({
    view: ViewState.DASHBOARD,
    settings: DEFAULT_SETTINGS,
    isLocalMode: true,
  }),
}));
