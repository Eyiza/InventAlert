import { useState } from 'react'
import { useSelector, useDispatch } from 'react-redux'
import { toast } from 'react-toastify'
import Layout from '../../components/layout/Layout'
import StatusBadge from '../../components/shared/StatusBadge'
import StatCard from '../../components/shared/StatCard'
import { addMovement } from '../../store/slices/stockSlice'
import { dispatchTransfer, acceptTransfer, rejectDelivery } from '../../store/slices/transfersSlice'
import { submitReconciliation } from '../../store/slices/reconciliationsSlice'

const fmtDT = d => new Date(d).toLocaleString('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })

function FormCard({ icon, iconBg, iconColor, title, subtitle, children }) {
  return (
    <div className="max-w-2xl mx-auto">
      <div className="bg-white rounded-2xl border border-gray-200 shadow-sm overflow-hidden">
        <div className="bg-linear-to-r from-teal-600 to-teal-500 px-6 py-5">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-white/20 flex items-center justify-center shrink-0">
              <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                {icon}
              </svg>
            </div>
            <div>
              <h2 className="text-white font-semibold text-base">{title}</h2>
              <p className="text-teal-100 text-sm">{subtitle}</p>
            </div>
          </div>
        </div>
        <div className="p-6">{children}</div>
      </div>
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
        <input className="w-full px-3.5 py-2.5 border border-gray-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-teal-600 focus:border-transparent transition-shadow bg-white" {...props} />
      )}
    </div>
  )
}

function Select({ label, children: options, ...props }) {
  return (
    <div>
      <label className="block text-sm font-medium text-gray-700 mb-1.5">{label}</label>
      <select className="w-full px-3.5 py-2.5 border border-gray-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-teal-600 bg-white" {...props}>
        {options}
      </select>
    </div>
  )
}

function SubmitBtn({ label, loading, color = 'teal' }) {
  const colors = {
    teal: 'bg-teal-600 hover:bg-teal-700',
    orange: 'bg-orange-500 hover:bg-orange-600',
    purple: 'bg-purple-600 hover:bg-purple-700',
  }
  return (
    <button
      type="submit"
      disabled={loading}
      className={`w-full py-3 ${colors[color]} text-white font-semibold rounded-xl transition-colors flex items-center justify-center gap-2 disabled:opacity-70`}
    >
      {loading && (
        <svg className="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
        </svg>
      )}
      {label}
    </button>
  )
}

// ── Intake Panel ──────────────────────────────────────────────────────────────

function IntakePanel() {
  const { products, warehouses } = useSelector(s => s.stock)
  const { warehouseId, user } = useSelector(s => s.auth)
  const dispatch = useDispatch()
  const [form, setForm] = useState({ productId: '', quantity: '', referenceNumber: '' })
  const [loading, setLoading] = useState(false)
  const ch = e => setForm(f => ({ ...f, [e.target.name]: e.target.value }))

  const myWarehouse = warehouses.find(w => w.id === warehouseId)
  const activeProducts = products.filter(p => p.isActive)

  const handleSubmit = e => {
    e.preventDefault()
    if (!form.productId) return
    setLoading(true)
    setTimeout(() => {
      dispatch(addMovement({ productId: form.productId, warehouseId, type: 'INTAKE', quantity: +form.quantity, referenceId: form.referenceNumber || null, createdBy: user.id }))
      toast.success('Stock intake recorded successfully')
      setForm({ productId: '', quantity: '', referenceNumber: '' })
      setLoading(false)
    }, 500)
  }

  return (
    <FormCard
      icon={<path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 13l-7 7-7-7m14-8l-7 7-7-7" />}
      title="Record Stock Intake"
      subtitle={`Recording to ${myWarehouse?.name || warehouseId}`}
    >
      <form onSubmit={handleSubmit} className="space-y-5">
        <Select label="Product" name="productId" value={form.productId} onChange={ch} required>
          <option value="">Select a product…</option>
          {activeProducts.map(p => <option key={p.id} value={p.id}>{p.name} ({p.sku})</option>)}
        </Select>
        <div className="grid grid-cols-2 gap-4">
          <Field label="Quantity Received" name="quantity" type="number" min="1" value={form.quantity} onChange={ch} placeholder="e.g. 50" required />
          <Field label="Reference Number" hint="optional" name="referenceNumber" value={form.referenceNumber} onChange={ch} placeholder="PO-2026-001" />
        </div>
        <div className="pt-1">
          <SubmitBtn label="Record Intake" loading={loading} />
        </div>
      </form>
    </FormCard>
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
  const currentStock = form.productId
    ? stockLevels.find(sl => sl.productId === form.productId && sl.warehouseId === warehouseId)?.currentStock ?? 0
    : null

  const ch = e => { setStockError(''); setForm(f => ({ ...f, [e.target.name]: e.target.value })) }

  const handleSubmit = e => {
    e.preventDefault()
    const qty = +form.quantity
    const stock = currentStock ?? 0
    if (qty > stock) { setStockError(`Only ${stock} ${selectedProduct?.unitOfMeasure || 'units'} available in stock`); return }
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
    <FormCard
      icon={<path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 11l7-7 7 7M5 19l7-7 7 7" />}
      title="Record Outbound Sale"
      subtitle={`Recording from ${myWarehouse?.name || warehouseId}`}
    >
      <form onSubmit={handleSubmit} className="space-y-5">
        <Select label="Product" name="productId" value={form.productId} onChange={ch} required>
          <option value="">Select a product…</option>
          {activeProducts.map(p => <option key={p.id} value={p.id}>{p.name} ({p.sku})</option>)}
        </Select>

        {currentStock !== null && (
          <div className={`flex items-center gap-2.5 px-4 py-3 rounded-xl text-sm font-medium border ${
            currentStock === 0 ? 'bg-red-50 text-red-700 border-red-200' :
            currentStock < 10 ? 'bg-amber-50 text-amber-700 border-amber-200' :
            'bg-teal-50 text-teal-700 border-teal-200'
          }`}>
            <svg className="w-4 h-4 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            Current stock: <strong>{currentStock} {selectedProduct?.unitOfMeasure}</strong>
            {currentStock === 0 && ' — out of stock'}
          </div>
        )}

        {stockError && (
          <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-xl text-sm flex items-center gap-2">
            <svg className="w-4 h-4 shrink-0" fill="currentColor" viewBox="0 0 20 20">
              <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
            </svg>
            {stockError}
          </div>
        )}

        <Field label="Quantity Being Sold" name="quantity" type="number" min="1" value={form.quantity} onChange={ch} placeholder="Units to sell" required />
        <div className="pt-1">
          <SubmitBtn label="Record Sale" loading={loading} color="orange" />
        </div>
      </form>
    </FormCard>
  )
}

// ── Incoming Transfers Panel ──────────────────────────────────────────────────

function IncomingPanel() {
  const { transfers } = useSelector(s => s.transfers)
  const { products, warehouses } = useSelector(s => s.stock)
  const { warehouseId } = useSelector(s => s.auth)
  const dispatch = useDispatch()

  const incoming = transfers
    .filter(t => t.toWarehouseId === warehouseId && t.status === 'IN_TRANSIT')
    .map(t => ({
      ...t,
      productName: products.find(p => p.id === t.productId)?.name || t.productId,
      fromName: warehouses.find(w => w.id === t.fromWarehouseId)?.name || t.fromWarehouseId,
    }))

  return (
    <div className="space-y-4">
      <StatCard title="Incoming Transfers (In Transit)" value={incoming.length} color={incoming.length > 0 ? 'blue' : 'teal'} />
      <div className="bg-white rounded-xl border border-gray-200">
        <div className="px-5 py-4 border-b border-gray-100">
          <h3 className="font-semibold text-gray-900">Incoming Transfers — Accept or Reject</h3>
        </div>
        {incoming.length === 0 ? (
          <div className="px-5 py-12 text-center text-gray-400">
            <svg className="w-10 h-10 text-gray-300 mx-auto mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" />
            </svg>
            <p className="text-sm">No incoming transfers awaiting your action.</p>
          </div>
        ) : (
          <div className="divide-y divide-gray-100">
            {incoming.map(t => (
              <div key={t.id} className="px-5 py-4 flex items-center justify-between gap-4">
                <div className="min-w-0">
                  <p className="font-medium text-gray-900">{t.productName}</p>
                  <p className="text-sm text-gray-500 mt-0.5">From: <strong className="text-gray-700">{t.fromName}</strong> · Qty: <strong className="text-gray-700">{t.quantity}</strong></p>
                  <p className="text-xs text-gray-400 mt-0.5">{fmtDT(t.updatedAt)}</p>
                </div>
                <div className="flex gap-2 shrink-0">
                  <button onClick={() => { dispatch(acceptTransfer(t.id)); toast.success('Transfer accepted — stock updated') }} className="px-3 py-1.5 bg-teal-600 text-white text-xs rounded-lg hover:bg-teal-700 font-medium">Accept</button>
                  <button onClick={() => { dispatch(rejectDelivery(t.id)); toast.info('Delivery rejected') }} className="px-3 py-1.5 bg-red-50 text-red-600 text-xs rounded-lg hover:bg-red-100 font-medium">Reject</button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

// ── Outgoing Transfers Panel ──────────────────────────────────────────────────

function OutgoingPanel() {
  const { transfers } = useSelector(s => s.transfers)
  const { products, warehouses } = useSelector(s => s.stock)
  const { warehouseId } = useSelector(s => s.auth)
  const dispatch = useDispatch()

  const outgoing = transfers
    .filter(t => t.fromWarehouseId === warehouseId && t.status === 'APPROVED')
    .map(t => ({
      ...t,
      productName: products.find(p => p.id === t.productId)?.name || t.productId,
      toName: warehouses.find(w => w.id === t.toWarehouseId)?.name || t.toWarehouseId,
    }))

  return (
    <div className="space-y-4">
      <StatCard title="Outgoing Transfers (Awaiting Dispatch)" value={outgoing.length} color={outgoing.length > 0 ? 'amber' : 'teal'} />
      <div className="bg-white rounded-xl border border-gray-200">
        <div className="px-5 py-4 border-b border-gray-100">
          <h3 className="font-semibold text-gray-900">Approved Transfers — Confirm Dispatch</h3>
        </div>
        {outgoing.length === 0 ? (
          <div className="px-5 py-12 text-center text-gray-400">
            <p className="text-sm">No approved transfers awaiting dispatch.</p>
          </div>
        ) : (
          <div className="divide-y divide-gray-100">
            {outgoing.map(t => (
              <div key={t.id} className="px-5 py-4 flex items-center justify-between gap-4">
                <div className="min-w-0">
                  <p className="font-medium text-gray-900">{t.productName}</p>
                  <p className="text-sm text-gray-500 mt-0.5">To: <strong className="text-gray-700">{t.toName}</strong> · Qty: <strong className="text-gray-700">{t.quantity}</strong></p>
                  <p className="text-xs text-gray-400 mt-0.5">Approved {fmtDT(t.updatedAt)}</p>
                </div>
                <button onClick={() => { dispatch(dispatchTransfer(t.id)); toast.success('Dispatch confirmed — now in transit') }} className="px-3 py-1.5 bg-blue-500 text-white text-xs rounded-lg hover:bg-blue-600 font-medium shrink-0">
                  Confirm Dispatch
                </button>
              </div>
            ))}
          </div>
        )}
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

  const systemCount = form.productId
    ? stockLevels.find(sl => sl.productId === form.productId && sl.warehouseId === warehouseId)?.currentStock ?? 0
    : null

  const discrepancy = systemCount !== null && form.physicalCount !== '' ? +form.physicalCount - systemCount : null

  const handleSubmit = e => {
    e.preventDefault()
    if (!form.productId || !form.physicalCount || !form.reason) return
    const physical = +form.physicalCount
    setLoading(true)
    setTimeout(() => {
      dispatch(submitReconciliation({ productId: form.productId, warehouseId, systemCount, physicalCount: physical, discrepancy: physical - systemCount, reason: form.reason, createdBy: user.id }))
      toast.success('Reconciliation request submitted — pending manager review')
      setForm({ productId: '', physicalCount: '', reason: '' })
      setLoading(false)
    }, 500)
  }

  return (
    <FormCard
      icon={<path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4" />}
      title="Submit Reconciliation Request"
      subtitle={`Physical count for ${myWarehouse?.name || warehouseId}`}
    >
      <form onSubmit={handleSubmit} className="space-y-5">
        <Select label="Product" name="productId" value={form.productId} onChange={ch} required>
          <option value="">Select a product…</option>
          {activeProducts.map(p => <option key={p.id} value={p.id}>{p.name} ({p.sku})</option>)}
        </Select>

        {systemCount !== null && (
          <div className="flex items-center gap-3 bg-gray-50 border border-gray-200 rounded-xl px-4 py-3">
            <svg className="w-5 h-5 text-gray-400 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
            </svg>
            <div>
              <p className="text-xs text-gray-500">System count</p>
              <p className="font-semibold text-gray-900 text-sm">{systemCount} units</p>
            </div>
          </div>
        )}

        <Field label="Physical Count" hint="what you counted on-site" name="physicalCount" type="number" min="0" value={form.physicalCount} onChange={ch} placeholder="Actual units counted" required />

        {discrepancy !== null && (
          <div className={`flex items-center gap-2.5 px-4 py-3 rounded-xl text-sm font-medium border ${
            discrepancy < 0 ? 'bg-red-50 text-red-700 border-red-200' :
            discrepancy > 0 ? 'bg-teal-50 text-teal-700 border-teal-200' :
            'bg-gray-50 text-gray-600 border-gray-200'
          }`}>
            <span className="font-bold text-base">{discrepancy > 0 ? '+' : ''}{discrepancy}</span>
            <span>
              {discrepancy < 0 && 'units missing — needs explanation'}
              {discrepancy > 0 && 'extra units found'}
              {discrepancy === 0 && 'counts match — no discrepancy'}
            </span>
          </div>
        )}

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1.5">Reason / Explanation</label>
          <textarea
            name="reason"
            value={form.reason}
            onChange={ch}
            rows={3}
            className="w-full px-3.5 py-2.5 border border-gray-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-teal-600 resize-none"
            placeholder="Describe the reason for the discrepancy…"
            required
          />
        </div>
        <div className="pt-1">
          <SubmitBtn label="Submit for Manager Review" loading={loading} color="purple" />
        </div>
      </form>
    </FormCard>
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
    { id: 'incoming', label: 'Incoming Transfers', badge: incomingCount, icon: <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" /></svg> },
    { id: 'outgoing', label: 'Outgoing Transfers', badge: outgoingCount, icon: <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" /></svg> },
    { id: 'reconciliation', label: 'Reconciliation', badge: 0, icon: <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4" /></svg> },
  ]

  return (
    <Layout title="Warehouse Staff" navItems={navItems} activeTab={activeTab} onTabChange={setActiveTab}>
      {activeTab === 'intake' && <IntakePanel />}
      {activeTab === 'sale' && <SalePanel />}
      {activeTab === 'incoming' && <IncomingPanel />}
      {activeTab === 'outgoing' && <OutgoingPanel />}
      {activeTab === 'reconciliation' && <ReconciliationPanel />}
    </Layout>
  )
}
