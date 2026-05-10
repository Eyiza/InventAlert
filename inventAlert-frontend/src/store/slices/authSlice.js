import { createSlice } from '@reduxjs/toolkit'

const MOCK_CREDENTIALS = [
  { id: 'user-1', companyId: 'comp-1', email: 'admin@techwave.com', password: 'password123', name: 'Alice Johnson', role: 'ADMIN', warehouseId: null, companyName: 'TechWave Logistics', mustChangePassword: false },
  { id: 'user-2', companyId: 'comp-1', email: 'manager@techwave.com', password: 'password123', name: 'Bob Smith', role: 'MANAGER', warehouseId: null, companyName: 'TechWave Logistics', mustChangePassword: false },
  { id: 'user-3', companyId: 'comp-1', email: 'staff.a@techwave.com', password: 'password123', name: 'Charlie Brown', role: 'WAREHOUSE_STAFF', warehouseId: 'wh-1', companyName: 'TechWave Logistics', mustChangePassword: false },
  { id: 'user-4', companyId: 'comp-1', email: 'staff.b@techwave.com', password: 'password123', name: 'Diana Ross', role: 'WAREHOUSE_STAFF', warehouseId: 'wh-2', companyName: 'TechWave Logistics', mustChangePassword: false },
  { id: 'user-5', companyId: 'comp-1', email: 'procurement@techwave.com', password: 'password123', name: 'Eve Martinez', role: 'PROCUREMENT_OFFICER', warehouseId: null, companyName: 'TechWave Logistics', mustChangePassword: false },
  { id: 'superadmin', companyId: null, email: 'superadmin@inventalert.com', password: 'superadmin123', name: 'Platform Admin', role: 'SUPERADMIN', warehouseId: null, companyName: null, mustChangePassword: false },
]

const authSlice = createSlice({
  name: 'auth',
  initialState: {
    user: null,
    token: null,
    role: null,
    companyId: null,
    companyName: null,
    companyLogo: null,
    warehouseId: null,
    isAuthenticated: false,
    mustChangePassword: false,
    error: null,
    localUsers: [],
  },
  reducers: {
    login: (state, action) => {
      const { email, password } = action.payload
      const found =
        state.localUsers.find(u => u.email === email && u.password === password) ||
        MOCK_CREDENTIALS.find(u => u.email === email && u.password === password)
      if (found) {
        state.user = { id: found.id, name: found.name, email: found.email }
        state.token = `mock-jwt-${found.role}-${found.id}`
        state.role = found.role
        state.companyId = found.companyId
        state.companyName = found.companyName
        state.companyLogo = found.companyLogo || null
        state.warehouseId = found.warehouseId
        state.isAuthenticated = true
        state.mustChangePassword = found.mustChangePassword || false
        state.error = null
      } else {
        state.error = 'Invalid email or password'
      }
    },
    signup: (state, action) => {
      const { companyName, email, logo } = action.payload
      state.user = { id: 'user-new', name: `${companyName} Admin`, email }
      state.token = 'mock-jwt-ADMIN-user-new'
      state.role = 'ADMIN'
      state.companyId = 'comp-new'
      state.companyName = companyName
      state.companyLogo = logo || null
      state.warehouseId = null
      state.isAuthenticated = true
      state.mustChangePassword = false
      state.error = null
    },
    registerLocalUser: (state, action) => {
      const existing = state.localUsers.findIndex(u => u.email === action.payload.email)
      if (existing >= 0) {
        state.localUsers[existing] = { ...action.payload, mustChangePassword: true }
      } else {
        state.localUsers.push({ ...action.payload, mustChangePassword: true })
      }
    },
    changePassword: (state, action) => {
      const { newPassword } = action.payload
      const localUser = state.localUsers.find(u => u.id === state.user?.id)
      if (localUser) {
        localUser.password = newPassword
        localUser.mustChangePassword = false
      } else {
        const mockUser = MOCK_CREDENTIALS.find(u => u.id === state.user?.id)
        if (mockUser) {
          state.localUsers.push({ ...mockUser, password: newPassword, mustChangePassword: false })
        }
      }
      state.mustChangePassword = false
    },
    resetPassword: (state, action) => {
      const { email, newPassword } = action.payload
      const localUser = state.localUsers.find(u => u.email === email)
      if (localUser) localUser.password = newPassword
    },
    setCompanyLogo: (state, action) => {
      state.companyLogo = action.payload
    },
    logout: (state) => {
      state.user = null
      state.token = null
      state.role = null
      state.companyId = null
      state.companyName = null
      state.companyLogo = null
      state.warehouseId = null
      state.isAuthenticated = false
      state.mustChangePassword = false
      state.error = null
    },
    clearError: (state) => {
      state.error = null
    },
  },
})

export const { login, signup, logout, clearError, setCompanyLogo, registerLocalUser, changePassword, resetPassword } = authSlice.actions
export default authSlice.reducer
