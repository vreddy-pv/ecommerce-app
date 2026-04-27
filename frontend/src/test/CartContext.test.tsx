import { describe, it, expect } from 'vitest'
import { render, screen, fireEvent } from './utils'
import { useCart } from '../context/CartContext'
import { sampleProduct } from './handlers'

function CartTestHarness() {
  const { items, totalItems, totalPrice, addItem, removeItem, updateQuantity, clearCart } = useCart()
  return (
    <div>
      <p data-testid="count">{totalItems}</p>
      <p data-testid="price">{totalPrice.toFixed(2)}</p>
      <p data-testid="items">{items.length}</p>
      <button onClick={() => addItem(sampleProduct)}>Add</button>
      <button onClick={() => removeItem(sampleProduct.id)}>Remove</button>
      <button onClick={() => updateQuantity(sampleProduct.id, 5)}>Set5</button>
      <button onClick={clearCart}>Clear</button>
    </div>
  )
}

describe('CartContext', () => {
  it('starts empty', () => {
    render(<CartTestHarness />)
    expect(screen.getByTestId('count').textContent).toBe('0')
  })

  it('adds item and increments count', () => {
    render(<CartTestHarness />)
    fireEvent.click(screen.getByText('Add'))
    expect(screen.getByTestId('count').textContent).toBe('1')
    expect(screen.getByTestId('price').textContent).toBe('1299.99')
  })

  it('adding same item increases quantity not item count', () => {
    render(<CartTestHarness />)
    fireEvent.click(screen.getByText('Add'))
    fireEvent.click(screen.getByText('Add'))
    expect(screen.getByTestId('items').textContent).toBe('1')
    expect(screen.getByTestId('count').textContent).toBe('2')
  })

  it('removes item', () => {
    render(<CartTestHarness />)
    fireEvent.click(screen.getByText('Add'))
    fireEvent.click(screen.getByText('Remove'))
    expect(screen.getByTestId('count').textContent).toBe('0')
  })

  it('updates quantity', () => {
    render(<CartTestHarness />)
    fireEvent.click(screen.getByText('Add'))
    fireEvent.click(screen.getByText('Set5'))
    expect(screen.getByTestId('count').textContent).toBe('5')
  })

  it('clears cart', () => {
    render(<CartTestHarness />)
    fireEvent.click(screen.getByText('Add'))
    fireEvent.click(screen.getByText('Clear'))
    expect(screen.getByTestId('count').textContent).toBe('0')
  })
})
