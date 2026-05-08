import { Navigate } from 'react-router'
import { useSelector } from 'react-redux'

const ROLE_HOME = {
  ADMIN: '/admin',
  MANAGER: '/manager',
  WAREHOUSE_STAFF: '/staff',
  PROCUREMENT_OFFICER: '/procurement',
  SUPERADMIN: '/superadmin',
}

export function ProtectedRoute({ children, allowedRoles }) {
  const { isAuthenticated, role } = useSelector(s => s.auth)
  if (!isAuthenticated) return <Navigate to="/login" replace />
  if (allowedRoles && !allowedRoles.includes(role)) {
    return <Navigate to={ROLE_HOME[role] || '/login'} replace />
  }
  return children
}

export function RoleRedirect() {
  const { isAuthenticated, role } = useSelector(s => s.auth)
  if (!isAuthenticated) return <Navigate to="/login" replace />
  return <Navigate to={ROLE_HOME[role] || '/login'} replace />
}
