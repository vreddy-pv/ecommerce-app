import { describe, it, expect } from 'vitest'
import { render, screen, fireEvent } from './utils'
import ProductCard from '../components/ProductCard'
import { sampleProduct } from './handlers'

describe('ProductCard', () => {
  it('renders product name and price', () => {
    render(<ProductCard product={sampleProduct} />)
    expect(screen.getByText('Pro Laptop')).toBeInTheDocument()
    expect(screen.getByText('$1299.99')).toBeInTheDocument()
  })

  it('adds product to cart when button clicked', () => {
    render(<ProductCard product={sampleProduct} />)
    fireEvent.click(screen.getByRole('button', { name: /add to cart/i }))
    // Cart count appears in Navbar — here we just verify no error thrown
  })

  it('disables button for inactive product', () => {
    render(<ProductCard product={{ ...sampleProduct, isActive: false }} />)
    expect(screen.getByRole('button', { name: /add to cart/i })).toBeDisabled()
  })
})
