import { useEffect, useState } from 'react'
import { adminApi } from '../api/client'

const ORDER_STATUSES = ['PLACED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED']
const SORT_OPTIONS = [
  { label: 'Name: A-Z', value: 'name:asc' },
  { label: 'Name: Z-A', value: 'name:desc' },
  { label: 'Price: Low to High', value: 'price:asc' },
  { label: 'Price: High to Low', value: 'price:desc' },
]
const FALLBACK_IMAGE = '/product-placeholder.svg'
const buildLocalPool = (folder, filePrefix) =>
  Array.from({ length: 20 }, (_, index) => `/catalog/${folder}/${filePrefix}-${index + 1}.jpg`)

const CATEGORY_IMAGE_POOLS = {
  'Ladies Wear': buildLocalPool('ladies-wear', 'ladies-wear'),
  'Gents Wear': buildLocalPool('gents-wear', 'gents-wear'),
  'Kids Wear': buildLocalPool('kids-wear', 'kids-wear'),
  'Mobile Phone': buildLocalPool('mobile', 'mobile'),
  Laptop: buildLocalPool('laptop', 'laptop'),
  Furniture: buildLocalPool('furniture', 'furniture'),
  Carpet: buildLocalPool('carpet', 'carpet'),
}

const normalizeCategory = (value) => (value || '').trim().toLowerCase().replace(/[_-]+/g, ' ')

const EMPTY_PRODUCT = {
  name: '',
  description: '',
  price: '',
  imageUrl: '',
  stock: 0,
  category: '',
  brand: '',
}

const toProductForm = (product) => ({
  name: product.name || '',
  description: product.description || '',
  price: product.price || '',
  imageUrl: product.imageUrl || '',
  stock: product.stock ?? 0,
  category: product.category || '',
  brand: product.brand || '',
})

function AdminPage() {
  const [products, setProducts] = useState([])
  const [orders, setOrders] = useState([])
  const [categories, setCategories] = useState([])
  const [users, setUsers] = useState([])
  const [productForm, setProductForm] = useState(EMPTY_PRODUCT)
  const [editingProductId, setEditingProductId] = useState(null)
  const [editForm, setEditForm] = useState(EMPTY_PRODUCT)
  const [newCategoryName, setNewCategoryName] = useState('')
  const [renameCategory, setRenameCategory] = useState({ currentCategory: '', newCategory: '' })
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [pendingDeleteProduct, setPendingDeleteProduct] = useState(null)
  const [userSearch, setUserSearch] = useState('')
  const [roleUpdatingUserId, setRoleUpdatingUserId] = useState(null)
  const [productSearch, setProductSearch] = useState('')
  const [productFilterCategory, setProductFilterCategory] = useState('')
  const [productFilterBrand, setProductFilterBrand] = useState('')
  const [productSort, setProductSort] = useState('name:asc')
  const brandOptions = Array.from(
    new Set(
      products
        .map((product) => (product?.brand || '').trim())
        .filter((brand) => brand.length > 0),
    ),
  ).sort((a, b) => a.localeCompare(b))

  const categoryOptions = Array.from(
    new Set(
      [...categories, productForm.category]
        .map((category) => (category || '').trim())
        .filter((category) => category.length > 0),
    ),
  ).sort((a, b) => a.localeCompare(b))
  const filteredProducts = products
    .filter((product) => {
      const search = productSearch.trim().toLowerCase()
      const name = (product?.name || '').toLowerCase()
      const category = (product?.category || '').toLowerCase()
      const brand = (product?.brand || '').toLowerCase()

      const matchesSearch =
        !search ||
        name.includes(search) ||
        category.includes(search) ||
        brand.includes(search)
      const matchesCategory =
        !productFilterCategory || (product?.category || '') === productFilterCategory
      const matchesBrand = !productFilterBrand || (product?.brand || '') === productFilterBrand

      return matchesSearch && matchesCategory && matchesBrand
    })
    .sort((a, b) => {
      const [sortBy, sortDir] = productSort.split(':')
      const direction = sortDir === 'desc' ? -1 : 1
      if (sortBy === 'price') {
        const aPrice = Number(a?.price || 0)
        const bPrice = Number(b?.price || 0)
        return (aPrice - bPrice) * direction
      }
      const aName = (a?.name || '').toLowerCase()
      const bName = (b?.name || '').toLowerCase()
      return aName.localeCompare(bName) * direction
    })

  const loadAdminData = async () => {
    setError('')
    try {
      const [productData, orderData, categoryData, userData] = await Promise.all([
        adminApi.listProducts(),
        adminApi.listOrders(),
        adminApi.listCategories(),
        adminApi.listUsers(),
      ])
      setProducts(Array.isArray(productData) ? productData : [])
      setOrders(Array.isArray(orderData) ? orderData : [])
      setCategories(Array.isArray(categoryData) ? categoryData : [])
      setUsers(Array.isArray(userData) ? userData : [])
    } catch (err) {
      setError(err.message || 'Failed to load admin data. Admin access required.')
    }
  }

  useEffect(() => {
    loadAdminData()
  }, [])

  const submitProduct = async (event) => {
    event.preventDefault()
    setError('')
    setSuccess('')
    try {
      const payload = {
        ...productForm,
        price: Number(productForm.price),
        stock: Number(productForm.stock),
      }
      await adminApi.createProduct(payload)
      setSuccess('Product created')
      setProductForm(EMPTY_PRODUCT)
      await loadAdminData()
    } catch (err) {
      setError(err.message || 'Unable to save product')
    }
  }

  const onEditProduct = (product) => {
    setEditingProductId(product.id)
    setEditForm(toProductForm(product))
  }

  const closeEditModal = () => {
    setEditingProductId(null)
    setEditForm(EMPTY_PRODUCT)
  }

  const submitEditProduct = async (event) => {
    event.preventDefault()
    if (!editingProductId) {
      return
    }
    setError('')
    setSuccess('')
    try {
      const payload = {
        ...editForm,
        price: Number(editForm.price),
        stock: Number(editForm.stock),
      }
      await adminApi.updateProduct(editingProductId, payload)
      setSuccess('Product updated')
      closeEditModal()
      await loadAdminData()
    } catch (err) {
      if (err?.status === 404) {
        closeEditModal()
        await loadAdminData()
        setError('Product not found. Select a product again and retry.')
        return
      }
      setError(err.message || 'Unable to update product')
    }
  }

  const onDeleteProduct = async (productId) => {
    setError('')
    try {
      await adminApi.deleteProduct(productId)
      if (editingProductId === productId) {
        setEditingProductId(null)
        setProductForm(EMPTY_PRODUCT)
      }
      setSuccess('Product deleted')
      await loadAdminData()
    } catch (err) {
      setError(err.message || 'Unable to delete product')
    }
  }

  const openDeleteConfirm = (product) => {
    setPendingDeleteProduct(product)
  }

  const closeDeleteConfirm = () => {
    setPendingDeleteProduct(null)
  }

  const confirmDeleteProduct = async () => {
    if (!pendingDeleteProduct?.id) {
      return
    }
    await onDeleteProduct(pendingDeleteProduct.id)
    closeDeleteConfirm()
  }

  const submitRenameCategory = async (event) => {
    event.preventDefault()
    setError('')
    try {
      const response = await adminApi.renameCategory(renameCategory.currentCategory, renameCategory.newCategory)
      setSuccess(response?.message || 'Category renamed')
      setRenameCategory({ currentCategory: '', newCategory: '' })
      await loadAdminData()
    } catch (err) {
      setError(err.message || 'Unable to rename category')
    }
  }

  const submitAddCategory = async (event) => {
    event.preventDefault()
    setError('')
    try {
      const response = await adminApi.addCategory(newCategoryName)
      setSuccess(response?.message || 'Category added')
      setNewCategoryName('')
      await loadAdminData()
    } catch (err) {
      setError(err.message || 'Unable to add category')
    }
  }

  const onOrderStatusChange = async (orderId, status) => {
    setError('')
    try {
      await adminApi.updateOrderStatus(orderId, status)
      setSuccess(`Order #${orderId} status updated`)
      await loadAdminData()
    } catch (err) {
      setError(err.message || 'Unable to update order status')
    }
  }

  const onUserRoleChange = async (userId, role) => {
    setError('')
    try {
      setRoleUpdatingUserId(userId)
      await adminApi.updateUserRole(userId, role)
      setSuccess(`User #${userId} role updated to ${role}`)
      await loadAdminData()
    } catch (err) {
      setError(err.message || 'Unable to update user role')
    } finally {
      setRoleUpdatingUserId(null)
    }
  }

  const visibleUsers = users.filter((user) => {
    const search = userSearch.trim().toLowerCase()
    if (!search) {
      return true
    }
    return (
      String(user.userId).includes(search) ||
      (user.name || '').toLowerCase().includes(search) ||
      (user.email || '').toLowerCase().includes(search)
    )
  })

  const getStaticCategoryImage = (product) => {
    if (!product) {
      return FALLBACK_IMAGE
    }
    const normalizedCategory = normalizeCategory(product.category)
    const matchedEntry = Object.entries(CATEGORY_IMAGE_POOLS).find(
      ([categoryName]) => normalizeCategory(categoryName) === normalizedCategory,
    )
    const imagePool = matchedEntry ? matchedEntry[1] : null
    if (Array.isArray(imagePool) && imagePool.length > 0) {
      const numericId = Number(product.id || 0)
      const poolIndex = Math.abs(numericId) % imagePool.length
      return imagePool[poolIndex]
    }
    return FALLBACK_IMAGE
  }

  const getPrimaryProductImage = (product) => {
    const remote = (product?.imageUrl || '').trim()
    if (remote) {
      return remote
    }
    return getStaticCategoryImage(product)
  }

  return (
    <section className="orders-page">
      <h2 className="section-title">Admin Panel</h2>
      {error && <p className="form-error">{error}</p>}
      {success && <p className="form-success">{success}</p>}

      <section className="orders-panel">
        <h3>Create Product</h3>
        <form className="account-form" onSubmit={submitProduct}>
          <input placeholder="Name" value={productForm.name} onChange={(e) => setProductForm((p) => ({ ...p, name: e.target.value }))} required />
          <input placeholder="Description" value={productForm.description} onChange={(e) => setProductForm((p) => ({ ...p, description: e.target.value }))} required />
          <input type="number" step="0.01" placeholder="Price" value={productForm.price} onChange={(e) => setProductForm((p) => ({ ...p, price: e.target.value }))} required />
          <input type="number" min="0" placeholder="Stock" value={productForm.stock} onChange={(e) => setProductForm((p) => ({ ...p, stock: e.target.value }))} required />
          <input placeholder="Image URL" value={productForm.imageUrl} onChange={(e) => setProductForm((p) => ({ ...p, imageUrl: e.target.value }))} required />
          <select
            value={productForm.category}
            onChange={(e) => setProductForm((p) => ({ ...p, category: e.target.value }))}
          >
            <option value="">Select category</option>
            {categoryOptions.map((category) => (
              <option key={category} value={category}>
                {category}
              </option>
            ))}
          </select>
          <select
            value={productForm.brand}
            onChange={(e) => setProductForm((p) => ({ ...p, brand: e.target.value }))}
          >
            <option value="">Select brand</option>
            {brandOptions.map((brand) => (
              <option key={brand} value={brand}>
                {brand}
              </option>
            ))}
          </select>
          <div className="account-actions">
            <button className="primary-btn" type="submit">Create Product</button>
          </div>
        </form>
      </section>

      <section className="orders-panel">
        <h3>Category Management</h3>
        <p>Existing categories: {categories.join(', ') || 'None'}</p>
        <form className="filters-row" onSubmit={submitAddCategory}>
          <input
            placeholder="New category name"
            value={newCategoryName}
            onChange={(e) => setNewCategoryName(e.target.value)}
            required
          />
          <button className="primary-btn" type="submit">Add Category</button>
        </form>
        <form className="filters-row" onSubmit={submitRenameCategory}>
          <input
            placeholder="Current category"
            value={renameCategory.currentCategory}
            onChange={(e) => setRenameCategory((p) => ({ ...p, currentCategory: e.target.value }))}
            required
          />
          <input
            placeholder="New category"
            value={renameCategory.newCategory}
            onChange={(e) => setRenameCategory((p) => ({ ...p, newCategory: e.target.value }))}
            required
          />
          <button className="primary-btn" type="submit">Rename</button>
        </form>
      </section>

      <section className="orders-panel">
        <h3>User Role Management</h3>
        <div className="filters-row">
          <input
            placeholder="Search by userId, name or email"
            value={userSearch}
            onChange={(e) => setUserSearch(e.target.value)}
          />
        </div>
        <div className="admin-users-list">
          {visibleUsers.map((user) => (
            <article className="admin-user-card" key={user.userId}>
              <div className="admin-user-head">
                <h4>{user.name || 'Unnamed User'}</h4>
                <span className="admin-user-role">{user.role}</span>
              </div>
              <p><strong>User ID:</strong> {user.userId}</p>
              <p><strong>Email:</strong> {user.email}</p>
              <div className="admin-user-actions">
                <button
                  className="admin-action-btn user"
                  onClick={() => onUserRoleChange(user.userId, 'USER')}
                  disabled={user.role === 'USER' || roleUpdatingUserId === user.userId}
                >
                  {roleUpdatingUserId === user.userId ? 'Updating...' : 'Set USER'}
                </button>
                <button
                  className="admin-action-btn admin"
                  onClick={() => onUserRoleChange(user.userId, 'ADMIN')}
                  disabled={user.role === 'ADMIN' || roleUpdatingUserId === user.userId}
                >
                  {roleUpdatingUserId === user.userId ? 'Updating...' : 'Set ADMIN'}
                </button>
              </div>
            </article>
          ))}
        </div>
        {visibleUsers.length === 0 && <p>No users found.</p>}
      </section>

      <section className="orders-panel">
        <h3>Products</h3>
        <div className="filters-row">
          <input
            placeholder="Search products"
            value={productSearch}
            onChange={(e) => setProductSearch(e.target.value)}
          />
          <select
            value={productFilterCategory}
            onChange={(e) => setProductFilterCategory(e.target.value)}
          >
            <option value="">All categories</option>
            {categoryOptions.map((category) => (
              <option key={category} value={category}>
                {category}
              </option>
            ))}
          </select>
          <select
            value={productFilterBrand}
            onChange={(e) => setProductFilterBrand(e.target.value)}
          >
            <option value="">All brands</option>
            {brandOptions.map((brand) => (
              <option key={brand} value={brand}>
                {brand}
              </option>
            ))}
          </select>
          <select value={productSort} onChange={(e) => setProductSort(e.target.value)}>
            {SORT_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </div>
        <div className="orders-list">
          {filteredProducts.map((product) => (
            <article className="order-card" key={product.id}>
              <h4>{product.name}</h4>
              <div className="admin-product-image-wrap">
                <img
                  src={getPrimaryProductImage(product)}
                  alt={product.name || 'Product image'}
                  className="admin-product-image"
                  loading="lazy"
                  decoding="async"
                  onError={(event) => {
                    const phase = event.currentTarget.dataset.phase || 'primary'
                    if (phase === 'primary') {
                      event.currentTarget.dataset.phase = 'secondary'
                      event.currentTarget.src = getStaticCategoryImage(product)
                      return
                    }
                    event.currentTarget.onerror = null
                    event.currentTarget.src = FALLBACK_IMAGE
                  }}
                />
              </div>
              <p>Category: {product.category || '-'}</p>
              <p>Brand: {product.brand || '-'}</p>
              <p>Price: Rs. {product.price}</p>
              <div className="admin-product-actions">
                <button className="admin-action-btn edit" onClick={() => onEditProduct(product)}>Edit</button>
                <button className="admin-action-btn delete" onClick={() => openDeleteConfirm(product)}>Delete</button>
              </div>
            </article>
          ))}
        </div>
        {filteredProducts.length === 0 && <p>No products found for selected filters.</p>}
      </section>

      {pendingDeleteProduct && (
        <div className="account-modal-backdrop" onClick={closeDeleteConfirm}>
          <div className="account-modal" onClick={(event) => event.stopPropagation()}>
            <h2 className="section-title">Delete Product</h2>
            <p>Are you sure you want to delete <strong>{pendingDeleteProduct.name}</strong>?</p>
            <div className="account-actions">
              <button className="nav-btn secondary" type="button" onClick={closeDeleteConfirm}>
                No
              </button>
              <button className="admin-action-btn delete" type="button" onClick={confirmDeleteProduct}>
                Yes
              </button>
            </div>
          </div>
        </div>
      )}

      {editingProductId && (
        <div className="account-modal-backdrop" onClick={closeEditModal}>
          <div className="account-modal" onClick={(event) => event.stopPropagation()}>
            <div className="account-header">
              <h2 className="section-title">Edit Product</h2>
              <span className="account-email">Product ID: {editingProductId}</span>
            </div>
            <form className="account-form" onSubmit={submitEditProduct}>
              <input value={editingProductId} disabled />
              <input
                placeholder="Name"
                value={editForm.name}
                onChange={(e) => setEditForm((p) => ({ ...p, name: e.target.value }))}
                required
              />
              <input
                placeholder="Description"
                value={editForm.description}
                onChange={(e) => setEditForm((p) => ({ ...p, description: e.target.value }))}
                required
              />
              <input
                type="number"
                step="0.01"
                placeholder="Price"
                value={editForm.price}
                onChange={(e) => setEditForm((p) => ({ ...p, price: e.target.value }))}
                required
              />
              <input
                type="number"
                min="0"
                placeholder="Stock"
                value={editForm.stock}
                onChange={(e) => setEditForm((p) => ({ ...p, stock: e.target.value }))}
                required
              />
              <input
                placeholder="Image URL"
                value={editForm.imageUrl}
                onChange={(e) => setEditForm((p) => ({ ...p, imageUrl: e.target.value }))}
                required
              />
              <select
                value={editForm.category}
                onChange={(e) => setEditForm((p) => ({ ...p, category: e.target.value }))}
              >
                <option value="">Select category</option>
                {categoryOptions.map((category) => (
                  <option key={category} value={category}>
                    {category}
                  </option>
                ))}
              </select>
              <select
                value={editForm.brand}
                onChange={(e) => setEditForm((p) => ({ ...p, brand: e.target.value }))}
              >
                <option value="">Select brand</option>
                {brandOptions.map((brand) => (
                  <option key={brand} value={brand}>
                    {brand}
                  </option>
                ))}
              </select>
              <div className="account-actions">
                <button className="nav-btn secondary" type="button" onClick={closeEditModal}>
                  Cancel
                </button>
                <button className="primary-btn" type="submit">Update Product</button>
              </div>
            </form>
          </div>
        </div>
      )}

      <section className="orders-panel">
        <h3>Orders</h3>
        <div className="orders-list">
          {orders.map((order) => (
            <article className="order-card" key={order.orderId}>
              <h4>Order #{order.orderId}</h4>
              <p>User: {order.userId}</p>
              <p>Total: Rs. {order.totalAmount}</p>
              <p>Current Status: {order.status}</p>
              <div className="filters-row">
                {ORDER_STATUSES.map((status) => (
                  <button
                    key={`${order.orderId}-${status}`}
                    className="nav-btn secondary"
                    onClick={() => onOrderStatusChange(order.orderId, status)}
                    disabled={status === order.status}
                  >
                    {status}
                  </button>
                ))}
              </div>
            </article>
          ))}
        </div>
      </section>
    </section>
  )
}

export default AdminPage
