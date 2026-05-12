import { useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { useNavigate } from 'react-router'
import { changePassword } from '../../store/slices/authSlice'
import { toast } from 'react-toastify'

const ROLE_HOME = {
  ADMIN: '/admin', MANAGER: '/manager', WAREHOUSE_STAFF: '/staff',
  PROCUREMENT_OFFICER: '/procurement', SUPER_ADMIN: '/superadmin',
}

function EyeIcon() {
  return (
    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
    </svg>
  )
}

function EyeOffIcon() {
  return (
    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" />
    </svg>
  )
}

export default function ChangePassword() {
  const [form, setForm] = useState({ newPassword: '', confirm: '' })
  const [show, setShow] = useState({ new: false, confirm: false })
  const [error, setError] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const dispatch = useDispatch()
  const navigate = useNavigate()
  const { user, role } = useSelector(s => s.auth)

  const handleSubmit = e => {
    e.preventDefault()
    setError('')
    if (form.newPassword.length < 8) { setError('Password must be at least 8 characters'); return }
    if (form.newPassword !== form.confirm) { setError('Passwords do not match'); return }
    setIsLoading(true)
    setTimeout(() => {
      dispatch(changePassword({ newPassword: form.newPassword }))
      toast.success('Password updated successfully — welcome!')
      navigate(ROLE_HOME[role] || '/login', { replace: true })
    }, 600)
  }

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-teal-600 shadow-lg shadow-teal-200 mb-4">
            <svg className="w-7 h-7 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
            </svg>
          </div>
          <h1 className="text-2xl font-bold text-gray-900">Set your password</h1>
          <p className="text-gray-500 mt-1 text-sm">Hi {user?.email?.split('@')[0]} — create a new password to continue.</p>
        </div>

        <div className="bg-white rounded-2xl shadow-sm border border-gray-200 p-8">
          <div className="bg-amber-50 border border-amber-200 rounded-lg px-4 py-3 mb-5 flex gap-2.5 items-start">
            <svg className="w-4 h-4 text-amber-500 shrink-0 mt-0.5" fill="currentColor" viewBox="0 0 20 20">
              <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
            </svg>
            <p className="text-sm text-amber-700">Your account was set up with a temporary password. Please choose a permanent one now.</p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            {error && (
              <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm">{error}</div>
            )}

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1.5">New Password</label>
              <div className="relative">
                <input
                  type={show.new ? 'text' : 'password'}
                  value={form.newPassword}
                  onChange={e => setForm(f => ({ ...f, newPassword: e.target.value }))}
                  className="w-full px-4 py-2.5 pr-11 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-600 focus:border-transparent text-sm"
                  placeholder="Minimum 8 characters"
                  required
                />
                <button type="button" onClick={() => setShow(s => ({ ...s, new: !s.new }))} tabIndex={-1} className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600">
                  {show.new ? <EyeOffIcon /> : <EyeIcon />}
                </button>
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1.5">Confirm New Password</label>
              <div className="relative">
                <input
                  type={show.confirm ? 'text' : 'password'}
                  value={form.confirm}
                  onChange={e => setForm(f => ({ ...f, confirm: e.target.value }))}
                  className="w-full px-4 py-2.5 pr-11 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-600 focus:border-transparent text-sm"
                  placeholder="••••••••"
                  required
                />
                <button type="button" onClick={() => setShow(s => ({ ...s, confirm: !s.confirm }))} tabIndex={-1} className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600">
                  {show.confirm ? <EyeOffIcon /> : <EyeIcon />}
                </button>
              </div>
            </div>

            <button
              type="submit"
              disabled={isLoading}
              className="w-full py-2.5 bg-teal-600 hover:bg-teal-700 text-white font-semibold rounded-lg transition-colors mt-2 flex items-center justify-center gap-2 disabled:opacity-70 disabled:cursor-not-allowed"
            >
              {isLoading && (
                <svg className="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                </svg>
              )}
              {isLoading ? 'Saving…' : 'Set New Password'}
            </button>
          </form>
        </div>
      </div>
    </div>
  )
}
