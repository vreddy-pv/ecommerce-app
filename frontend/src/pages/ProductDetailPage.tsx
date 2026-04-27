import { useState, useEffect } from 'react'
import { useParams, Link } from 'react-router-dom'
import { getProduct } from '../api/catalog'
import client from '../api/client'
import { useCart } from '../context/CartContext'
import type { Product, ApiResponse, StockStatus } from '../types'

export default function ProductDetailPage() {
  const { id } = useParams<{ id: string }>()
  const { addItem } = useCart()
  const [product, setProduct] = useState<Product | null>(null)
  const [stock, setStock] = useState<number | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [added, setAdded] = useState(false)

  useEffect(() => {
    if (!id) return
    setLoading(true)
    Promise.all([
      getProduct(Number(id)),
      client
        .get<ApiResponse<StockStatus>>(`/inventory/${id}/stock`)
        .then((r) => r.data.data)
        .catch(() => null),
    ])
      .then(([p, s]) => {
        setProduct(p)
        setStock(s ? s.available : null)
        setError(null)
      })
      .catch(() => setError('Product not found.'))
      .finally(() => setLoading(false))
  }, [id])

  const handleAddToCart = () => {
    if (!product) return
    addItem(product)
    setAdded(true)
    setTimeout(() => setAdded(false), 2000)
  }

  if (loading) return <div style={styles.container}><p>Loading...</p></div>
  if (error || !product) return (
    <div style={styles.container}>
      <p style={{ color: 'red' }}>{error ?? 'Product not found.'}</p>
      <Link to="/products" style={styles.back}>← Back to Products</Link>
    </div>
  )

  const inStock = stock === null ? product.isActive : stock > 0

  return (
    <div style={styles.container}>
      <Link to="/products" style={styles.back}>← Back to Products</Link>

      <div style={styles.card}>
        {/* Image placeholder */}
        <div style={styles.imagePlaceholder}>
          <span style={{ fontSize: 48, color: '#ccc' }}>📦</span>
        </div>

        <div style={styles.info}>
          <p style={styles.category}>{product.categoryName}</p>
          <h1 style={styles.name}>{product.name}</h1>
          <p style={styles.sku}>SKU: {product.sku}</p>
          <p style={styles.description}>{product.description}</p>

          <div style={styles.priceRow}>
            <span style={styles.price}>${product.price.toFixed(2)}</span>
            <span style={{ ...styles.badge, background: inStock ? '#e6f4ea' : '#fce8e6', color: inStock ? '#137333' : '#c5221f' }}>
              {stock !== null ? (inStock ? `${stock} in stock` : 'Out of stock') : (inStock ? 'In stock' : 'Out of stock')}
            </span>
          </div>

          <button
            onClick={handleAddToCart}
            disabled={!inStock}
            style={{
              ...styles.btn,
              background: added ? '#137333' : inStock ? '#1a73e8' : '#ccc',
              cursor: inStock ? 'pointer' : 'not-allowed',
            }}
          >
            {added ? '✓ Added to Cart' : 'Add to Cart'}
          </button>
        </div>
      </div>
    </div>
  )
}

const styles: Record<string, React.CSSProperties> = {
  container: { maxWidth: 900, margin: '0 auto', padding: 24 },
  back: { display: 'inline-block', marginBottom: 20, color: '#1a73e8', textDecoration: 'none', fontSize: 14 },
  card: { display: 'flex', gap: 32, background: '#fff', borderRadius: 12, border: '1px solid #ddd', padding: 32 },
  imagePlaceholder: {
    width: 280, minWidth: 280, height: 280, background: '#f5f5f5',
    borderRadius: 8, display: 'flex', alignItems: 'center', justifyContent: 'center',
  },
  info: { flex: 1 },
  category: { color: '#888', fontSize: 13, margin: '0 0 6px', textTransform: 'uppercase', letterSpacing: 1 },
  name: { fontSize: 26, fontWeight: 700, margin: '0 0 6px' },
  sku: { color: '#aaa', fontSize: 12, margin: '0 0 16px' },
  description: { color: '#444', lineHeight: 1.6, margin: '0 0 24px' },
  priceRow: { display: 'flex', alignItems: 'center', gap: 16, marginBottom: 24 },
  price: { fontSize: 32, fontWeight: 700, color: '#1a1a1a' },
  badge: { padding: '4px 12px', borderRadius: 20, fontSize: 13, fontWeight: 500 },
  btn: {
    padding: '12px 32px', color: '#fff', border: 'none',
    borderRadius: 6, fontSize: 16, fontWeight: 600, transition: 'background 0.2s',
  },
}
