import client from './client'
import type { ApiResponse, Order, CartItem } from '../types'

export const createOrder = (items: CartItem[], idempotencyKey: string) =>
  client
    .post<ApiResponse<Order>>(
      '/orders',
      {
        items: items.map((i) => ({
          productId: i.product.id,
          quantity: i.quantity,
          unitPrice: i.product.price,
        })),
      },
      { headers: { 'Idempotency-Key': idempotencyKey } }
    )
    .then((r) => r.data.data)

export const listOrders = () =>
  client.get<ApiResponse<Order[]>>('/orders').then((r) => r.data.data)

export const getOrder = (id: number) =>
  client.get<ApiResponse<Order>>(`/orders/${id}`).then((r) => r.data.data)

export const cancelOrder = (id: number) =>
  client.put<ApiResponse<Order>>(`/orders/${id}/cancel`).then((r) => r.data.data)
