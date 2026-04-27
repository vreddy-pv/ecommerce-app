export interface Product {
  id: number
  sku: string
  name: string
  description: string
  price: number
  categoryId: number
  categoryName: string
  images: string[]
  isActive: boolean
}

export interface Category {
  id: number
  name: string
  parentId: number | null
}

export interface PagedResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  last: boolean
}

export interface ApiResponse<T> {
  success: boolean
  data: T
  errorCode?: string
  message?: string
}

export interface CartItem {
  product: Product
  quantity: number
}

export interface OrderItem {
  productId: number
  productName: string
  quantity: number
  unitPrice: number
}

export interface Order {
  id: number
  status: 'PENDING' | 'CONFIRMED' | 'PROCESSING' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED'
  items: OrderItem[]
  totalAmount: number
  createdAt: string
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  username: string
  role: string
}

export interface StockStatus {
  productId: number
  available: number
  reserved: number
}
