import { createSlice } from '@reduxjs/toolkit'

const purchaseOrdersSlice = createSlice({
  name: 'purchaseOrders',
  initialState: { purchaseOrders: [] },
  reducers: {
    createPO: (state, action) => {
      state.purchaseOrders.unshift({
        id: `po-${Date.now()}`,
        status: 'ORDERED',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        ...action.payload,
      })
    },
    receivePO: (state, action) => {
      const po = state.purchaseOrders.find(p => p.id === action.payload)
      if (po) { po.status = 'RECEIVED'; po.updatedAt = new Date().toISOString() }
    },
    cancelPO: (state, action) => {
      const po = state.purchaseOrders.find(p => p.id === action.payload)
      if (po) { po.status = 'CANCELLED'; po.updatedAt = new Date().toISOString() }
    },
  },
})

export const { createPO, receivePO, cancelPO } = purchaseOrdersSlice.actions
export default purchaseOrdersSlice.reducer
