import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useCart } from '../context/CartContext'
import { ThemeSwitcher } from '../context/ThemeContext'

export default function Navbar() {
  const { isAuthenticated, user, logout } = useAuth()
  const { totalItems } = useCart()

  // Keycloak logout redirects the browser to Keycloak and then back to the app root.
  const handleLogout = () => logout()

  return (
    <nav style={{ display: 'flex', alignItems: 'center', padding: '12px 24px', background: '#1a73e8', color: '#fff', gap: 16 }}>
      <Link to="/" style={{ color: '#fff', textDecoration: 'none', fontWeight: 'bold', fontSize: 18 }}>
        ShopFast
      </Link>
      <div style={{ flex: 1 }} />
      <Link to="/products" style={{ color: '#fff', textDecoration: 'none' }}>Products</Link>
      {isAuthenticated && (
        <Link to="/orders" style={{ color: '#fff', textDecoration: 'none' }}>My Orders</Link>
      )}
      <Link to="/cart" style={{ color: '#fff', textDecoration: 'none' }}>
        Cart {totalItems > 0 && <span data-testid="cart-count">({totalItems})</span>}
      </Link>
      <ThemeSwitcher />
      {isAuthenticated ? (
        <>
          <span style={{ fontSize: 14 }}>Hi, {user?.username}</span>
          <button onClick={handleLogout} style={{ background: 'transparent', border: '1px solid #fff', color: '#fff', borderRadius: 4, padding: '4px 12px', cursor: 'pointer' }}>
            Logout
          </button>
        </>
      ) : (
        <Link to="/login" style={{ color: '#fff', textDecoration: 'none' }}>Login</Link>
      )}
    </nav>
  )
}
