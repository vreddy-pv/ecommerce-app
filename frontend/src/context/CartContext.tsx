import { createContext, useContext, useReducer, type ReactNode } from 'react'
import type { CartItem, Product } from '../types'

type CartAction =
  | { type: 'ADD'; product: Product }
  | { type: 'REMOVE'; productId: number }
  | { type: 'UPDATE_QTY'; productId: number; quantity: number }
  | { type: 'CLEAR' }

interface CartContextValue {
  items: CartItem[]
  totalItems: number
  totalPrice: number
  addItem: (product: Product) => void
  removeItem: (productId: number) => void
  updateQuantity: (productId: number, quantity: number) => void
  clearCart: () => void
}

function cartReducer(state: CartItem[], action: CartAction): CartItem[] {
  switch (action.type) {
    case 'ADD': {
      const existing = state.find((i) => i.product.id === action.product.id)
      if (existing)
        return state.map((i) =>
          i.product.id === action.product.id ? { ...i, quantity: i.quantity + 1 } : i
        )
      return [...state, { product: action.product, quantity: 1 }]
    }
    case 'REMOVE':
      return state.filter((i) => i.product.id !== action.productId)
    case 'UPDATE_QTY':
      return state.map((i) =>
        i.product.id === action.productId ? { ...i, quantity: action.quantity } : i
      )
    case 'CLEAR':
      return []
    default:
      return state
  }
}

const CartContext = createContext<CartContextValue | null>(null)

export function CartProvider({ children }: { children: ReactNode }) {
  const [items, dispatch] = useReducer(cartReducer, [])

  const totalItems = items.reduce((s, i) => s + i.quantity, 0)
  const totalPrice = items.reduce((s, i) => s + i.product.price * i.quantity, 0)

  return (
    <CartContext.Provider
      value={{
        items,
        totalItems,
        totalPrice,
        addItem: (p) => dispatch({ type: 'ADD', product: p }),
        removeItem: (id) => dispatch({ type: 'REMOVE', productId: id }),
        updateQuantity: (id, qty) => dispatch({ type: 'UPDATE_QTY', productId: id, quantity: qty }),
        clearCart: () => dispatch({ type: 'CLEAR' }),
      }}
    >
      {children}
    </CartContext.Provider>
  )
}

export function useCart() {
  const ctx = useContext(CartContext)
  if (!ctx) throw new Error('useCart must be used within CartProvider')
  return ctx
}
