import { createSlice } from '@reduxjs/toolkit'
import { analytics } from '../../data/mockData'

const analyticsSlice = createSlice({
  name: 'analytics',
  initialState: analytics,
  reducers: {},
})

export default analyticsSlice.reducer
