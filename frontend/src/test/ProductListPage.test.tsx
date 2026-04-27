import { describe, it, expect } from 'vitest'
import { render, screen, waitFor } from './utils'
import ProductListPage from '../pages/ProductListPage'

describe('ProductListPage', () => {
  it('renders product cards after loading', async () => {
    render(<ProductListPage />)
    await waitFor(() => expect(screen.getByText('Pro Laptop')).toBeInTheDocument())
  })

  it('shows loading text initially', () => {
    render(<ProductListPage />)
    expect(screen.getByText('Loading...')).toBeInTheDocument()
  })

  it('renders search form', () => {
    render(<ProductListPage />)
    expect(screen.getByPlaceholderText('Search products...')).toBeInTheDocument()
  })
})
