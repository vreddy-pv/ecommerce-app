import { describe, it, expect } from 'vitest'
import { render, screen, waitFor } from './utils'
import OrderHistoryPage from '../pages/OrderHistoryPage'

describe('OrderHistoryPage', () => {
  it('renders order cards after loading', async () => {
    render(<OrderHistoryPage />)
    await waitFor(() => expect(screen.getByTestId('order-card')).toBeInTheDocument())
  })

  it('shows order id and status', async () => {
    render(<OrderHistoryPage />)
    await waitFor(() => {
      expect(screen.getByText('Order #101')).toBeInTheDocument()
      expect(screen.getByText('PENDING')).toBeInTheDocument()
    })
  })
})
