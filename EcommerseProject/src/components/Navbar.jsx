import { useAuth } from '../context/AuthContext.jsx'

function Navbar({ navigate, user }) {
  const { logout } = useAuth()

  const onAccountDetails = () => {
    navigate('/home')
    setTimeout(() => {
      window.dispatchEvent(new Event('open-account-modal'))
    }, 50)
  }

  const onLogout = async () => {
    try {
      await logout()
      navigate('/login')
    } catch (error) {
      alert(error.message)
    }
  }

  return (
    <header className="top-nav">
      <div className="brand" onClick={() => navigate('/home')}>
        ShopSphere
      </div>

      <nav className="nav-links">
        {user ? (
          <>
            <span className="welcome-text">Hello, {user.name || user.email}</span>
            {user.role === 'ADMIN' && (
              <button className="nav-btn secondary" onClick={() => navigate('/admin')}>
                Admin
              </button>
            )}
            <button className="nav-btn secondary" onClick={onAccountDetails}>
              Account Details
            </button>
            <button className="nav-btn secondary" onClick={() => navigate('/cart')}>
              Cart
            </button>
            <button className="nav-btn secondary" onClick={() => navigate('/orders')}>
              Order History
            </button>
            <button className="nav-btn secondary" onClick={onLogout}>
              Logout
            </button>
          </>
        ) : (
          <>
            <button className="nav-btn secondary" onClick={() => navigate('/login')}>
              Login
            </button>
            <button className="nav-btn" onClick={() => navigate('/register')}>
              Register
            </button>
          </>
        )}
      </nav>
    </header>
  )
}

export default Navbar
