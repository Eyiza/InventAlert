import { createBrowserRouter, Navigate } from 'react-router'
import { ProtectedRoute, RoleRedirect } from './ProtectedRoute'
import Login from '../pages/auth/Login'
import Signup from '../pages/auth/Signup'
import AdminDashboard from '../pages/admin/AdminDashboard'
import ManagerDashboard from '../pages/manager/ManagerDashboard'
import StaffDashboard from '../pages/staff/StaffDashboard'
import ProcurementDashboard from '../pages/procurement/ProcurementDashboard'
import SuperAdminPortal from '../pages/superadmin/SuperAdminPortal'

const router = createBrowserRouter([
  { path: '/login', element: <Login /> },
  { path: '/signup', element: <Signup /> },
  { path: '/', element: <RoleRedirect /> },
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
      <ProtectedRoute allowedRoles={['SUPERADMIN']}>
        <SuperAdminPortal />
      </ProtectedRoute>
    ),
  },
  { path: '*', element: <Navigate to="/login" replace /> },
])

export default router