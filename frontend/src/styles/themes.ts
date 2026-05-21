/**
 * Theme definitions for the agent chat UI.
 * Easily switch between pre-built themes or create custom ones.
 */

export interface Theme {
  name: string
  colors: {
    primary: string
    primaryGradient: [string, string]
    secondary: string
    background: string
    surface: string
    surfaceAlt: string
    text: string
    textMuted: string
    border: string
    error: string
    errorBg: string
    success: string
    warning: string
    userMessage: string
    agentMessage: string
  }
  fonts: {
    family: string
    mono: string
    sizeLarge: string
    sizeBase: string
    sizeSmall: string
    sizeXSmall: string
  }
  spacing: {
    xs: string
    sm: string
    md: string
    lg: string
    xl: string
  }
  radius: {
    sm: string
    md: string
    lg: string
  }
  shadows: {
    sm: string
    md: string
    lg: string
  }
}

// ============================================================================
// Light Theme (Default)
// ============================================================================

export const lightTheme: Theme = {
  name: 'light',
  colors: {
    primary: '#667eea',
    primaryGradient: ['#667eea', '#764ba2'],
    secondary: '#f093fb',
    background: 'linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%)',
    surface: '#ffffff',
    surfaceAlt: '#f9f9f9',
    text: '#333333',
    textMuted: '#666666',
    border: '#e5e5e5',
    error: '#c33333',
    errorBg: '#fee',
    success: '#2ecc71',
    warning: '#f39c12',
    userMessage: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    agentMessage: '#f0f4f8',
  },
  fonts: {
    family: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif',
    mono: '"Fira Code", "Courier New", monospace',
    sizeLarge: '18px',
    sizeBase: '14px',
    sizeSmall: '12px',
    sizeXSmall: '11px',
  },
  spacing: {
    xs: '4px',
    sm: '8px',
    md: '16px',
    lg: '20px',
    xl: '24px',
  },
  radius: {
    sm: '4px',
    md: '8px',
    lg: '12px',
  },
  shadows: {
    sm: '0 2px 4px rgba(0, 0, 0, 0.1)',
    md: '0 4px 6px rgba(0, 0, 0, 0.1)',
    lg: '0 10px 40px rgba(0, 0, 0, 0.2)',
  },
}

// ============================================================================
// Dark Theme
// ============================================================================

export const darkTheme: Theme = {
  name: 'dark',
  colors: {
    primary: '#5a7fd8',
    primaryGradient: ['#5a7fd8', '#6b4fa0'],
    secondary: '#e075d9',
    background: 'linear-gradient(135deg, #1a1a2e 0%, #16213e 100%)',
    surface: '#0f0f1e',
    surfaceAlt: '#1a1a2e',
    text: '#e0e0e0',
    textMuted: '#999999',
    border: '#333333',
    error: '#ff6b6b',
    errorBg: '#3d2222',
    success: '#51cf66',
    warning: '#ffa94d',
    userMessage: 'linear-gradient(135deg, #5a7fd8 0%, #6b4fa0 100%)',
    agentMessage: '#1a1a2e',
  },
  fonts: {
    family: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif',
    mono: '"Fira Code", "Courier New", monospace',
    sizeLarge: '18px',
    sizeBase: '14px',
    sizeSmall: '12px',
    sizeXSmall: '11px',
  },
  spacing: {
    xs: '4px',
    sm: '8px',
    md: '16px',
    lg: '20px',
    xl: '24px',
  },
  radius: {
    sm: '4px',
    md: '8px',
    lg: '12px',
  },
  shadows: {
    sm: '0 2px 4px rgba(0, 0, 0, 0.3)',
    md: '0 4px 6px rgba(0, 0, 0, 0.4)',
    lg: '0 10px 40px rgba(0, 0, 0, 0.5)',
  },
}

// ============================================================================
// Corporate Theme (Professional Blues)
// ============================================================================

export const corporateTheme: Theme = {
  name: 'corporate',
  colors: {
    primary: '#0052cc',
    primaryGradient: ['#0052cc', '#003d99'],
    secondary: '#0066ff',
    background: 'linear-gradient(135deg, #f5f6f8 0%, #e8ebf1 100%)',
    surface: '#ffffff',
    surfaceAlt: '#f5f6f8',
    text: '#1a1a1a',
    textMuted: '#555555',
    border: '#d0d7de',
    error: '#d1242f',
    errorBg: '#fce8e8',
    success: '#2da44e',
    warning: '#e3b341',
    userMessage: 'linear-gradient(135deg, #0052cc 0%, #003d99 100%)',
    agentMessage: '#f5f6f8',
  },
  fonts: {
    family: '"Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif',
    mono: '"SF Mono", Monaco, "Cascadia Code", "Courier New", monospace',
    sizeLarge: '16px',
    sizeBase: '13px',
    sizeSmall: '11px',
    sizeXSmall: '10px',
  },
  spacing: {
    xs: '4px',
    sm: '8px',
    md: '16px',
    lg: '24px',
    xl: '32px',
  },
  radius: {
    sm: '3px',
    md: '6px',
    lg: '8px',
  },
  shadows: {
    sm: '0 1px 3px rgba(0, 0, 0, 0.08)',
    md: '0 4px 12px rgba(0, 0, 0, 0.1)',
    lg: '0 8px 24px rgba(0, 0, 0, 0.12)',
  },
}

// ============================================================================
// Minimal Theme (Clean, Minimalist)
// ============================================================================

export const minimalTheme: Theme = {
  name: 'minimal',
  colors: {
    primary: '#000000',
    primaryGradient: ['#333333', '#000000'],
    secondary: '#666666',
    background: '#ffffff',
    surface: '#ffffff',
    surfaceAlt: '#f8f8f8',
    text: '#222222',
    textMuted: '#888888',
    border: '#e0e0e0',
    error: '#d32f2f',
    errorBg: '#fff5f5',
    success: '#388e3c',
    warning: '#f57c00',
    userMessage: '#f0f0f0',
    agentMessage: '#ffffff',
  },
  fonts: {
    family: 'system-ui, -apple-system, sans-serif',
    mono: '"Menlo", "Monaco", "Courier New", monospace',
    sizeLarge: '16px',
    sizeBase: '14px',
    sizeSmall: '12px',
    sizeXSmall: '11px',
  },
  spacing: {
    xs: '2px',
    sm: '4px',
    md: '8px',
    lg: '16px',
    xl: '24px',
  },
  radius: {
    sm: '2px',
    md: '4px',
    lg: '6px',
  },
  shadows: {
    sm: '0 1px 2px rgba(0, 0, 0, 0.05)',
    md: '0 2px 4px rgba(0, 0, 0, 0.1)',
    lg: '0 4px 12px rgba(0, 0, 0, 0.15)',
  },
}

// ============================================================================
// Ocean Theme (Blues and Teals)
// ============================================================================

export const oceanTheme: Theme = {
  name: 'ocean',
  colors: {
    primary: '#0891b2',
    primaryGradient: ['#0891b2', '#0e7490'],
    secondary: '#06b6d4',
    background: 'linear-gradient(135deg, #e0f2fe 0%, #cffafe 100%)',
    surface: '#ffffff',
    surfaceAlt: '#f0f9ff',
    text: '#164e63',
    textMuted: '#06b6d4',
    border: '#a5f3fc',
    error: '#be123c',
    errorBg: '#ffe4e6',
    success: '#0891b2',
    warning: '#d97706',
    userMessage: 'linear-gradient(135deg, #0891b2 0%, #0e7490 100%)',
    agentMessage: '#ecf8ff',
  },
  fonts: {
    family: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
    mono: '"Fira Code", monospace',
    sizeLarge: '18px',
    sizeBase: '14px',
    sizeSmall: '12px',
    sizeXSmall: '11px',
  },
  spacing: {
    xs: '4px',
    sm: '8px',
    md: '16px',
    lg: '20px',
    xl: '24px',
  },
  radius: {
    sm: '6px',
    md: '8px',
    lg: '12px',
  },
  shadows: {
    sm: '0 1px 3px rgba(6, 182, 212, 0.1)',
    md: '0 4px 12px rgba(6, 182, 212, 0.15)',
    lg: '0 10px 40px rgba(6, 182, 212, 0.2)',
  },
}

// ============================================================================
// All Themes Registry
// ============================================================================

export const THEMES: Record<string, Theme> = {
  light: lightTheme,
  dark: darkTheme,
  corporate: corporateTheme,
  minimal: minimalTheme,
  ocean: oceanTheme,
}

export type ThemeName = keyof typeof THEMES
