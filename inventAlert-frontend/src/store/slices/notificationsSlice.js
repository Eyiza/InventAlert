import { createSlice } from '@reduxjs/toolkit'

const notificationsSlice = createSlice({
  name: 'notifications',
  initialState: { notifications: [], wsConnected: false },
  reducers: {
    markAsRead: (state, action) => {
      const n = state.notifications.find(x => x.id === action.payload)
      if (n) n.isRead = true
    },
    markAllAsRead: (state, action) => {
      state.notifications
        .filter(n => n.userId === action.payload)
        .forEach(n => { n.isRead = true })
    },
    addNotification: (state, action) => {
      state.notifications.unshift({
        ...action.payload,
        id: `notif-${Date.now()}`,
        isRead: false,
        createdAt: new Date().toISOString(),
      })
    },
    setWsConnected: (state, action) => {
      state.wsConnected = action.payload
    },
  },
})

export const { markAsRead, markAllAsRead, addNotification, setWsConnected } = notificationsSlice.actions
export default notificationsSlice.reducer
