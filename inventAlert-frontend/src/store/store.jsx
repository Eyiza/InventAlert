import { configureStore } from '@reduxjs/toolkit'
import authReducer from './slices/authSlice'
import stockReducer from './slices/stockSlice'
import usersReducer from './slices/usersSlice'
import alertsReducer from './slices/alertsSlice'
import transfersReducer from './slices/transfersSlice'
import reconciliationsReducer from './slices/reconciliationsSlice'
import notificationsReducer from './slices/notificationsSlice'
import analyticsReducer from './slices/analyticsSlice'
import superadminReducer from './slices/superadminSlice'
import purchaseOrdersReducer from './slices/purchaseOrdersSlice'

const store = configureStore({
  reducer: {
    auth: authReducer,
    stock: stockReducer,
    users: usersReducer,
    alerts: alertsReducer,
    transfers: transfersReducer,
    reconciliations: reconciliationsReducer,
    notifications: notificationsReducer,
    analytics: analyticsReducer,
    superadmin: superadminReducer,
    purchaseOrders: purchaseOrdersReducer,
  },
  middleware: (getDefaultMiddleware) => getDefaultMiddleware(),
})

export default store