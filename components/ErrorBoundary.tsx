import React, { Component, ReactNode } from 'react';
import { systemLogService } from '../services/systemLogService';
import { handleError, getErrorInfo, type ErrorInfo as AppErrorInfo } from '../utils/errors/ErrorHandler';
import { TRANSLATIONS } from '../i18n';
import { Language } from '../types';

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
  onError?: (error: Error, errorInfo: React.ErrorInfo) => void;
  showDetails?: boolean;
}

interface State {
  hasError: boolean;
  error: Error | null;
  errorInfo: AppErrorInfo | null;
}

export class ErrorBoundary extends Component<Props, State> {
  declare state: State;

  constructor(props: Props) {
    super(props);
    this.state = {
      hasError: false,
      error: null,
      errorInfo: null
    };
  }

  public static getDerivedStateFromError(error: Error): Partial<State> {
    return { hasError: true, error };
  }

  public componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    const appError = handleError(error);
    const info = getErrorInfo(appError.code);

    systemLogService.error('error', error.message, error, {
      componentStack: errorInfo.componentStack,
      code: appError.code,
      category: appError.category
    });

    this.setState({ errorInfo: info });
    this.props.onError?.(error, errorInfo);
  }

  private handleRetry = () => {
    this.setState({ hasError: false, error: null, errorInfo: null });
  };

  private handleRefresh = () => {
    window.location.reload();
  };

  private getTranslations = () => {
    try {
      const stored = localStorage.getItem('popsmoke_settings');
      if (stored) {
        const settings = JSON.parse(stored);
        const language = (settings?.language as Language) || 'en';
        return TRANSLATIONS[language] || TRANSLATIONS.en;
      }
      return TRANSLATIONS.en;
    } catch {
      return TRANSLATIONS.en;
    }
  };

  public render() {
    if (this.state.hasError) {
      if (this.props.fallback) {
        return this.props.fallback;
      }

      const t = this.getTranslations();
      const info = this.state.errorInfo;
      const isRecoverable = info?.recoverable ?? true;
      const action = info?.action;

      return (
        <div className="min-h-screen bg-paper flex items-center justify-center p-4">
          <div className="bg-white border-4 border-black p-8 max-w-md w-full shadow-pop">
            <div className="text-center">
              <div className="text-6xl mb-4">💥</div>
              <h2 className="font-display text-2xl uppercase tracking-wider mb-4">
                {t.somethingWentWrong}
              </h2>
              <p className="text-gray-600 mb-6">
                {info?.userMessage || 'An unexpected error occurred'}
              </p>

              {this.props.showDetails && this.state.error && (
                <details className="text-left mb-6 p-4 bg-gray-100 border-2 border-black">
                  <summary className="cursor-pointer font-display uppercase text-sm">
                    {t.errorDetails}
                  </summary>
                  <pre className="mt-2 text-xs overflow-auto">
                    {this.state.error.message}
                    {this.state.error.stack && (
                      <>
                        {'\n\n'}
                        {this.state.error.stack}
                      </>
                    )}
                  </pre>
                </details>
              )}

              <div className="flex gap-4 justify-center">
                {isRecoverable && action === 'retry' && (
                  <button
                    onClick={this.handleRetry}
                    className="px-6 py-2 font-display text-sm uppercase tracking-wider border-4 border-black shadow-pop transition-all transform hover:shadow-pop-hover hover:translate-x-[2px] hover:translate-y-[2px]"
                    style={{ backgroundColor: '#FFD700' }}
                  >
                    {t.tryAgain}
                  </button>
                )}
                <button
                  onClick={this.handleRefresh}
                  className="px-6 py-2 font-display text-sm uppercase tracking-wider border-4 border-black shadow-pop transition-all transform hover:shadow-pop-hover hover:translate-x-[2px] hover:translate-y-[2px] bg-white"
                >
                  {t.refreshPage}
                </button>
              </div>
            </div>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}

export function withErrorBoundary<P extends object>(
  WrappedComponent: React.ComponentType<P>,
  errorBoundaryProps?: Omit<Props, 'children'>
) {
  const WithErrorBoundaryComponent = (props: P) => (
    <ErrorBoundary {...errorBoundaryProps}>
      <WrappedComponent {...props} />
    </ErrorBoundary>
  );

  WithErrorBoundaryComponent.displayName = `withErrorBoundary(${WrappedComponent.displayName || WrappedComponent.name || 'Component'})`;

  return WithErrorBoundaryComponent;
}
