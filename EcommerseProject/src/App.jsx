import { useEffect, useState } from 'react'
import { useAuth } from './context/AuthContext.jsx'
import Navbar from './components/Navbar.jsx'
import HomePage from './pages/HomePage.jsx'
import LoginPage from './pages/LoginPage.jsx'
import RegisterPage from './pages/RegisterPage.jsx'
import CartPage from './pages/CartPage.jsx'
import OrdersPage from './pages/OrdersPage.jsx'
import ForgotPasswordPage from './pages/ForgotPasswordPage.jsx'
import AdminPage from './pages/AdminPage.jsx'

const getPath = () => window.location.pathname.toLowerCase()

function useSimpleRouter() {
  const [path, setPath] = useState(getPath())

  useEffect(() => {
    const onLocationChange = () => setPath(getPath())
    window.addEventListener('popstate', onLocationChange)
    return () => window.removeEventListener('popstate', onLocationChange)
  }, [])

  const navigate = (nextPath) => {
    if (window.location.pathname !== nextPath) {
      window.history.pushState({}, '', nextPath)
      setPath(nextPath.toLowerCase())
    }
  }

  return { path, navigate }
}

function App() {
  const { user, loading } = useAuth()
  const { path, navigate } = useSimpleRouter()

  if (loading) {
    return <div className="centered-page">Loading...</div>
  }

  const currentPath = path === '/' ? '/home' : path

  if (
    !user &&
    currentPath !== '/login' &&
    currentPath !== '/register' &&
    currentPath !== '/forgot-password'
  ) {
    navigate('/login')
    return null
  }

  if (user && (currentPath === '/register' || currentPath === '/forgot-password')) {
    navigate('/home')
    return null
  }

  if (user && currentPath === '/admin' && user.role !== 'ADMIN') {
    navigate('/home')
    return null
  }

  if (
    currentPath !== '/home' &&
    currentPath !== '/cart' &&
    currentPath !== '/orders' &&
    currentPath !== '/admin' &&
    currentPath !== '/login' &&
    currentPath !== '/register' &&
    currentPath !== '/forgot-password'
  ) {
    navigate(user ? '/home' : '/login')
    return null
  }

  return (
    <div className="app-shell">
      <Navbar navigate={navigate} user={user} />
      <main className="main-content">
        {currentPath === '/login' && <LoginPage navigate={navigate} />}
        {currentPath === '/register' && <RegisterPage navigate={navigate} />}
        {currentPath === '/forgot-password' && <ForgotPasswordPage navigate={navigate} />}
        {currentPath === '/home' && <HomePage navigate={navigate} />}
        {currentPath === '/cart' && <CartPage navigate={navigate} />}
        {currentPath === '/orders' && <OrdersPage navigate={navigate} />}
        {currentPath === '/admin' && <AdminPage navigate={navigate} />}
      </main>
    </div>
  )
}

export default App
