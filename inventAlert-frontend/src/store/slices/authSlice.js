import { createSlice } from '@reduxjs/toolkit'

const MOCK_CREDENTIALS = [
  { id: 'user-1', companyId: 'comp-1', email: 'admin@techwave.com', password: 'password123', name: 'Alice Johnson', role: 'ADMIN', warehouseId: null, companyName: 'TechWave Logistics' },
  { id: 'user-2', companyId: 'comp-1', email: 'manager@techwave.com', password: 'password123', name: 'Bob Smith', role: 'MANAGER', warehouseId: null, companyName: 'TechWave Logistics' },
  { id: 'user-3', companyId: 'comp-1', email: 'staff.a@techwave.com', password: 'password123', name: 'Charlie Brown', role: 'WAREHOUSE_STAFF', warehouseId: 'wh-1', companyName: 'TechWave Logistics' },
  { id: 'user-4', companyId: 'comp-1', email: 'staff.b@techwave.com', password: 'password123', name: 'Diana Ross', role: 'WAREHOUSE_STAFF', warehouseId: 'wh-2', companyName: 'TechWave Logistics' },
  { id: 'user-5', companyId: 'comp-1', email: 'procurement@techwave.com', password: 'password123', name: 'Eve Martinez', role: 'PROCUREMENT_OFFICER', warehouseId: null, companyName: 'TechWave Logistics' },
  { id: 'superadmin', companyId: null, email: 'superadmin@inventalert.com', password: 'superadmin123', name: 'Platform Admin', role: 'SUPERADMIN', warehouseId: null, companyName: null },
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
    error: null,
  },
  reducers: {
    login: (state, action) => {
      const { email, password } = action.payload
      const found = MOCK_CREDENTIALS.find(u => u.email === email && u.password === password)
      if (found) {
        state.user = { id: found.id, name: found.name, email: found.email }
        state.token = `mock-jwt-${found.role}-${found.id}`
        state.role = found.role
        state.companyId = found.companyId
        state.companyName = found.companyName
        state.warehouseId = found.warehouseId
        state.isAuthenticated = true
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
      state.error = null
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
      state.error = null
    },
    clearError: (state) => {
      state.error = null
    },
  },
})

export const { login, signup, logout, clearError, setCompanyLogo } = authSlice.actions
export default authSlice.reducer
