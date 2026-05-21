/**
 * ThemeContext — Manage theme switching across the app.
 * Supports 5 pre-built themes + custom theme creation.
 */
import React, { createContext, useContext, useState, useEffect } from 'react'
import { Theme, THEMES, ThemeName } from '../styles/themes'

interface ThemeContextType {
  currentTheme: Theme
  themeName: ThemeName
  setTheme: (name: ThemeName) => void
  customTheme: Theme | null
  setCustomTheme: (theme: Theme | null) => void
  availableThemes: ThemeName[]
}

const ThemeContext = createContext<ThemeContextType | undefined>(undefined)

export const ThemeProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [themeName, setThemeName] = useState<ThemeName>('light')
  const [customTheme, setCustomTheme] = useState<Theme | null>(null)

  // Load theme from localStorage on mount
  useEffect(() => {
    const saved = localStorage.getItem('agent-theme')
    if (saved && saved in THEMES) {
      setThemeName(saved as ThemeName)
    }
  }, [])

  // Save theme to localStorage when changed
  useEffect(() => {
    localStorage.setItem('agent-theme', themeName)
    applyThemeToDOM(customTheme || THEMES[themeName])
  }, [themeName, customTheme])

  const currentTheme = customTheme || THEMES[themeName]

  return (
    <ThemeContext.Provider
      value={{
        currentTheme,
        themeName,
        setTheme: setThemeName,
        customTheme,
        setCustomTheme,
        availableThemes: Object.keys(THEMES) as ThemeName[],
      }}
    >
      {children}
    </ThemeContext.Provider>
  )
}

export const useTheme = (): ThemeContextType => {
  const context = useContext(ThemeContext)
  if (!context) {
    throw new Error('useTheme must be used within ThemeProvider')
  }
  return context
}

/**
 * Apply theme to DOM via CSS variables.
 * Updates :root CSS variables that all components reference.
 */
function applyThemeToDOM(theme: Theme) {
  const root = document.documentElement
  const style = root.style

  // Colors
  style.setProperty('--color-primary', theme.colors.primary)
  style.setProperty('--color-primary-gradient', theme.colors.primaryGradient.join(', '))
  style.setProperty('--color-secondary', theme.colors.secondary)
  style.setProperty('--color-background', theme.colors.background)
  style.setProperty('--color-surface', theme.colors.surface)
  style.setProperty('--color-surface-alt', theme.colors.surfaceAlt)
  style.setProperty('--color-text', theme.colors.text)
  style.setProperty('--color-text-muted', theme.colors.textMuted)
  style.setProperty('--color-border', theme.colors.border)
  style.setProperty('--color-error', theme.colors.error)
  style.setProperty('--color-error-bg', theme.colors.errorBg)
  style.setProperty('--color-success', theme.colors.success)
  style.setProperty('--color-warning', theme.colors.warning)
  style.setProperty('--color-user-message', theme.colors.userMessage)
  style.setProperty('--color-agent-message', theme.colors.agentMessage)

  // Fonts
  style.setProperty('--font-family', theme.fonts.family)
  style.setProperty('--font-mono', theme.fonts.mono)
  style.setProperty('--font-size-lg', theme.fonts.sizeLarge)
  style.setProperty('--font-size-base', theme.fonts.sizeBase)
  style.setProperty('--font-size-sm', theme.fonts.sizeSmall)
  style.setProperty('--font-size-xs', theme.fonts.sizeXSmall)

  // Spacing
  style.setProperty('--space-xs', theme.spacing.xs)
  style.setProperty('--space-sm', theme.spacing.sm)
  style.setProperty('--space-md', theme.spacing.md)
  style.setProperty('--space-lg', theme.spacing.lg)
  style.setProperty('--space-xl', theme.spacing.xl)

  // Radius
  style.setProperty('--radius-sm', theme.radius.sm)
  style.setProperty('--radius-md', theme.radius.md)
  style.setProperty('--radius-lg', theme.radius.lg)

  // Shadows
  style.setProperty('--shadow-sm', theme.shadows.sm)
  style.setProperty('--shadow-md', theme.shadows.md)
  style.setProperty('--shadow-lg', theme.shadows.lg)
}

/**
 * Theme Switcher Component (ready to use in navbar)
 */
export const ThemeSwitcher: React.FC = () => {
  const { themeName, setTheme, availableThemes } = useTheme()

  return (
    <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
      <label htmlFor="theme-select" style={{ fontSize: '14px', fontWeight: 500 }}>
        Theme:
      </label>
      <select
        id="theme-select"
        value={themeName}
        onChange={(e) => setTheme(e.target.value as ThemeName)}
        style={{
          padding: '6px 12px',
          border: `1px solid var(--color-border)`,
          borderRadius: 'var(--radius-md)',
          background: 'var(--color-surface)',
          color: 'var(--color-text)',
          fontFamily: 'var(--font-family)',
          cursor: 'pointer',
        }}
      >
        {availableThemes.map((theme) => (
          <option key={theme} value={theme}>
            {theme.charAt(0).toUpperCase() + theme.slice(1)}
          </option>
        ))}
      </select>
    </div>
  )
}
