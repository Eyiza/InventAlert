import { createSlice } from '@reduxjs/toolkit'

const transfersSlice = createSlice({
  name: 'transfers',
  initialState: { transfers: [] },
  reducers: {
    approveTransfer: (state, action) => {
      const t = state.transfers.find(x => x.id === action.payload.id)
      if (t) { t.status = 'APPROVED'; t.approvedBy = action.payload.userId; t.updatedAt = new Date().toISOString() }
    },
    rejectTransfer: (state, action) => {
      const t = state.transfers.find(x => x.id === action.payload)
      if (t) { t.status = 'REJECTED'; t.updatedAt = new Date().toISOString() }
    },
    dispatchTransfer: (state, action) => {
      const t = state.transfers.find(x => x.id === action.payload)
      if (t) { t.status = 'IN_TRANSIT'; t.updatedAt = new Date().toISOString() }
    },
    acceptTransfer: (state, action) => {
      const t = state.transfers.find(x => x.id === action.payload)
      if (t) { t.status = 'COMPLETED'; t.updatedAt = new Date().toISOString() }
    },
    rejectDelivery: (state, action) => {
      const t = state.transfers.find(x => x.id === action.payload)
      if (t) { t.status = 'DELIVERY_REJECTED'; t.updatedAt = new Date().toISOString() }
    },
    addTransfer: (state, action) => {
      const items = Array.isArray(action.payload) ? action.payload : [action.payload]
      const batchId = `batch-${Date.now()}`
      items.forEach((item, i) => {
        state.transfers.unshift({
          ...item,
          id: `trans-${Date.now()}-${i}`,
          batchId,
          status: 'SUGGESTED',
          approvedBy: null,
          distanceKm: null,
          distanceSource: 'MANUAL',
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
        })
      })
    },
  },
})

export const { approveTransfer, rejectTransfer, dispatchTransfer, acceptTransfer, rejectDelivery, addTransfer } = transfersSlice.actions
export default transfersSlice.reducer
