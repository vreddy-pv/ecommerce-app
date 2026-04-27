import client from './client'
import type { ApiResponse, PagedResponse, Product, Category } from '../types'

export const listProducts = (page = 0, size = 12, categoryId?: number) =>
  client
    .get<ApiResponse<PagedResponse<Product>>>('/catalog/products', {
      params: { page, size, ...(categoryId ? { categoryId } : {}) },
    })
    .then((r) => r.data.data)

export const getProduct = (id: number) =>
  client
    .get<ApiResponse<Product>>(`/catalog/products/${id}`)
    .then((r) => r.data.data)

export const searchProducts = (q: string, page = 0) =>
  client
    .get<ApiResponse<PagedResponse<Product>>>('/catalog/products/search', {
      params: { q, page },
    })
    .then((r) => r.data.data)

export const listCategories = () =>
  client
    .get<ApiResponse<Category[]>>('/catalog/categories')
    .then((r) => r.data.data)
