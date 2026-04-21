import { useEffect, useState } from 'react'
import { cartApi } from '../api/client'
import { useAuth } from '../context/AuthContext.jsx'

function CartPage({ navigate }) {
  const { user } = useAuth()
  const [cart, setCart] = useState({ items: [], totalAmount: 0 })
  const [loading, setLoading] = useState(true)
  const [actionLoading, setActionLoading] = useState(false)
  const [error, setError] = useState('')
  const [successMessage, setSuccessMessage] = useState('')

  useEffect(() => {
    const loadCartPage = async () => {
      if (!user?.id) {
        setLoading(false)
        return
      }

      try {
        const cartData = await cartApi.get(user.id)
        setCart(
          cartData && Array.isArray(cartData.items)
            ? cartData
            : { items: [], totalAmount: 0 },
        )
      } catch (err) {
        setError(err.message || 'Failed to load cart')
      } finally {
        setLoading(false)
      }
    }

    loadCartPage()
  }, [user?.id])

  const onIncrease = async (item) => {
    setActionLoading(true)
    setError('')
    setSuccessMessage('')
    try {
      const updated = await cartApi.update({
        userId: user.id,
        productId: item.productId,
        quantity: item.quantity + 1,
      })
      setCart(updated)
    } catch (err) {
      setError(err.message || 'Unable to update quantity')
    } finally {
      setActionLoading(false)
    }
  }

  const onDecrease = async (item) => {
    setActionLoading(true)
    setError('')
    setSuccessMessage('')
    try {
      if (item.quantity === 1) {
        const updated = await cartApi.remove(user.id, item.productId)
        setCart(updated)
      } else {
        const updated = await cartApi.update({
          userId: user.id,
          productId: item.productId,
          quantity: item.quantity - 1,
        })
        setCart(updated)
      }
    } catch (err) {
      setError(err.message || 'Unable to update quantity')
    } finally {
      setActionLoading(false)
    }
  }

  const onBuyNow = async () => {
    setActionLoading(true)
    setError('')
    setSuccessMessage('')
    try {
      const order = await cartApi.buy(user.id)
      setSuccessMessage(
        `${order.message}. Order #${order.orderId} placed for Rs. ${order.totalAmount}`,
      )
      const cartData = await cartApi.get(user.id)
      setCart(cartData)
    } catch (err) {
      setError(err.message || 'Unable to place order')
    } finally {
      setActionLoading(false)
    }
  }

  if (loading) {
    return <p>Loading cart...</p>
  }

  return (
    <section className="cart-page">
      <div className="cart-page-header">
        <h2 className="section-title">My Cart</h2>
        <button className="nav-btn secondary" onClick={() => navigate('/home')}>
          Shop More
        </button>
      </div>
      {error && <p className="form-error cart-status">{error}</p>}
      {successMessage && <p className="form-success cart-status">{successMessage}</p>}
      {successMessage && (
        <button className="order-history-btn" onClick={() => navigate('/orders')}>
          View Order History
        </button>
      )}

      <section className="cart-panel">
        {cart.items.length === 0 ? (
          <div className="cart-empty">
            <p>Your cart is empty.</p>
            <button className="primary-btn" onClick={() => navigate('/home')}>
              Shop More
            </button>
          </div>
        ) : (
          <>
            <div className="cart-items">
              {cart.items.map((item) => (
                <div className="cart-row" key={item.productId}>
                  <span className="cart-item-name">{item.productName}</span>
                  <div className="qty-controls">
                    <button
                      className="qty-btn"
                      onClick={() => onDecrease(item)}
                      disabled={actionLoading}
                    >
                      -
                    </button>
                    <span>{item.quantity}</span>
                    <button
                      className="qty-btn"
                      onClick={() => onIncrease(item)}
                      disabled={actionLoading}
                    >
                      +
                    </button>
                  </div>
                  <span>Rs. {item.lineTotal}</span>
                </div>
              ))}
            </div>
            <div className="cart-footer">
              <strong>Total: Rs. {cart.totalAmount}</strong>
              <button
                className="primary-btn"
                onClick={onBuyNow}
                disabled={actionLoading || cart.items.length === 0}
              >
                {actionLoading ? 'Processing...' : 'Buy Now'}
              </button>
            </div>
          </>
        )}
      </section>

    </section>
  )
}

export default CartPage
