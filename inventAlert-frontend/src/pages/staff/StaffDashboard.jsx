import { useState } from 'react'
import { useSelector, useDispatch } from 'react-redux'
import { toast } from 'react-toastify'
import Layout from '../../components/layout/Layout'
import StatusBadge from '../../components/shared/StatusBadge'
import StatCard from '../../components/shared/StatCard'
import { addMovement } from '../../store/slices/stockSlice'
import { dispatchTransfer, acceptTransfer, rejectDelivery, addTransfer } from '../../store/slices/transfersSlice'
import { submitReconciliation } from '../../store/slices/reconciliationsSlice'

const fmtDT = d => new Date(d).toLocaleString('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })

// ── Shared page-level layout ──────────────────────────────────────────────────

function PageHeader({ title, subtitle, action }) {
  return (
    <div className="flex items-start justify-between mb-6">
      <div>
        <h2 className="text-xl font-bold text-gray-900">{title}</h2>
        {subtitle && <p className="text-sm text-gray-500 mt-0.5">{subtitle}</p>}
      </div>
      {action}
    </div>
  )
}

function FormSection({ children }) {
  return (
    <div className="max-w-2xl space-y-5">
      {children}
    </div>
  )
}

function Field({ label, hint, children, ...props }) {
  return (
    <div>
      <div className="flex items-center justify-between mb-1.5">
        <label className="block text-sm font-medium text-gray-700">{label}</label>
        {hint && <span className="text-xs text-gray-400">{hint}</span>}
      </div>
      {children || (
        <input className="w-full px-3.5 py-2.5 border border-gray-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-teal-600 focus:border-transparent bg-white" {...props} />
      )}
    </div>
  )
}

function Select({ label, hint, children: options, ...props }) {
  return (
    <div>
      <div className="flex items-center justify-between mb-1.5">
        <label className="block text-sm font-medium text-gray-700">{label}</label>
        {hint && <span className="text-xs text-gray-400">{hint}</span>}
      </div>
      <select className="w-full px-3.5 py-2.5 border border-gray-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-teal-600 bg-white" {...props}>
        {options}
      </select>
    </div>
  )
}

function SubmitBtn({ label, loading, color = 'teal' }) {
  const colors = { teal: 'bg-teal-600 hover:bg-teal-700', orange: 'bg-orange-500 hover:bg-orange-600', purple: 'bg-purple-600 hover:bg-purple-700', blue: 'bg-blue-600 hover:bg-blue-700' }
  return (
    <button type="submit" disabled={loading} className={`px-6 py-2.5 ${colors[color]} text-white font-semibold rounded-xl transition-colors flex items-center justify-center gap-2 disabled:opacity-70`}>
      {loading && <svg className="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" /><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" /></svg>}
      {label}
    </button>
  )
}

// ── Intake Panel ──────────────────────────────────────────────────────────────

const EMPTY_INTAKE = { productId: '', quantity: '', referenceNumber: '' }

function IntakePanel() {
  const { products, warehouses } = useSelector(s => s.stock)
  const { warehouseId, user } = useSelector(s => s.auth)
  const dispatch = useDispatch()
  const [rows, setRows] = useState([{ ...EMPTY_INTAKE }])
  const [loading, setLoading] = useState(false)
  const myWarehouse = warehouses.find(w => w.id === warehouseId)
  const activeProducts = products.filter(p => p.isActive)

  const updateRow = (i, field, val) => setRows(rs => rs.map((r, idx) => idx === i ? { ...r, [field]: val } : r))
  const removeRow = i => setRows(rs => rs.filter((_, idx) => idx !== i))

  const handleSubmit = e => {
    e.preventDefault()
    const valid = rows.filter(r => r.productId && r.quantity)
    if (!valid.length) return
    setLoading(true)
    setTimeout(() => {
      valid.forEach(r => dispatch(addMovement({ productId: r.productId, warehouseId, type: 'INTAKE', quantity: +r.quantity, referenceId: r.referenceNumber || null, createdBy: user.id })))
      toast.success(`${valid.length} intake${valid.length > 1 ? 's' : ''} recorded`)
      setRows([{ ...EMPTY_INTAKE }])
      setLoading(false)
    }, 500)
  }

  return (
    <div>
      <PageHeader
        title="Record Stock Intake"
        subtitle={`Adding stock to ${myWarehouse?.name || 'your warehouse'}`}
      />
      <form onSubmit={handleSubmit} className="max-w-2xl space-y-4">
        {rows.map((row, i) => (
          <div key={i} className="bg-gray-50 border border-gray-200 rounded-xl p-4">
            <div className="flex items-center justify-between mb-3">
              <span className="text-xs font-bold text-gray-400 uppercase tracking-widest">Item {String(i + 1).padStart(2, '0')}</span>
              <button type="button" onClick={() => removeRow(i)} disabled={rows.length === 1} className="w-7 h-7 flex items-center justify-center rounded-lg text-gray-300 hover:text-red-500 hover:bg-red-50 disabled:opacity-0 disabled:pointer-events-none transition-colors">
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /></svg>
              </button>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="col-span-2">
                <label className="block text-xs font-medium text-gray-500 mb-1">Product <span className="text-red-400">*</span></label>
                <select value={row.productId} onChange={e => updateRow(i, 'productId', e.target.value)} required className="w-full px-3 py-2.5 border border-gray-300 rounded-xl text-sm bg-white focus:outline-none focus:ring-2 focus:ring-teal-600">
                  <option value="">Select a product…</option>
                  {activeProducts.map(p => <option key={p.id} value={p.id}>{p.name} ({p.sku})</option>)}
                </select>
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-500 mb-1">Quantity <span className="text-red-400">*</span></label>
                <input type="number" min="1" value={row.quantity} onChange={e => updateRow(i, 'quantity', e.target.value)} placeholder="e.g. 50" required className="w-full px-3 py-2.5 border border-gray-300 rounded-xl text-sm bg-white focus:outline-none focus:ring-2 focus:ring-teal-600" />
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-500 mb-1">Reference <span className="text-gray-300 text-xs">(optional)</span></label>
                <input type="text" value={row.referenceNumber} onChange={e => updateRow(i, 'referenceNumber', e.target.value)} placeholder="PO-2026-001" className="w-full px-3 py-2.5 border border-gray-300 rounded-xl text-sm bg-white focus:outline-none focus:ring-2 focus:ring-teal-600" />
              </div>
            </div>
          </div>
        ))}
        <button type="button" onClick={() => setRows(rs => [...rs, { ...EMPTY_INTAKE }])} className="w-full py-2.5 border-2 border-dashed border-gray-300 rounded-xl text-sm text-gray-500 hover:border-teal-400 hover:text-teal-600 hover:bg-teal-50/40 transition-colors font-medium flex items-center justify-center gap-2">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" /></svg>
          Add another item
        </button>
        <div className="pt-1">
          <SubmitBtn label={`Record ${rows.length > 1 ? `${rows.filter(r => r.productId).length} Intakes` : 'Intake'}`} loading={loading} />
        </div>
      </form>
    </div>
  )
}

// ── Sale Panel ────────────────────────────────────────────────────────────────

function SalePanel() {
  const { products, stockLevels, warehouses } = useSelector(s => s.stock)
  const { warehouseId, user } = useSelector(s => s.auth)
  const dispatch = useDispatch()
  const [form, setForm] = useState({ productId: '', quantity: '' })
  const [stockError, setStockError] = useState('')
  const [loading, setLoading] = useState(false)
  const myWarehouse = warehouses.find(w => w.id === warehouseId)
  const activeProducts = products.filter(p => p.isActive)
  const selectedProduct = products.find(p => p.id === form.productId)
  const currentStock = form.productId ? stockLevels.find(sl => sl.productId === form.productId && sl.warehouseId === warehouseId)?.currentStock ?? 0 : null
  const ch = e => { setStockError(''); setForm(f => ({ ...f, [e.target.name]: e.target.value })) }

  const handleSubmit = e => {
    e.preventDefault()
    const qty = +form.quantity
    if (qty > (currentStock ?? 0)) { setStockError(`Only ${currentStock} ${selectedProduct?.unitOfMeasure || 'units'} available`); return }
    setLoading(true)
    setTimeout(() => {
      dispatch(addMovement({ productId: form.productId, warehouseId, type: 'OUTBOUND_SALE', quantity: qty, referenceId: null, createdBy: user.id }))
      toast.success('Outbound sale recorded')
      setForm({ productId: '', quantity: '' })
      setStockError('')
      setLoading(false)
    }, 500)
  }

  return (
    <div>
      <PageHeader title="Record Outbound Sale" subtitle={`Selling from ${myWarehouse?.name || 'your warehouse'}`} />
      <FormSection>
        <form onSubmit={handleSubmit} className="space-y-5">
          <Select label="Product" name="productId" value={form.productId} onChange={ch} required>
            <option value="">Select a product…</option>
            {activeProducts.map(p => <option key={p.id} value={p.id}>{p.name} ({p.sku})</option>)}
          </Select>
          {currentStock !== null && (
            <div className={`flex items-center gap-2.5 px-4 py-3 rounded-xl text-sm font-medium border ${currentStock === 0 ? 'bg-red-50 text-red-700 border-red-200' : currentStock < 10 ? 'bg-amber-50 text-amber-700 border-amber-200' : 'bg-teal-50 text-teal-700 border-teal-200'}`}>
              Current stock: <strong>{currentStock} {selectedProduct?.unitOfMeasure}</strong>
              {currentStock === 0 && ' — out of stock'}
            </div>
          )}
          {stockError && <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-xl text-sm">{stockError}</div>}
          <Field label="Quantity Being Sold" name="quantity" type="number" min="1" value={form.quantity} onChange={ch} placeholder="Units to sell" required />
          <div className="pt-1"><SubmitBtn label="Record Sale" loading={loading} color="orange" /></div>
        </form>
      </FormSection>
    </div>
  )
}

// ── Request Transfer Panel ────────────────────────────────────────────────────

const EMPTY_TRANSFER = { productId: '', quantity: '' }

function RequestTransferPanel() {
  const { products, warehouses, stockLevels } = useSelector(s => s.stock)
  const { warehouseId, user } = useSelector(s => s.auth)
  const dispatch = useDispatch()
  const [toWarehouseId, setToWarehouseId] = useState('')
  const [rows, setRows] = useState([{ ...EMPTY_TRANSFER }])
  const [loading, setLoading] = useState(false)
  const myWarehouse = warehouses.find(w => w.id === warehouseId)
  const otherWarehouses = warehouses.filter(w => w.isActive && w.id !== warehouseId)
  const activeProducts = products.filter(p => p.isActive)

  const updateRow = (i, field, val) => setRows(rs => rs.map((r, idx) => idx === i ? { ...r, [field]: val } : r))
  const removeRow = i => setRows(rs => rs.filter((_, idx) => idx !== i))

  const getStock = productId => stockLevels.find(sl => sl.productId === productId && sl.warehouseId === warehouseId)?.currentStock ?? 0

  const handleSubmit = e => {
    e.preventDefault()
    if (!toWarehouseId) return
    const valid = rows.filter(r => r.productId && r.quantity && +r.quantity > 0)
    if (!valid.length) return
    setLoading(true)
    setTimeout(() => {
      dispatch(addTransfer(valid.map(r => ({ productId: r.productId, fromWarehouseId: warehouseId, toWarehouseId, quantity: +r.quantity, requestedBy: user.id }))))
      toast.success(`Transfer request submitted for ${valid.length} item${valid.length > 1 ? 's' : ''} — awaiting manager approval`)
      setRows([{ ...EMPTY_TRANSFER }])
      setToWarehouseId('')
      setLoading(false)
    }, 500)
  }

  return (
    <div>
      <PageHeader title="Request Transfer Out" subtitle={`Sending stock from ${myWarehouse?.name || 'your warehouse'} to another`} />
      <form onSubmit={handleSubmit} className="max-w-2xl space-y-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1.5">Destination Warehouse <span className="text-red-400">*</span></label>
          <select value={toWarehouseId} onChange={e => setToWarehouseId(e.target.value)} required className="w-full px-3.5 py-2.5 border border-gray-300 rounded-xl text-sm bg-white focus:outline-none focus:ring-2 focus:ring-teal-600">
            <option value="">Select destination…</option>
            {otherWarehouses.map(w => <option key={w.id} value={w.id}>{w.name}</option>)}
          </select>
        </div>

        {rows.map((row, i) => {
          const avail = row.productId ? getStock(row.productId) : null
          const prod = products.find(p => p.id === row.productId)
          return (
            <div key={i} className="bg-gray-50 border border-gray-200 rounded-xl p-4">
              <div className="flex items-center justify-between mb-3">
                <span className="text-xs font-bold text-gray-400 uppercase tracking-widest">Item {String(i + 1).padStart(2, '0')}</span>
                <button type="button" onClick={() => removeRow(i)} disabled={rows.length === 1} className="w-7 h-7 flex items-center justify-center rounded-lg text-gray-300 hover:text-red-500 hover:bg-red-50 disabled:opacity-0 disabled:pointer-events-none">
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /></svg>
                </button>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div className="col-span-2">
                  <label className="block text-xs font-medium text-gray-500 mb-1">Product <span className="text-red-400">*</span></label>
                  <select value={row.productId} onChange={e => updateRow(i, 'productId', e.target.value)} required className="w-full px-3 py-2.5 border border-gray-300 rounded-xl text-sm bg-white focus:outline-none focus:ring-2 focus:ring-teal-600">
                    <option value="">Select a product…</option>
                    {activeProducts.map(p => <option key={p.id} value={p.id}>{p.name} ({p.sku})</option>)}
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-medium text-gray-500 mb-1">Quantity <span className="text-red-400">*</span></label>
                  <input type="number" min="1" max={avail ?? undefined} value={row.quantity} onChange={e => updateRow(i, 'quantity', e.target.value)} placeholder="Qty" required className="w-full px-3 py-2.5 border border-gray-300 rounded-xl text-sm bg-white focus:outline-none focus:ring-2 focus:ring-teal-600" />
                </div>
                {avail !== null && (
                  <div className="flex items-center">
                    <span className={`text-xs px-2.5 py-1 rounded-lg font-medium ${avail === 0 ? 'bg-red-50 text-red-600' : avail < 10 ? 'bg-amber-50 text-amber-600' : 'bg-teal-50 text-teal-600'}`}>
                      {avail} {prod?.unitOfMeasure || 'units'} available
                    </span>
                  </div>
                )}
              </div>
            </div>
          )
        })}

        <button type="button" onClick={() => setRows(rs => [...rs, { ...EMPTY_TRANSFER }])} className="w-full py-2.5 border-2 border-dashed border-gray-300 rounded-xl text-sm text-gray-500 hover:border-teal-400 hover:text-teal-600 hover:bg-teal-50/40 transition-colors font-medium flex items-center justify-center gap-2">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" /></svg>
          Add another item
        </button>
        <div className="pt-1"><SubmitBtn label={`Submit Transfer Request (${rows.filter(r => r.productId).length} item${rows.filter(r => r.productId).length !== 1 ? 's' : ''})`} loading={loading} color="blue" /></div>
      </form>
    </div>
  )
}

// ── Incoming Transfers Panel ──────────────────────────────────────────────────

function IncomingPanel() {
  const { transfers } = useSelector(s => s.transfers)
  const { products, warehouses } = useSelector(s => s.stock)
  const { warehouseId, user } = useSelector(s => s.auth)
  const dispatch = useDispatch()

  const incoming = transfers
    .filter(t => t.toWarehouseId === warehouseId && t.status === 'IN_TRANSIT')
    .map(t => ({ ...t, productName: products.find(p => p.id === t.productId)?.name || t.productId, fromName: warehouses.find(w => w.id === t.fromWarehouseId)?.name || t.fromWarehouseId }))

  const handleAccept = t => {
    dispatch(acceptTransfer(t.id))
    dispatch(addMovement({ productId: t.productId, warehouseId, type: 'TRANSFER_IN', quantity: t.quantity, referenceId: t.id, createdBy: user.id }))
    toast.success('Transfer accepted — stock updated')
  }

  return (
    <div>
      <PageHeader title="Incoming Transfers" subtitle="Shipments in transit headed to your warehouse" />
      <div className="space-y-4 max-w-2xl">
        {incoming.length === 0 ? (
          <div className="bg-white rounded-xl border border-gray-200 px-5 py-12 text-center text-gray-400">
            <svg className="w-10 h-10 text-gray-300 mx-auto mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" /></svg>
            <p className="text-sm">No incoming transfers awaiting your action.</p>
          </div>
        ) : incoming.map(t => (
          <div key={t.id} className="bg-white rounded-xl border border-gray-200 px-5 py-4 flex items-center justify-between gap-4">
            <div>
              <p className="font-semibold text-gray-900">{t.productName}</p>
              <p className="text-sm text-gray-500 mt-0.5">From: <strong className="text-gray-700">{t.fromName}</strong> · Qty: <strong className="text-gray-700">{t.quantity}</strong></p>
              <p className="text-xs text-gray-400 mt-0.5">{fmtDT(t.updatedAt)}</p>
            </div>
            <div className="flex gap-2 shrink-0">
              <button onClick={() => handleAccept(t)} className="px-3 py-1.5 bg-teal-600 text-white text-xs rounded-lg hover:bg-teal-700 font-medium">Accept</button>
              <button onClick={() => { dispatch(rejectDelivery(t.id)); toast.info('Delivery rejected') }} className="px-3 py-1.5 bg-red-50 text-red-600 text-xs rounded-lg hover:bg-red-100 font-medium">Reject</button>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}

// ── Outgoing Transfers Panel ──────────────────────────────────────────────────

function OutgoingPanel() {
  const { transfers } = useSelector(s => s.transfers)
  const { products, warehouses } = useSelector(s => s.stock)
  const { warehouseId, user } = useSelector(s => s.auth)
  const dispatch = useDispatch()

  const outgoing = transfers
    .filter(t => t.fromWarehouseId === warehouseId && t.status === 'APPROVED')
    .map(t => ({ ...t, productName: products.find(p => p.id === t.productId)?.name || t.productId, toName: warehouses.find(w => w.id === t.toWarehouseId)?.name || t.toWarehouseId }))

  const handleDispatch = t => {
    dispatch(dispatchTransfer(t.id))
    dispatch(addMovement({ productId: t.productId, warehouseId, type: 'TRANSFER_OUT', quantity: t.quantity, referenceId: t.id, createdBy: user.id }))
    toast.success('Dispatch confirmed — stock deducted, now in transit')
  }

  return (
    <div>
      <PageHeader title="Outgoing Transfers" subtitle="Approved transfers ready to be dispatched from your warehouse" />
      <div className="space-y-4 max-w-2xl">
        {outgoing.length === 0 ? (
          <div className="bg-white rounded-xl border border-gray-200 px-5 py-12 text-center text-gray-400">
            <p className="text-sm">No approved transfers awaiting dispatch.</p>
          </div>
        ) : outgoing.map(t => (
          <div key={t.id} className="bg-white rounded-xl border border-gray-200 px-5 py-4 flex items-center justify-between gap-4">
            <div>
              <p className="font-semibold text-gray-900">{t.productName}</p>
              <p className="text-sm text-gray-500 mt-0.5">To: <strong className="text-gray-700">{t.toName}</strong> · Qty: <strong className="text-gray-700">{t.quantity}</strong></p>
              <p className="text-xs text-gray-400 mt-0.5">Approved {fmtDT(t.updatedAt)}</p>
            </div>
            <button onClick={() => handleDispatch(t)} className="px-3 py-1.5 bg-blue-500 text-white text-xs rounded-lg hover:bg-blue-600 font-medium shrink-0">
              Confirm Dispatch
            </button>
          </div>
        ))}
      </div>
    </div>
  )
}

// ── Reconciliation Panel ──────────────────────────────────────────────────────

function ReconciliationPanel() {
  const { products, stockLevels, warehouses } = useSelector(s => s.stock)
  const { warehouseId, user } = useSelector(s => s.auth)
  const dispatch = useDispatch()
  const [form, setForm] = useState({ productId: '', physicalCount: '', reason: '' })
  const [loading, setLoading] = useState(false)
  const ch = e => setForm(f => ({ ...f, [e.target.name]: e.target.value }))
  const myWarehouse = warehouses.find(w => w.id === warehouseId)
  const activeProducts = products.filter(p => p.isActive)
  const systemCount = form.productId ? stockLevels.find(sl => sl.productId === form.productId && sl.warehouseId === warehouseId)?.currentStock ?? 0 : null
  const discrepancy = systemCount !== null && form.physicalCount !== '' ? +form.physicalCount - systemCount : null

  const handleSubmit = e => {
    e.preventDefault()
    if (!form.productId || !form.physicalCount || !form.reason) return
    const physical = +form.physicalCount
    setLoading(true)
    setTimeout(() => {
      dispatch(submitReconciliation({ productId: form.productId, warehouseId, systemCount, physicalCount: physical, discrepancy: physical - systemCount, reason: form.reason, createdBy: user.id }))
      toast.success('Reconciliation submitted — awaiting manager review')
      setForm({ productId: '', physicalCount: '', reason: '' })
      setLoading(false)
    }, 500)
  }

  return (
    <div>
      <PageHeader title="Submit Reconciliation" subtitle={`Physical count for ${myWarehouse?.name || 'your warehouse'}`} />
      <FormSection>
        <form onSubmit={handleSubmit} className="space-y-5">
          <Select label="Product" name="productId" value={form.productId} onChange={ch} required>
            <option value="">Select a product…</option>
            {activeProducts.map(p => <option key={p.id} value={p.id}>{p.name} ({p.sku})</option>)}
          </Select>
          {systemCount !== null && (
            <div className="flex items-center gap-3 bg-gray-50 border border-gray-200 rounded-xl px-4 py-3">
              <svg className="w-5 h-5 text-gray-400 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" /></svg>
              <div>
                <p className="text-xs text-gray-500">System count</p>
                <p className="font-semibold text-gray-900 text-sm">{systemCount} units</p>
              </div>
            </div>
          )}
          <Field label="Physical Count" hint="what you counted on-site" name="physicalCount" type="number" min="0" value={form.physicalCount} onChange={ch} placeholder="Actual units counted" required />
          {discrepancy !== null && (
            <div className={`flex items-center gap-2.5 px-4 py-3 rounded-xl text-sm font-medium border ${discrepancy < 0 ? 'bg-red-50 text-red-700 border-red-200' : discrepancy > 0 ? 'bg-teal-50 text-teal-700 border-teal-200' : 'bg-gray-50 text-gray-600 border-gray-200'}`}>
              <span className="font-bold text-base">{discrepancy > 0 ? '+' : ''}{discrepancy}</span>
              <span>{discrepancy < 0 && 'units missing'}{discrepancy > 0 && 'extra units found'}{discrepancy === 0 && 'counts match — no discrepancy'}</span>
            </div>
          )}
          <Field label="Reason / Explanation">
            <textarea name="reason" value={form.reason} onChange={ch} rows={3} className="w-full px-3.5 py-2.5 border border-gray-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-teal-600 resize-none" placeholder="Describe the reason for the discrepancy…" required />
          </Field>
          <div className="pt-1"><SubmitBtn label="Submit for Manager Review" loading={loading} color="purple" /></div>
        </form>
      </FormSection>
    </div>
  )
}

// ── Main ──────────────────────────────────────────────────────────────────────

export default function StaffDashboard() {
  const [activeTab, setActiveTab] = useState('intake')
  const { transfers } = useSelector(s => s.transfers)
  const { warehouseId } = useSelector(s => s.auth)
  const incomingCount = transfers.filter(t => t.toWarehouseId === warehouseId && t.status === 'IN_TRANSIT').length
  const outgoingCount = transfers.filter(t => t.fromWarehouseId === warehouseId && t.status === 'APPROVED').length

  const navItems = [
    { id: 'intake', label: 'Record Intake', badge: 0, icon: <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 13l-7 7-7-7m14-8l-7 7-7-7" /></svg> },
    { id: 'sale', label: 'Record Sale', badge: 0, icon: <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 11l7-7 7 7M5 19l7-7 7 7" /></svg> },
    { id: 'request-transfer', label: 'Request Transfer', badge: 0, icon: <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" /></svg> },
    { id: 'incoming', label: 'Incoming', badge: incomingCount, icon: <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" /></svg> },
    { id: 'outgoing', label: 'Outgoing', badge: outgoingCount, icon: <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4" /></svg> },
    { id: 'reconciliation', label: 'Reconciliation', badge: 0, icon: <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4" /></svg> },
  ]

  return (
    <Layout title="Warehouse Staff" navItems={navItems} activeTab={activeTab} onTabChange={setActiveTab}>
      {activeTab === 'intake' && <IntakePanel />}
      {activeTab === 'sale' && <SalePanel />}
      {activeTab === 'request-transfer' && <RequestTransferPanel />}
      {activeTab === 'incoming' && <IncomingPanel />}
      {activeTab === 'outgoing' && <OutgoingPanel />}
      {activeTab === 'reconciliation' && <ReconciliationPanel />}
    </Layout>
  )
}
