import { useEffect } from 'react'
import { Navigate } from 'react-router'
import { useSelector, useDispatch } from 'react-redux'
import { useGetMyCompanyQuery } from '../apis/inventAlertApi'
import { setCompanyLogo } from '../store/slices/authSlice'

const ROLE_HOME = {
  ADMIN: '/admin',
  MANAGER: '/manager',
  WAREHOUSE_STAFF: '/staff',
  PROCUREMENT_OFFICER: '/procurement',
  SUPERADMIN: '/superadmin',
}

function CompanyLogoSync() {
  const dispatch = useDispatch()
  const companyLogo = useSelector(s => s.auth.companyLogo)
  const { data } = useGetMyCompanyQuery()

  useEffect(() => {
    if (data?.logoUrl && data.logoUrl !== companyLogo) {
      dispatch(setCompanyLogo(data.logoUrl))
    }
  }, [data?.logoUrl, companyLogo, dispatch])

  return null
}

export function ProtectedRoute({ children, allowedRoles }) {
  const { isAuthenticated, role, mustChangePassword } = useSelector(s => s.auth)
  if (!isAuthenticated) return <Navigate to="/login" replace />
  if (mustChangePassword) return <Navigate to="/change-password" replace />
  if (allowedRoles && !allowedRoles.includes(role)) {
    return <Navigate to={ROLE_HOME[role] || '/login'} replace />
  }
  const isCompanyUser = role !== 'SUPER_ADMIN'
  return (
    <>
      {isCompanyUser && <CompanyLogoSync />}
      {children}
    </>
  )
}

export function RoleRedirect() {
  const { isAuthenticated, role, mustChangePassword } = useSelector(s => s.auth)
  if (!isAuthenticated) return <Navigate to="/" replace />
  if (mustChangePassword) return <Navigate to="/change-password" replace />
  return <Navigate to={ROLE_HOME[role] || '/login'} replace />
}
