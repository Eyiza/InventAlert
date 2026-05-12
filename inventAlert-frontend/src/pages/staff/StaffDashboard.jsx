import { useState } from 'react'
import { useSelector, useDispatch } from 'react-redux'
import { toast } from 'react-toastify'
import Layout from '../../components/layout/Layout'
import StatusBadge from '../../components/shared/StatusBadge'
import StatCard from '../../components/shared/StatCard'
import { addMovement } from '../../store/slices/stockSlice'
import { dispatchTransfer, acceptTransfer, rejectDelivery, addTransfer } from '../../store/slices/transfersSlice'
import { submitReconciliation } from '../../store/slices/reconciliationsSlice'
import { submitComplaint } from '../../store/slices/superadminSlice'
import ConfirmDialog from '../../components/shared/ConfirmDialog'

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
        className="w-full px-3.5 py-2.5 border border-gray-300 rounded-xl text-sm bg-white focus:outline-none focus:ring-2 focus:ring-teal-600 focus:border-transparent"
      />
      {open && (
        <div className="absolute z-50 top-full mt-0.5 left-0 right-0 bg-white border border-gray-200 rounded-xl shadow-lg max-h-44 overflow-y-auto">
          {filtered.length === 0 ? (
            <p className="px-3 py-2.5 text-sm text-gray-400 italic">No matches found</p>
          ) : filtered.map(o => (
            <button key={o.value} type="button"
              onMouseDown={e => e.preventDefault()}
              onClick={() => { onChange(o.value); setQuery(''); setOpen(false) }}
              className={`w-full text-left px-3.5 py-2.5 text-sm transition-colors ${value === o.value ? 'bg-teal-50 text-teal-700 font-medium' : 'text-gray-700 hover:bg-gray-50'}`}>
              {o.label}
            </button>
          ))}
        </div>
      )}
      {/* hidden native select for required validation */}
      <select
        value={value}
        onChange={e => onChange(e.target.value)}
        required={required}
        tabIndex={-1}
        aria-hidden
        className="sr-only"
      >
        <option value="">{placeholder}</option>
        {options.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
      </select>
    </div>
  )
}

function ViewToggle({ mode, onChange }) {
  return (
    <div className="flex items-center rounded-lg border border-gray-200 overflow-hidden shrink-0" title="Switch view">
      <button
        type="button"
        onClick={() => onChange('cards')}
        className={`px-2.5 py-1.5 transition-colors ${mode === 'cards' ? 'bg-teal-600 text-white' : 'bg-white text-gray-400 hover:bg-gray-50'}`}
        title="Card view"
      >
        <svg className="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 16 16">
          <rect x="1" y="1" width="6" height="6" rx="1" /><rect x="9" y="1" width="6" height="6" rx="1" />
          <rect x="1" y="9" width="6" height="6" rx="1" /><rect x="9" y="9" width="6" height="6" rx="1" />
        </svg>
      </button>
      <button
        type="button"
        onClick={() => onChange('table')}
        className={`px-2.5 py-1.5 border-l border-gray-200 transition-colors ${mode === 'table' ? 'bg-teal-600 text-white' : 'bg-white text-gray-400 hover:bg-gray-50'}`}
        title="Table view"
      >
        <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 16 16">
          <rect x="1" y="1" width="14" height="14" rx="1.5" strokeWidth="1.5" />
          <line x1="1" y1="5.5" x2="15" y2="5.5" strokeWidth="1" />
          <line x1="1" y1="10" x2="15" y2="10" strokeWidth="1" />
          <line x1="5.5" y1="1" x2="5.5" y2="15" strokeWidth="1" />
        </svg>
      </button>
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

// ── Stock Panel ───────────────────────────────────────────────────────────────

function StockPanel() {
  const { stockLevels, products } = useSelector(s => s.stock)
  const { warehouseId } = useSelector(s => s.auth)
  const [filter, setFilter] = useState('ALL')
  const [search, setSearch] = useState('')

  const rows = stockLevels
    .filter(sl => sl.warehouseId === warehouseId)
    .map(sl => ({
      ...sl,
      productName: products.find(p => p.id === sl.productId)?.name || sl.productId,
      sku: products.find(p => p.id === sl.productId)?.sku || '',
      unit: products.find(p => p.id === sl.productId)?.unitOfMeasure || '',
      status: sl.currentStock < sl.threshold ? 'CRITICAL' : sl.currentStock < sl.threshold * 1.25 ? 'WARNING' : 'OK',
    }))
    .filter(r => filter === 'ALL' || r.status === filter)
    .filter(r => !search || r.productName.toLowerCase().includes(search.toLowerCase()) || r.sku.toLowerCase().includes(search.toLowerCase()))

  const critical = rows.filter(r => r.status === 'CRITICAL').length
  const warning = rows.filter(r => r.status === 'WARNING').length

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        <StatCard title="Total Items" value={rows.length} color="blue" />
        <StatCard title="Critical" value={critical} color={critical > 0 ? 'red' : 'green'} />
        <StatCard title="Warning" value={warning} color={warning > 0 ? 'amber' : 'green'} />
        <StatCard title="Healthy" value={rows.filter(r => r.status === 'OK').length} color="green" />
      </div>
      <div className="bg-white rounded-xl border border-gray-200">
        <div className="flex flex-wrap items-center justify-between gap-3 px-5 py-4 border-b border-gray-100">
          <h3 className="font-semibold text-gray-900">Warehouse Stock</h3>
          <div className="flex items-center gap-2 flex-wrap">
            <div className="relative">
              <svg className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
              <input type="text" value={search} onChange={e => setSearch(e.target.value)} placeholder="Search product or SKU…"
                className="pl-9 pr-4 py-1.5 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-teal-600 w-72" />
            </div>
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
                {['Product', 'SKU', 'Unit', 'Stock', 'Threshold', 'Status', 'Days Until Empty'].map(h => (
                  <th key={h} className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wide">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {rows.length === 0 ? (
                <tr><td colSpan={7} className="px-4 py-8 text-center text-gray-400 text-sm">No stock items found.</td></tr>
              ) : rows.map(r => (
                <tr key={r.id} className="hover:bg-gray-50/60">
                  <td className="px-4 py-3 font-medium text-gray-900">{r.productName}</td>
                  <td className="px-4 py-3 font-mono text-xs text-gray-500">{r.sku}</td>
                  <td className="px-4 py-3 text-gray-500">{r.unit}</td>
                  <td className="px-4 py-3">
                    <span className={`px-2 py-0.5 rounded font-semibold text-sm ${r.status === 'CRITICAL' ? 'text-red-700 bg-red-50' : r.status === 'WARNING' ? 'text-amber-700 bg-amber-50' : 'text-green-700 bg-green-50'}`}>
                      {r.currentStock}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-gray-600">{r.threshold}</td>
                  <td className="px-4 py-3"><StatusBadge status={r.status} /></td>
                  <td className="px-4 py-3">
                    {r.daysUntilEmpty != null ? (
                      <span className={`font-medium ${r.daysUntilEmpty <= 7 ? 'text-red-600' : r.daysUntilEmpty <= 14 ? 'text-amber-600' : 'text-green-600'}`}>{r.daysUntilEmpty}d</span>
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

// ── Intake Panel ──────────────────────────────────────────────────────────────

const EMPTY_INTAKE = { productId: '', quantity: '' }

function IntakePanel() {
  const { products, warehouses } = useSelector(s => s.stock)
  const { warehouseId, user } = useSelector(s => s.auth)
  const dispatch = useDispatch()
  const [rows, setRows] = useState([{ ...EMPTY_INTAKE }])
  const [summary, setSummary] = useState(null)
  const [loading, setLoading] = useState(false)
  const [viewMode, setViewMode] = useState('cards')
  const myWarehouse = warehouses.find(w => w.id === warehouseId)
  const activeProducts = products.filter(p => p.isActive)

  const updateRow = (i, field, val) => setRows(rs => rs.map((r, idx) => idx === i ? { ...r, [field]: val } : r))
  const removeRow = i => setRows(rs => rs.filter((_, idx) => idx !== i))

  const handleSubmit = e => {
    e.preventDefault()
    const valid = rows.filter(r => r.productId && r.quantity && +r.quantity > 0)
    if (!valid.length) return
    setSummary(valid.map(r => ({ ...r, productName: activeProducts.find(p => p.id === r.productId)?.name || r.productId, sku: activeProducts.find(p => p.id === r.productId)?.sku || '' })))
  }

  const handleConfirm = () => {
    setLoading(true)
    setTimeout(() => {
      summary.forEach(r => dispatch(addMovement({ productId: r.productId, warehouseId, type: 'INTAKE', quantity: +r.quantity, referenceId: null, createdBy: user.id })))
      toast.success(`${summary.length} intake${summary.length > 1 ? 's' : ''} recorded`)
      setRows([{ ...EMPTY_INTAKE }])
      setSummary(null)
      setLoading(false)
    }, 500)
  }

  if (summary) {
    return (
      <div className="bg-white rounded-xl border border-gray-200 p-6">
        <div className="flex items-center gap-3 mb-6">
          <button onClick={() => setSummary(null)} className="flex items-center gap-1.5 text-sm text-gray-500 hover:text-teal-600 transition-colors">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" /></svg>
            Back to Edit
          </button>
          <div className="h-4 w-px bg-gray-200" />
          <h2 className="text-lg font-bold text-gray-900">Review Intake</h2>
          <span className="text-sm text-gray-500">— confirm before submitting</span>
        </div>
        <div className="bg-teal-50 border border-teal-200 rounded-xl px-4 py-3 mb-5 text-sm text-teal-800">
          You are about to add stock to <strong>{myWarehouse?.name || 'your warehouse'}</strong>. Please review the items below.
        </div>
        <div className="border border-gray-200 rounded-xl overflow-hidden mb-6">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-100">
                <th className="text-left px-5 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wide">#</th>
                <th className="text-left px-5 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wide">Product</th>
                <th className="text-left px-5 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wide">SKU</th>
                <th className="text-left px-5 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wide">Qty to Add</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {summary.map((r, i) => (
                <tr key={i} className="hover:bg-gray-50/60">
                  <td className="px-5 py-3 text-gray-400 text-xs">{String(i + 1).padStart(2, '0')}</td>
                  <td className="px-5 py-3 font-medium text-gray-900">{r.productName}</td>
                  <td className="px-5 py-3 font-mono text-xs text-gray-500">{r.sku}</td>
                  <td className="px-5 py-3 font-semibold text-teal-700">+{r.quantity}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="flex gap-3">
          <button onClick={() => setSummary(null)} className="flex-1 py-2.5 border border-gray-300 text-gray-700 rounded-xl text-sm hover:bg-gray-50 font-medium">Edit Items</button>
          <SubmitBtn label={`Confirm & Record ${summary.length} Intake${summary.length > 1 ? 's' : ''}`} loading={loading} />
        </div>
      </div>
    )
  }

  const validCount = rows.filter(r => r.productId).length

  return (
    <div className="bg-white rounded-xl border border-gray-200 p-6">
      <PageHeader
        title="Record Stock Intake"
        subtitle={`Adding stock to ${myWarehouse?.name || 'your warehouse'}`}
        action={<ViewToggle mode={viewMode} onChange={setViewMode} />}
      />
      <form onSubmit={handleSubmit} className="space-y-4">
        {viewMode === 'table' ? (
          <div className="rounded-xl border border-gray-200 overflow-visible">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-gray-50 border-b border-gray-100">
                  <th className="text-left px-3 py-2.5 text-xs font-semibold text-gray-500 uppercase tracking-wide w-10">#</th>
                  <th className="text-left px-3 py-2.5 text-xs font-semibold text-gray-500 uppercase tracking-wide">Product</th>
                  <th className="text-left px-3 py-2.5 text-xs font-semibold text-gray-500 uppercase tracking-wide w-36">Qty to Add</th>
                  <th className="w-10"></th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50">
                {rows.map((row, i) => (
                  <tr key={i}>
                    <td className="px-3 py-2 text-gray-400 text-xs font-mono">{String(i + 1).padStart(2, '0')}</td>
                    <td className="px-3 py-2">
                      <FilterableSelect
                        value={row.productId}
                        onChange={val => updateRow(i, 'productId', val)}
                        options={activeProducts.map(p => ({ value: p.id, label: `${p.name} (${p.sku})` }))}
                        placeholder="Select a product…"
                        required
                      />
                    </td>
                    <td className="px-3 py-2">
                      <input type="number" min="1" value={row.quantity} onChange={e => updateRow(i, 'quantity', e.target.value)} placeholder="e.g. 50" required className="w-full px-2.5 py-1.5 border border-gray-300 rounded-lg text-sm bg-white focus:outline-none focus:ring-2 focus:ring-teal-600" />
                    </td>
                    <td className="px-3 py-2">
                      <button type="button" onClick={() => removeRow(i)} disabled={rows.length === 1} className="w-7 h-7 flex items-center justify-center rounded-lg text-gray-300 hover:text-red-500 hover:bg-red-50 disabled:opacity-0 disabled:pointer-events-none transition-colors">
                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /></svg>
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            <button type="button" onClick={() => setRows(rs => [...rs, { ...EMPTY_INTAKE }])} className="w-full py-2.5 text-sm text-gray-500 hover:text-teal-600 hover:bg-teal-50/40 transition-colors font-medium flex items-center justify-center gap-1.5 border-t border-gray-100">
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" /></svg>
              Add row
            </button>
          </div>
        ) : (
          <>
            {rows.map((row, i) => (
              <div key={i} className="bg-gray-50 border border-gray-200 rounded-xl p-4">
                <div className="flex items-center justify-between mb-3">
                  <span className="text-xs font-bold text-gray-400 uppercase tracking-widest">Item {String(i + 1).padStart(2, '0')}</span>
                  <button type="button" onClick={() => removeRow(i)} disabled={rows.length === 1} className="w-7 h-7 flex items-center justify-center rounded-lg text-gray-300 hover:text-red-500 hover:bg-red-50 disabled:opacity-0 disabled:pointer-events-none transition-colors">
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
                      placeholder="Select a product…"
                      required
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-medium text-gray-500 mb-1">Quantity <span className="text-red-400">*</span></label>
                    <input type="number" min="1" value={row.quantity} onChange={e => updateRow(i, 'quantity', e.target.value)} placeholder="e.g. 50" required className="w-full px-3 py-2.5 border border-gray-300 rounded-xl text-sm bg-white focus:outline-none focus:ring-2 focus:ring-teal-600" />
                  </div>
                </div>
              </div>
            ))}
            <button type="button" onClick={() => setRows(rs => [...rs, { ...EMPTY_INTAKE }])} className="w-full py-2.5 border-2 border-dashed border-gray-300 rounded-xl text-sm text-gray-500 hover:border-teal-400 hover:text-teal-600 hover:bg-teal-50/40 transition-colors font-medium flex items-center justify-center gap-2">
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" /></svg>
              Add another item
            </button>
          </>
        )}
        <div className="pt-1">
          <SubmitBtn label={`Review ${validCount > 1 ? `${validCount} Intakes` : 'Intake'}`} loading={false} />
        </div>
      </form>
    </div>
  )
}

// ── Sale Panel ────────────────────────────────────────────────────────────────

const EMPTY_SALE = { productId: '', quantity: '' }

function SalePanel() {
  const { products, stockLevels, warehouses } = useSelector(s => s.stock)
  const { warehouseId, user } = useSelector(s => s.auth)
  const dispatch = useDispatch()
  const [rows, setRows] = useState([{ ...EMPTY_SALE }])
  const [rowErrors, setRowErrors] = useState({})
  const [summary, setSummary] = useState(null)
  const [loading, setLoading] = useState(false)
  const [viewMode, setViewMode] = useState('cards')
  const myWarehouse = warehouses.find(w => w.id === warehouseId)
  const activeProducts = products.filter(p => p.isActive)

  const getStock = productId => productId ? (stockLevels.find(sl => sl.productId === productId && sl.warehouseId === warehouseId)?.currentStock ?? 0) : null
  const getProd = productId => products.find(p => p.id === productId)

  const updateRow = (i, field, val) => {
    setRows(rs => rs.map((r, idx) => idx === i ? { ...r, [field]: val } : r))
    setRowErrors(e => { const n = { ...e }; delete n[i]; return n })
  }
  const removeRow = i => {
    setRows(rs => rs.filter((_, idx) => idx !== i))
    setRowErrors(e => {
      const n = {}
      Object.keys(e).forEach(k => { const ki = +k; if (ki !== i) n[ki > i ? ki - 1 : ki] = e[k] })
      return n
    })
  }

  const handleSubmit = e => {
    e.preventDefault()
    const valid = rows.filter(r => r.productId && r.quantity)
    if (!valid.length) return
    const newErrors = {}
    rows.forEach((r, i) => {
      if (!r.productId || !r.quantity) return
      const avail = getStock(r.productId) ?? 0
      if (+r.quantity > avail) newErrors[i] = `Only ${avail} ${getProd(r.productId)?.unitOfMeasure || 'units'} available`
    })
    if (Object.keys(newErrors).length > 0) { setRowErrors(newErrors); return }
    setSummary(valid.map(r => {
      const prod = getProd(r.productId)
      return { ...r, productName: prod?.name || r.productId, sku: prod?.sku || '', unit: prod?.unitOfMeasure || 'units', available: getStock(r.productId) ?? 0 }
    }))
  }

  const handleConfirm = () => {
    setLoading(true)
    setTimeout(() => {
      summary.forEach(r => dispatch(addMovement({ productId: r.productId, warehouseId, type: 'OUTBOUND_SALE', quantity: +r.quantity, referenceId: null, createdBy: user.id })))
      toast.success(`${summary.length} sale${summary.length > 1 ? 's' : ''} recorded`)
      setRows([{ ...EMPTY_SALE }])
      setRowErrors({})
      setSummary(null)
      setLoading(false)
    }, 500)
  }

  if (summary) {
    return (
      <div className="bg-white rounded-xl border border-gray-200 p-6">
        <div className="flex items-center gap-3 mb-6">
          <button onClick={() => setSummary(null)} className="flex items-center gap-1.5 text-sm text-gray-500 hover:text-orange-600 transition-colors">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" /></svg>
            Back to Edit
          </button>
          <div className="h-4 w-px bg-gray-200" />
          <h2 className="text-lg font-bold text-gray-900">Review Sale</h2>
          <span className="text-sm text-gray-500">— confirm before submitting</span>
        </div>
        <div className="bg-orange-50 border border-orange-200 rounded-xl px-4 py-3 mb-5 text-sm text-orange-800">
          You are about to record outbound sales from <strong>{myWarehouse?.name || 'your warehouse'}</strong>. Stock levels will be deducted.
        </div>
        <div className="border border-gray-200 rounded-xl overflow-hidden mb-6">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-100">
                <th className="text-left px-5 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wide">#</th>
                <th className="text-left px-5 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wide">Product</th>
                <th className="text-left px-5 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wide">SKU</th>
                <th className="text-left px-5 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wide">Qty Sold</th>
                <th className="text-left px-5 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wide">Available</th>
                <th className="text-left px-5 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wide">After Sale</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {summary.map((r, i) => {
                const after = r.available - +r.quantity
                return (
                  <tr key={i} className="hover:bg-gray-50/60">
                    <td className="px-5 py-3 text-gray-400 text-xs">{String(i + 1).padStart(2, '0')}</td>
                    <td className="px-5 py-3 font-medium text-gray-900">{r.productName}</td>
                    <td className="px-5 py-3 font-mono text-xs text-gray-500">{r.sku}</td>
                    <td className="px-5 py-3 font-semibold text-orange-600">-{r.quantity} {r.unit}</td>
                    <td className="px-5 py-3 text-gray-600">{r.available} {r.unit}</td>
                    <td className="px-5 py-3 font-semibold">
                      <span className={after <= 0 ? 'text-red-600' : after < 5 ? 'text-amber-600' : 'text-gray-700'}>{after} {r.unit}</span>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
        <div className="flex gap-3">
          <button onClick={() => setSummary(null)} className="flex-1 py-2.5 border border-gray-300 text-gray-700 rounded-xl text-sm hover:bg-gray-50 font-medium">Edit Items</button>
          <SubmitBtn label={`Confirm & Record ${summary.length} Sale${summary.length > 1 ? 's' : ''}`} loading={loading} color="orange" />
        </div>
      </div>
    )
  }

  const validCount = rows.filter(r => r.productId).length

  return (
    <div className="bg-white rounded-xl border border-gray-200 p-6">
      <PageHeader title="Record Outbound Sale" subtitle={`Selling from ${myWarehouse?.name || 'your warehouse'}`} action={<ViewToggle mode={viewMode} onChange={setViewMode} />} />
      <form onSubmit={handleSubmit} className="space-y-4">
        {viewMode === 'table' ? (
          <div className="rounded-xl border border-gray-200 overflow-visible">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-gray-50 border-b border-gray-100">
                  <th className="text-left px-3 py-2.5 text-xs font-semibold text-gray-500 uppercase tracking-wide w-10">#</th>
                  <th className="text-left px-3 py-2.5 text-xs font-semibold text-gray-500 uppercase tracking-wide">Product</th>
                  <th className="text-left px-3 py-2.5 text-xs font-semibold text-gray-500 uppercase tracking-wide w-28">Available</th>
                  <th className="text-left px-3 py-2.5 text-xs font-semibold text-gray-500 uppercase tracking-wide w-36">Qty to Sell</th>
                  <th className="w-10"></th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50">
                {rows.map((row, i) => {
                  const avail = getStock(row.productId)
                  const prod = getProd(row.productId)
                  return (
                    <tr key={i}>
                      <td className="px-3 py-2 text-gray-400 text-xs font-mono">{String(i + 1).padStart(2, '0')}</td>
                      <td className="px-3 py-2">
                        <FilterableSelect
                          value={row.productId}
                          onChange={val => updateRow(i, 'productId', val)}
                          options={activeProducts.map(p => ({ value: p.id, label: `${p.name} (${p.sku})` }))}
                          placeholder="Select a product…"
                          required
                        />
                      </td>
                      <td className="px-3 py-2">
                        {avail !== null ? (
                          <span className={`text-xs font-medium ${avail === 0 ? 'text-red-600' : avail < 10 ? 'text-amber-600' : 'text-teal-600'}`}>
                            {avail} {prod?.unitOfMeasure || 'units'}
                          </span>
                        ) : <span className="text-gray-300 text-xs">—</span>}
                      </td>
                      <td className="px-3 py-2">
                        <input type="number" min="1" value={row.quantity} onChange={e => updateRow(i, 'quantity', e.target.value)} placeholder="e.g. 5" required className="w-full px-2.5 py-1.5 border border-gray-300 rounded-lg text-sm bg-white focus:outline-none focus:ring-2 focus:ring-orange-400" />
                        {rowErrors[i] && <p className="text-xs text-red-600 mt-0.5">{rowErrors[i]}</p>}
                      </td>
                      <td className="px-3 py-2">
                        <button type="button" onClick={() => removeRow(i)} disabled={rows.length === 1} className="w-7 h-7 flex items-center justify-center rounded-lg text-gray-300 hover:text-red-500 hover:bg-red-50 disabled:opacity-0 disabled:pointer-events-none transition-colors">
                          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /></svg>
                        </button>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
            <button type="button" onClick={() => setRows(rs => [...rs, { ...EMPTY_SALE }])} className="w-full py-2.5 text-sm text-gray-500 hover:text-orange-600 hover:bg-orange-50/40 transition-colors font-medium flex items-center justify-center gap-1.5 border-t border-gray-100">
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" /></svg>
              Add row
            </button>
          </div>
        ) : (
          <>
            {rows.map((row, i) => {
              const avail = getStock(row.productId)
              const prod = getProd(row.productId)
              return (
                <div key={i} className="bg-gray-50 border border-gray-200 rounded-xl p-4">
                  <div className="flex items-center justify-between mb-3">
                    <span className="text-xs font-bold text-gray-400 uppercase tracking-widest">Item {String(i + 1).padStart(2, '0')}</span>
                    <button type="button" onClick={() => removeRow(i)} disabled={rows.length === 1} className="w-7 h-7 flex items-center justify-center rounded-lg text-gray-300 hover:text-red-500 hover:bg-red-50 disabled:opacity-0 disabled:pointer-events-none transition-colors">
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
                        placeholder="Select a product…"
                        required
                      />
                      {avail !== null && (
                        <span className={`mt-1.5 inline-block text-xs px-2.5 py-1 rounded-lg font-medium ${avail === 0 ? 'bg-red-50 text-red-600' : avail < 10 ? 'bg-amber-50 text-amber-600' : 'bg-teal-50 text-teal-600'}`}>
                          {avail} {prod?.unitOfMeasure || 'units'} available
                        </span>
                      )}
                    </div>
                    <div>
                      <label className="block text-xs font-medium text-gray-500 mb-1">Quantity <span className="text-red-400">*</span></label>
                      <input type="number" min="1" value={row.quantity} onChange={e => updateRow(i, 'quantity', e.target.value)} placeholder="e.g. 5" required className="w-full px-3 py-2.5 border border-gray-300 rounded-xl text-sm bg-white focus:outline-none focus:ring-2 focus:ring-teal-600" />
                      {rowErrors[i] && (
                        <span className="mt-1.5 inline-block text-xs text-red-600 bg-red-50 px-2.5 py-1 rounded-lg border border-red-200">{rowErrors[i]}</span>
                      )}
                    </div>
                  </div>
                </div>
              )
            })}
            <button type="button" onClick={() => setRows(rs => [...rs, { ...EMPTY_SALE }])} className="w-full py-2.5 border-2 border-dashed border-gray-300 rounded-xl text-sm text-gray-500 hover:border-orange-400 hover:text-orange-600 hover:bg-orange-50/40 transition-colors font-medium flex items-center justify-center gap-2">
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" /></svg>
              Add another item
            </button>
          </>
        )}
        <div className="pt-1">
          <SubmitBtn label={`Review ${validCount > 1 ? `${validCount} Sales` : 'Sale'}`} loading={false} color="orange" />
        </div>
      </form>
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
  const [summary, setSummary] = useState(null)
  const [loading, setLoading] = useState(false)
  const [viewMode, setViewMode] = useState('cards')
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
    const dest = warehouses.find(w => w.id === toWarehouseId)
    setSummary({
      dest,
      items: valid.map(r => {
        const prod = products.find(p => p.id === r.productId)
        return { ...r, productName: prod?.name || r.productId, sku: prod?.sku || '', unit: prod?.unitOfMeasure || 'units', available: getStock(r.productId) }
      })
    })
  }

  const handleConfirm = () => {
    setLoading(true)
    setTimeout(() => {
      dispatch(addTransfer(summary.items.map(r => ({ productId: r.productId, fromWarehouseId: warehouseId, toWarehouseId, quantity: +r.quantity, requestedBy: user.id }))))
      toast.success(`Transfer request submitted for ${summary.items.length} item${summary.items.length > 1 ? 's' : ''} — awaiting manager approval`)
      setRows([{ ...EMPTY_TRANSFER }])
      setToWarehouseId('')
      setSummary(null)
      setLoading(false)
    }, 500)
  }

  if (summary) {
    return (
      <div className="bg-white rounded-xl border border-gray-200 p-6">
        <div className="flex items-center gap-3 mb-6">
          <button onClick={() => setSummary(null)} className="flex items-center gap-1.5 text-sm text-gray-500 hover:text-blue-600 transition-colors">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" /></svg>
            Back to Edit
          </button>
          <div className="h-4 w-px bg-gray-200" />
          <h2 className="text-lg font-bold text-gray-900">Review Transfer Request</h2>
        </div>
        <div className="bg-blue-50 border border-blue-200 rounded-xl px-4 py-3 mb-5 text-sm text-blue-800">
          <span className="font-semibold">{myWarehouse?.name || 'Your warehouse'}</span>
          <span className="mx-2">→</span>
          <span className="font-semibold">{summary.dest?.name || 'Destination'}</span>
          <span className="text-blue-600 ml-2">· This request will need manager approval before stock moves.</span>
        </div>
        <div className="border border-gray-200 rounded-xl overflow-hidden mb-6">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-100">
                <th className="text-left px-5 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wide">#</th>
                <th className="text-left px-5 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wide">Product</th>
                <th className="text-left px-5 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wide">SKU</th>
                <th className="text-left px-5 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wide">Qty Requested</th>
                <th className="text-left px-5 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wide">Available</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {summary.items.map((r, i) => (
                <tr key={i} className="hover:bg-gray-50/60">
                  <td className="px-5 py-3 text-gray-400 text-xs">{String(i + 1).padStart(2, '0')}</td>
                  <td className="px-5 py-3 font-medium text-gray-900">{r.productName}</td>
                  <td className="px-5 py-3 font-mono text-xs text-gray-500">{r.sku}</td>
                  <td className="px-5 py-3 font-semibold text-blue-700">{r.quantity} {r.unit}</td>
                  <td className="px-5 py-3">
                    <span className={`text-xs font-medium ${r.available <= 0 ? 'text-red-600' : r.available < 10 ? 'text-amber-600' : 'text-gray-600'}`}>{r.available} {r.unit}</span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="flex gap-3">
          <button onClick={() => setSummary(null)} className="flex-1 py-2.5 border border-gray-300 text-gray-700 rounded-xl text-sm hover:bg-gray-50 font-medium">Edit Items</button>
          <SubmitBtn label={`Submit Request (${summary.items.length} item${summary.items.length > 1 ? 's' : ''})`} loading={loading} color="blue" />
        </div>
      </div>
    )
  }

  const validCount = rows.filter(r => r.productId).length

  return (
    <div className="bg-white rounded-xl border border-gray-200 p-6">
      <PageHeader title="Request Transfer Out" subtitle={`Sending stock from ${myWarehouse?.name || 'your warehouse'} to another`} action={<ViewToggle mode={viewMode} onChange={setViewMode} />} />
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1.5">Destination Warehouse <span className="text-red-400">*</span></label>
          <select value={toWarehouseId} onChange={e => setToWarehouseId(e.target.value)} required className="w-full px-3.5 py-2.5 border border-gray-300 rounded-xl text-sm bg-white focus:outline-none focus:ring-2 focus:ring-teal-600">
            <option value="">Select destination…</option>
            {otherWarehouses.map(w => <option key={w.id} value={w.id}>{w.name}</option>)}
          </select>
        </div>

        {viewMode === 'table' ? (
          <div className="rounded-xl border border-gray-200 overflow-visible">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-gray-50 border-b border-gray-100">
                  <th className="text-left px-3 py-2.5 text-xs font-semibold text-gray-500 uppercase tracking-wide w-10">#</th>
                  <th className="text-left px-3 py-2.5 text-xs font-semibold text-gray-500 uppercase tracking-wide">Product</th>
                  <th className="text-left px-3 py-2.5 text-xs font-semibold text-gray-500 uppercase tracking-wide w-28">Available</th>
                  <th className="text-left px-3 py-2.5 text-xs font-semibold text-gray-500 uppercase tracking-wide w-36">Qty</th>
                  <th className="w-10"></th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50">
                {rows.map((row, i) => {
                  const avail = row.productId ? getStock(row.productId) : null
                  const prod = products.find(p => p.id === row.productId)
                  return (
                    <tr key={i}>
                      <td className="px-3 py-2 text-gray-400 text-xs font-mono">{String(i + 1).padStart(2, '0')}</td>
                      <td className="px-3 py-2">
                        <FilterableSelect
                          value={row.productId}
                          onChange={val => updateRow(i, 'productId', val)}
                          options={activeProducts.map(p => ({ value: p.id, label: `${p.name} (${p.sku})` }))}
                          placeholder="Select a product…"
                          required
                        />
                      </td>
                      <td className="px-3 py-2">
                        {avail !== null ? (
                          <span className={`text-xs font-medium ${avail === 0 ? 'text-red-600' : avail < 10 ? 'text-amber-600' : 'text-teal-600'}`}>
                            {avail} {prod?.unitOfMeasure || 'units'}
                          </span>
                        ) : <span className="text-gray-300 text-xs">—</span>}
                      </td>
                      <td className="px-3 py-2">
                        <input type="number" min="1" max={avail ?? undefined} value={row.quantity} onChange={e => updateRow(i, 'quantity', e.target.value)} placeholder="Qty" required className="w-full px-2.5 py-1.5 border border-gray-300 rounded-lg text-sm bg-white focus:outline-none focus:ring-2 focus:ring-blue-500" />
                      </td>
                      <td className="px-3 py-2">
                        <button type="button" onClick={() => removeRow(i)} disabled={rows.length === 1} className="w-7 h-7 flex items-center justify-center rounded-lg text-gray-300 hover:text-red-500 hover:bg-red-50 disabled:opacity-0 disabled:pointer-events-none transition-colors">
                          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /></svg>
                        </button>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
            <button type="button" onClick={() => setRows(rs => [...rs, { ...EMPTY_TRANSFER }])} className="w-full py-2.5 text-sm text-gray-500 hover:text-blue-600 hover:bg-blue-50/40 transition-colors font-medium flex items-center justify-center gap-1.5 border-t border-gray-100">
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" /></svg>
              Add row
            </button>
          </div>
        ) : (
          <>
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
                    <div>
                      <label className="block text-xs font-medium text-gray-500 mb-1">Product <span className="text-red-400">*</span></label>
                      <FilterableSelect
                        value={row.productId}
                        onChange={val => updateRow(i, 'productId', val)}
                        options={activeProducts.map(p => ({ value: p.id, label: `${p.name} (${p.sku})` }))}
                        placeholder="Select a product…"
                        required
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-medium text-gray-500 mb-1">Quantity <span className="text-red-400">*</span></label>
                      <input type="number" min="1" max={avail ?? undefined} value={row.quantity} onChange={e => updateRow(i, 'quantity', e.target.value)} placeholder="Qty" required className="w-full px-3 py-2.5 border border-gray-300 rounded-xl text-sm bg-white focus:outline-none focus:ring-2 focus:ring-teal-600" />
                      {avail !== null && (
                        <span className={`mt-1.5 inline-block text-xs px-2.5 py-1 rounded-lg font-medium ${avail === 0 ? 'bg-red-50 text-red-600' : avail < 10 ? 'bg-amber-50 text-amber-600' : 'bg-teal-50 text-teal-600'}`}>
                          {avail} {prod?.unitOfMeasure || 'units'} available
                        </span>
                      )}
                    </div>
                  </div>
                </div>
              )
            })}
            <button type="button" onClick={() => setRows(rs => [...rs, { ...EMPTY_TRANSFER }])} className="w-full py-2.5 border-2 border-dashed border-gray-300 rounded-xl text-sm text-gray-500 hover:border-teal-400 hover:text-teal-600 hover:bg-teal-50/40 transition-colors font-medium flex items-center justify-center gap-2">
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" /></svg>
              Add another item
            </button>
          </>
        )}
        <div className="pt-1"><SubmitBtn label={`Review Request (${validCount} item${validCount !== 1 ? 's' : ''})`} loading={false} color="blue" /></div>
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
  const [confirm, setConfirm] = useState(null)

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
      <div className="bg-white rounded-xl border border-gray-200">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-100">
                {['Product', 'From', 'Qty', 'Date', 'Actions'].map(h => (
                  <th key={h} className="text-left px-5 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wide">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {incoming.length === 0 ? (
                <tr><td colSpan={5} className="px-5 py-12 text-center text-gray-400 text-sm">No incoming transfers awaiting your action.</td></tr>
              ) : incoming.map(t => (
                <tr key={t.id} className="hover:bg-gray-50/60">
                  <td className="px-5 py-3 font-semibold text-gray-900">{t.productName}</td>
                  <td className="px-5 py-3 text-gray-600">{t.fromName}</td>
                  <td className="px-5 py-3 font-medium text-gray-900">{t.quantity}</td>
                  <td className="px-5 py-3 text-gray-400 text-xs whitespace-nowrap">{fmtDT(t.updatedAt)}</td>
                  <td className="px-5 py-3">
                    <div className="flex gap-2">
                      <button onClick={() => setConfirm({ action: () => handleAccept(t), title: 'Accept Delivery', message: `Accept ${t.quantity} units of ${t.productName} from ${t.fromName}? Stock will be added to your warehouse.`, label: 'Accept Delivery' })} className="px-3 py-1.5 bg-teal-600 text-white text-xs rounded-lg hover:bg-teal-700 font-medium">Accept</button>
                      <button onClick={() => setConfirm({ action: () => { dispatch(rejectDelivery(t.id)); toast.info('Delivery rejected') }, title: 'Reject Delivery', message: `Reject the delivery of ${t.quantity} units of ${t.productName} from ${t.fromName}?`, label: 'Reject Delivery', danger: true })} className="px-3 py-1.5 bg-red-50 text-red-600 text-xs rounded-lg hover:bg-red-100 font-medium">Reject</button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
      {confirm && <ConfirmDialog title={confirm.title} message={confirm.message} danger={confirm.danger} confirmLabel={confirm.label} onConfirm={() => { confirm.action(); setConfirm(null) }} onCancel={() => setConfirm(null)} />}
    </div>
  )
}

// ── Outgoing Transfers Panel ──────────────────────────────────────────────────

function OutgoingPanel() {
  const { transfers } = useSelector(s => s.transfers)
  const { products, warehouses } = useSelector(s => s.stock)
  const { warehouseId, user } = useSelector(s => s.auth)
  const dispatch = useDispatch()
  const [confirm, setConfirm] = useState(null)

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
      <div className="bg-white rounded-xl border border-gray-200">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-100">
                {['Product', 'To', 'Qty', 'Approved', 'Actions'].map(h => (
                  <th key={h} className="text-left px-5 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wide">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {outgoing.length === 0 ? (
                <tr><td colSpan={5} className="px-5 py-12 text-center text-gray-400 text-sm">No approved transfers awaiting dispatch.</td></tr>
              ) : outgoing.map(t => (
                <tr key={t.id} className="hover:bg-gray-50/60">
                  <td className="px-5 py-3 font-semibold text-gray-900">{t.productName}</td>
                  <td className="px-5 py-3 text-gray-600">{t.toName}</td>
                  <td className="px-5 py-3 font-medium text-gray-900">{t.quantity}</td>
                  <td className="px-5 py-3 text-gray-400 text-xs whitespace-nowrap">{fmtDT(t.updatedAt)}</td>
                  <td className="px-5 py-3">
                    <button onClick={() => setConfirm({ transfer: t })} className="px-3 py-1.5 bg-blue-500 text-white text-xs rounded-lg hover:bg-blue-600 font-medium">
                      Confirm Dispatch
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
      {confirm && (
        <ConfirmDialog
          title="Confirm Dispatch"
          message={`Dispatch ${confirm.transfer.quantity} units of ${confirm.transfer.productName} to ${confirm.transfer.toName}? Stock will be deducted from your warehouse and the transfer marked as IN TRANSIT.`}
          confirmLabel="Confirm Dispatch"
          onConfirm={() => { handleDispatch(confirm.transfer); setConfirm(null) }}
          onCancel={() => setConfirm(null)}
        />
      )}
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
    <div className="bg-white rounded-xl border border-gray-200 p-6">
      <PageHeader title="Submit Reconciliation" subtitle={`Physical count for ${myWarehouse?.name || 'your warehouse'}`} />
      <form onSubmit={handleSubmit} className="space-y-5 w-full">
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
    </div>
  )
}

// ── Complaints Panel ──────────────────────────────────────────────────────────

function ComplaintsPanel() {
  const { user: me, companyId, companyName } = useSelector(s => s.auth)
  const dispatch = useDispatch()
  const [form, setForm] = useState({ subject: '', priority: 'MEDIUM', message: '' })
  const [loading, setLoading] = useState(false)
  const [submitted, setSubmitted] = useState(false)
  const ch = e => setForm(f => ({ ...f, [e.target.name]: e.target.value }))

  const handleSubmit = e => {
    e.preventDefault()
    if (!form.subject || !form.message) return
    setLoading(true)
    setTimeout(() => {
      dispatch(submitComplaint({ subject: form.subject, priority: form.priority, message: form.message, submittedBy: me?.name, email: me?.email, companyName, companyId }))
      setLoading(false)
      setSubmitted(true)
    }, 500)
  }

  if (submitted) {
    return (
      <div className="bg-white rounded-xl border border-gray-200 p-6">
        <div className="flex flex-col items-center justify-center py-12 gap-4">
          <div className="w-14 h-14 rounded-full bg-teal-50 flex items-center justify-center">
            <svg className="w-7 h-7 text-teal-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" /></svg>
          </div>
          <h3 className="text-lg font-bold text-gray-900">Feedback Submitted</h3>
          <p className="text-sm text-gray-500 text-center max-w-sm">Your feedback has been submitted and will be reviewed by the admin team.</p>
          <button onClick={() => { setSubmitted(false); setForm({ subject: '', priority: 'MEDIUM', message: '' }) }} className="px-5 py-2 bg-teal-600 text-white text-sm font-semibold rounded-xl hover:bg-teal-700 transition-colors">Submit Another</button>
        </div>
      </div>
    )
  }

  return (
    <div className="bg-white rounded-xl border border-gray-200 p-6">
      <PageHeader title="Feedback & Support" subtitle="Submit a complaint, inquiry, or suggestion to the admin team" />
      <form onSubmit={handleSubmit} className="space-y-5 w-full">
        <Field label="Subject" name="subject" value={form.subject} onChange={ch} placeholder="Brief description of the issue" required />
        <Select label="Priority" name="priority" value={form.priority} onChange={ch}>
          <option value="HIGH">High</option>
          <option value="MEDIUM">Medium</option>
          <option value="LOW">Low</option>
        </Select>
        <Field label="Message">
          <textarea name="message" value={form.message} onChange={ch} rows={5} className="w-full px-3.5 py-2.5 border border-gray-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-teal-600 resize-none" placeholder="Describe the issue in detail…" required />
        </Field>
        <div className="pt-1"><SubmitBtn label="Submit Feedback" loading={loading} /></div>
      </form>
    </div>
  )
}

// ── Main ──────────────────────────────────────────────────────────────────────

export default function StaffDashboard() {
  const [activeTab, setActiveTab] = useState('stock')
  const { transfers } = useSelector(s => s.transfers)
  const { warehouseId } = useSelector(s => s.auth)
  const { stockLevels, warehouses } = useSelector(s => s.stock)
  const myWarehouse = warehouses.find(w => w.id === warehouseId) || null
  const incomingCount = transfers.filter(t => t.toWarehouseId === warehouseId && t.status === 'IN_TRANSIT').length
  const outgoingCount = transfers.filter(t => t.fromWarehouseId === warehouseId && t.status === 'APPROVED').length

  const navItems = [
    { id: 'stock', label: 'Stock', badge: stockLevels.filter(sl => sl.warehouseId === warehouseId && sl.currentStock < sl.threshold).length,
      icon: <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 10h18M3 14h18M10 6v12M14 6v12M5 6h14a1 1 0 011 1v10a1 1 0 01-1 1H5a1 1 0 01-1-1V7a1 1 0 011-1z" /></svg>,
    },
    { id: 'intake', label: 'Record Intake', badge: 0, icon: <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 13l-7 7-7-7m14-8l-7 7-7-7" /></svg> },
    { id: 'sale', label: 'Record Sale', badge: 0, icon: <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 11l7-7 7 7M5 19l7-7 7 7" /></svg> },
    { id: 'request-transfer', label: 'Request Transfer', badge: 0, icon: <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" /></svg> },
    { id: 'incoming', label: 'Incoming', badge: incomingCount, icon: <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" /></svg> },
    { id: 'outgoing', label: 'Outgoing', badge: outgoingCount, icon: <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4" /></svg> },
    { id: 'reconciliation', label: 'Reconciliation', badge: 0, icon: <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4" /></svg> },
    { id: 'complaints', label: 'Feedback & Support', badge: 0,
      icon: <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" /></svg>
    },
  ]

  return (
    <Layout title="Warehouse Staff" navItems={navItems} activeTab={activeTab} onTabChange={setActiveTab}>
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
      {activeTab === 'stock' && <StockPanel />}
      {activeTab === 'intake' && <IntakePanel />}
      {activeTab === 'sale' && <SalePanel />}
      {activeTab === 'request-transfer' && <RequestTransferPanel />}
      {activeTab === 'incoming' && <IncomingPanel />}
      {activeTab === 'outgoing' && <OutgoingPanel />}
      {activeTab === 'reconciliation' && <ReconciliationPanel />}
      {activeTab === 'complaints' && <ComplaintsPanel />}
    </Layout>
  )
}
