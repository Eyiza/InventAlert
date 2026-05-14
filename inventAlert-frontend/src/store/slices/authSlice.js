import { createSlice } from '@reduxjs/toolkit'

const STORAGE_KEY = 'inventalert_auth'

const loadFromStorage = () => {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return null
    const { token, user, role, companyId, companyName, companyLogo, warehouseId, mustChangePassword } = JSON.parse(raw)
    if (!token) return null
    return { token, user, role, companyId, companyName, companyLogo, warehouseId, isAuthenticated: true, mustChangePassword: mustChangePassword ?? false }
  } catch {
    return null
  }
}

const saveToStorage = (state) => {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({
      token: state.token, user: state.user, role: state.role,
      companyId: state.companyId, companyName: state.companyName,
      companyLogo: state.companyLogo, warehouseId: state.warehouseId,
      mustChangePassword: state.mustChangePassword,
    }))
  } catch {}
}

const clearStorage = () => {
  try { localStorage.removeItem(STORAGE_KEY) } catch {}
}

const defaultState = {
  user: null, token: null, role: null,
  companyId: null, companyName: null, companyLogo: null,
  warehouseId: null, isAuthenticated: false, mustChangePassword: false,
}

const initialState = loadFromStorage() ?? defaultState

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    setCredentials: (state, { payload }) => {
      state.token = payload.token
      state.user = { id: payload.userId, name: payload.name || null, email: payload.email || null }
      state.role = payload.role
      state.companyId = payload.companyId || null
      state.companyName = payload.companyName || null
      state.companyLogo = payload.companyLogo || null
      state.warehouseId = payload.warehouseId || null
      state.isAuthenticated = true
      state.mustChangePassword = payload.mustChangePassword ?? false
      saveToStorage(state)
    },
    logout: () => {
      clearStorage()
      return defaultState
    },
    setCompanyLogo: (state, { payload }) => {
      state.companyLogo = payload
      saveToStorage(state)
    },
    setCompanyName: (state, { payload }) => {
      state.companyName = payload
      saveToStorage(state)
    },
    // Stubs — kept so existing imports in other components don't break
    changePassword: state => { state.mustChangePassword = false },
    registerLocalUser: () => {},
    clearError: () => {},
  },
})

export const { setCredentials, logout, setCompanyLogo, setCompanyName, changePassword, registerLocalUser, clearError } = authSlice.actions
export default authSlice.reducer
