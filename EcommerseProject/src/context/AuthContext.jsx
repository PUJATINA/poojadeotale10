import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import { authApi } from '../api/client'

const AuthContext = createContext(null)
const AUTH_USER_KEY = 'auth_user'

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const hydrateAuth = async () => {
      try {
        const me = await authApi.me()
        const hydratedUser = {
          id: me.userId,
          name: me.name,
          email: me.email,
          role: me.role || 'USER',
        }
        localStorage.setItem(AUTH_USER_KEY, JSON.stringify(hydratedUser))
        setUser(hydratedUser)
      } catch {
        try {
          await authApi.refreshSession()
          const me = await authApi.me()
          const refreshedUser = {
            id: me.userId,
            name: me.name,
            email: me.email,
            role: me.role || 'USER',
          }
          localStorage.setItem(AUTH_USER_KEY, JSON.stringify(refreshedUser))
          setUser(refreshedUser)
        } catch {
          localStorage.removeItem(AUTH_USER_KEY)
          setUser(null)
        }
      } finally {
        setLoading(false)
      }
    }

    hydrateAuth()
  }, [])

  const loginWithOtp = async (email, password, otp) => {
    const payload = await authApi.verifyLoginOtp({ email, password, otp })
    const loggedInUser = {
      id: payload.userId,
      name: payload.name,
      email: payload.email,
      role: payload.role || 'USER',
    }
    localStorage.setItem(AUTH_USER_KEY, JSON.stringify(loggedInUser))
    setUser(loggedInUser)
    return loggedInUser
  }

  const registerWithOtp = async ({ name, email, password, otp }) => {
    const payload = await authApi.verifyRegisterOtp({ name, email, password, otp })
    const registeredUser = {
      id: payload.userId,
      name: payload.name,
      email: payload.email,
      role: payload.role || 'USER',
    }
    localStorage.setItem(AUTH_USER_KEY, JSON.stringify(registeredUser))
    setUser(registeredUser)
    return registeredUser
  }

  const logout = async () => {
    try {
      await authApi.logout()
    } catch {
      // ignore server logout failure and clear local state anyway
    }
    localStorage.removeItem(AUTH_USER_KEY)
    setUser(null)
  }

  const value = useMemo(
    () => ({
      user,
      loading,
      requestLoginOtp: authApi.requestLoginOtp,
      requestRegisterOtp: authApi.requestRegisterOtp,
      loginWithOtp,
      registerWithOtp,
      logout,
    }),
    [user, loading],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)

  if (!context) {
    throw new Error('useAuth must be used inside AuthProvider')
  }

  return context
}
