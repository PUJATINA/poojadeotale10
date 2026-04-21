import { useState } from 'react'
import { useAuth } from '../context/AuthContext.jsx'

const EMAIL_REGEX = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/

function LoginPage({ navigate }) {
  const { requestLoginOtp, loginWithOtp } = useAuth()
  const [form, setForm] = useState({ email: '', password: '', otp: '' })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [otpRequested, setOtpRequested] = useState(false)

  const onChange = (event) => {
    setForm((prev) => ({ ...prev, [event.target.name]: event.target.value }))
  }

  const onSubmit = async (event) => {
    event.preventDefault()
    setError('')
    setSuccess('')

    const normalizedEmail = form.email.trim().toLowerCase()
    if (!EMAIL_REGEX.test(normalizedEmail)) {
      setError('Please enter a valid email address (example: name@example.com).')
      return
    }

    if (otpRequested && !/^\d{6}$/.test(form.otp)) {
      setError('Please enter a valid 6-digit OTP.')
      return
    }

    setLoading(true)
    try {
      if (!otpRequested) {
        const otpResponse = await requestLoginOtp({
          email: normalizedEmail,
          password: form.password,
        })
        setOtpRequested(true)
        if (otpResponse?.debugOtp) {
          setSuccess(`OTP sent. (Dev OTP: ${otpResponse.debugOtp})`)
        } else {
          setSuccess('OTP sent to your email.')
        }
      } else {
        await loginWithOtp(normalizedEmail, form.password, form.otp)
        navigate('/home')
      }
    } catch (err) {
      const status = err?.status
      const rawMessage = (err?.message || '').toLowerCase()

      if (
        status === 401 ||
        status === 403 ||
        rawMessage.includes('unauthorized') ||
        rawMessage.includes('forbidden') ||
        rawMessage.includes('invalid')
      ) {
        setError(otpRequested ? 'Invalid OTP or credentials.' : 'Invalid email or password.')
      } else {
        setError(err.message || 'Unable to login. Please try again.')
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-page">
      <section className="auth-container auth-panel">
        <div className="auth-strip" aria-hidden="true" />
        <div className="auth-header">
          <h1>Login</h1>
          <p>Use your account to continue shopping.</p>
        </div>
        <form className="auth-form needs-validation" onSubmit={onSubmit}>
          <label className="form-label">
            Email
            <input
              className="form-control"
              type="email"
              name="email"
              value={form.email}
              onChange={onChange}
              placeholder="you@example.com"
              inputMode="email"
              maxLength={120}
              required
            />
          </label>
          <label className="form-label">
            Password
            <input
              className="form-control"
              type="password"
              name="password"
              value={form.password}
              onChange={onChange}
              placeholder="********"
              required
              disabled={otpRequested}
            />
          </label>
          {otpRequested && (
            <label className="form-label">
              OTP
              <input
                className="form-control"
                type="text"
                name="otp"
                value={form.otp}
                onChange={onChange}
                placeholder="Enter 6-digit OTP"
                maxLength={6}
                required
              />
            </label>
          )}
          {error && <p className="form-error">{error}</p>}
          {success && <p className="form-success">{success}</p>}
          <button type="submit" className="primary-btn btn btn-primary w-100" disabled={loading}>
            {loading ? (otpRequested ? 'Verifying OTP...' : 'Sending OTP...') : otpRequested ? 'Verify OTP & Login' : 'Send OTP'}
          </button>
        </form>
        <div className="auth-footer">
          <button className="link-btn btn btn-link" onClick={() => navigate('/forgot-password')}>
            Forgot password?
          </button>
          <button className="link-btn btn btn-link" onClick={() => navigate('/register')}>
            New user? Create an account
          </button>
        </div>
      </section>
    </div>
  )
}

export default LoginPage
