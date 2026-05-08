import { useDispatch, useSelector } from 'react-redux'
import { useNavigate } from 'react-router'
import { logout } from '../../store/slices/authSlice'
import NotificationBell from '../shared/NotificationBell'

export default function Navbar({ title }) {
  const dispatch = useDispatch()
  const navigate = useNavigate()
  const { user, role } = useSelector(s => s.auth)

  const handleLogout = () => {
    dispatch(logout())
    navigate('/login', { replace: true })
  }

  const roleLabel = {
    ADMIN: 'Admin',
    MANAGER: 'Manager',
    WAREHOUSE_STAFF: 'Warehouse Staff',
    PROCUREMENT_OFFICER: 'Procurement Officer',
    SUPERADMIN: 'Platform Admin',
  }[role] || role

  return (
    <header className="h-16 bg-white border-b border-gray-200 flex items-center justify-between px-6 shrink-0">
      <div className="flex items-center gap-3">
        <h1 className="text-lg font-semibold text-gray-900">{title}</h1>
      </div>

      <div className="flex items-center gap-3">
        <NotificationBell />

        <div className="h-6 w-px bg-gray-200" />

        <div className="flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-full bg-teal-600 flex items-center justify-center text-white text-sm font-semibold">
            {user?.name?.[0]?.toUpperCase() || 'U'}
          </div>
          <div className="hidden sm:block">
            <p className="text-sm font-medium text-gray-900 leading-none">{user?.name}</p>
            <p className="text-xs text-gray-500 mt-0.5">{roleLabel}</p>
          </div>
        </div>

        <button
          onClick={handleLogout}
          className="ml-1 flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-red-50 text-red-600 hover:bg-red-100 active:bg-red-200 transition-colors text-sm font-medium"
          title="Sign out"
        >
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
              d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
          </svg>
          <span className="hidden sm:inline">Sign Out</span>
        </button>
      </div>
    </header>
  )
}
