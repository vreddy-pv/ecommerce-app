import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useCart } from '../context/CartContext'
import { useAuth } from '../context/AuthContext'
import { createOrder } from '../api/orders'

function generateIdempotencyKey() {
  return `${Date.now()}-${Math.random().toString(36).slice(2)}`
}

export default function CartPage() {
  const { items, totalPrice, removeItem, updateQuantity, clearCart } = useCart()
  const { isAuthenticated } = useAuth()
  const navigate = useNavigate()
  const [placing, setPlacing] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const handleCheckout = async () => {
    if (!isAuthenticated) {
      navigate('/login')
      return
    }
    setPlacing(true)
    setError(null)
    try {
      const order = await createOrder(items, generateIdempotencyKey())
      clearCart()
      navigate(`/orders/${order.id}`)
    } catch {
      setError('Failed to place order. Please try again.')
    } finally {
      setPlacing(false)
    }
  }

  if (items.length === 0) {
    return (
      <div style={{ padding: 24 }}>
        <h2>Your cart is empty</h2>
        <button onClick={() => navigate('/products')} style={{ padding: '8px 16px', background: '#1a73e8', color: '#fff', border: 'none', borderRadius: 4, cursor: 'pointer' }}>
          Browse Products
        </button>
      </div>
    )
  }

  return (
    <div style={{ padding: 24, maxWidth: 640, margin: '0 auto' }}>
      <h2>Shopping Cart</h2>
      {items.map((item) => (
        <div key={item.product.id} data-testid="cart-item" style={{ display: 'flex', alignItems: 'center', gap: 16, padding: '12px 0', borderBottom: '1px solid #eee' }}>
          <div style={{ flex: 1 }}>
            <p style={{ margin: 0, fontWeight: 'bold' }}>{item.product.name}</p>
            <p style={{ margin: 0, color: '#666' }}>${item.product.price.toFixed(2)} each</p>
          </div>
          <input
            type="number"
            min={1}
            value={item.quantity}
            onChange={(e) => updateQuantity(item.product.id, Number(e.target.value))}
            style={{ width: 60, padding: 4, border: '1px solid #ddd', borderRadius: 4 }}
          />
          <button onClick={() => removeItem(item.product.id)} style={{ background: 'transparent', border: 'none', color: '#e53935', cursor: 'pointer', fontSize: 18 }}>✕</button>
        </div>
      ))}
      <div style={{ marginTop: 16, textAlign: 'right' }}>
        <p style={{ fontWeight: 'bold', fontSize: 18 }}>Total: ${totalPrice.toFixed(2)}</p>
        {error && <p style={{ color: 'red' }}>{error}</p>}
        <button
          onClick={handleCheckout}
          disabled={placing}
          style={{ padding: '10px 24px', background: '#1a73e8', color: '#fff', border: 'none', borderRadius: 4, cursor: 'pointer', fontSize: 16 }}
        >
          {placing ? 'Placing Order...' : 'Place Order'}
        </button>
      </div>
    </div>
  )
}
