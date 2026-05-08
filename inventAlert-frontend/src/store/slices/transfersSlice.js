import { createSlice } from '@reduxjs/toolkit'
import { transfers } from '../../data/mockData'

const transfersSlice = createSlice({
  name: 'transfers',
  initialState: { transfers },
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
  },
})

export const { approveTransfer, rejectTransfer, dispatchTransfer, acceptTransfer, rejectDelivery } = transfersSlice.actions
export default transfersSlice.reducer
