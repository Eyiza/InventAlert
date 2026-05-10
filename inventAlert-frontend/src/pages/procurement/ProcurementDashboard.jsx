import { useState } from 'react'
import { useSelector, useDispatch } from 'react-redux'
import { toast } from 'react-toastify'
import Layout from '../../components/layout/Layout'
import ConfirmDialog from '../../components/shared/ConfirmDialog'
import StatusBadge from '../../components/shared/StatusBadge'
import StatCard from '../../components/shared/StatCard'
import { acknowledgeAlert, markOrderPlaced, resolveAlert } from '../../store/slices/alertsSlice'
import { addMovement } from '../../store/slices/stockSlice'
import { createPO, receivePO, cancelPO } from '../../store/slices/purchaseOrdersSlice'
import { submitComplaint } from '../../store/slices/superadminSlice'

const fmtDT = d => new Date(d).toLocaleString('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
const fmtDate = d => new Date(d).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })

// ── Shared ────────────────────────────────────────────────────────────────────

function FilterableSelect({ value, onChange, options, placeholder = 'Select…', required }) {
  const [query, setQuery] = useState('')
  const [open, setOpen] = useState(false)
  const selected = options.find(o => o.value === value)
  const filtered = !query ? options : options.filter(o => o.label.toLowerCase().includes(query.toLowerCase()))
  return (
    <div
      className="relative"
      onBlur={e => { if (!e.currentTarget.contains(e.relatedTarget)) { setOpen(false); setQuery('') } }}
    >
      <input
        type="text"
        value={open ? query : (selected?.label || '')}
        placeholder={placeholder}
        onChange={e => { setQuery(e.target.value); setOpen(true) }}
        onFocus={() => { setOpen(true); setQuery('') }}
        required={required && !value}
        className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm bg-white focus:outline-none focus:ring-2 focus:ring-teal-600 focus:border-transparent"
      />
      {open && (
        <div className="absolute z-50 top-full mt-0.5 left-0 right-0 bg-white border border-gray-200 rounded-lg shadow-lg max-h-44 overflow-y-auto">
          {filtered.length === 0 ? (
            <p className="px-3 py-2.5 text-sm text-gray-400 italic">No matches found</p>
          ) : filtered.map(o => (
            <button key={o.value} type="button"
              onMouseDown={e => e.preventDefault()}
              onClick={() => { onChange(o.value); setQuery(''); setOpen(false) }}
              className={`w-full text-left px-3 py-2.5 text-sm transition-colors ${value === o.value ? 'bg-teal-50 text-teal-700 font-medium' : 'text-gray-700 hover:bg-gray-50'}`}>
              {o.label}
            </button>
          ))}
        </div>
      )}
      <select value={value} onChange={e => onChange(e.target.value)} required={required} tabIndex={-1} aria-hidden className="sr-only">
        <option value="">{placeholder}</option>
        {options.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
      </select>
    </div>
  )
}

function SearchBar({ value, onChange, placeholder }) {
  return (
    <div className="relative">
      <svg className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
      </svg>
      <input
        type="text" value={value} onChange={e => onChange(e.target.value)} placeholder={placeholder}
        className="pl-9 pr-4 py-1.5 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-teal-600 w-72"
      />
    </div>
  )
}

// ── Alert Pipeline ────────────────────────────────────────────────────────────

const PIPELINE_STAGES = [
  { status: 'OPEN', label: 'Open', color: 'bg-amber-50 border-amber-200', dot: 'bg-amber-500', hdr: 'text-amber-700' },
  { status: 'ACKNOWLEDGED', label: 'Acknowledged', color: 'bg-blue-50 border-blue-200', dot: 'bg-blue-500', hdr: 'text-blue-700' },
  { status: 'ORDER_PLACED', label: 'Order Placed', color: 'bg-indigo-50 border-indigo-200', dot: 'bg-indigo-500', hdr: 'text-indigo-700' },
  { status: 'RESOLVED', label: 'Resolved', color: 'bg-green-50 border-green-200', dot: 'bg-green-500', hdr: 'text-green-700' },
]

function AlertCard({ alert, productName, warehouseName, onAction }) {
  const { status, stockAtAlert, threshold, createdAt } = alert
  return (
    <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-4 space-y-2">
      <div className="flex items-start justify-between gap-2">
        <div>
          <p className="font-medium text-gray-900 text-sm leading-tight">{productName}</p>
          <p className="text-xs text-gray-500">{warehouseName}</p>
        </div>
        <StatusBadge status={status} size="xs" />
      </div>
      <div className="flex gap-3 text-xs">
        <span className="text-gray-500">Stock at alert: <strong className="text-red-600">{stockAtAlert}</strong></span>
        <span className="text-gray-500">Threshold: <strong className="text-gray-700">{threshold}</strong></span>
      </div>
      <p className="text-xs text-gray-400">{fmtDT(createdAt)}</p>
      {onAction && (
        <button onClick={onAction} className="w-full py-1.5 bg-teal-600 hover:bg-teal-700 text-white text-xs font-semibold rounded-lg transition-colors">
          {status === 'OPEN' && 'Acknowledge'}
          {status === 'ACKNOWLEDGED' && 'Mark Order Placed'}
          {status === 'ORDER_PLACED' && 'Mark Resolved'}
        </button>
      )}
    </div>
  )
}

function AlertPipelinePanel({ myWarehouseIds }) {
  const { alerts } = useSelector(s => s.alerts)
  const { products, warehouses } = useSelector(s => s.stock)
  const { user } = useSelector(s => s.auth)
  const dispatch = useDispatch()

  const myAlerts = alerts.filter(a => myWarehouseIds.includes(a.warehouseId))

  const getAction = (alert) => {
    if (alert.status === 'OPEN') return () => { dispatch(acknowledgeAlert({ alertId: alert.id, userId: user.id })); toast.success('Alert acknowledged') }
    if (alert.status === 'ACKNOWLEDGED') return () => { dispatch(markOrderPlaced(alert.id)); toast.success('Order marked as placed') }
    if (alert.status === 'ORDER_PLACED') return () => { dispatch(resolveAlert(alert.id)); toast.success('Alert resolved') }
    return null
  }

  const openCount = myAlerts.filter(a => a.status === 'OPEN').length

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        <StatCard title="Open Alerts" value={myAlerts.filter(a => a.status === 'OPEN').length} color={openCount > 0 ? 'amber' : 'green'} />
        <StatCard title="Acknowledged" value={myAlerts.filter(a => a.status === 'ACKNOWLEDGED').length} color="blue" />
        <StatCard title="Order Placed" value={myAlerts.filter(a => a.status === 'ORDER_PLACED').length} color="purple" />
        <StatCard title="Resolved" value={myAlerts.filter(a => a.status === 'RESOLVED').length} color="green" />
      </div>

      {myWarehouseIds.length === 0 && (
        <div className="bg-amber-50 border border-amber-200 rounded-xl p-5 text-sm text-amber-800">
          You are not assigned to any warehouses. Ask your admin to assign you to a warehouse.
        </div>
      )}

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {PIPELINE_STAGES.map(stage => {
          const stageAlerts = myAlerts.filter(a => a.status === stage.status)
          return (
            <div key={stage.status} className={`rounded-xl border-2 p-3 ${stage.color}`}>
              <div className="flex items-center gap-2 mb-3">
                <div className={`w-2.5 h-2.5 rounded-full ${stage.dot}`} />
                <h4 className={`text-sm font-semibold ${stage.hdr}`}>{stage.label}</h4>
                <span className={`ml-auto text-xs font-bold px-2 py-0.5 rounded-full ${stage.dot} text-white`}>{stageAlerts.length}</span>
              </div>
              <div className="space-y-2">
                {stageAlerts.length === 0 ? (
                  <p className="text-xs text-gray-400 text-center py-4">No alerts</p>
                ) : stageAlerts.map(alert => (
                  <AlertCard
                    key={alert.id}
                    alert={alert}
                    productName={products.find(p => p.id === alert.productId)?.name || alert.productId}
                    warehouseName={warehouses.find(w => w.id === alert.warehouseId)?.name || alert.warehouseId}
                    onAction={getAction(alert)}
                  />
                ))}
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}

// ── Stock Overview ────────────────────────────────────────────────────────────

function StockOverviewPanel({ myWarehouseIds }) {
  const { stockLevels, products, warehouses } = useSelector(s => s.stock)
  const [filter, setFilter] = useState('ALL')
  const [search, setSearch] = useState('')

  const rows = stockLevels
    .filter(sl => myWarehouseIds.includes(sl.warehouseId))
    .map(sl => ({
      ...sl,
      productName: products.find(p => p.id === sl.productId)?.name || sl.productId,
      sku: products.find(p => p.id === sl.productId)?.sku || '',
      warehouseName: warehouses.find(w => w.id === sl.warehouseId)?.name || sl.warehouseId,
      status: sl.currentStock < sl.threshold ? 'CRITICAL' : sl.currentStock < sl.threshold * 1.25 ? 'WARNING' : 'OK',
    }))
    .filter(r => filter === 'ALL' || r.status === filter)
    .filter(r => !search || r.productName.toLowerCase().includes(search.toLowerCase()) || r.warehouseName.toLowerCase().includes(search.toLowerCase()) || r.sku.toLowerCase().includes(search.toLowerCase()))
    .sort((a, b) => (a.daysUntilEmpty ?? 999) - (b.daysUntilEmpty ?? 999))

  const criticalCount = rows.filter(r => r.status === 'CRITICAL').length
  const warningCount = rows.filter(r => r.status === 'WARNING').length

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        <StatCard title="Products Tracked" value={stockLevels.filter(sl => myWarehouseIds.includes(sl.warehouseId)).length} color="blue" />
        <StatCard title="Critical" value={criticalCount} color={criticalCount > 0 ? 'red' : 'green'} />
        <StatCard title="Warning" value={warningCount} color={warningCount > 0 ? 'amber' : 'green'} />
        <StatCard title="Healthy" value={rows.filter(r => r.status === 'OK').length} color="green" />
      </div>

      <div className="bg-white rounded-xl border border-gray-200">
        <div className="flex flex-wrap items-center justify-between gap-3 px-5 py-4 border-b border-gray-100">
          <h3 className="font-semibold text-gray-900">Warehouse Stock</h3>
          <div className="flex items-center gap-2 flex-wrap">
            <SearchBar value={search} onChange={setSearch} placeholder="Search product or SKU…" />
            <div className="flex gap-1">
              {['ALL', 'CRITICAL', 'WARNING', 'OK'].map(f => (
                <button key={f} onClick={() => setFilter(f)} className={`px-2.5 py-1 rounded-lg text-xs font-medium transition-colors ${filter === f ? 'bg-teal-600 text-white' : 'text-gray-500 hover:bg-gray-100'}`}>{f}</button>
              ))}
            </div>
          </div>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-100">
                {['Product', 'SKU', 'Warehouse', 'Stock', 'Threshold', 'Status', 'Days Until Empty'].map(h => (
                  <th key={h} className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wide">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {rows.length === 0 ? (
                <tr><td colSpan={7} className="px-4 py-8 text-center text-gray-400 text-sm">No stock data for your warehouses</td></tr>
              ) : rows.map(r => (
                <tr key={r.id} className="hover:bg-gray-50/60">
                  <td className="px-4 py-3 font-medium text-gray-900">{r.productName}</td>
                  <td className="px-4 py-3 font-mono text-xs text-gray-500">{r.sku}</td>
                  <td className="px-4 py-3 text-gray-600">{r.warehouseName}</td>
                  <td className="px-4 py-3">
                    <span className={`px-2 py-0.5 rounded font-semibold ${r.status === 'CRITICAL' ? 'text-red-700 bg-red-50' : r.status === 'WARNING' ? 'text-amber-700 bg-amber-50' : 'text-green-700 bg-green-50'}`}>
                      {r.currentStock}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-gray-600">{r.threshold}</td>
                  <td className="px-4 py-3"><StatusBadge status={r.status} /></td>
                  <td className="px-4 py-3">
                    {r.daysUntilEmpty != null ? (
                      <span className={`font-medium ${r.daysUntilEmpty <= 7 ? 'text-red-600' : r.daysUntilEmpty <= 14 ? 'text-amber-600' : 'text-green-600'}`}>
                        {r.daysUntilEmpty}d
                      </span>
                    ) : '—'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}

// ── Receive Goods ─────────────────────────────────────────────────────────────

const EMPTY_GOODS_ROW = { productId: '', quantity: '' }

function ReceiveGoodsPanel({ myWarehouseIds }) {
  const { products, warehouses } = useSelector(s => s.stock)
  const { user } = useSelector(s => s.auth)
  const dispatch = useDispatch()
  const [warehouseId, setWarehouseId] = useState(myWarehouseIds[0] || '')
  const [rows, setRows] = useState([{ ...EMPTY_GOODS_ROW }])
  const [success, setSuccess] = useState(false)
  const [summary, setSummary] = useState(null)

  const updateRow = (i, field, val) => setRows(rs => rs.map((r, idx) => idx === i ? { ...r, [field]: val } : r))
  const removeRow = i => setRows(rs => rs.filter((_, idx) => idx !== i))

  const myWarehouses = warehouses.filter(w => myWarehouseIds.includes(w.id) && w.isActive)
  const activeProducts = products.filter(p => p.isActive)
  const validRows = rows.filter(r => r.productId && +r.quantity > 0)

  const handleSubmit = e => {
    e.preventDefault()
    if (!validRows.length || !warehouseId) return
    const warehouseName = myWarehouses.find(w => w.id === warehouseId)?.name || warehouseId
    const items = validRows.map(r => {
      const product = activeProducts.find(p => p.id === r.productId)
      return { productId: r.productId, productName: product?.name || r.productId, sku: product?.sku || '—', quantity: +r.quantity }
    })
    setSummary({ warehouseName, items })
  }

  const handleConfirm = () => {
    summary.items.forEach(item => {
      dispatch(addMovement({
        productId: item.productId,
        warehouseId,
        type: 'INTAKE',
        quantity: item.quantity,
        createdBy: user.id,
        referenceId: null,
      }))
    })
    toast.success(`${summary.items.length} goods item${summary.items.length > 1 ? 's' : ''} received — stock updated`)
    setRows([{ ...EMPTY_GOODS_ROW }])
    setSummary(null)
    setSuccess(true)
    setTimeout(() => setSuccess(false), 4000)
  }

  if (summary) {
    return (
      <div className="space-y-4">
        <div className="bg-white rounded-xl border border-gray-200 p-6">
          <h3 className="font-semibold text-gray-900 mb-1">Review Receipt</h3>
          <p className="text-sm text-gray-500 mb-5">Check the items below before recording. Stock will be updated immediately after confirmation.</p>

          <div className="bg-teal-50 border border-teal-200 rounded-xl p-4 flex items-start gap-3 mb-5">
            <svg className="w-5 h-5 text-teal-600 shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <p className="text-sm text-teal-800">
              Receiving <span className="font-semibold">{summary.items.length} item{summary.items.length > 1 ? 's' : ''}</span> into <span className="font-semibold">{summary.warehouseName}</span>. Stock levels will be increased immediately.
            </p>
          </div>

          <div className="rounded-xl border border-gray-200 overflow-hidden mb-5">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-gray-50 border-b border-gray-100">
                  {['#', 'Product', 'SKU', 'Qty to Receive'].map(h => (
                    <th key={h} className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wide">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50">
                {summary.items.map((item, i) => (
                  <tr key={i} className="hover:bg-gray-50/60">
                    <td className="px-4 py-3 text-gray-400 text-xs font-mono">{String(i + 1).padStart(2, '0')}</td>
                    <td className="px-4 py-3 font-medium text-gray-900">{item.productName}</td>
                    <td className="px-4 py-3 font-mono text-xs text-gray-500">{item.sku}</td>
                    <td className="px-4 py-3 font-semibold text-teal-700">+{item.quantity}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="flex gap-3">
            <button
              type="button" onClick={() => setSummary(null)}
              className="flex-1 py-2.5 border border-gray-300 text-gray-700 rounded-xl text-sm font-medium hover:bg-gray-50 transition-colors"
            >
              Edit Items
            </button>
            <button
              type="button" onClick={handleConfirm}
              className="flex-1 py-2.5 bg-teal-600 hover:bg-teal-700 text-white font-semibold rounded-xl text-sm transition-colors"
            >
              Confirm & Record {summary.items.length} Receipt{summary.items.length > 1 ? 's' : ''}
            </button>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      {success && (
        <div className="bg-green-50 border border-green-200 rounded-xl p-4 flex items-center gap-3">
          <svg className="w-5 h-5 text-green-600 shrink-0" fill="currentColor" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
          </svg>
          <p className="text-sm text-green-800 font-medium">Goods received and stock levels updated successfully.</p>
        </div>
      )}

      <div className="bg-white rounded-xl border border-gray-200 p-6">
        <h3 className="font-semibold text-gray-900 mb-1">Receive Goods</h3>
        <p className="text-sm text-gray-500 mb-5">Record goods you have received into your warehouse. This updates stock levels immediately.</p>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1.5">Destination Warehouse <span className="text-red-400">*</span></label>
            <select
              value={warehouseId} onChange={e => setWarehouseId(e.target.value)} required
              className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-teal-600"
            >
              <option value="">Select warehouse…</option>
              {myWarehouses.map(w => <option key={w.id} value={w.id}>{w.name}</option>)}
            </select>
          </div>

          <div className="space-y-3">
            {rows.map((row, i) => (
              <div key={i} className="bg-gray-50 border border-gray-200 rounded-xl p-4">
                <div className="flex items-center justify-between mb-3">
                  <span className="text-xs font-bold text-gray-400 uppercase tracking-widest">Item {String(i + 1).padStart(2, '0')}</span>
                  <button
                    type="button" onClick={() => removeRow(i)} disabled={rows.length === 1}
                    className="w-7 h-7 flex items-center justify-center rounded-lg text-gray-300 hover:text-red-500 hover:bg-red-50 disabled:opacity-0 disabled:pointer-events-none transition-colors"
                  >
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /></svg>
                  </button>
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs font-medium text-gray-500 mb-1">Product <span className="text-red-400">*</span></label>
                    <FilterableSelect
                      value={row.productId}
                      onChange={val => updateRow(i, 'productId', val)}
                      options={activeProducts.map(p => ({ value: p.id, label: `${p.name} (${p.sku})` }))}
                      placeholder="Select product…"
                      required
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-medium text-gray-500 mb-1">Quantity Received <span className="text-red-400">*</span></label>
                    <input
                      type="number" min="1" value={row.quantity} onChange={e => updateRow(i, 'quantity', e.target.value)} required
                      placeholder="e.g. 50"
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm bg-white focus:outline-none focus:ring-2 focus:ring-teal-600"
                    />
                  </div>
                </div>
              </div>
            ))}
          </div>

          <button
            type="button" onClick={() => setRows(rs => [...rs, { ...EMPTY_GOODS_ROW }])}
            className="flex items-center gap-2 w-full py-2.5 border-2 border-dashed border-gray-300 rounded-xl text-sm text-gray-500 hover:border-teal-400 hover:text-teal-600 hover:bg-teal-50/40 transition-colors justify-center font-medium"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" /></svg>
            Add another item
          </button>

          <button
            type="submit"
            disabled={!validRows.length || !warehouseId}
            className="w-full py-2.5 bg-teal-600 hover:bg-teal-700 text-white font-semibold rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            Review {validRows.length > 0 ? `${validRows.length} ` : ''}Receipt{validRows.length !== 1 ? 's' : ''}
          </button>
        </form>
      </div>
    </div>
  )
}

// ── Purchase Orders ───────────────────────────────────────────────────────────

const EMPTY_PO_ROW = { productId: '', quantity: '', unitCost: '' }

function PurchaseOrdersPanel({ myWarehouseIds }) {
  const { purchaseOrders } = useSelector(s => s.purchaseOrders)
  const { products, warehouses } = useSelector(s => s.stock)
  const { user } = useSelector(s => s.auth)
  const dispatch = useDispatch()
  const [showCreate, setShowCreate] = useState(false)
  const [form, setForm] = useState({ warehouseId: myWarehouseIds[0] || '', supplier: '', expectedDate: '', notes: '' })
  const [poRows, setPoRows] = useState([{ ...EMPTY_PO_ROW }])
  const [confirm, setConfirm] = useState(null)
  const ch = e => setForm(f => ({ ...f, [e.target.name]: e.target.value }))

  const updatePoRow = (i, field, val) => setPoRows(rs => rs.map((r, idx) => idx === i ? { ...r, [field]: val } : r))
  const removePoRow = i => setPoRows(rs => rs.filter((_, idx) => idx !== i))

  const myWarehouses = warehouses.filter(w => myWarehouseIds.includes(w.id) && w.isActive)
  const activeProducts = products.filter(p => p.isActive)
  const myPOs = purchaseOrders.filter(po => myWarehouseIds.includes(po.warehouseId))
  const validPoRows = poRows.filter(r => r.productId && +r.quantity > 0)

  const handleCreatePO = e => {
    e.preventDefault()
    if (!validPoRows.length || !form.warehouseId || !form.supplier) return
    dispatch(createPO({
      ...form,
      items: validPoRows.map(r => ({
        productId: r.productId,
        quantity: +r.quantity,
        unitCost: +r.unitCost || 0,
        productName: products.find(p => p.id === r.productId)?.name || r.productId,
      })),
      createdBy: user.id,
    }))
    toast.success('Purchase order created')
    setShowCreate(false)
    setForm({ warehouseId: myWarehouseIds[0] || '', supplier: '', expectedDate: '', notes: '' })
    setPoRows([{ ...EMPTY_PO_ROW }])
  }

  const handleReceivePO = (po) => {
    po.items.forEach(item => {
      dispatch(addMovement({
        productId: item.productId,
        warehouseId: po.warehouseId,
        type: 'INTAKE',
        quantity: item.quantity,
        createdBy: user.id,
        referenceId: po.id,
      }))
    })
    dispatch(receivePO(po.id))
    toast.success('PO received — stock levels updated')
  }

  const STATUS_COLORS = {
    ORDERED: 'bg-blue-50 text-blue-700 border-blue-100',
    RECEIVED: 'bg-green-50 text-green-700 border-green-100',
    CANCELLED: 'bg-gray-50 text-gray-500 border-gray-100',
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="grid grid-cols-3 gap-4 flex-1">
          <StatCard title="Total POs" value={myPOs.length} color="blue" />
          <StatCard title="Ordered" value={myPOs.filter(p => p.status === 'ORDERED').length} color="amber" />
          <StatCard title="Received" value={myPOs.filter(p => p.status === 'RECEIVED').length} color="green" />
        </div>
        <button
          onClick={() => setShowCreate(s => !s)}
          className="flex items-center gap-1.5 px-4 py-2 bg-teal-600 text-white text-sm font-medium rounded-lg hover:bg-teal-700 shrink-0"
        >
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" /></svg>
          New Purchase Order
        </button>
      </div>

      {showCreate && (
        <div className="bg-white rounded-xl border border-gray-200 p-6">
          <div className="flex items-center justify-between mb-4">
            <h3 className="font-semibold text-gray-900">Create Purchase Order</h3>
            <button onClick={() => setShowCreate(false)} className="text-gray-400 hover:text-gray-600 p-1 rounded hover:bg-gray-100">
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /></svg>
            </button>
          </div>
          <form onSubmit={handleCreatePO} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1.5">Warehouse <span className="text-red-400">*</span></label>
                <select
                  name="warehouseId" value={form.warehouseId} onChange={ch} required
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-teal-600"
                >
                  <option value="">Select warehouse…</option>
                  {myWarehouses.map(w => <option key={w.id} value={w.id}>{w.name}</option>)}
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1.5">Supplier <span className="text-red-400">*</span></label>
                <input
                  name="supplier" value={form.supplier} onChange={ch} required placeholder="Supplier name"
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-teal-600"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1.5">Expected Delivery</label>
                <input
                  name="expectedDate" type="date" value={form.expectedDate} onChange={ch}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-teal-600"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1.5">Notes</label>
                <input
                  name="notes" value={form.notes} onChange={ch} placeholder="Optional notes"
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-teal-600"
                />
              </div>
            </div>

            <div>
              <p className="text-sm font-medium text-gray-700 mb-2">Order Items</p>
              <div className="space-y-3">
                {poRows.map((row, i) => (
                  <div key={i} className="bg-gray-50 border border-gray-200 rounded-xl p-4">
                    <div className="flex items-center justify-between mb-3">
                      <span className="text-xs font-bold text-gray-400 uppercase tracking-widest">Item {String(i + 1).padStart(2, '0')}</span>
                      <button
                        type="button" onClick={() => removePoRow(i)} disabled={poRows.length === 1}
                        className="w-7 h-7 flex items-center justify-center rounded-lg text-gray-300 hover:text-red-500 hover:bg-red-50 disabled:opacity-0 disabled:pointer-events-none transition-colors"
                      >
                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /></svg>
                      </button>
                    </div>
                    <div className="grid grid-cols-3 gap-3">
                      <div className="col-span-2">
                        <label className="block text-xs font-medium text-gray-500 mb-1">Product <span className="text-red-400">*</span></label>
                        <FilterableSelect
                          value={row.productId}
                          onChange={val => updatePoRow(i, 'productId', val)}
                          options={activeProducts.map(p => ({ value: p.id, label: `${p.name} (${p.sku})` }))}
                          placeholder="Select product…"
                          required
                        />
                      </div>
                      <div>
                        <label className="block text-xs font-medium text-gray-500 mb-1">Qty <span className="text-red-400">*</span></label>
                        <input
                          type="number" min="1" value={row.quantity} onChange={e => updatePoRow(i, 'quantity', e.target.value)} required
                          placeholder="e.g. 100"
                          className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm bg-white focus:outline-none focus:ring-2 focus:ring-teal-600"
                        />
                      </div>
                    </div>
                  </div>
                ))}
              </div>
              <button
                type="button" onClick={() => setPoRows(rs => [...rs, { ...EMPTY_PO_ROW }])}
                className="flex items-center gap-2 w-full py-2.5 mt-3 border-2 border-dashed border-gray-300 rounded-xl text-sm text-gray-500 hover:border-teal-400 hover:text-teal-600 hover:bg-teal-50/40 transition-colors justify-center font-medium"
              >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" /></svg>
                Add another item
              </button>
            </div>

            <div className="flex gap-2 pt-1">
              <button type="button" onClick={() => setShowCreate(false)} className="flex-1 py-2 border border-gray-300 text-gray-700 rounded-lg text-sm hover:bg-gray-50">Cancel</button>
              <button
                type="submit" disabled={!validPoRows.length || !form.warehouseId || !form.supplier}
                className="flex-1 py-2 bg-teal-600 text-white rounded-lg text-sm font-medium hover:bg-teal-700 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Create PO — {validPoRows.length} item{validPoRows.length !== 1 ? 's' : ''}
              </button>
            </div>
          </form>
        </div>
      )}

      <div className="bg-white rounded-xl border border-gray-200">
        <div className="px-5 py-4 border-b border-gray-100">
          <h3 className="font-semibold text-gray-900">Purchase Orders</h3>
        </div>
        {myPOs.length === 0 ? (
          <div className="px-5 py-10 text-center">
            <svg className="w-10 h-10 text-gray-300 mx-auto mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
            </svg>
            <p className="text-sm text-gray-400">No purchase orders yet. Create one above.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-gray-50 border-b border-gray-100">
                  {['PO #', 'Supplier', 'Warehouse', 'Items', 'Expected', 'Status', 'Actions'].map(h => (
                    <th key={h} className="text-left px-5 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wide">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50">
                {myPOs.map(po => (
                  <tr key={po.id} className="hover:bg-gray-50/60">
                    <td className="px-5 py-3 font-mono text-xs text-gray-500">{po.id.slice(-8).toUpperCase()}</td>
                    <td className="px-5 py-3 font-medium text-gray-900">{po.supplier}</td>
                    <td className="px-5 py-3 text-gray-600">{warehouses.find(w => w.id === po.warehouseId)?.name || po.warehouseId}</td>
                    <td className="px-5 py-3 text-gray-600">{po.items.length} product{po.items.length !== 1 ? 's' : ''}</td>
                    <td className="px-5 py-3 text-gray-500">{po.expectedDate ? fmtDate(po.expectedDate) : '—'}</td>
                    <td className="px-5 py-3">
                      <span className={`px-2.5 py-1 rounded-full text-xs font-semibold border ${STATUS_COLORS[po.status] || ''}`}>
                        {po.status}
                      </span>
                    </td>
                    <td className="px-5 py-3">
                      {po.status === 'ORDERED' && (
                        <div className="flex gap-2">
                          <button
                            onClick={() => setConfirm({
                              action: () => handleReceivePO(po),
                              title: 'Receive Purchase Order',
                              message: `Mark PO from "${po.supplier}" as received? Stock levels for ${po.items.length} product${po.items.length !== 1 ? 's' : ''} will be updated immediately.`,
                              label: 'Receive PO',
                              danger: false,
                            })}
                            className="px-2.5 py-1 rounded-lg text-xs font-medium bg-teal-50 text-teal-700 border border-teal-100 hover:bg-teal-100 transition-colors"
                          >
                            Receive
                          </button>
                          <button
                            onClick={() => setConfirm({
                              action: () => { dispatch(cancelPO(po.id)); toast.info('PO cancelled') },
                              title: 'Cancel Purchase Order',
                              message: `Cancel the PO from "${po.supplier}"? This cannot be undone.`,
                              label: 'Cancel PO',
                              danger: true,
                            })}
                            className="px-2.5 py-1 rounded-lg text-xs font-medium bg-red-50 text-red-600 border border-red-100 hover:bg-red-100 transition-colors"
                          >
                            Cancel
                          </button>
                        </div>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
      {confirm && (
        <ConfirmDialog
          title={confirm.title}
          message={confirm.message}
          danger={confirm.danger}
          confirmLabel={confirm.label}
          onConfirm={() => { confirm.action(); setConfirm(null) }}
          onCancel={() => setConfirm(null)}
        />
      )}
    </div>
  )
}

// ── Alert Frequency ───────────────────────────────────────────────────────────

function AlertFrequencyPanel({ myWarehouseIds }) {
  const { alertFrequency } = useSelector(s => s.analytics)
  const filtered = alertFrequency.filter(a => myWarehouseIds.length === 0 || myWarehouseIds.some(id => a.warehouseName?.toLowerCase().includes('alpha') || true))
  const maxCount = Math.max(...filtered.map(a => a.alertCount), 1)

  return (
    <div className="space-y-4">
      <div className="bg-white rounded-xl border border-gray-200">
        <div className="px-5 py-4 border-b border-gray-100">
          <h3 className="font-semibold text-gray-900">Alert Frequency Analysis</h3>
          <p className="text-xs text-gray-500 mt-0.5">Products that trigger restock alerts most frequently in your warehouses</p>
        </div>
        <div className="p-5 space-y-4">
          {filtered.length === 0 ? (
            <p className="text-sm text-gray-400 text-center py-4">No alert frequency data available.</p>
          ) : filtered.map((a, i) => (
            <div key={i} className="space-y-1">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-gray-900">{a.productName}</p>
                  <p className="text-xs text-gray-500">{a.warehouseName}</p>
                </div>
                <div className="text-right">
                  <p className="text-sm font-bold text-amber-700">{a.alertCount} alerts</p>
                  <p className="text-xs text-gray-400">every {a.avgDaysBetweenAlerts} days</p>
                </div>
              </div>
              <div className="w-full bg-gray-100 rounded-full h-2">
                <div className="bg-amber-500 h-2 rounded-full transition-all" style={{ width: `${(a.alertCount / maxCount) * 100}%` }} />
              </div>
            </div>
          ))}
        </div>
      </div>

      <div className="bg-amber-50 border border-amber-200 rounded-xl p-5">
        <h4 className="text-sm font-semibold text-amber-800 mb-2">Procurement Insight</h4>
        <p className="text-sm text-amber-700">
          Products with short alert cycles may benefit from standing supply agreements or higher safety stock levels to reduce restocking frequency.
        </p>
      </div>
    </div>
  )
}

// ── Complaints ────────────────────────────────────────────────────────────────

function ComplaintsPanel() {
  const { user: me, companyId, companyName } = useSelector(s => s.auth)
  const dispatch = useDispatch()
  const [form, setForm] = useState({ subject: '', priority: 'MEDIUM', message: '' })
  const [submitted, setSubmitted] = useState(false)
  const ch = e => setForm(f => ({ ...f, [e.target.name]: e.target.value }))

  const handleSubmit = e => {
    e.preventDefault()
    dispatch(submitComplaint({ subject: form.subject, priority: form.priority, message: form.message, submittedBy: me?.name || 'Unknown', email: me?.email || '', companyName: companyName || '', companyId }))
    setSubmitted(true)
    setForm({ subject: '', priority: 'MEDIUM', message: '' })
    setTimeout(() => setSubmitted(false), 5000)
  }

  return (
    <div className="space-y-4">
      <div className="bg-white rounded-xl border border-gray-200 p-6">
        <h3 className="font-semibold text-gray-900 mb-1">Feedback & Support</h3>
        <p className="text-sm text-gray-500 mb-5">Submit a complaint, inquiry, or suggestion to the platform support team.</p>
        {submitted && (
          <div className="flex items-center gap-2 bg-teal-50 border border-teal-200 rounded-xl p-4 mb-4">
            <svg className="w-5 h-5 text-teal-600 shrink-0" fill="currentColor" viewBox="0 0 20 20"><path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" /></svg>
            <p className="text-sm text-teal-800 font-medium">Feedback submitted successfully. Our team will review it shortly.</p>
          </div>
        )}
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1.5">Subject <span className="text-red-400">*</span></label>
            <input name="subject" value={form.subject} onChange={ch} required placeholder="Brief description of your feedback…"
              className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-teal-600" />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1.5">Priority</label>
            <select name="priority" value={form.priority} onChange={ch}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-teal-600">
              <option value="LOW">Low</option>
              <option value="MEDIUM">Medium</option>
              <option value="HIGH">High</option>
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1.5">Message <span className="text-red-400">*</span></label>
            <textarea name="message" value={form.message} onChange={ch} rows={4} required placeholder="Describe your feedback in detail…"
              className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-teal-600 resize-none" />
          </div>
          <button type="submit" className="px-6 py-2.5 bg-teal-600 hover:bg-teal-700 text-white font-semibold rounded-lg text-sm transition-colors">
            Submit Feedback
          </button>
        </form>
      </div>
    </div>
  )
}

// ── Main ──────────────────────────────────────────────────────────────────────

export default function ProcurementDashboard() {
  const { user } = useSelector(s => s.auth)
  const { warehouseAssignments } = useSelector(s => s.users)
  const { warehouses } = useSelector(s => s.stock)
  const { alerts } = useSelector(s => s.alerts)
  const { purchaseOrders } = useSelector(s => s.purchaseOrders)
  const [activeTab, setActiveTab] = useState('alerts')

  const myWarehouseIds = warehouseAssignments
    .filter(a => a.userId === user.id)
    .map(a => a.warehouseId)

  const myWarehouseId = myWarehouseIds[0] || null
  const myWarehouse = warehouses.find(w => w.id === myWarehouseId) || null

  const myAlerts = alerts.filter(a => myWarehouseIds.includes(a.warehouseId))
  const openAlerts = myAlerts.filter(a => a.status === 'OPEN').length
  const myPOs = purchaseOrders.filter(po => myWarehouseIds.includes(po.warehouseId))
  const pendingPOs = myPOs.filter(po => po.status === 'ORDERED').length

  const navItems = [
    {
      id: 'alerts', label: 'Alert Pipeline', badge: openAlerts,
      icon: <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" /></svg>,
    },
    {
      id: 'stock', label: 'Stock Overview', badge: 0,
      icon: <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 10h18M3 14h18M10 6v12M14 6v12M5 6h14a1 1 0 011 1v10a1 1 0 01-1 1H5a1 1 0 01-1-1V7a1 1 0 011-1z" /></svg>,
    },
    {
      id: 'receive', label: 'Receive Goods', badge: 0,
      icon: <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" /></svg>,
    },
    {
      id: 'orders', label: 'Purchase Orders', badge: pendingPOs,
      icon: <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" /></svg>,
    },
    {
      id: 'frequency', label: 'Alert Frequency', badge: 0,
      icon: <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" /></svg>,
    },
    {
      id: 'complaints', label: 'Feedback & Support', badge: 0,
      icon: <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" /></svg>,
    },
  ]

  return (
    <Layout title="Procurement Dashboard" navItems={navItems} activeTab={activeTab} onTabChange={setActiveTab}>
      {myWarehouse && (
        <div className="flex items-center gap-2 mb-4 px-4 py-2.5 bg-teal-50 border border-teal-200 rounded-xl">
          <svg className="w-4 h-4 text-teal-600 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
          </svg>
          <span className="text-sm text-teal-800 font-medium">{myWarehouse.name}</span>
          <span className="text-xs text-teal-600">{myWarehouse.address}</span>
        </div>
      )}
      {!myWarehouse && (
        <div className="flex items-center gap-2 mb-4 px-4 py-2.5 bg-amber-50 border border-amber-200 rounded-xl">
          <svg className="w-4 h-4 text-amber-600 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
          <span className="text-sm text-amber-800">You are not assigned to a warehouse. Contact your admin.</span>
        </div>
      )}
      {activeTab === 'alerts' && <AlertPipelinePanel myWarehouseIds={myWarehouseIds} />}
      {activeTab === 'stock' && <StockOverviewPanel myWarehouseIds={myWarehouseIds} />}
      {activeTab === 'receive' && <ReceiveGoodsPanel myWarehouseIds={myWarehouseIds} />}
      {activeTab === 'orders' && <PurchaseOrdersPanel myWarehouseIds={myWarehouseIds} />}
      {activeTab === 'frequency' && <AlertFrequencyPanel myWarehouseIds={myWarehouseIds} />}
      {activeTab === 'complaints' && <ComplaintsPanel />}
    </Layout>
  )
}
