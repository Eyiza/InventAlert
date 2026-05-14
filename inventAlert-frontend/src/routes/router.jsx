import { createBrowserRouter, Navigate } from 'react-router'
import { ProtectedRoute, RoleRedirect } from './ProtectedRoute'
import LandingPage from '../pages/landing/LandingPage'
import Login from '../pages/auth/Login'
import Signup from '../pages/auth/Signup'
import ForgotPassword from '../pages/auth/ForgotPassword'
import ResetPassword from '../pages/auth/ResetPassword'
import ChangePassword from '../pages/auth/ChangePassword'
import AdminDashboard from '../pages/admin/AdminDashboard'
import ManagerDashboard from '../pages/manager/ManagerDashboard'
import StaffDashboard from '../pages/staff/StaffDashboard'
import ProcurementDashboard from '../pages/procurement/ProcurementDashboard'
import SuperAdminPortal from '../pages/superadmin/SuperAdminPortal'
import SuperAdminLogin from '../pages/superadmin/SuperAdminLogin'

const router = createBrowserRouter([
  { path: '/', element: <LandingPage /> },
  { path: '/login', element: <Login /> },
  { path: '/superadmin/login', element: <SuperAdminLogin /> },
  { path: '/signup', element: <Signup /> },
  { path: '/forgot-password', element: <ForgotPassword /> },
  { path: '/reset-password', element: <ResetPassword /> },
  { path: '/change-password', element: <ChangePassword /> },
  { path: '/dashboard', element: <RoleRedirect /> },
  {
    path: '/admin',
    element: (
      <ProtectedRoute allowedRoles={['ADMIN']}>
        <AdminDashboard />
      </ProtectedRoute>
    ),
  },
  {
    path: '/manager',
    element: (
      <ProtectedRoute allowedRoles={['MANAGER']}>
        <ManagerDashboard />
      </ProtectedRoute>
    ),
  },
  {
    path: '/staff',
    element: (
      <ProtectedRoute allowedRoles={['WAREHOUSE_STAFF']}>
        <StaffDashboard />
      </ProtectedRoute>
    ),
  },
  {
    path: '/procurement',
    element: (
      <ProtectedRoute allowedRoles={['PROCUREMENT_OFFICER']}>
        <ProcurementDashboard />
      </ProtectedRoute>
    ),
  },
  {
    path: '/superadmin',
    element: (
      <ProtectedRoute allowedRoles={['SUPER_ADMIN']}>
        <SuperAdminPortal />
      </ProtectedRoute>
    ),
  },
  { path: '*', element: <Navigate to="/" replace /> },
])

export default router
