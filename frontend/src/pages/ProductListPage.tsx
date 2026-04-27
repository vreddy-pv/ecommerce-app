import { useState, useEffect } from 'react'
import { useSearchParams } from 'react-router-dom'
import { listProducts, searchProducts } from '../api/catalog'
import ProductCard from '../components/ProductCard'
import type { Product } from '../types'

export default function ProductListPage() {
  const [products, setProducts] = useState<Product[]>([])
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [searchParams, setSearchParams] = useSearchParams()
  const query = searchParams.get('q') ?? ''
  const page = Number(searchParams.get('page') ?? '0')

  useEffect(() => {
    setLoading(true)
    const fetch = query
      ? searchProducts(query, page)
      : listProducts(page)
    fetch
      .then((r) => {
        setProducts(r.content)
        setTotalPages(r.totalPages)
        setError(null)
      })
      .catch(() => setError('Failed to load products'))
      .finally(() => setLoading(false))
  }, [query, page])

  const handleSearch = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    const q = (e.currentTarget.elements.namedItem('q') as HTMLInputElement).value
    setSearchParams(q ? { q } : {})
  }

  return (
    <div style={{ padding: 24 }}>
      <form onSubmit={handleSearch} style={{ marginBottom: 24, display: 'flex', gap: 8 }}>
        <input
          name="q"
          defaultValue={query}
          placeholder="Search products..."
          style={{ flex: 1, padding: '8px 12px', border: '1px solid #ddd', borderRadius: 4 }}
        />
        <button type="submit" style={{ padding: '8px 16px', background: '#1a73e8', color: '#fff', border: 'none', borderRadius: 4, cursor: 'pointer' }}>
          Search
        </button>
      </form>

      {loading && <p>Loading...</p>}
      {error && <p style={{ color: 'red' }}>{error}</p>}

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))', gap: 16 }}>
        {products.map((p) => (
          <ProductCard key={p.id} product={p} />
        ))}
      </div>

      {!loading && products.length === 0 && !error && (
        <p>No products found.</p>
      )}

      <div style={{ marginTop: 24, display: 'flex', gap: 8, justifyContent: 'center' }}>
        {Array.from({ length: totalPages }, (_, i) => (
          <button
            key={i}
            onClick={() => setSearchParams({ ...(query ? { q: query } : {}), page: String(i) })}
            style={{ padding: '4px 12px', background: i === page ? '#1a73e8' : '#eee', color: i === page ? '#fff' : '#333', border: 'none', borderRadius: 4, cursor: 'pointer' }}
          >
            {i + 1}
          </button>
        ))}
      </div>
    </div>
  )
}
