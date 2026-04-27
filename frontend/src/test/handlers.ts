import { http, HttpResponse } from 'msw'
import type { Product, Order } from '../types'

export const sampleProduct: Product = {
  id: 1,
  sku: 'LAP-001',
  name: 'Pro Laptop',
  description: '16GB RAM',
  price: 1299.99,
  categoryId: 1,
  categoryName: 'Electronics',
  images: [],
  isActive: true,
}

export const sampleOrder: Order = {
  id: 101,
  status: 'PENDING',
  items: [{ productId: 1, productName: 'Pro Laptop', quantity: 1, unitPrice: 1299.99 }],
  totalAmount: 1299.99,
  createdAt: new Date().toISOString(),
}

export const handlers = [
  http.get('/api/catalog/products', () =>
    HttpResponse.json({
      success: true,
      data: {
        content: [sampleProduct],
        page: 0,
        size: 12,
        totalElements: 1,
        totalPages: 1,
        last: true,
      },
    })
  ),

  http.get('/api/catalog/products/search', () =>
    HttpResponse.json({
      success: true,
      data: {
        content: [sampleProduct],
        page: 0,
        size: 12,
        totalElements: 1,
        totalPages: 1,
        last: true,
      },
    })
  ),

  http.get('/api/orders', () =>
    HttpResponse.json({ success: true, data: [sampleOrder] })
  ),

  http.post('/api/orders', () =>
    HttpResponse.json({ success: true, data: sampleOrder }, { status: 201 })
  ),

  http.post('/api/auth/login', () =>
    HttpResponse.json({
      success: true,
      data: {
        accessToken: 'test.access.token',
        refreshToken: 'test-refresh-token',
        username: 'alice',
        role: 'USER',
      },
    })
  ),

  http.post('/api/auth/logout', () => new HttpResponse(null, { status: 204 })),
]
