import { createSlice } from '@reduxjs/toolkit'
import { alerts } from '../../data/mockData'

const alertsSlice = createSlice({
  name: 'alerts',
  initialState: { alerts },
  reducers: {
    acknowledgeAlert: (state, action) => {
      const alert = state.alerts.find(a => a.id === action.payload.alertId)
      if (alert) {
        alert.status = 'ACKNOWLEDGED'
        alert.assignedTo = action.payload.userId
        alert.updatedAt = new Date().toISOString()
      }
    },
    markOrderPlaced: (state, action) => {
      const alert = state.alerts.find(a => a.id === action.payload)
      if (alert) { alert.status = 'ORDER_PLACED'; alert.updatedAt = new Date().toISOString() }
    },
    resolveAlert: (state, action) => {
      const alert = state.alerts.find(a => a.id === action.payload)
      if (alert) { alert.status = 'RESOLVED'; alert.updatedAt = new Date().toISOString() }
    },
    addAlert: (state, action) => {
      state.alerts.unshift({
        ...action.payload,
        id: `alert-${Date.now()}`,
        status: 'OPEN',
        assignedTo: null,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      })
    },
  },
})

export const { acknowledgeAlert, markOrderPlaced, resolveAlert, addAlert } = alertsSlice.actions
export default alertsSlice.reducer
