import { createSlice } from '@reduxjs/toolkit'

const reconciliationsSlice = createSlice({
  name: 'reconciliations',
  initialState: { reconciliations: [] },
  reducers: {
    submitReconciliation: (state, action) => {
      state.reconciliations.unshift({
        ...action.payload,
        id: `rec-${Date.now()}`,
        status: 'PENDING_APPROVAL',
        approvedBy: null,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      })
    },
    approveReconciliation: (state, action) => {
      const rec = state.reconciliations.find(r => r.id === action.payload.id)
      if (rec) {
        rec.status = 'APPROVED'
        rec.approvedBy = action.payload.userId
        rec.updatedAt = new Date().toISOString()
      }
    },
    rejectReconciliation: (state, action) => {
      const rec = state.reconciliations.find(r => r.id === action.payload)
      if (rec) { rec.status = 'REJECTED'; rec.updatedAt = new Date().toISOString() }
    },
  },
})

export const { submitReconciliation, approveReconciliation, rejectReconciliation } = reconciliationsSlice.actions
export default reconciliationsSlice.reducer
