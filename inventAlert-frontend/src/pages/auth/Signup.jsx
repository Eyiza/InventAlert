import { useState, useEffect } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { useNavigate, Link } from 'react-router'
import { useSignupMutation } from '../../apis/inventAlertApi'
import { setCredentials } from '../../store/slices/authSlice'
import { uploadToCloudinary } from '../../services/cloudinary'

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

export default function Signup() {
  const [form, setForm] = useState({ companyName: '', adminEmail: '', password: '', confirm: '' })
  const [showPassword, setShowPassword] = useState(false)
  const [showConfirm, setShowConfirm] = useState(false)
  const [logoPreview, setLogoPreview] = useState(null)
  const [logoFile, setLogoFile] = useState(null)
  const [error, setError] = useState('')
  const dispatch = useDispatch()
  const navigate = useNavigate()
  const { isAuthenticated } = useSelector(s => s.auth)

  const [signupMutation, { isLoading }] = useSignupMutation()

  useEffect(() => {
    if (isAuthenticated) navigate('/admin', { replace: true })
  }, [isAuthenticated, navigate])

  const handleChange = (e) => setForm(f => ({ ...f, [e.target.name]: e.target.value }))

  const handleLogoChange = (e) => {
    const file = e.target.files[0]
    if (!file) return
    setLogoFile(file)
    const reader = new FileReader()
    reader.onloadend = () => setLogoPreview(reader.result)
    reader.readAsDataURL(file)
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    if (form.password !== form.confirm) { setError('Passwords do not match'); return }
    if (form.password.length < 8) { setError('Password must be at least 8 characters'); return }

    let companyLogo = null
    if (logoFile) {
      try {
        companyLogo = await uploadToCloudinary(logoFile)
      } catch {
        setError('Logo upload failed. Please try again.')
        return
      }
    }

    const result = await signupMutation({
      companyName: form.companyName,
      adminEmail: form.adminEmail,
      password: form.password,
    })

    if (result.data) {
      dispatch(setCredentials({ ...result.data, companyLogo }))
    } else {
      setError(result.error?.data?.message || 'Sign up failed. Please try again.')
    }
  }

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        <div className="mb-4">
          <Link to="/" className="inline-flex items-center gap-1.5 text-sm text-gray-500 hover:text-teal-600 transition-colors">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" /></svg>
            Back to home
          </Link>
        </div>
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-teal-600 shadow-lg shadow-teal-200 mb-4">
            <svg className="w-8 h-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
            </svg>
          </div>
          <h1 className="text-2xl font-bold text-gray-900">InventAlert</h1>
          <p className="text-gray-500 mt-1 text-sm">Intelligent Warehouse Management</p>
        </div>

        <div className="bg-white rounded-2xl shadow-sm border border-gray-200 p-8">
          <h2 className="text-lg font-semibold text-gray-900 mb-1">Create your company account</h2>
          <p className="text-sm text-gray-500 mb-6">You'll be the Admin for your company workspace.</p>

          <form onSubmit={handleSubmit} className="space-y-4">
            {error && (
              <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm">
                {error}
              </div>
            )}

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1.5">Company Logo</label>
              <div className="flex items-center gap-4">
                {logoPreview ? (
                  <img src={logoPreview} alt="logo preview" className="w-14 h-14 rounded-xl object-contain border border-gray-200 bg-gray-50" />
                ) : (
                  <div className="w-14 h-14 rounded-xl bg-gray-100 flex items-center justify-center border-2 border-dashed border-gray-300">
                    <svg className="w-6 h-6 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                    </svg>
                  </div>
                )}
                <div>
                  <label htmlFor="logo-upload" className="cursor-pointer inline-block px-3 py-1.5 border border-gray-300 rounded-lg text-sm text-gray-700 hover:bg-gray-50 transition-colors">
                    {logoPreview ? 'Change Logo' : 'Upload Logo'}
                  </label>
                  <input id="logo-upload" type="file" accept="image/*" onChange={handleLogoChange} className="hidden" />
                  {logoPreview && (
                    <button type="button" onClick={() => { setLogoPreview(null); setLogoFile(null) }} className="ml-2 text-xs text-red-500 hover:underline">Remove</button>
                  )}
                  <p className="text-xs text-gray-400 mt-1">PNG, JPG, SVG — up to 2 MB</p>
                </div>
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1.5">Company Name</label>
              <input
                name="companyName"
                value={form.companyName}
                onChange={handleChange}
                className="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-600 focus:border-transparent text-sm"
                placeholder="Acme Logistics Ltd."
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1.5">Admin Email</label>
              <input
                name="adminEmail"
                type="email"
                value={form.adminEmail}
                onChange={handleChange}
                className="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-600 focus:border-transparent text-sm"
                placeholder="admin@yourcompany.com"
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1.5">Password</label>
              <div className="relative">
                <input
                  name="password"
                  type={showPassword ? 'text' : 'password'}
                  value={form.password}
                  onChange={handleChange}
                  className="w-full px-4 py-2.5 pr-11 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-600 focus:border-transparent text-sm"
                  placeholder="Minimum 8 characters"
                  required
                />
                <button type="button" onClick={() => setShowPassword(s => !s)} className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 transition-colors" tabIndex={-1}>
                  {showPassword ? <EyeOffIcon /> : <EyeIcon />}
                </button>
              </div>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1.5">Confirm Password</label>
              <div className="relative">
                <input
                  name="confirm"
                  type={showConfirm ? 'text' : 'password'}
                  value={form.confirm}
                  onChange={handleChange}
                  className="w-full px-4 py-2.5 pr-11 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-600 focus:border-transparent text-sm"
                  placeholder="••••••••"
                  required
                />
                <button type="button" onClick={() => setShowConfirm(s => !s)} className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 transition-colors" tabIndex={-1}>
                  {showConfirm ? <EyeOffIcon /> : <EyeIcon />}
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
              {isLoading ? 'Creating account…' : 'Create Account'}
            </button>
          </form>
          <p className="text-center text-sm text-gray-500 mt-6">
            Already have an account?{' '}
            <Link to="/login" className="text-teal-600 font-medium hover:underline">
              Sign in
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}
