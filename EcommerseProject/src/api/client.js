const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8888/api'

function shouldAttemptRefresh(path) {
  return !(
    path.startsWith('/auth/refresh') ||
    path.startsWith('/auth/login/') ||
    path.startsWith('/auth/register/') ||
    path.startsWith('/auth/password/') ||
    path.startsWith('/auth/logout')
  )
}

async function refreshSessionRequest() {
  const response = await fetch(`${API_BASE_URL}/auth/refresh`, {
    method: 'POST',
    credentials: 'include',
  })
  if (!response.ok) {
    throw new Error('Session refresh failed')
  }
}

async function request(path, options = {}, retryOnAuthFailure = true) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers || {}),
    },
    ...options,
  })

  if (response.status === 401 && retryOnAuthFailure && shouldAttemptRefresh(path)) {
    try {
      await refreshSessionRequest()
      return request(path, options, false)
    } catch {
      // fall through to response parsing below
    }
  }

  const text = await response.text()
  let data = null

  if (text) {
    try {
      data = JSON.parse(text)
    } catch {
      data = { message: text }
    }
  }

  if (!response.ok) {
    const message =
      data?.message ||
      data?.error ||
      data?.details ||
      `Request failed: ${response.status}`
    const error = new Error(message)
    error.status = response.status
    throw error
  }

  return data
}

async function requestBlob(path, options = {}, retryOnAuthFailure = true) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    credentials: 'include',
    ...options,
  })

  if (response.status === 401 && retryOnAuthFailure && shouldAttemptRefresh(path)) {
    try {
      await refreshSessionRequest()
      return requestBlob(path, options, false)
    } catch {
      // fall through to response parsing below
    }
  }

  if (!response.ok) {
    let message = `Request failed: ${response.status}`
    try {
      const text = await response.text()
      if (text) {
        const data = JSON.parse(text)
        message = data?.message || data?.error || message
      }
    } catch {
      // ignore parsing errors
    }
    const error = new Error(message)
    error.status = response.status
    throw error
  }

  return response.blob()
}

export const authApi = {
  requestRegisterOtp(payload) {
    return request('/auth/register/request-otp', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },
  verifyRegisterOtp(payload) {
    return request('/auth/register/verify-otp', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },
  requestLoginOtp(payload) {
    return request('/auth/login/request-otp', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },
  verifyLoginOtp(payload) {
    return request('/auth/login/verify-otp', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },
  requestPasswordResetOtp(payload) {
    return request('/auth/password/request-otp', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },
  verifyPasswordResetOtp(payload) {
    return request('/auth/password/verify-otp', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },
  refreshSession() {
    return request('/auth/refresh', {
      method: 'POST',
    }, false)
  },
  logout() {
    return request('/auth/logout', {
      method: 'POST',
    }, false)
  },
  me() {
    return request('/auth/me', {
      method: 'GET',
    }, false)
  },
}

export const productApi = {
  list(params = {}) {
    const query = new URLSearchParams()
    if (params.category) {
      query.set('category', params.category)
    }
    if (params.brand) {
      query.set('brand', params.brand)
    }
    if (params.q) {
      query.set('q', params.q)
    }
    if (params.sortBy) {
      query.set('sortBy', params.sortBy)
    }
    if (params.sortDir) {
      query.set('sortDir', params.sortDir)
    }
    if (params.page !== undefined) {
      query.set('page', String(params.page))
    }
    if (params.size !== undefined) {
      query.set('size', String(params.size))
    }
    const suffix = query.toString() ? `?${query.toString()}` : ''
    return request(`/products${suffix}`)
  },
  imageUrl(productId) {
    return `${API_BASE_URL}/products/${productId}/image?v=2`
  },
}

export const cartApi = {
  get(userId) {
    return request(`/cart/${userId}`)
  },
  add(payload) {
    return request('/cart/add', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },
  remove(userId, productId) {
    const params = new URLSearchParams({
      userId: String(userId),
      productId: String(productId),
    })
    return request(`/cart/remove?${params.toString()}`, {
      method: 'DELETE',
    })
  },
  update(payload) {
    return request('/cart/update', {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
  },
  buy(userId) {
    return request('/cart/buy', {
      method: 'POST',
      body: JSON.stringify({ userId }),
    })
  },
}

export const orderApi = {
  list(userId) {
    return request(`/orders/${userId}`)
  },
  async downloadInvoice(orderId, userId) {
    return requestBlob(`/orders/${orderId}/invoice?userId=${userId}`)
  },
  emailInvoice(orderId, userId) {
    return request(`/orders/${orderId}/invoice/email?userId=${userId}`, {
      method: 'POST',
    })
  },
}

export const userApi = {
  getProfile(userId) {
    return request(`/users/${userId}/profile`)
  },
  updateProfile(userId, payload) {
    return request(`/users/${userId}/profile`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
  },
}

export const adminApi = {
  listProducts() {
    return request('/admin/products')
  },
  createProduct(payload) {
    return request('/admin/products', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },
  updateProduct(productId, payload) {
    return request(`/admin/products/${productId}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
  },
  updateStock(productId, stock) {
    return request(`/admin/products/${productId}/stock`, {
      method: 'PATCH',
      body: JSON.stringify({ stock }),
    })
  },
  deleteProduct(productId) {
    return request(`/admin/products/${productId}`, {
      method: 'DELETE',
    })
  },
  listCategories() {
    return request('/admin/categories')
  },
  addCategory(name) {
    return request('/admin/categories', {
      method: 'POST',
      body: JSON.stringify({ name }),
    })
  },
  renameCategory(currentCategory, newCategory) {
    return request('/admin/categories/rename', {
      method: 'POST',
      body: JSON.stringify({ currentCategory, newCategory }),
    })
  },
  listOrders() {
    return request('/admin/orders')
  },
  updateOrderStatus(orderId, status) {
    return request(`/admin/orders/${orderId}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status }),
    })
  },
  listUsers() {
    return request('/admin/users')
  },
  updateUserRole(userId, role) {
    return request(`/admin/users/${userId}/role`, {
      method: 'PATCH',
      body: JSON.stringify({ role }),
    })
  },
}
