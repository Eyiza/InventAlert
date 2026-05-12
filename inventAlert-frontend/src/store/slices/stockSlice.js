import { createSlice } from '@reduxjs/toolkit'

const stockSlice = createSlice({
  name: 'stock',
  initialState: { warehouses: [], products: [], stockLevels: [], movements: [] },
  reducers: {
    addWarehouse: (state, action) => {
      state.warehouses.push({
        ...action.payload,
        id: `wh-${Date.now()}`,
        isActive: true,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      })
    },
    updateWarehouse: (state, action) => {
      const idx = state.warehouses.findIndex(w => w.id === action.payload.id)
      if (idx >= 0) state.warehouses[idx] = { ...state.warehouses[idx], ...action.payload, updatedAt: new Date().toISOString() }
    },
    toggleWarehouseActive: (state, action) => {
      const wh = state.warehouses.find(w => w.id === action.payload)
      if (wh) { wh.isActive = !wh.isActive; wh.updatedAt = new Date().toISOString() }
    },
    addProduct: (state, action) => {
      state.products.push({
        ...action.payload,
        id: `prod-${Date.now()}`,
        isActive: true,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      })
    },
    updateProduct: (state, action) => {
      const idx = state.products.findIndex(p => p.id === action.payload.id)
      if (idx >= 0) state.products[idx] = { ...state.products[idx], ...action.payload, updatedAt: new Date().toISOString() }
    },
    toggleProductActive: (state, action) => {
      const prod = state.products.find(p => p.id === action.payload)
      if (prod) { prod.isActive = !prod.isActive; prod.updatedAt = new Date().toISOString() }
    },
    addMovement: (state, action) => {
      const movement = { ...action.payload, id: `mov-${Date.now()}`, createdAt: new Date().toISOString() }
      state.movements.unshift(movement)
      const slIdx = state.stockLevels.findIndex(
        sl => sl.productId === movement.productId && sl.warehouseId === movement.warehouseId
      )
      if (slIdx >= 0) {
        if (movement.type === 'INTAKE' || movement.type === 'TRANSFER_IN' || movement.type === 'RECONCILIATION') {
          state.stockLevels[slIdx].currentStock += movement.quantity
        } else if (movement.type === 'OUTBOUND_SALE' || movement.type === 'TRANSFER_OUT') {
          state.stockLevels[slIdx].currentStock = Math.max(0, state.stockLevels[slIdx].currentStock - movement.quantity)
        }
        state.stockLevels[slIdx].updatedAt = new Date().toISOString()
      } else {
        state.stockLevels.push({
          id: `sl-${Date.now()}`,
          productId: movement.productId,
          warehouseId: movement.warehouseId,
          currentStock: movement.quantity,
          threshold: 0,
          velocityPerDay: 0,
          daysUntilEmpty: null,
          updatedAt: new Date().toISOString(),
        })
      }
    },
    updateThreshold: (state, action) => {
      const { productId, warehouseId, threshold } = action.payload
      const sl = state.stockLevels.find(s => s.productId === productId && s.warehouseId === warehouseId)
      if (sl) { sl.threshold = threshold; sl.updatedAt = new Date().toISOString() }
    },
  },
})

export const { addWarehouse, updateWarehouse, toggleWarehouseActive, addProduct, updateProduct, toggleProductActive, addMovement, updateThreshold } = stockSlice.actions
export default stockSlice.reducer
