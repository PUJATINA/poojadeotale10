import { useEffect, useState } from 'react'
import { cartApi, productApi, userApi } from '../api/client'
import { useAuth } from '../context/AuthContext.jsx'

const FALLBACK_IMAGE = '/product-placeholder.svg'
const CATEGORY_IMAGE_MAP = {
  'Ladies Wear': '/catalog/ladies-wear.jpg',
  'Gents Wear': '/catalog/gents-wear.jpg',
  'Kids Wear': '/catalog/kids-wear.jpg',
  'Mobile Phone': '/catalog/mobile-phone.jpg',
  Laptop: '/catalog/laptop.jpg',
  Furniture: '/catalog/furniture.jpg',
  Carpet: '/catalog/carpet.jpg',
}
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

const CATEGORY_OPTIONS = [
  'Ladies Wear',
  'Gents Wear',
  'Kids Wear',
  'Mobile Phone',
  'Laptop',
  'Furniture',
  'Carpet',
]

const BRAND_OPTIONS = ['Nova', 'Aster', 'UrbanMint', 'Velora', 'Zentro']

const SORT_OPTIONS = [
  { label: 'Name: A-Z', sortBy: 'name', sortDir: 'asc' },
  { label: 'Name: Z-A', sortBy: 'name', sortDir: 'desc' },
  { label: 'Price: Low to High', sortBy: 'price', sortDir: 'asc' },
  { label: 'Price: High to Low', sortBy: 'price', sortDir: 'desc' },
]

const STATE_CITY_MAP = {
  Maharashtra: ['Mumbai', 'Pune', 'Nagpur', 'Nashik', 'Thane'],
  Karnataka: ['Bengaluru', 'Mysuru', 'Hubballi', 'Mangaluru', 'Belagavi'],
  'Tamil Nadu': ['Chennai', 'Coimbatore', 'Madurai', 'Salem', 'Tiruchirappalli'],
  Telangana: ['Hyderabad', 'Warangal', 'Nizamabad', 'Karimnagar', 'Khammam'],
  Gujarat: ['Ahmedabad', 'Surat', 'Vadodara', 'Rajkot', 'Bhavnagar'],
  Rajasthan: ['Jaipur', 'Jodhpur', 'Udaipur', 'Kota', 'Ajmer'],
  'Uttar Pradesh': ['Lucknow', 'Kanpur', 'Noida', 'Varanasi', 'Agra'],
  'Madhya Pradesh': ['Indore', 'Bhopal', 'Gwalior', 'Jabalpur', 'Ujjain'],
  'West Bengal': ['Kolkata', 'Howrah', 'Durgapur', 'Asansol', 'Siliguri'],
  Delhi: ['New Delhi', 'Dwarka', 'Rohini', 'Saket', 'Karol Bagh'],
}

function HomePage({ navigate }) {
  const { user } = useAuth()
  const [products, setProducts] = useState([])
  const [cart, setCart] = useState({ items: [], totalAmount: 0 })
  const [currentPage, setCurrentPage] = useState(0)
  const [totalPages, setTotalPages] = useState(1)
  const [totalItems, setTotalItems] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [actionLoading, setActionLoading] = useState(false)
  const [successMessage, setSuccessMessage] = useState('')
  const [selectedCategory, setSelectedCategory] = useState('')
  const [selectedBrand, setSelectedBrand] = useState('')
  const [selectedSort, setSelectedSort] = useState('name:asc')
  const [searchInput, setSearchInput] = useState('')
  const [searchQuery, setSearchQuery] = useState('')
  const [selectedProduct, setSelectedProduct] = useState(null)
  const [profile, setProfile] = useState(null)
  const [profileForm, setProfileForm] = useState({
    name: '',
    phoneNumber: '',
    alternatePhoneNumber: '',
    age: '',
    gender: '',
    addressLine1: '',
    addressLine2: '',
    city: '',
    state: '',
    pincode: '',
    occupation: '',
  })
  const [profileLoading, setProfileLoading] = useState(false)
  const [profileSaving, setProfileSaving] = useState(false)
  const [profileError, setProfileError] = useState('')
  const [profileSuccess, setProfileSuccess] = useState('')
  const [showAccountModal, setShowAccountModal] = useState(false)

  useEffect(() => {
    const timer = setTimeout(() => {
      setSearchQuery(searchInput.trim())
      setCurrentPage(0)
    }, 400)
    return () => clearTimeout(timer)
  }, [searchInput])

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true)
      try {
        const [sortBy, sortDir] = selectedSort.split(':')
        const [productsData, cartData] = await Promise.all([
          productApi.list({
            category: selectedCategory,
            brand: selectedBrand,
            q: searchQuery,
            sortBy,
            sortDir,
            page: currentPage,
            size: 20,
          }),
          user?.id ? cartApi.get(user.id) : Promise.resolve({ items: [], totalAmount: 0 }),
        ])
        setProducts(Array.isArray(productsData?.items) ? productsData.items : [])
        setCurrentPage(Number.isInteger(productsData?.currentPage) ? productsData.currentPage : 0)
        setTotalPages(Number.isInteger(productsData?.totalPages) ? productsData.totalPages : 1)
        setTotalItems(Number.isInteger(productsData?.totalItems) ? productsData.totalItems : 0)
        setCart(
          cartData && Array.isArray(cartData.items)
            ? cartData
            : { items: [], totalAmount: 0 },
        )
      } catch (err) {
        setError(err.message || 'Failed to load products')
      } finally {
        setLoading(false)
      }
    }

    fetchData()
  }, [user?.id, selectedCategory, selectedBrand, selectedSort, searchQuery, currentPage])

  useEffect(() => {
    const fetchProfile = async () => {
      if (!user?.id) {
        return
      }

      setProfileLoading(true)
      setProfileError('')
      try {
        const profileData = await userApi.getProfile(user.id)
        setProfile(profileData)
        setProfileForm({
          name: profileData?.name || '',
          phoneNumber: profileData?.phoneNumber || '',
          alternatePhoneNumber: profileData?.alternatePhoneNumber || '',
          age: profileData?.age ? String(profileData.age) : '',
          gender: profileData?.gender || '',
          addressLine1: profileData?.addressLine1 || '',
          addressLine2: profileData?.addressLine2 || '',
          city: profileData?.city || '',
          state: profileData?.state || '',
          pincode: profileData?.pincode || '',
          occupation: profileData?.occupation || '',
        })
      } catch (err) {
        setProfileError(err.message || 'Unable to load profile')
      } finally {
        setProfileLoading(false)
      }
    }

    fetchProfile()
  }, [user?.id])

  useEffect(() => {
    const openAccountModal = () => setShowAccountModal(true)
    window.addEventListener('open-account-modal', openAccountModal)
    return () => window.removeEventListener('open-account-modal', openAccountModal)
  }, [])

  const onCategoryChange = (value) => {
    setSelectedCategory(value)
    setCurrentPage(0)
  }

  const onBrandChange = (value) => {
    setSelectedBrand(value)
    setCurrentPage(0)
  }

  const onSortChange = (value) => {
    setSelectedSort(value)
    setCurrentPage(0)
  }

  const onProfileFieldChange = (event) => {
    const { name, value } = event.target
    setProfileForm((prev) => {
      if (name === 'state') {
        return { ...prev, state: value, city: '' }
      }
      return { ...prev, [name]: value }
    })
  }

  const onSaveProfile = async (event) => {
    event.preventDefault()
    if (!user?.id) {
      return
    }

    setProfileSaving(true)
    setProfileError('')
    setProfileSuccess('')

    try {
      const payload = {
        name: profileForm.name.trim(),
        phoneNumber: profileForm.phoneNumber.trim() || null,
        alternatePhoneNumber: profileForm.alternatePhoneNumber.trim() || null,
        age: profileForm.age ? Number(profileForm.age) : null,
        gender: profileForm.gender || null,
        addressLine1: profileForm.addressLine1.trim() || null,
        addressLine2: profileForm.addressLine2.trim() || null,
        city: profileForm.city.trim() || null,
        state: profileForm.state.trim() || null,
        pincode: profileForm.pincode.trim() || null,
        occupation: profileForm.occupation.trim() || null,
      }

      const updated = await userApi.updateProfile(user.id, payload)
      setProfile(updated)
      setProfileSuccess('Account details updated successfully.')
      setShowAccountModal(false)
    } catch (err) {
      setProfileError(err.message || 'Unable to update profile')
    } finally {
      setProfileSaving(false)
    }
  }

  const cityOptions = profileForm.state ? STATE_CITY_MAP[profileForm.state] || [] : []

  const onAddToCart = async (productId) => {
    if (!user?.id) {
      setError('Please login again to continue.')
      return
    }

    setActionLoading(true)
    setError('')
    setSuccessMessage('')
    try {
      const cartResponse = await cartApi.add({
        userId: user.id,
        productId,
        quantity: 1,
      })
      const itemCount = Array.isArray(cartResponse.items)
        ? cartResponse.items.reduce((sum, item) => sum + item.quantity, 0)
        : 0
      setCart(cartResponse)
      setSuccessMessage('Item added to cart')
      if (itemCount > 0) {
        setSuccessMessage(`Item added to cart. Cart has ${itemCount} item(s).`)
      }
    } catch (err) {
      setError(err.message || 'Unable to add item')
    } finally {
      setActionLoading(false)
    }
  }

  const onIncrease = async (productId) => {
    const existingItem = cart.items.find((item) => item.productId === productId)
    if (!existingItem || !user?.id) {
      return
    }

    setActionLoading(true)
    setError('')
    setSuccessMessage('')
    try {
      const updated = await cartApi.update({
        userId: user.id,
        productId,
        quantity: existingItem.quantity + 1,
      })
      setCart(updated)
    } catch (err) {
      setError(err.message || 'Unable to update quantity')
    } finally {
      setActionLoading(false)
    }
  }

  const onDecrease = async (productId) => {
    const existingItem = cart.items.find((item) => item.productId === productId)
    if (!existingItem || !user?.id) {
      return
    }

    setActionLoading(true)
    setError('')
    setSuccessMessage('')
    try {
      const updated =
        existingItem.quantity === 1
          ? await cartApi.remove(user.id, productId)
          : await cartApi.update({
              userId: user.id,
              productId,
              quantity: existingItem.quantity - 1,
            })
      setCart(updated)
    } catch (err) {
      setError(err.message || 'Unable to update quantity')
    } finally {
      setActionLoading(false)
    }
  }

  const getCartQuantity = (productId) => {
    const item = cart.items.find((cartItem) => cartItem.productId === productId)
    return item ? item.quantity : 0
  }

  const getProductImage = (product) => {
    if (!product) {
      return FALLBACK_IMAGE
    }

    const imagePool = CATEGORY_IMAGE_POOLS[product.category]
    if (Array.isArray(imagePool) && imagePool.length > 0) {
      const numericId = Number(product.id || 0)
      const poolIndex = Math.abs(numericId) % imagePool.length
      return imagePool[poolIndex]
    }

    return CATEGORY_IMAGE_MAP[product.category] || FALLBACK_IMAGE
  }

  return (
    <section>
      <div className="hero">
        <h1>ShopSphere Online Store</h1>
        <p>Discover fashion, electronics, and home essentials with a smooth shopping experience.</p>
      </div>

      <div className="home-actions">
        <button className="primary-btn open-cart-btn" onClick={() => navigate('/cart')}>
          Open Cart
        </button>
      </div>

      <h2 className="section-title">Products</h2>

      <div className="filters-row">
        <label className="filter-control">
          Search
          <input
            type="text"
            value={searchInput}
            onChange={(event) => setSearchInput(event.target.value)}
            placeholder="Search by product name..."
          />
        </label>

        <label className="filter-control">
          Category
          <select
            value={selectedCategory}
            onChange={(event) => onCategoryChange(event.target.value)}
          >
            <option value="">All</option>
            {CATEGORY_OPTIONS.map((category) => (
              <option key={category} value={category}>
                {category}
              </option>
            ))}
          </select>
        </label>

        <label className="filter-control">
          Brand
          <select value={selectedBrand} onChange={(event) => onBrandChange(event.target.value)}>
            <option value="">All</option>
            {BRAND_OPTIONS.map((brand) => (
              <option key={brand} value={brand}>
                {brand}
              </option>
            ))}
          </select>
        </label>

        <label className="filter-control">
          Sort
          <select value={selectedSort} onChange={(event) => onSortChange(event.target.value)}>
            {SORT_OPTIONS.map((option) => (
              <option key={`${option.sortBy}:${option.sortDir}`} value={`${option.sortBy}:${option.sortDir}`}>
                {option.label}
              </option>
            ))}
          </select>
        </label>
      </div>

      {loading && <p>Loading products...</p>}
      {error && <p className="form-error">{error}</p>}
      {successMessage && <p className="form-success">{successMessage}</p>}
      {successMessage && (
        <button className="nav-btn secondary" onClick={() => navigate('/cart')}>
          View Cart
        </button>
      )}

      {!loading && !error && products.length === 0 && (
        <p>
          No products found. Add products in backend API endpoint <code>/api/products</code>.
        </p>
      )}

      <div className="product-grid">
        {products.map((product) => (
          <article
            className="product-card product-clickable"
            key={product.id}
            onClick={() => setSelectedProduct(product)}
          >
            <div className="product-image-wrap">
              <img
                src={getProductImage(product)}
                alt={product.name || 'Product image'}
                className="product-image"
                loading="lazy"
                decoding="async"
                onError={(event) => {
                  event.currentTarget.onerror = null
                  event.currentTarget.src = FALLBACK_IMAGE
                }}
              />
            </div>
            <h3>{product.name}</h3>
            <p className="product-meta">
              {product.category || 'General'} | {product.brand || 'No Brand'}
            </p>
            <p className="product-desc">{product.description || 'No description available.'}</p>
            <p className="price">Rs. {product.price}</p>
            {getCartQuantity(product.id) > 0 ? (
              <div className="product-qty-row">
                <button
                  className="qty-btn"
                  onClick={(event) => {
                    event.stopPropagation()
                    onDecrease(product.id)
                  }}
                  disabled={actionLoading}
                >
                  -
                </button>
                <span className="product-qty-value">{getCartQuantity(product.id)}</span>
                <button
                  className="qty-btn"
                  onClick={(event) => {
                    event.stopPropagation()
                    onIncrease(product.id)
                  }}
                  disabled={actionLoading}
                >
                  +
                </button>
              </div>
            ) : (
              <button
                className="primary-btn"
                onClick={(event) => {
                  event.stopPropagation()
                  onAddToCart(product.id)
                }}
                disabled={actionLoading || product.stock <= 0}
              >
                {product.stock > 0 ? 'Add to Cart' : 'Out of Stock'}
              </button>
            )}
          </article>
        ))}
      </div>

      {!loading && !error && (
        <div className="pagination-row">
          <button
            className="nav-btn secondary"
            onClick={() => setCurrentPage((prev) => Math.max(prev - 1, 0))}
            disabled={currentPage === 0}
          >
            Prev
          </button>
          <span>
            Page {currentPage + 1} of {totalPages} ({totalItems} items)
          </span>
          <button
            className="nav-btn secondary"
            onClick={() =>
              setCurrentPage((prev) => Math.min(prev + 1, Math.max(totalPages - 1, 0)))
            }
            disabled={currentPage >= totalPages - 1}
          >
            Next
          </button>
        </div>
      )}

      {selectedProduct && (
        <div className="product-modal-backdrop" onClick={() => setSelectedProduct(null)}>
          <div className="product-modal" onClick={(event) => event.stopPropagation()}>
            <button
              className="modal-close-btn"
              onClick={() => setSelectedProduct(null)}
              aria-label="Close product details"
            >
              x
            </button>
            <div className="product-modal-image-wrap">
              <img
                src={getProductImage(selectedProduct)}
                alt={selectedProduct.name || 'Product image'}
                className="product-modal-image"
                loading="eager"
                decoding="async"
                onError={(event) => {
                  event.currentTarget.onerror = null
                  event.currentTarget.src = FALLBACK_IMAGE
                }}
              />
            </div>
            <h3>{selectedProduct.name}</h3>
            <p className="product-meta">
              {selectedProduct.category || 'General'} | {selectedProduct.brand || 'No Brand'}
            </p>
            <p className="product-desc">
              {selectedProduct.description || 'No description available.'}
            </p>
            <p className="price">Rs. {selectedProduct.price}</p>
            <p className="product-modal-stock">
              Stock: {selectedProduct.stock > 0 ? selectedProduct.stock : 'Out of stock'}
            </p>
          </div>
        </div>
      )}

      {showAccountModal && (
        <div className="account-modal-backdrop" onClick={() => setShowAccountModal(false)}>
          <div className="account-modal" onClick={(event) => event.stopPropagation()}>
            <div className="account-header">
              <h2 className="section-title">My Account</h2>
              {profile?.email && <span className="account-email">{profile.email}</span>}
            </div>

            {profileLoading && <p>Loading account details...</p>}
            {profileError && <p className="form-error">{profileError}</p>}
            {profileSuccess && <p className="form-success">{profileSuccess}</p>}

            <form className="account-form" onSubmit={onSaveProfile}>
              <label>
                Full Name
                <input name="name" value={profileForm.name} onChange={onProfileFieldChange} required />
              </label>
              <label>
                Phone Number
                <input
                  name="phoneNumber"
                  value={profileForm.phoneNumber}
                  onChange={onProfileFieldChange}
                  placeholder="10 digit number"
                />
              </label>
              <label>
                Alternate Phone
                <input
                  name="alternatePhoneNumber"
                  value={profileForm.alternatePhoneNumber}
                  onChange={onProfileFieldChange}
                  placeholder="10 digit number"
                />
              </label>
              <label>
                Age
                <input
                  type="number"
                  name="age"
                  value={profileForm.age}
                  onChange={onProfileFieldChange}
                  min={10}
                  max={100}
                />
              </label>
              <label>
                Gender
                <select name="gender" value={profileForm.gender} onChange={onProfileFieldChange}>
                  <option value="">Select</option>
                  <option value="Male">Male</option>
                  <option value="Female">Female</option>
                  <option value="Other">Other</option>
                </select>
              </label>
              <label>
                Occupation
                <input name="occupation" value={profileForm.occupation} onChange={onProfileFieldChange} />
              </label>
              <label>
                Address Line 1
                <input name="addressLine1" value={profileForm.addressLine1} onChange={onProfileFieldChange} />
              </label>
              <label>
                Address Line 2
                <input name="addressLine2" value={profileForm.addressLine2} onChange={onProfileFieldChange} />
              </label>
              <label>
                City
                <select name="city" value={profileForm.city} onChange={onProfileFieldChange}>
                  <option value="">Select city</option>
                  {cityOptions.map((city) => (
                    <option key={city} value={city}>
                      {city}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                State
                <select name="state" value={profileForm.state} onChange={onProfileFieldChange}>
                  <option value="">Select state</option>
                  {Object.keys(STATE_CITY_MAP).map((state) => (
                    <option key={state} value={state}>
                      {state}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Pincode
                <input name="pincode" value={profileForm.pincode} onChange={onProfileFieldChange} />
              </label>

              <div className="account-actions">
                <button type="button" className="nav-btn secondary" onClick={() => setShowAccountModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="primary-btn" disabled={profileSaving}>
                  {profileSaving ? 'Saving...' : 'Save Details'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </section>
  )
}

export default HomePage

