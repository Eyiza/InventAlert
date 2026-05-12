import { createSlice } from '@reduxjs/toolkit'

const analyticsSlice = createSlice({
  name: 'analytics',
  initialState: {
    stockVelocity: [],
    lowStockForecast: [],
    reorderRecommendations: [],
    alertFrequency: [],
    transferEfficiency: [],
    movementSummary: [],
  },
  reducers: {},
})

export default analyticsSlice.reducer
