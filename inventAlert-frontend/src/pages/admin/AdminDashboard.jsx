import { useState, useRef } from 'react'
import { useSelector, useDispatch } from 'react-redux'
import { toast } from 'react-toastify'
import Layout from '../../components/layout/Layout'
import StatusBadge from '../../components/shared/StatusBadge'
import StatCard from '../../components/shared/StatCard'
import { setCompanyLogo } from '../../store/slices/authSlice'
import {
  addWarehouse, updateWarehouse, toggleWarehouseActive,
  addProduct, updateProduct, toggleProductActive,
} from '../../store/slices/stockSlice'
import {
  addUser, deactivateUser, reactivateUser,
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
  const ch = e => setForm(f => ({ ...f, [e.target.name]: e.target.value }))

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
                    <div className="flex gap-3">
                      <button onClick={() => openEdit(wh)} className="text-xs font-medium text-blue-600 hover:underline">Edit</button>
                      <button
                        onClick={() => { dispatch(toggleWarehouseActive(wh.id)); toast.success(wh.isActive ? 'Deactivated' : 'Activated') }}
                        className={`text-xs font-medium hover:underline ${wh.isActive ? 'text-red-500' : 'text-teal-600'}`}
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

  const openAdd = () => { setForm({ name: '', sku: '', unitOfMeasure: '', defaultThreshold: '' }); setEdit(null); setShowAdd(true) }
  const openEdit = p => { setForm({ name: p.name, sku: p.sku, unitOfMeasure: p.unitOfMeasure, defaultThreshold: p.defaultThreshold }); setEdit(p); setShowAdd(true) }

  const handleSubmit = e => {
    e.preventDefault()
    const payload = { ...form, defaultThreshold: +form.defaultThreshold }
    if (edit) {
      dispatch(updateProduct({ id: edit.id, ...payload }))
      toast.success('Product updated')
    } else {
      dispatch(addProduct({ ...payload, createdBy: user.id }))
      toast.success('Product added')
    }
    setShowAdd(false)
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
                    <div className="flex gap-3">
                      <button onClick={() => openEdit(p)} className="text-xs font-medium text-blue-600 hover:underline">Edit</button>
                      <button
                        onClick={() => { dispatch(toggleProductActive(p.id)); toast.success(p.isActive ? 'Deactivated' : 'Activated') }}
                        className={`text-xs font-medium hover:underline ${p.isActive ? 'text-red-500' : 'text-teal-600'}`}
                      >
                        {p.isActive ? 'Deactivate' : 'Activate'}
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
        <Modal title={edit ? 'Edit Product' : 'Add Product'} onClose={() => setShowAdd(false)}>
          <form onSubmit={handleSubmit} className="space-y-3">
            <Field label="Product Name" name="name" value={form.name} onChange={ch} placeholder="Industrial Laptop" required />
            <Field label="SKU" name="sku" value={form.sku} onChange={ch} placeholder="LAP-001" required />
            <Field label="Unit of Measure" name="unitOfMeasure" value={form.unitOfMeasure} onChange={ch} placeholder="units / boxes / kg" required />
            <Field label="Default Threshold" name="defaultThreshold" type="number" min="0" value={form.defaultThreshold} onChange={ch} required />
            <BtnRow onClose={() => setShowAdd(false)} submitLabel={edit ? 'Update Product' : 'Add Product'} />
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

function UsersPanel() {
  const { users, warehouseAssignments } = useSelector(s => s.users)
  const { warehouses } = useSelector(s => s.stock)
  const { user: me, companyId } = useSelector(s => s.auth)
  const dispatch = useDispatch()
  const [showAdd, setShowAdd] = useState(false)
  const [assignModal, setAssignModal] = useState(null)
  const [form, setForm] = useState({ name: '', email: '', role: 'MANAGER', password: '' })
  const [assignWh, setAssignWh] = useState('')
  const [search, setSearch] = useState('')
  const ch = e => setForm(f => ({ ...f, [e.target.name]: e.target.value }))

  const companyUsers = users
    .filter(u => u.companyId === companyId)
    .filter(u => u.name.toLowerCase().includes(search.toLowerCase()) || u.email.toLowerCase().includes(search.toLowerCase()))

  const handleAdd = e => {
    e.preventDefault()
    dispatch(addUser({ ...form, companyId }))
    toast.success(`${form.name} added to team`)
    setShowAdd(false)
    setForm({ name: '', email: '', role: 'MANAGER', password: '' })
  }

  const handleAssign = e => {
    e.preventDefault()
    if (!assignWh) return
    dispatch(assignWarehouse({ userId: assignModal, warehouseId: assignWh, companyId }))
    toast.success('Warehouse assigned')
    setAssignModal(null)
    setAssignWh('')
  }

  const getAssignments = userId =>
    warehouseAssignments
      .filter(a => a.userId === userId)
      .map(a => ({ ...a, warehouseName: warehouses.find(w => w.id === a.warehouseId)?.name || a.warehouseId }))

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
                {['Name', 'Email', 'Role', 'Status', 'Warehouse Assignments', 'Actions'].map(h => (
                  <th key={h} className="text-left px-5 py-3 font-semibold text-gray-600 text-xs uppercase tracking-wide">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {companyUsers.map(u => {
                const assignments = getAssignments(u.id)
                return (
                  <tr key={u.id} className="hover:bg-gray-50/60">
                    <td className="px-5 py-3 font-medium text-gray-900">{u.name}</td>
                    <td className="px-5 py-3 text-gray-600">{u.email}</td>
                    <td className="px-5 py-3"><StatusBadge status={u.role} /></td>
                    <td className="px-5 py-3"><StatusBadge status={u.isActive ? 'ACTIVE' : 'SUSPENDED'} /></td>
                    <td className="px-5 py-3">
                      {u.role === 'WAREHOUSE_STAFF' ? (
                        <div className="flex flex-wrap items-center gap-1">
                          {assignments.map(a => (
                            <span key={a.id} className="inline-flex items-center gap-1 bg-teal-50 text-teal-700 text-xs px-2 py-0.5 rounded-full border border-teal-200">
                              {a.warehouseName}
                              <button onClick={() => { dispatch(removeAssignment(a.id)); toast.info('Assignment removed') }} className="text-teal-400 hover:text-red-500 font-bold leading-none">×</button>
                            </span>
                          ))}
                          <button onClick={() => setAssignModal(u.id)} className="text-xs text-teal-600 hover:underline">+ Assign</button>
                        </div>
                      ) : (
                        <span className="text-gray-400 text-xs italic">—</span>
                      )}
                    </td>
                    <td className="px-5 py-3">
                      {u.id !== me.id && (
                        <button
                          onClick={() => { u.isActive ? dispatch(deactivateUser(u.id)) : dispatch(reactivateUser(u.id)); toast.success(u.isActive ? 'User deactivated' : 'User reactivated') }}
                          className={`text-xs font-medium hover:underline ${u.isActive ? 'text-red-500' : 'text-teal-600'}`}
                        >
                          {u.isActive ? 'Deactivate' : 'Reactivate'}
                        </button>
                      )}
                    </td>
                  </tr>
                )
              })}
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
            <Field label="Temporary Password" name="password" type="password" value={form.password} onChange={ch} placeholder="Min 8 characters" required />
            <BtnRow onClose={() => setShowAdd(false)} submitLabel="Add User" />
          </form>
        </Modal>
      )}

      {assignModal && (
        <Modal title="Assign to Warehouse" onClose={() => setAssignModal(null)}>
          <form onSubmit={handleAssign} className="space-y-3">
            <Field label="Warehouse">
              <select value={assignWh} onChange={e => setAssignWh(e.target.value)} className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-teal-600">
                <option value="">Select warehouse…</option>
                {warehouses.filter(w => w.isActive).map(w => <option key={w.id} value={w.id}>{w.name}</option>)}
              </select>
            </Field>
            <BtnRow onClose={() => setAssignModal(null)} submitLabel="Assign" />
          </form>
        </Modal>
      )}
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
