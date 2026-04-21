import { useEffect, useState } from 'react'
import { orderApi } from '../api/client'
import { useAuth } from '../context/AuthContext.jsx'

const FALLBACK_IMAGE = '/product-placeholder.svg'
const MOBILE_IMAGE_POOL = Array.from(
  { length: 20 },
  (_, index) => `/catalog/mobile/mobile-${index + 1}.jpg`,
)

const CATEGORY_IMAGE_MAP = {
  carpet: '/catalog/carpet.jpg',
  furniture: '/catalog/furniture.jpg',
  laptop: '/catalog/laptop.jpg',
  mobile: '/catalog/mobile-phone.jpg',
  ladies: '/catalog/ladies-wear.jpg',
  gents: '/catalog/gents-wear.jpg',
  kids: '/catalog/kids-wear.jpg',
}

const detectCategoryFromName = (name = '') => {
  const value = name.toLowerCase()
  if (value.includes('phone') || value.includes('mobile')) return 'mobile'
  if (value.includes('laptop')) return 'laptop'
  if (value.includes('carpet') || value.includes('rug')) return 'carpet'
  if (value.includes('sofa') || value.includes('chair') || value.includes('table') || value.includes('furniture')) return 'furniture'
  if (value.includes('kids') || value.includes('baby')) return 'kids'
  if (value.includes('gents') || value.includes('men') || value.includes('shirt') || value.includes('jeans')) return 'gents'
  if (value.includes('ladies') || value.includes('women') || value.includes('dress') || value.includes('kurti') || value.includes('saree')) return 'ladies'
  return ''
}

const stableHash = (text = '') => {
  let hash = 0
  for (let i = 0; i < text.length; i += 1) {
    hash = (hash * 31 + text.charCodeAt(i)) >>> 0
  }
  return hash
}

const isSlowOrBrokenRemoteImage = (url = '') =>
  url.includes('via.placeholder.com') ||
  url.includes('source.unsplash.com') ||
  url.includes('loremflickr.com')

function OrdersPage({ navigate }) {
  const { user } = useAuth()
  const [orders, setOrders] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [invoiceMessage, setInvoiceMessage] = useState('')
  const [invoiceError, setInvoiceError] = useState('')
  const [invoiceInProgress, setInvoiceInProgress] = useState(null)
  const [expandedOrderId, setExpandedOrderId] = useState(null)
  const [selectedItem, setSelectedItem] = useState(null)

  useEffect(() => {
    const loadOrders = async () => {
      if (!user?.id) {
        setLoading(false)
        return
      }

      try {
        const ordersData = await orderApi.list(user.id)
        setOrders(Array.isArray(ordersData) ? ordersData : [])
      } catch (err) {
        setError(err.message || 'Failed to load orders')
      } finally {
        setLoading(false)
      }
    }

    loadOrders()
  }, [user?.id])

  if (loading) {
    return <p>Loading order history...</p>
  }

  const getOrderItemImage = (item) => {
    const category = detectCategoryFromName(item?.productName || '')
    if (category === 'mobile') {
      const index = stableHash(item?.productName || '') % MOBILE_IMAGE_POOL.length
      return MOBILE_IMAGE_POOL[index]
    }

    if (CATEGORY_IMAGE_MAP[category]) {
      return CATEGORY_IMAGE_MAP[category]
    }

    if (item?.imageUrl && item.imageUrl.trim() && !isSlowOrBrokenRemoteImage(item.imageUrl)) {
      return item.imageUrl
    }

    return FALLBACK_IMAGE
  }

  const onDownloadInvoice = async (event, orderId) => {
    event.stopPropagation()
    if (!user?.id) {
      return
    }
    if (invoiceInProgress) {
      return
    }
    setInvoiceMessage('')
    setInvoiceError('')
    setInvoiceInProgress({ orderId, action: 'download' })
    try {
      const blob = await orderApi.downloadInvoice(orderId, user.id)
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `invoice-order-${orderId}.pdf`
      document.body.appendChild(link)
      link.click()
      link.remove()
      window.URL.revokeObjectURL(url)
      setInvoiceMessage(`Invoice downloaded for Order #${orderId}.`)
    } catch (err) {
      setInvoiceError(err.message || 'Unable to download invoice.')
    } finally {
      setInvoiceInProgress(null)
    }
  }

  const onEmailInvoice = async (event, orderId) => {
    event.stopPropagation()
    if (!user?.id) {
      return
    }
    if (invoiceInProgress) {
      return
    }
    setInvoiceMessage('')
    setInvoiceError('')
    setInvoiceInProgress({ orderId, action: 'email' })
    try {
      const response = await orderApi.emailInvoice(orderId, user.id)
      setInvoiceMessage(response?.message || `Invoice emailed for Order #${orderId}.`)
    } catch (err) {
      setInvoiceError(err.message || 'Unable to send invoice email.')
    } finally {
      setInvoiceInProgress(null)
    }
  }

  return (
    <section className="orders-page">
      <div className="cart-page-header">
        <h2 className="section-title">Order History</h2>
        <button className="nav-btn secondary" onClick={() => navigate('/home')}>
          Continue Shopping
        </button>
      </div>

      {error && <p className="form-error cart-status">{error}</p>}
      {invoiceError && <p className="form-error cart-status">{invoiceError}</p>}
      {invoiceMessage && <p className="form-success cart-status">{invoiceMessage}</p>}

      <section className="orders-panel">
        {orders.length === 0 ? (
          <p>No orders placed yet.</p>
        ) : (
          <div className="orders-list">
            {orders.map((order) => (
              <article
                className="order-card"
                key={order.orderId}
              >
                <div className="order-top">
                  <h3>Order #{order.orderId}</h3>
                  <span className="order-badge">{order.items.length} item(s)</span>
                </div>
                <p>Total: Rs. {order.totalAmount}</p>
                <button
                  className="view-items-btn"
                  onClick={() =>
                    setExpandedOrderId((prev) => (prev === order.orderId ? null : order.orderId))
                  }
                >
                  {expandedOrderId === order.orderId ? 'Hide Items' : 'View Items'}
                </button>
                <div className="order-invoice-actions">
                  <button
                    className="invoice-action-btn"
                    onClick={(event) => onDownloadInvoice(event, order.orderId)}
                    disabled={invoiceInProgress?.orderId === order.orderId && invoiceInProgress?.action === 'download'}
                  >
                    {invoiceInProgress?.orderId === order.orderId && invoiceInProgress?.action === 'download'
                      ? 'Downloading...'
                      : 'Download Invoice'}
                  </button>
                  <button
                    className="invoice-action-btn secondary"
                    onClick={(event) => onEmailInvoice(event, order.orderId)}
                    disabled={invoiceInProgress?.orderId === order.orderId && invoiceInProgress?.action === 'email'}
                  >
                    {invoiceInProgress?.orderId === order.orderId && invoiceInProgress?.action === 'email'
                      ? 'Sending...'
                      : 'Email Invoice'}
                  </button>
                </div>

                {expandedOrderId === order.orderId && (
                  <div className="order-items-grid">
                    {order.items.map((item, index) => (
                      <button
                        type="button"
                        className="order-item-card"
                        key={`${order.orderId}-${item.productName}-${index}`}
                        onClick={() => setSelectedItem(item)}
                      >
                        <img
                          src={getOrderItemImage(item)}
                          alt={item.productName}
                          className="order-item-thumb"
                          loading="lazy"
                          decoding="async"
                          onError={(event) => {
                            event.currentTarget.onerror = null
                            event.currentTarget.src = FALLBACK_IMAGE
                          }}
                        />
                        <div className="order-item-meta">
                          <strong>{item.productName}</strong>
                          <span>{item.productDescription || 'No description available.'}</span>
                          <span>
                            Qty: {item.quantity} | Rs. {item.price}
                          </span>
                        </div>
                      </button>
                    ))}
                  </div>
                )}
              </article>
            ))}
          </div>
        )}
      </section>

      {selectedItem && (
        <div className="product-modal-backdrop" onClick={() => setSelectedItem(null)}>
          <div className="product-modal" onClick={(event) => event.stopPropagation()}>
            <button className="modal-close-btn" onClick={() => setSelectedItem(null)} aria-label="Close item details">
              x
            </button>
            <div className="product-modal-image-wrap">
              <img
                src={getOrderItemImage(selectedItem)}
                alt={selectedItem.productName || 'Product image'}
                className="product-modal-image"
                loading="eager"
                decoding="async"
                onError={(event) => {
                  event.currentTarget.onerror = null
                  event.currentTarget.src = FALLBACK_IMAGE
                }}
              />
            </div>
            <h3>{selectedItem.productName}</h3>
            <p className="product-desc">{selectedItem.productDescription || 'No description available.'}</p>
            <p className="price">Rs. {selectedItem.price}</p>
            <p className="product-modal-stock">Quantity: {selectedItem.quantity}</p>
          </div>
        </div>
      )}
    </section>
  )
}

export default OrdersPage
