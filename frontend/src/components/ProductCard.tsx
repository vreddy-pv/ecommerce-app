import { useCart } from '../context/CartContext'
import type { Product } from '../types'
import { Link } from 'react-router-dom'

interface Props {
  product: Product
}

export default function ProductCard({ product }: Props) {
  const { addItem } = useCart()

  return (
    <div data-testid="product-card" style={{ border: '1px solid #ddd', borderRadius: 8, padding: 16 }}>
      <Link to={`/products/${product.id}`} style={{ textDecoration: 'none', color: 'inherit' }}>
        <h3 style={{ margin: '0 0 8px' }}>{product.name}</h3>
        <p style={{ color: '#666', fontSize: 14, margin: '0 0 8px' }}>{product.categoryName}</p>
        <p style={{ fontWeight: 'bold', fontSize: 18, margin: '0 0 12px' }}>
          ${product.price.toFixed(2)}
        </p>
      </Link>
      <button
        onClick={() => addItem(product)}
        disabled={!product.isActive}
        style={{
          width: '100%',
          padding: '8px 0',
          background: product.isActive ? '#1a73e8' : '#ccc',
          color: '#fff',
          border: 'none',
          borderRadius: 4,
          cursor: product.isActive ? 'pointer' : 'not-allowed',
        }}
      >
        Add to Cart
      </button>
    </div>
  )
}
