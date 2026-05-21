/**
 * ErrorBoundary — Catches React errors and logs them.
 * Prevents entire app from crashing on component error.
 */
import React, { ReactNode } from 'react'

interface Props {
  children: ReactNode
}

interface State {
  hasError: boolean
  error: Error | null
  errorInfo: React.ErrorInfo | null
}

export class ErrorBoundary extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.state = { hasError: false, error: null, errorInfo: null }
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error, errorInfo: null }
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    // Log to console
    console.error('ErrorBoundary caught an error:', error, errorInfo)

    // Log to backend (optional)
    this.logErrorToBackend(error, errorInfo)

    // Update state
    this.setState({ errorInfo })
  }

  private logErrorToBackend(error: Error, errorInfo: React.ErrorInfo) {
    /**
     * Send error to backend for monitoring.
     * You can use: Sentry, LogRocket, or custom logging endpoint
     */
    try {
      fetch('/api/errors/log', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          timestamp: new Date().toISOString(),
          message: error.toString(),
          stack: error.stack,
          componentStack: errorInfo.componentStack,
          userAgent: navigator.userAgent,
        }),
      }).catch((err) => console.warn('Failed to log error:', err))
    } catch (err) {
      // Silently fail if logging fails
    }
  }

  render() {
    if (this.state.hasError) {
      return (
        <div
          style={{
            padding: '20px',
            margin: '20px',
            border: '2px solid #c33',
            borderRadius: '8px',
            background: '#fee',
            color: '#333',
          }}
        >
          <h2 style={{ marginTop: 0, color: '#c33' }}>⚠️ Something Went Wrong</h2>
          <p>The application encountered an error. Here's what happened:</p>

          {this.state.error && (
            <details style={{ whiteSpace: 'pre-wrap', cursor: 'pointer' }}>
              <summary style={{ marginBottom: '10px', fontWeight: 'bold' }}>
                {this.state.error.toString()}
              </summary>
              {this.state.errorInfo?.componentStack && (
                <pre style={{ background: '#fff', padding: '10px', overflow: 'auto' }}>
                  {this.state.errorInfo.componentStack}
                </pre>
              )}
            </details>
          )}

          <p style={{ marginTop: '15px', fontSize: '14px' }}>
            <button
              onClick={() => window.location.reload()}
              style={{
                padding: '8px 16px',
                background: '#667eea',
                color: 'white',
                border: 'none',
                borderRadius: '4px',
                cursor: 'pointer',
              }}
            >
              Reload Page
            </button>
          </p>
        </div>
      )
    }

    return this.props.children
  }
}
