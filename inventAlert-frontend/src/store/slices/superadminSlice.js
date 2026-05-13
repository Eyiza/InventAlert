import { createSlice } from '@reduxjs/toolkit'

const superadminSlice = createSlice({
  name: 'superadmin',
  initialState: { companies: [], complaints: [] },
  reducers: {
    suspendCompany: (state, action) => {
      const c = state.companies.find(x => x.id === action.payload)
      if (c) c.status = 'SUSPENDED'
    },
    reactivateCompany: (state, action) => {
      const c = state.companies.find(x => x.id === action.payload)
      if (c) c.status = 'ACTIVE'
    },
    addCompany: (state, action) => {
      state.companies.unshift({
        ...action.payload,
        id: `comp-${Date.now()}`,
        status: 'ACTIVE',
        logo: null,
        createdAt: new Date().toISOString(),
      })
    },
    updateCompanyLogo: (state, action) => {
      const { companyId, logo } = action.payload
      const c = state.companies.find(x => x.id === companyId)
      if (c) c.logo = logo
    },
    resolveComplaint: (state, action) => {
      const t = state.complaints.find(x => x.id === action.payload)
      if (t) t.status = 'RESOLVED'
    },
    reviewComplaint: (state, action) => {
      const t = state.complaints.find(x => x.id === action.payload)
      if (t) t.status = 'IN_REVIEW'
    },
    submitComplaint: (state, action) => {
      state.complaints.unshift({
        ...action.payload,
        id: `complaint-${Date.now()}`,
        status: 'OPEN',
        createdAt: new Date().toISOString(),
      })
    },
  },
})

export const { suspendCompany, reactivateCompany, addCompany, updateCompanyLogo, resolveComplaint, reviewComplaint, submitComplaint } = superadminSlice.actions
export default superadminSlice.reducer
