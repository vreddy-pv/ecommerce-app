import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { listOrders } from '../api/orders'
import type { Order } from '../types'

const STATUS_COLOR: Record<Order['status'], string> = {
  PENDING: '#f9a825',
  CONFIRMED: '#1565c0',
  PROCESSING: '#6a1b9a',
  SHIPPED: '#00838f',
  DELIVERED: '#2e7d32',
  CANCELLED: '#c62828',
}

export default function OrderHistoryPage() {
  const [orders, setOrders] = useState<Order[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    listOrders()
      .then(setOrders)
      .catch(() => setError('Failed to load orders'))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <p style={{ padding: 24 }}>Loading orders...</p>
  if (error) return <p style={{ padding: 24, color: 'red' }}>{error}</p>

  return (
    <div style={{ padding: 24, maxWidth: 720, margin: '0 auto' }}>
      <h2>My Orders</h2>
      {orders.length === 0 && <p>No orders yet.</p>}
      {orders.map((order) => (
        <div key={order.id} data-testid="order-card" style={{ border: '1px solid #ddd', borderRadius: 8, padding: 16, marginBottom: 12 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <Link to={`/orders/${order.id}`} style={{ fontWeight: 'bold', textDecoration: 'none', color: '#1a73e8' }}>
              Order #{order.id}
            </Link>
            <span style={{ background: STATUS_COLOR[order.status], color: '#fff', padding: '2px 10px', borderRadius: 12, fontSize: 13 }}>
              {order.status}
            </span>
          </div>
          <p style={{ margin: '8px 0 0', color: '#666', fontSize: 14 }}>
            {order.items.length} item{order.items.length !== 1 ? 's' : ''} · Total: ${order.totalAmount.toFixed(2)} · {new Date(order.createdAt).toLocaleDateString()}
          </p>
        </div>
      ))}
    </div>
  )
}
