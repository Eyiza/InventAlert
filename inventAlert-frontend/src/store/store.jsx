import { configureStore, combineReducers } from '@reduxjs/toolkit'
import { setupListeners } from '@reduxjs/toolkit/query'
import { inventAlertApi } from '../apis/inventAlertApi'
import authReducer from './slices/authSlice'
import stockReducer from './slices/stockSlice'
import usersReducer from './slices/usersSlice'
import alertsReducer from './slices/alertsSlice'
import reconciliationsReducer from './slices/reconciliationsSlice'
import notificationsReducer from './slices/notificationsSlice'
import analyticsReducer from './slices/analyticsSlice'
import superadminReducer from './slices/superadminSlice'
import purchaseOrdersReducer from './slices/purchaseOrdersSlice'

const appReducer = combineReducers({
  auth: authReducer,
  stock: stockReducer,
  users: usersReducer,
  alerts: alertsReducer,
  reconciliations: reconciliationsReducer,
  notifications: notificationsReducer,
  analytics: analyticsReducer,
  superadmin: superadminReducer,
  purchaseOrders: purchaseOrdersReducer,
  [inventAlertApi.reducerPath]: inventAlertApi.reducer,
})

// On logout, pass undefined so every slice — including the RTK Query cache —
// resets to its own initialState. This prevents any tenant's data leaking into
// the next session.
const rootReducer = (state, action) => {
  if (action.type === 'auth/logout') {
    return appReducer(undefined, action)
  }
  return appReducer(state, action)
}

const store = configureStore({
  reducer: rootReducer,
  middleware: getDefaultMiddleware =>
    getDefaultMiddleware().concat(inventAlertApi.middleware),
})

setupListeners(store.dispatch)

export default store
