import { useState, useRef } from 'react'
import { useSelector, useDispatch } from 'react-redux'
import { toast } from 'react-toastify'
import Layout from '../../components/layout/Layout'
import StatusBadge from '../../components/shared/StatusBadge'
import StatCard from '../../components/shared/StatCard'
import { setCompanyLogo, registerLocalUser } from '../../store/slices/authSlice'
import {
  addWarehouse, updateWarehouse, toggleWarehouseActive,
  addProduct, updateProduct, toggleProductActive,
} from '../../store/slices/stockSlice'
import {
  addUser, updateUserRole, deactivateUser, reactivateUser,
  assignWarehouse, removeAssignment,
} from '../../store/slices/usersSlice'

function Modal({ title, onClose, children, wide }) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4" onClick={onClose}>
      <div className={`bg-white rounded-2xl shadow-xl p-6 w-full ${wide ? 'max-w-2xl' : 'max-w-md'}`} onClick={e => e.stopPropagation()}>
        <div className="flex items-center justify-between mb-5">
          <h3 className="text-base font-semibold text-gray-900">{title}</h3>
          <button onClick={onClose} className="p-1 rounded text-gray-400 hover:text-gray-600 hover:bg-gray-100">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
        {children}
      </div>
    </div>
  )
}

function Field({ label, children, ...props }) {
  if (children) return (
    <div>
      <label className="block text-sm font-medium text-gray-700 mb-1">{label}</label>
      {children}
    </div>
  )
  return (
    <div>
      <label className="block text-sm font-medium text-gray-700 mb-1">{label}</label>
      <input className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-teal-600 focus:border-transparent" {...props} />
    </div>
  )
}

function BtnRow({ onClose, submitLabel }) {
  return (
    <div className="flex gap-2 pt-2">
      <button type="button" onClick={onClose} className="flex-1 py-2 border border-gray-300 text-gray-700 rounded-lg text-sm hover:bg-gray-50">Cancel</button>
      <button type="submit" className="flex-1 py-2 bg-teal-600 text-white rounded-lg text-sm font-medium hover:bg-teal-700">{submitLabel}</button>
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
        type="text"
        value={value}
        onChange={e => onChange(e.target.value)}
        placeholder={placeholder}
        className="pl-9 pr-4 py-1.5 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-teal-600 focus:border-transparent w-56"
      />
    </div>
  )
}

const AddIcon = () => (
  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
  </svg>
)

const CSV_TEMPLATE = `name,sku,unitOfMeasure,defaultThreshold\nIndustrial Laptop,LAP-001,units,20\nOffice Chair,CHR-002,units,15\nPrinter Ink Cartridge,INK-003,boxes,50`

function parseCSV(text) {
  const lines = text.trim().split('\n').filter(l => l.trim())
  if (lines.length < 2) return []
  const headers = lines[0].split(',').map(h => h.trim())
  return lines.slice(1).map(line => {
    const vals = line.split(',').map(v => v.trim())
    return headers.reduce((obj, h, i) => ({ ...obj, [h]: vals[i] || '' }), {})
  }).filter(r => r.name && r.sku)
}

const fmtDT = d => new Date(d).toLocaleString('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })

const ROLE_PILL = {
  MANAGER: 'bg-purple-100 text-purple-700',
  WAREHOUSE_STAFF: 'bg-teal-100 text-teal-700',
  PROCUREMENT_OFFICER: 'bg-blue-100 text-blue-700',
  ADMIN: 'bg-gray-100 text-gray-700',
}

function WarehouseDetail({ warehouse, onBack }) {
  const { warehouseAssignments, users } = useSelector(s => s.users)
  const { stockLevels, products, movements, warehouses } = useSelector(s => s.stock)
  const { transfers } = useSelector(s => s.transfers)

  const assignedUserIds = warehouseAssignments.filter(a => a.warehouseId === warehouse.id).map(a => a.userId)
  const assignedUsers = users.filter(u => assignedUserIds.includes(u.id))

  const whStock = stockLevels
    .filter(sl => sl.warehouseId === warehouse.id)
    .map(sl => ({
      ...sl,
      productName: products.find(p => p.id === sl.productId)?.name || sl.productId,
      sku: products.find(p => p.id === sl.productId)?.sku || '',
      unit: products.find(p => p.id === sl.productId)?.unitOfMeasure || '',
      status: sl.currentStock < sl.threshold ? 'CRITICAL' : sl.currentStock < sl.threshold * 1.25 ? 'WARNING' : 'OK',
    }))

  const whMovements = movements
    .filter(m => m.warehouseId === warehouse.id)
    .slice(0, 10)
    .map(m => ({
      ...m,
      productName: products.find(p => p.id === m.productId)?.name || m.productId,
      createdByName: users.find(u => u.id === m.createdBy)?.name || m.createdBy,
    }))

  const whTransfers = transfers
    .filter(t => t.fromWarehouseId === warehouse.id || t.toWarehouseId === warehouse.id)
    .slice(0, 10)
    .map(t => ({
      ...t,
      productName: products.find(p => p.id === t.productId)?.name || t.productId,
      direction: t.fromWarehouseId === warehouse.id ? 'OUT' : 'IN',
      otherWarehouse: warehouses.find(w => w.id === (t.fromWarehouseId === warehouse.id ? t.toWarehouseId : t.fromWarehouseId))?.name || '?',
    }))

  const criticalCount = whStock.filter(s => s.status === 'CRITICAL').length
  const activeTransfers = whTransfers.filter(t => ['SUGGESTED', 'APPROVED', 'IN_TRANSIT'].includes(t.status)).length

  return (
    <div className="space-y-5">
      {/* Header */}
      <div className="flex items-start gap-4">
        <button
          onClick={onBack}
          className="flex items-center gap-1.5 text-sm text-gray-500 hover:text-teal-600 transition-colors mt-1 shrink-0"
        >
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
          </svg>
          Warehouses
        </button>
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-3 flex-wrap">
            <h2 className="text-xl font-bold text-gray-900">{warehouse.name}</h2>
            <StatusBadge status={warehouse.isActive ? 'ACTIVE' : 'SUSPENDED'} />
          </div>
          <p className="text-sm text-gray-500 mt-0.5">{warehouse.address}</p>
        </div>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        <StatCard title="Assigned Users" value={assignedUsers.length} color="blue" />
        <StatCard title="Products in Stock" value={whStock.length} color="teal" />
        <StatCard title="Critical Items" value={criticalCount} color={criticalCount > 0 ? 'red' : 'green'} />
        <StatCard title="Active Transfers" value={activeTransfers} color={activeTransfers > 0 ? 'amber' : 'green'} />
      </div>

      {/* Assigned Team */}
      <div className="bg-white rounded-xl border border-gray-200">
        <div className="px-5 py-4 border-b border-gray-100">
          <h3 className="font-semibold text-gray-900">Assigned Team <span className="text-gray-400 font-normal text-sm">({assignedUsers.length})</span></h3>
        </div>
        {assignedUsers.length === 0 ? (
          <p className="px-5 py-6 text-sm text-gray-400 italic">No users assigned to this warehouse.</p>
        ) : (
          <div className="p-4 flex flex-wrap gap-3">
            {assignedUsers.map(u => (
              <div key={u.id} className="flex items-center gap-3 bg-gray-50 rounded-xl px-4 py-3 border border-gray-100">
                <div className="w-9 h-9 rounded-full bg-teal-100 flex items-center justify-center text-teal-700 font-bold text-sm shrink-0">
                  {u.name[0]}
                </div>
                <div>
                  <p className="text-sm font-medium text-gray-900 leading-tight">{u.name}</p>
                  <span className={`text-xs px-2 py-0.5 rounded-full font-medium mt-0.5 inline-block ${ROLE_PILL[u.role] || 'bg-gray-100 text-gray-600'}`}>
                    {u.role.replace(/_/g, ' ')}
                  </span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Stock Levels */}
      <div className="bg-white rounded-xl border border-gray-200">
        <div className="px-5 py-4 border-b border-gray-100">
          <h3 className="font-semibold text-gray-900">Stock Levels</h3>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-100">
                {['Product', 'SKU', 'Unit', 'Stock', 'Threshold', 'Status', 'Days Left'].map(h => (
                  <th key={h} className="text-left px-5 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wide">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {whStock.length === 0 ? (
                <tr><td colSpan={7} className="px-5 py-8 text-center text-gray-400 text-sm">No stock recorded for this warehouse</td></tr>
              ) : whStock.map(s => (
                <tr key={s.id} className="hover:bg-gray-50/60">
                  <td className="px-5 py-3 font-medium text-gray-900">{s.productName}</td>
                  <td className="px-5 py-3 font-mono text-xs text-gray-500">{s.sku}</td>
                  <td className="px-5 py-3 text-gray-600">{s.unit}</td>
                  <td className="px-5 py-3">
                    <span className={`px-2 py-0.5 rounded font-semibold ${s.status === 'CRITICAL' ? 'text-red-700 bg-red-50' : s.status === 'WARNING' ? 'text-amber-700 bg-amber-50' : 'text-green-700 bg-green-50'}`}>
                      {s.currentStock}
                    </span>
                  </td>
                  <td className="px-5 py-3 text-gray-600">{s.threshold}</td>
                  <td className="px-5 py-3"><StatusBadge status={s.status} /></td>
                  <td className="px-5 py-3">
                    {s.daysUntilEmpty != null ? (
                      <span className={`font-medium ${s.daysUntilEmpty <= 7 ? 'text-red-600' : s.daysUntilEmpty <= 14 ? 'text-amber-600' : 'text-green-600'}`}>
                        {s.daysUntilEmpty}d
                      </span>
                    ) : '—'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Movements + Transfers side by side */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <div className="bg-white rounded-xl border border-gray-200">
          <div className="px-5 py-4 border-b border-gray-100">
            <h3 className="font-semibold text-gray-900">Recent Movements <span className="text-gray-400 font-normal text-xs">(last 10)</span></h3>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-gray-50 border-b border-gray-100">
                  {['Product', 'Type', 'Qty', 'By', 'Date'].map(h => (
                    <th key={h} className="text-left px-4 py-2.5 text-xs font-semibold text-gray-500 uppercase tracking-wide">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50">
                {whMovements.length === 0 ? (
                  <tr><td colSpan={5} className="px-4 py-6 text-center text-gray-400 text-sm">No movements yet</td></tr>
                ) : whMovements.map(m => (
                  <tr key={m.id} className="hover:bg-gray-50/60">
                    <td className="px-4 py-2.5 font-medium text-gray-900 max-w-30 truncate">{m.productName}</td>
                    <td className="px-4 py-2.5"><StatusBadge status={m.type} size="xs" /></td>
                    <td className="px-4 py-2.5 font-medium text-gray-900">{m.quantity}</td>
                    <td className="px-4 py-2.5 text-gray-500 max-w-20 truncate">{m.createdByName}</td>
                    <td className="px-4 py-2.5 text-gray-400 text-xs whitespace-nowrap">{fmtDT(m.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        <div className="bg-white rounded-xl border border-gray-200">
          <div className="px-5 py-4 border-b border-gray-100">
            <h3 className="font-semibold text-gray-900">Recent Transfers <span className="text-gray-400 font-normal text-xs">(last 10)</span></h3>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-gray-50 border-b border-gray-100">
                  {['Product', 'Dir', 'Other WH', 'Qty', 'Status'].map(h => (
                    <th key={h} className="text-left px-4 py-2.5 text-xs font-semibold text-gray-500 uppercase tracking-wide">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50">
                {whTransfers.length === 0 ? (
                  <tr><td colSpan={5} className="px-4 py-6 text-center text-gray-400 text-sm">No transfers yet</td></tr>
                ) : whTransfers.map(t => (
                  <tr key={t.id} className="hover:bg-gray-50/60">
                    <td className="px-4 py-2.5 font-medium text-gray-900 max-w-30 truncate">{t.productName}</td>
                    <td className="px-4 py-2.5">
                      <span className={`px-2 py-0.5 rounded-full text-xs font-semibold ${t.direction === 'OUT' ? 'bg-orange-50 text-orange-700' : 'bg-green-50 text-green-700'}`}>
                        {t.direction}
                      </span>
                    </td>
                    <td className="px-4 py-2.5 text-gray-600 max-w-25 truncate">{t.otherWarehouse}</td>
                    <td className="px-4 py-2.5 font-medium text-gray-900">{t.quantity}</td>
                    <td className="px-4 py-2.5"><StatusBadge status={t.status} size="xs" /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  )
}

// ── Company Profile Panel ──────────────────────────────────────────────────────

function CompanyPanel() {
  const { companyName, companyLogo } = useSelector(s => s.auth)
  const dispatch = useDispatch()
  const fileRef = useRef()

  const handleLogoChange = (e) => {
    const file = e.target.files[0]
    if (!file) return
    const reader = new FileReader()
    reader.onloadend = () => {
      dispatch(setCompanyLogo(reader.result))
      toast.success('Company logo updated')
    }
    reader.readAsDataURL(file)
  }

  return (
    <div className="space-y-4 max-w-xl">
      <div className="bg-white rounded-xl border border-gray-200 p-6">
        <h3 className="font-semibold text-gray-900 mb-4">Company Branding</h3>
        <div className="flex items-center gap-6">
          <div className="shrink-0">
            {companyLogo ? (
              <img src={companyLogo} alt="Company logo" className="w-20 h-20 rounded-2xl object-contain border border-gray-200 bg-gray-50" />
            ) : (
              <div className="w-20 h-20 rounded-2xl bg-teal-100 flex items-center justify-center border-2 border-dashed border-teal-300">
                <span className="text-3xl font-bold text-teal-600">{companyName?.[0]}</span>
              </div>
            )}
          </div>
          <div>
            <p className="text-sm font-medium text-gray-900 mb-1">{companyName}</p>
            <p className="text-xs text-gray-500 mb-3">Upload your company logo to personalise your dashboard. It appears in the sidebar for all team members.</p>
            <div className="flex gap-2">
              <button
                onClick={() => fileRef.current?.click()}
                className="px-3 py-1.5 bg-teal-600 text-white text-sm rounded-lg hover:bg-teal-700 font-medium"
              >
                {companyLogo ? 'Change Logo' : 'Upload Logo'}
              </button>
              {companyLogo && (
                <button onClick={() => { dispatch(setCompanyLogo(null)); toast.info('Logo removed') }} className="px-3 py-1.5 border border-gray-300 text-gray-700 text-sm rounded-lg hover:bg-gray-50">
                  Remove
                </button>
              )}
            </div>
            <input ref={fileRef} type="file" accept="image/*" onChange={handleLogoChange} className="hidden" />
            <p className="text-xs text-gray-400 mt-2">PNG, JPG, SVG — up to 2 MB</p>
          </div>
        </div>
      </div>
    </div>
  )
}

// ── Warehouses Panel ──────────────────────────────────────────────────────────

function WarehousesPanel() {
  const { warehouses } = useSelector(s => s.stock)
  const { user } = useSelector(s => s.auth)
  const dispatch = useDispatch()
  const [showAdd, setShowAdd] = useState(false)
  const [edit, setEdit] = useState(null)
  const [form, setForm] = useState({ name: '', address: '' })
  const [search, setSearch] = useState('')
  const [selectedWh, setSelectedWh] = useState(null)
  const ch = e => setForm(f => ({ ...f, [e.target.name]: e.target.value }))

  if (selectedWh) {
    const live = warehouses.find(w => w.id === selectedWh.id) || selectedWh
    return <WarehouseDetail warehouse={live} onBack={() => setSelectedWh(null)} />
  }

  const filtered = warehouses.filter(w =>
    w.name.toLowerCase().includes(search.toLowerCase()) ||
    w.address.toLowerCase().includes(search.toLowerCase())
  )

  const openAdd = () => { setForm({ name: '', address: '' }); setEdit(null); setShowAdd(true) }
  const openEdit = wh => { setForm({ name: wh.name, address: wh.address }); setEdit(wh); setShowAdd(true) }

  const handleSubmit = e => {
    e.preventDefault()
    if (edit) {
      dispatch(updateWarehouse({ id: edit.id, ...form }))
      toast.success('Warehouse updated')
    } else {
      dispatch(addWarehouse({ ...form, createdBy: user.id }))
      toast.success('Warehouse added')
    }
    setShowAdd(false)
  }

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-2 sm:grid-cols-3 gap-4">
        <StatCard title="Total Warehouses" value={warehouses.length} color="teal" />
        <StatCard title="Active" value={warehouses.filter(w => w.isActive).length} color="teal" />
        <StatCard title="Inactive" value={warehouses.filter(w => !w.isActive).length} color="gray" />
      </div>
      <div className="bg-white rounded-xl border border-gray-200">
        <div className="flex flex-wrap items-center justify-between gap-3 px-5 py-4 border-b border-gray-100">
          <h3 className="font-semibold text-gray-900">Warehouses</h3>
          <div className="flex items-center gap-2">
            <SearchBar value={search} onChange={setSearch} placeholder="Search warehouses…" />
            <button onClick={openAdd} className="flex items-center gap-1.5 px-3 py-1.5 bg-teal-600 text-white text-sm font-medium rounded-lg hover:bg-teal-700">
              <AddIcon /> Add Warehouse
            </button>
          </div>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-100">
                {['Name', 'Address', 'Status', 'Actions'].map(h => (
                  <th key={h} className="text-left px-5 py-3 font-semibold text-gray-600 text-xs uppercase tracking-wide">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {filtered.length === 0 ? (
                <tr><td colSpan={4} className="px-5 py-8 text-center text-gray-400 text-sm">No warehouses found</td></tr>
              ) : filtered.map(wh => (
                <tr key={wh.id} className="hover:bg-gray-50/60">
                  <td className="px-5 py-3 font-medium text-gray-900">{wh.name}</td>
                  <td className="px-5 py-3 text-gray-600">{wh.address}</td>
                  <td className="px-5 py-3"><StatusBadge status={wh.isActive ? 'ACTIVE' : 'SUSPENDED'} /></td>
                  <td className="px-5 py-3">
                    <div className="flex items-center gap-2">
                      <button onClick={() => setSelectedWh(wh)} className="px-2.5 py-1 rounded-lg text-xs font-medium bg-teal-50 text-teal-700 border border-teal-100 hover:bg-teal-100 transition-colors">
                        View Details
                      </button>
                      <button onClick={() => openEdit(wh)} className="px-2.5 py-1 rounded-lg text-xs font-medium bg-blue-50 text-blue-600 border border-blue-100 hover:bg-blue-100 transition-colors">
                        Edit
                      </button>
                      <button
                        onClick={() => { dispatch(toggleWarehouseActive(wh.id)); toast.success(wh.isActive ? 'Deactivated' : 'Activated') }}
                        className={`px-2.5 py-1 rounded-lg text-xs font-medium border transition-colors ${wh.isActive ? 'bg-red-50 text-red-600 border-red-100 hover:bg-red-100' : 'bg-teal-50 text-teal-700 border-teal-100 hover:bg-teal-100'}`}
                      >
                        {wh.isActive ? 'Deactivate' : 'Activate'}
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
      {showAdd && (
        <Modal title={edit ? 'Edit Warehouse' : 'Add Warehouse'} onClose={() => setShowAdd(false)}>
          <form onSubmit={handleSubmit} className="space-y-3">
            <Field label="Warehouse Name" name="name" value={form.name} onChange={ch} placeholder="Warehouse Alpha" required />
            <Field label="Address" name="address" value={form.address} onChange={ch} placeholder="123 Street, City, State" required />
            <BtnRow onClose={() => setShowAdd(false)} submitLabel={edit ? 'Update Warehouse' : 'Add Warehouse'} />
          </form>
        </Modal>
      )}
    </div>
  )
}

// ── Products Panel ────────────────────────────────────────────────────────────

function ProductsPanel() {
  const { products } = useSelector(s => s.stock)
  const { user } = useSelector(s => s.auth)
  const dispatch = useDispatch()
  const [showAdd, setShowAdd] = useState(false)
  const [showBatch, setShowBatch] = useState(false)
  const [edit, setEdit] = useState(null)
  const [form, setForm] = useState({ name: '', sku: '', unitOfMeasure: '', defaultThreshold: '' })
  const [search, setSearch] = useState('')
  const [csvText, setCsvText] = useState('')
  const [csvPreview, setCsvPreview] = useState([])
  const [csvError, setCsvError] = useState('')
  const fileRef = useRef()
  const ch = e => setForm(f => ({ ...f, [e.target.name]: e.target.value }))

  const filtered = products.filter(p =>
    p.name.toLowerCase().includes(search.toLowerCase()) ||
    p.sku.toLowerCase().includes(search.toLowerCase())
  )

  const EMPTY_ROW = { name: '', sku: '', unitOfMeasure: '', defaultThreshold: '' }
  const [rows, setRows] = useState([{ ...EMPTY_ROW }])
  const updateRow = (i, field, val) => setRows(rs => rs.map((r, idx) => idx === i ? { ...r, [field]: val } : r))
  const removeRow = i => setRows(rs => rs.filter((_, idx) => idx !== i))

  const openAdd = () => { setRows([{ ...EMPTY_ROW }]); setEdit(null); setShowAdd(true) }
  const openEdit = p => { setForm({ name: p.name, sku: p.sku, unitOfMeasure: p.unitOfMeasure, defaultThreshold: p.defaultThreshold }); setEdit(p); setShowAdd(true) }

  const handleSubmit = e => {
    e.preventDefault()
    if (edit) {
      dispatch(updateProduct({ id: edit.id, ...form, defaultThreshold: +form.defaultThreshold }))
      toast.success('Product updated')
      setShowAdd(false)
    } else {
      const valid = rows.filter(r => r.name && r.sku)
      valid.forEach(r => dispatch(addProduct({ ...r, defaultThreshold: +r.defaultThreshold || 0, createdBy: user.id })))
      toast.success(`${valid.length} product${valid.length !== 1 ? 's' : ''} added`)
      setShowAdd(false)
      setRows([{ ...EMPTY_ROW }])
    }
  }

  const handleCSVFile = (e) => {
    const file = e.target.files[0]
    if (!file) return
    const reader = new FileReader()
    reader.onload = (ev) => {
      const text = ev.target.result
      setCsvText(text)
      const parsed = parseCSV(text)
      if (parsed.length === 0) { setCsvError('No valid rows found. Check the format.'); setCsvPreview([]) }
      else { setCsvError(''); setCsvPreview(parsed) }
    }
    reader.readAsText(file)
  }

  const handleBatchImport = () => {
    if (csvPreview.length === 0) return
    csvPreview.forEach(row => {
      dispatch(addProduct({
        name: row.name,
        sku: row.sku,
        unitOfMeasure: row.unitOfMeasure || 'units',
        defaultThreshold: +row.defaultThreshold || 10,
        createdBy: user.id,
      }))
    })
    toast.success(`${csvPreview.length} products imported successfully`)
    setShowBatch(false)
    setCsvPreview([])
    setCsvText('')
  }

  const downloadTemplate = () => {
    const blob = new Blob([CSV_TEMPLATE], { type: 'text/csv' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url; a.download = 'products_template.csv'; a.click()
    URL.revokeObjectURL(url)
  }

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-2 sm:grid-cols-3 gap-4">
        <StatCard title="Total Products" value={products.length} color="blue" />
        <StatCard title="Active" value={products.filter(p => p.isActive).length} color="teal" />
        <StatCard title="Inactive" value={products.filter(p => !p.isActive).length} color="gray" />
      </div>
      <div className="bg-white rounded-xl border border-gray-200">
        <div className="flex flex-wrap items-center justify-between gap-3 px-5 py-4 border-b border-gray-100">
          <h3 className="font-semibold text-gray-900">Products</h3>
          <div className="flex items-center gap-2">
            <SearchBar value={search} onChange={setSearch} placeholder="Search products…" />
            <button onClick={() => setShowBatch(true)} className="flex items-center gap-1.5 px-3 py-1.5 border border-teal-600 text-teal-600 text-sm font-medium rounded-lg hover:bg-teal-50">
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12" /></svg>
              Batch Import
            </button>
            <button onClick={openAdd} className="flex items-center gap-1.5 px-3 py-1.5 bg-teal-600 text-white text-sm font-medium rounded-lg hover:bg-teal-700">
              <AddIcon /> Add Product
            </button>
          </div>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-100">
                {['Name', 'SKU', 'Unit', 'Default Threshold', 'Status', 'Actions'].map(h => (
                  <th key={h} className="text-left px-5 py-3 font-semibold text-gray-600 text-xs uppercase tracking-wide">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {filtered.length === 0 ? (
                <tr><td colSpan={6} className="px-5 py-8 text-center text-gray-400 text-sm">No products found</td></tr>
              ) : filtered.map(p => (
                <tr key={p.id} className="hover:bg-gray-50/60">
                  <td className="px-5 py-3 font-medium text-gray-900">{p.name}</td>
                  <td className="px-5 py-3 font-mono text-xs text-gray-600">{p.sku}</td>
                  <td className="px-5 py-3 text-gray-600">{p.unitOfMeasure}</td>
                  <td className="px-5 py-3 font-medium text-gray-900">{p.defaultThreshold}</td>
                  <td className="px-5 py-3"><StatusBadge status={p.isActive ? 'ACTIVE' : 'SUSPENDED'} /></td>
                  <td className="px-5 py-3">
                    <div className="flex items-center gap-2">
                      <button onClick={() => openEdit(p)} className="px-2.5 py-1 rounded-lg text-xs font-medium bg-blue-50 text-blue-600 border border-blue-100 hover:bg-blue-100 transition-colors">
                        Edit
                      </button>
                      <button
                        onClick={() => { dispatch(toggleProductActive(p.id)); toast.success(p.isActive ? 'Product archived' : 'Product unarchived') }}
                        className={`px-2.5 py-1 rounded-lg text-xs font-medium border transition-colors ${p.isActive ? 'bg-red-50 text-red-600 border-red-100 hover:bg-red-100' : 'bg-teal-50 text-teal-700 border-teal-100 hover:bg-teal-100'}`}
                      >
                        {p.isActive ? 'Archive' : 'Unarchive'}
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {showAdd && edit && (
        <Modal title="Edit Product" onClose={() => setShowAdd(false)}>
          <form onSubmit={handleSubmit} className="space-y-3">
            <Field label="Product Name" name="name" value={form.name} onChange={ch} placeholder="Industrial Laptop" required />
            <Field label="SKU" name="sku" value={form.sku} onChange={ch} placeholder="LAP-001" required />
            <Field label="Unit of Measure" name="unitOfMeasure" value={form.unitOfMeasure} onChange={ch} placeholder="units / boxes / kg" required />
            <Field label="Default Threshold" name="defaultThreshold" type="number" min="0" value={form.defaultThreshold} onChange={ch} required />
            <BtnRow onClose={() => setShowAdd(false)} submitLabel="Update Product" />
          </form>
        </Modal>
      )}

      {showAdd && !edit && (
        <Modal title="Add Products" wide onClose={() => { setShowAdd(false); setRows([{ ...EMPTY_ROW }]) }}>
          <form onSubmit={handleSubmit}>
            <div className="space-y-3 mb-4 max-h-105 overflow-y-auto pr-1">
              {rows.map((row, i) => (
                <div key={i} className="bg-gray-50 border border-gray-200 rounded-xl p-4 relative">
                  <div className="flex items-center justify-between mb-3">
                    <span className="text-xs font-bold text-gray-400 uppercase tracking-widest">
                      Product {String(i + 1).padStart(2, '0')}
                    </span>
                    <button
                      type="button" onClick={() => removeRow(i)}
                      disabled={rows.length === 1}
                      className="w-7 h-7 flex items-center justify-center rounded-lg text-gray-300 hover:text-red-500 hover:bg-red-50 disabled:opacity-0 disabled:pointer-events-none transition-colors"
                    >
                      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /></svg>
                    </button>
                  </div>
                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="block text-xs font-medium text-gray-500 mb-1">Name <span className="text-red-400">*</span></label>
                      <input
                        value={row.name} onChange={e => updateRow(i, 'name', e.target.value)}
                        placeholder="e.g. Industrial Laptop" required
                        className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm bg-white focus:outline-none focus:ring-2 focus:ring-teal-600 focus:border-transparent"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-medium text-gray-500 mb-1">SKU <span className="text-red-400">*</span></label>
                      <input
                        value={row.sku} onChange={e => updateRow(i, 'sku', e.target.value)}
                        placeholder="e.g. LAP-001" required
                        className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm bg-white font-mono focus:outline-none focus:ring-2 focus:ring-teal-600 focus:border-transparent"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-medium text-gray-500 mb-1">Unit of Measure</label>
                      <input
                        value={row.unitOfMeasure} onChange={e => updateRow(i, 'unitOfMeasure', e.target.value)}
                        placeholder="units / kg / boxes"
                        className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm bg-white focus:outline-none focus:ring-2 focus:ring-teal-600 focus:border-transparent"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-medium text-gray-500 mb-1">Default Threshold</label>
                      <input
                        type="number" min="0"
                        value={row.defaultThreshold} onChange={e => updateRow(i, 'defaultThreshold', e.target.value)}
                        placeholder="e.g. 20"
                        className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm bg-white focus:outline-none focus:ring-2 focus:ring-teal-600 focus:border-transparent"
                      />
                    </div>
                  </div>
                </div>
              ))}
            </div>
            <button
              type="button"
              onClick={() => setRows(rs => [...rs, { ...EMPTY_ROW }])}
              className="flex items-center gap-2 w-full py-2.5 border-2 border-dashed border-gray-300 rounded-xl text-sm text-gray-500 hover:border-teal-400 hover:text-teal-600 hover:bg-teal-50/40 transition-colors justify-center font-medium mb-4"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" /></svg>
              Add another product
            </button>
            <BtnRow onClose={() => { setShowAdd(false); setRows([{ ...EMPTY_ROW }]) }} submitLabel={`Add ${rows.length > 1 ? `${rows.length} Products` : 'Product'}`} />
          </form>
        </Modal>
      )}

      {showBatch && (
        <Modal title="Batch Import Products" wide onClose={() => { setShowBatch(false); setCsvPreview([]); setCsvError('') }}>
          <div className="space-y-4">
            <div className="bg-teal-50 border border-teal-200 rounded-lg p-4">
              <p className="text-sm text-teal-800 font-medium mb-1">CSV Format</p>
              <p className="text-xs text-teal-700 mb-2">Upload a CSV file with columns: <code className="bg-teal-100 px-1 rounded">name, sku, unitOfMeasure, defaultThreshold</code></p>
              <button onClick={downloadTemplate} className="text-xs text-teal-700 font-semibold hover:underline flex items-center gap-1">
                <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" /></svg>
                Download template
              </button>
            </div>
            <div
              className="border-2 border-dashed border-gray-300 rounded-xl p-8 text-center cursor-pointer hover:border-teal-400 hover:bg-teal-50/30 transition-colors"
              onClick={() => fileRef.current?.click()}
            >
              <svg className="w-10 h-10 text-gray-400 mx-auto mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 13h6m-3-3v6m5 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
              <p className="text-sm text-gray-600 font-medium">Click to upload CSV file</p>
              <p className="text-xs text-gray-400 mt-1">or drag and drop</p>
              <input ref={fileRef} type="file" accept=".csv,text/csv" onChange={handleCSVFile} className="hidden" />
            </div>
            {csvError && <p className="text-sm text-red-600 bg-red-50 border border-red-200 rounded-lg px-3 py-2">{csvError}</p>}
            {csvPreview.length > 0 && (
              <div>
                <p className="text-sm font-medium text-gray-700 mb-2">{csvPreview.length} products ready to import:</p>
                <div className="max-h-48 overflow-y-auto border border-gray-200 rounded-lg">
                  <table className="w-full text-xs">
                    <thead className="bg-gray-50 sticky top-0">
                      <tr>{['Name', 'SKU', 'Unit', 'Threshold'].map(h => <th key={h} className="text-left px-3 py-2 font-semibold text-gray-500">{h}</th>)}</tr>
                    </thead>
                    <tbody className="divide-y divide-gray-100">
                      {csvPreview.map((r, i) => (
                        <tr key={i} className="hover:bg-gray-50">
                          <td className="px-3 py-2 text-gray-900">{r.name}</td>
                          <td className="px-3 py-2 font-mono text-gray-600">{r.sku}</td>
                          <td className="px-3 py-2 text-gray-600">{r.unitOfMeasure}</td>
                          <td className="px-3 py-2 text-gray-600">{r.defaultThreshold}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}
            <div className="flex gap-2 pt-1">
              <button type="button" onClick={() => { setShowBatch(false); setCsvPreview([]); setCsvError('') }} className="flex-1 py-2 border border-gray-300 text-gray-700 rounded-lg text-sm hover:bg-gray-50">Cancel</button>
              <button
                type="button"
                onClick={handleBatchImport}
                disabled={csvPreview.length === 0}
                className="flex-1 py-2 bg-teal-600 text-white rounded-lg text-sm font-medium hover:bg-teal-700 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Import {csvPreview.length > 0 ? `${csvPreview.length} Products` : 'Products'}
              </button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  )
}

// ── Users Panel ───────────────────────────────────────────────────────────────

function ManageUserModal({ user: u, onClose }) {
  const { warehouses } = useSelector(s => s.stock)
  const { warehouseAssignments } = useSelector(s => s.users)
  const { companyId } = useSelector(s => s.auth)
  const dispatch = useDispatch()
  const [role, setRole] = useState(u.role)
  const [addWh, setAddWh] = useState('')

  const assignments = warehouseAssignments
    .filter(a => a.userId === u.id)
    .map(a => ({ ...a, warehouseName: warehouses.find(w => w.id === a.warehouseId)?.name || a.warehouseId }))

  const saveRole = () => {
    dispatch(updateUserRole({ id: u.id, role }))
    toast.success('Role updated')
  }

  const doAssign = e => {
    e.preventDefault()
    if (!addWh) return
    dispatch(assignWarehouse({ userId: u.id, warehouseId: addWh, companyId }))
    toast.success('Warehouse assigned')
    setAddWh('')
  }

  const toggleActive = () => {
    u.isActive ? dispatch(deactivateUser(u.id)) : dispatch(reactivateUser(u.id))
    toast.success(u.isActive ? 'User deactivated' : 'User reactivated')
    onClose()
  }

  return (
    <Modal title={`Manage — ${u.name}`} wide onClose={onClose}>
      <div className="space-y-5">
        {/* Role */}
        <div>
          <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-2">Role</p>
          <div className="flex gap-2 items-center">
            <select
              value={role} onChange={e => setRole(e.target.value)}
              className="flex-1 px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-teal-600"
            >
              <option value="MANAGER">Manager</option>
              <option value="WAREHOUSE_STAFF">Warehouse Staff</option>
              <option value="PROCUREMENT_OFFICER">Procurement Officer</option>
            </select>
            <button
              onClick={saveRole}
              disabled={role === u.role}
              className="px-4 py-2 bg-teal-600 text-white text-sm font-medium rounded-lg hover:bg-teal-700 disabled:opacity-40 disabled:cursor-not-allowed"
            >
              Save Role
            </button>
          </div>
        </div>

        {/* Warehouse Assignments */}
        <div>
          <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-2">Warehouse Assignments</p>
          <div className="space-y-2">
            {assignments.length === 0 ? (
              <p className="text-sm text-gray-400 italic">No warehouses assigned yet.</p>
            ) : (
              <div className="flex flex-wrap gap-1.5">
                {assignments.map(a => (
                  <span key={a.id} className="inline-flex items-center gap-1.5 bg-teal-50 text-teal-700 text-xs px-2.5 py-1 rounded-full border border-teal-200 font-medium">
                    {a.warehouseName}
                    <button
                      onClick={() => { dispatch(removeAssignment(a.id)); toast.info('Assignment removed') }}
                      className="text-teal-400 hover:text-red-500 leading-none font-bold"
                    >×</button>
                  </span>
                ))}
              </div>
            )}
            <form onSubmit={doAssign} className="flex gap-2 mt-2">
              <select
                value={addWh} onChange={e => setAddWh(e.target.value)}
                className="flex-1 px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-teal-600"
              >
                <option value="">Add warehouse…</option>
                {warehouses.filter(w => w.isActive && !assignments.find(a => a.warehouseId === w.id)).map(w => (
                  <option key={w.id} value={w.id}>{w.name}</option>
                ))}
              </select>
              <button type="submit" disabled={!addWh} className="px-4 py-2 bg-teal-600 text-white text-sm font-medium rounded-lg hover:bg-teal-700 disabled:opacity-40 disabled:cursor-not-allowed">
                Assign
              </button>
            </form>
          </div>
        </div>

        {/* Account status */}
        <div className="pt-4 border-t border-gray-100 flex items-center justify-between">
          <div>
            <p className="text-sm font-medium text-gray-700">{u.name}</p>
            <p className="text-xs text-gray-400">{u.email}</p>
          </div>
          <button
            onClick={toggleActive}
            className={`px-4 py-2 rounded-lg text-sm font-semibold transition-colors ${u.isActive ? 'bg-red-50 text-red-600 hover:bg-red-100' : 'bg-teal-50 text-teal-700 hover:bg-teal-100'}`}
          >
            {u.isActive ? 'Deactivate Account' : 'Reactivate Account'}
          </button>
        </div>
      </div>
    </Modal>
  )
}

function UsersPanel() {
  const { users } = useSelector(s => s.users)
  const { user: me, companyId } = useSelector(s => s.auth)
  const dispatch = useDispatch()
  const [showAdd, setShowAdd] = useState(false)
  const [showTempPass, setShowTempPass] = useState(false)
  const [manageUser, setManageUser] = useState(null)
  const [form, setForm] = useState({ name: '', email: '', role: 'MANAGER', password: '' })
  const [search, setSearch] = useState('')
  const ch = e => setForm(f => ({ ...f, [e.target.name]: e.target.value }))

  const companyUsers = users
    .filter(u => u.companyId === companyId)
    .filter(u => u.name.toLowerCase().includes(search.toLowerCase()) || u.email.toLowerCase().includes(search.toLowerCase()))

  const handleAdd = e => {
    e.preventDefault()
    const newId = `user-${Date.now()}`
    dispatch(addUser({ ...form, companyId, id: newId }))
    dispatch(registerLocalUser({ id: newId, name: form.name, email: form.email, password: form.password, role: form.role, companyId, companyName: null, warehouseId: null }))
    toast.success(`${form.name} added — they must set a new password on first login`)
    setShowAdd(false)
    setShowTempPass(false)
    setForm({ name: '', email: '', role: 'MANAGER', password: '' })
  }

  const roleCount = r => users.filter(u => u.companyId === companyId && u.role === r).length

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        <StatCard title="Total Users" value={users.filter(u => u.companyId === companyId).length} color="blue" />
        <StatCard title="Active" value={users.filter(u => u.companyId === companyId && u.isActive).length} color="teal" />
        <StatCard title="Staff" value={roleCount('WAREHOUSE_STAFF')} color="amber" />
        <StatCard title="Inactive" value={users.filter(u => u.companyId === companyId && !u.isActive).length} color="gray" />
      </div>
      <div className="bg-white rounded-xl border border-gray-200">
        <div className="flex flex-wrap items-center justify-between gap-3 px-5 py-4 border-b border-gray-100">
          <h3 className="font-semibold text-gray-900">Team Members</h3>
          <div className="flex items-center gap-2">
            <SearchBar value={search} onChange={setSearch} placeholder="Search members…" />
            <button onClick={() => setShowAdd(true)} className="flex items-center gap-1.5 px-3 py-1.5 bg-teal-600 text-white text-sm font-medium rounded-lg hover:bg-teal-700">
              <AddIcon /> Add User
            </button>
          </div>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-100">
                {['Name', 'Email', 'Role', 'Status', 'Actions'].map(h => (
                  <th key={h} className="text-left px-5 py-3 font-semibold text-gray-600 text-xs uppercase tracking-wide">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {companyUsers.map(u => (
                <tr key={u.id} className={`hover:bg-gray-50/60 ${!u.isActive ? 'opacity-60' : ''}`}>
                  <td className="px-5 py-3">
                    <div className="flex items-center gap-3">
                      <div className="w-8 h-8 rounded-full bg-teal-100 flex items-center justify-center text-teal-700 font-semibold text-sm shrink-0">
                        {u.name[0]}
                      </div>
                      <span className="font-medium text-gray-900">{u.name}</span>
                    </div>
                  </td>
                  <td className="px-5 py-3 text-gray-600">{u.email}</td>
                  <td className="px-5 py-3"><StatusBadge status={u.role} /></td>
                  <td className="px-5 py-3"><StatusBadge status={u.isActive ? 'ACTIVE' : 'SUSPENDED'} /></td>
                  <td className="px-5 py-3">
                    {u.id !== me.id ? (
                      <button
                        onClick={() => setManageUser(u)}
                        className="px-3 py-1.5 bg-teal-50 text-teal-700 text-xs font-semibold rounded-lg border border-teal-200 hover:bg-teal-100 transition-colors"
                      >
                        Manage
                      </button>
                    ) : (
                      <span className="text-xs text-gray-400 italic">You</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {showAdd && (
        <Modal title="Add Team Member" onClose={() => setShowAdd(false)}>
          <form onSubmit={handleAdd} className="space-y-3">
            <Field label="Full Name" name="name" value={form.name} onChange={ch} placeholder="Jane Doe" required />
            <Field label="Email" name="email" type="email" value={form.email} onChange={ch} placeholder="jane@company.com" required />
            <Field label="Role">
              <select name="role" value={form.role} onChange={ch} className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-teal-600">
                <option value="MANAGER">Manager</option>
                <option value="WAREHOUSE_STAFF">Warehouse Staff</option>
                <option value="PROCUREMENT_OFFICER">Procurement Officer</option>
              </select>
            </Field>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Temporary Password</label>
              <div className="relative">
                <input
                  name="password"
                  type={showTempPass ? 'text' : 'password'}
                  value={form.password}
                  onChange={ch}
                  placeholder="Min 8 characters"
                  required
                  className="w-full px-3 py-2 pr-10 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-teal-600 focus:border-transparent"
                />
                <button type="button" onClick={() => setShowTempPass(s => !s)} tabIndex={-1} className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600">
                  {showTempPass
                    ? <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" /></svg>
                    : <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" /><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" /></svg>
                  }
                </button>
              </div>
              <p className="text-xs text-gray-400 mt-1">The user will be prompted to change this on first login.</p>
            </div>
            <BtnRow onClose={() => setShowAdd(false)} submitLabel="Add User" />
          </form>
        </Modal>
      )}

      {manageUser && <ManageUserModal user={manageUser} onClose={() => setManageUser(null)} />}
    </div>
  )
}

// ── Main ──────────────────────────────────────────────────────────────────────

export default function AdminDashboard() {
  const [activeTab, setActiveTab] = useState('warehouses')
  const { users } = useSelector(s => s.users)
  const { companyId } = useSelector(s => s.auth)

  const navItems = [
    {
      id: 'company', label: 'Company Profile', badge: 0,
      icon: <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" /></svg>,
    },
    {
      id: 'warehouses', label: 'Warehouses', badge: 0,
      icon: <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" /></svg>,
    },
    {
      id: 'products', label: 'Products', badge: 0,
      icon: <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" /></svg>,
    },
    {
      id: 'users', label: 'Users', badge: users.filter(u => u.companyId === companyId).length,
      icon: <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" /></svg>,
    },
  ]

  return (
    <Layout title="Admin Dashboard" navItems={navItems} activeTab={activeTab} onTabChange={setActiveTab}>
      {activeTab === 'company' && <CompanyPanel />}
      {activeTab === 'warehouses' && <WarehousesPanel />}
      {activeTab === 'products' && <ProductsPanel />}
      {activeTab === 'users' && <UsersPanel />}
    </Layout>
  )
}
