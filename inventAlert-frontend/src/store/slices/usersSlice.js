import { createSlice } from '@reduxjs/toolkit'

const usersSlice = createSlice({
  name: 'users',
  initialState: { users: [], warehouseAssignments: [] },
  reducers: {
    addUser: (state, action) => {
      state.users.push({
        id: action.payload.id || `user-${Date.now()}`,
        ...action.payload,
        isActive: true,
        createdAt: new Date().toISOString(),
      })
    },
    updateUserRole: (state, action) => {
      const user = state.users.find(u => u.id === action.payload.id)
      if (user) user.role = action.payload.role
    },
    deactivateUser: (state, action) => {
      const user = state.users.find(u => u.id === action.payload)
      if (user) user.isActive = false
    },
    reactivateUser: (state, action) => {
      const user = state.users.find(u => u.id === action.payload)
      if (user) user.isActive = true
    },
    assignWarehouse: (state, action) => {
      const exists = state.warehouseAssignments.find(
        a => a.userId === action.payload.userId && a.warehouseId === action.payload.warehouseId
      )
      if (!exists) {
        state.warehouseAssignments.push({
          ...action.payload,
          id: `assign-${Date.now()}`,
          assignedAt: new Date().toISOString(),
        })
      }
    },
    removeAssignment: (state, action) => {
      state.warehouseAssignments = state.warehouseAssignments.filter(a => a.id !== action.payload)
    },
  },
})

export const { addUser, updateUserRole, deactivateUser, reactivateUser, assignWarehouse, removeAssignment } = usersSlice.actions
export default usersSlice.reducer
