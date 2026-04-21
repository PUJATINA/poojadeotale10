import { useState } from 'react'
import { authApi } from '../api/client'

const EMAIL_REGEX = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/
const PASSWORD_REGEX = /^(?=.*\d)(?=.*[^A-Za-z0-9]).{6,}$/

function ForgotPasswordPage({ navigate }) {
  const [form, setForm] = useState({ email: '', otp: '', newPassword: '', confirmPassword: '' })
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

    if (otpRequested && !PASSWORD_REGEX.test(form.newPassword)) {
      setError('Password must be at least 6 characters and include at least 1 number and 1 special character.')
      return
    }

    if (otpRequested && form.newPassword !== form.confirmPassword) {
      setError('Passwords do not match.')
      return
    }

    setLoading(true)
    try {
      if (!otpRequested) {
        const otpResponse = await authApi.requestPasswordResetOtp({ email: normalizedEmail })
        setOtpRequested(true)
        setSuccess(
          otpResponse?.debugOtp
            ? `OTP sent. (Dev OTP: ${otpResponse.debugOtp})`
            : 'OTP sent to your email.',
        )
      } else {
        const response = await authApi.verifyPasswordResetOtp({
          email: normalizedEmail,
          otp: form.otp,
          newPassword: form.newPassword,
        })
        setSuccess(response?.message || 'Password reset successful.')
        setTimeout(() => navigate('/login'), 800)
      }
    } catch (err) {
      setError(err.message || 'Unable to process request.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <section className="auth-container">
      <h1>Forgot Password</h1>
      <p>Reset password securely using OTP verification.</p>
      <form className="auth-form" onSubmit={onSubmit}>
        <label>
          Email
          <input
            type="email"
            name="email"
            value={form.email}
            onChange={onChange}
            placeholder="you@example.com"
            required
            disabled={otpRequested}
          />
        </label>
        {otpRequested && (
          <>
            <label>
              OTP
              <input
                type="text"
                name="otp"
                value={form.otp}
                onChange={onChange}
                placeholder="Enter 6-digit OTP"
                maxLength={6}
                required
              />
            </label>
            <label>
              New Password
              <input
                type="password"
                name="newPassword"
                value={form.newPassword}
                onChange={onChange}
                placeholder="Min 6 chars, 1 number, 1 special char"
                required
              />
            </label>
            <label>
              Confirm New Password
              <input
                type="password"
                name="confirmPassword"
                value={form.confirmPassword}
                onChange={onChange}
                placeholder="Re-enter new password"
                required
              />
            </label>
          </>
        )}
        {error && <p className="form-error">{error}</p>}
        {success && <p className="form-success">{success}</p>}
        <button type="submit" className="primary-btn" disabled={loading}>
          {loading
            ? otpRequested
              ? 'Resetting...'
              : 'Sending OTP...'
            : otpRequested
              ? 'Verify OTP & Reset'
              : 'Send OTP'}
        </button>
      </form>
      <button className="link-btn" onClick={() => navigate('/login')}>
        Back to login
      </button>
    </section>
  )
}

export default ForgotPasswordPage
