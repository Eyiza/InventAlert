import { createSlice } from '@reduxjs/toolkit'

const initialState = {
  user: null,
  token: null,
  role: null,
  companyId: null,
  companyName: null,
  companyLogo: null,
  warehouseId: null,
  isAuthenticated: false,
  mustChangePassword: false,
}

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    setCredentials: (state, { payload }) => {
      state.token = payload.token
      state.user = { id: payload.userId, email: payload.email || null }
      state.role = payload.role
      state.companyId = payload.companyId || null
      state.companyName = payload.companyName || null
      state.companyLogo = payload.companyLogo || null
      state.warehouseId = payload.warehouseId || null
      state.isAuthenticated = true
      state.mustChangePassword = false
    },
    logout: () => initialState,
    setCompanyLogo: (state, { payload }) => {
      state.companyLogo = payload
    },
    // Stubs — kept so existing imports in other components don't break
    changePassword: state => { state.mustChangePassword = false },
    registerLocalUser: () => {},
    clearError: () => {},
  },
})

export const { setCredentials, logout, setCompanyLogo, changePassword, registerLocalUser, clearError } = authSlice.actions
export default authSlice.reducer
