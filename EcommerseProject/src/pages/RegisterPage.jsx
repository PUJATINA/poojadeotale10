import { useState } from 'react'
import { useAuth } from '../context/AuthContext.jsx'

const EMAIL_REGEX = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/
const PASSWORD_REGEX = /^(?=.*\d)(?=.*[^A-Za-z0-9]).{6,}$/

function RegisterPage({ navigate }) {
  const { requestRegisterOtp, registerWithOtp } = useAuth()
  const [form, setForm] = useState({
    name: '',
    email: '',
    password: '',
    confirmPassword: '',
    otp: '',
  })
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

    if (!PASSWORD_REGEX.test(form.password)) {
      setError(
        'Password must be at least 6 characters and include at least 1 number and 1 special character.',
      )
      return
    }

    if (form.password !== form.confirmPassword) {
      setError('Passwords do not match')
      return
    }

    if (otpRequested && !/^\d{6}$/.test(form.otp)) {
      setError('Please enter a valid 6-digit OTP.')
      return
    }

    setLoading(true)
    try {
      if (!otpRequested) {
        const otpResponse = await requestRegisterOtp({
          name: form.name,
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
        await registerWithOtp({
          name: form.name,
          email: normalizedEmail,
          password: form.password,
          otp: form.otp,
        })
        navigate('/home')
      }
    } catch (err) {
      if (err?.status === 409) {
        setError('Email is already registered. Please login with this email.')
      } else {
        setError(err.message || 'Unable to register')
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
          <h1>Create Account</h1>
          <p>Register to start shopping on ShopSphere.</p>
        </div>
        <form className="auth-form needs-validation" onSubmit={onSubmit}>
          <label className="form-label">
            Full Name
            <input
              className="form-control"
              type="text"
              name="name"
              value={form.name}
              onChange={onChange}
              placeholder="John Doe"
              required
            />
          </label>
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
              placeholder="Min 6 chars, 1 number, 1 special char"
              required
              minLength={6}
              disabled={otpRequested}
            />
          </label>
          <label className="form-label">
            Confirm Password
            <input
              className="form-control"
              type="password"
              name="confirmPassword"
              value={form.confirmPassword}
              onChange={onChange}
              placeholder="Re-enter password"
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
            {loading ? (otpRequested ? 'Verifying OTP...' : 'Sending OTP...') : otpRequested ? 'Verify OTP & Register' : 'Send OTP'}
          </button>
        </form>
        <div className="auth-footer">
          <button className="link-btn btn btn-link" onClick={() => navigate('/login')}>
            Already have an account? Login
          </button>
        </div>
      </section>
    </div>
  )
}

export default RegisterPage
