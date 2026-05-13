import { useState, useRef } from 'react'
import { useSelector, useDispatch } from 'react-redux'
import { toast } from 'react-toastify'
import Layout from '../../components/layout/Layout'
import StatusBadge from '../../components/shared/StatusBadge'
import StatCard from '../../components/shared/StatCard'
import { setCompanyLogo } from '../../store/slices/authSlice'
import { uploadToCloudinary } from '../../services/cloudinary'
import { submitComplaint } from '../../store/slices/superadminSlice'
import {
  useGetMyCompanyQuery, useUpdateMyCompanyMutation,
  useGetUsersQuery, useCreateUserMutation,
  useUpdateUserRoleMutation, useDeactivateUserMutation, useReactivateUserMutation,
  useGetUserAssignmentsQuery, useAssignToWarehouseMutation, useRemoveAssignmentMutation,
  useGetWarehousesQuery, useCreateWarehouseMutation, useUpdateWarehouseMutation,
  useDeactivateWarehouseMutation, useActivateWarehouseMutation,
  useGetProductsQuery, useCreateProductMutation, useUpdateProductMutation, useImportProductsMutation,
  useGetStockByWarehouseQuery,
} from '../../apis/inventAlertApi'
import ConfirmDialog from '../../components/shared/ConfirmDialog'
import PlacesAutocompleteInput from '../../components/shared/PlacesAutocompleteInput'

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
        className="pl-9 pr-4 py-1.5 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-teal-600 focus:border-transparent w-72"
      />
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
const fmtDate = d => new Date(d).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })

const ROLE_PILL = {
  MANAGER: 'bg-purple-100 text-purple-700',
  WAREHOUSE_STAFF: 'bg-teal-100 text-teal-700',
  PROCUREMENT_OFFICER: 'bg-blue-100 text-blue-700',
  ADMIN: 'bg-gray-100 text-gray-700',
}

function WarehouseDetail({ warehouse, onBack }) {
  const { data: stockLevels = [], isLoading: stockLoading } = useGetStockByWarehouseQuery(warehouse.id)
  const { data: products = [] } = useGetProductsQuery()
  const { data: warehouses = [] } = useGetWarehousesQuery()
  const [stockSearch, setStockSearch] = useState('')
  const [stockPage, setStockPage] = useState(0)
  const WH_PAGE = 5

  const whStock = stockLevels.map(sl => ({
    ...sl,
    productName: products.find(p => p.id === sl.productId)?.name || sl.productId,
    sku: products.find(p => p.id === sl.productId)?.sku || '',
    unit: products.find(p => p.id === sl.productId)?.unitOfMeasure || '',
    status: sl.currentStock < sl.threshold ? 'CRITICAL' : sl.currentStock < sl.threshold * 1.25 ? 'WARNING' : 'OK',
  }))

  const criticalCount = whStock.filter(s => s.status === 'CRITICAL').length

  const filteredStock = whStock.filter(s => !stockSearch || s.productName.toLowerCase().includes(stockSearch.toLowerCase()) || s.sku.toLowerCase().includes(stockSearch.toLowerCase()))
  const stockPages = Math.max(1, Math.ceil(filteredStock.length / WH_PAGE))
  const pagedStock = filteredStock.slice(stockPage * WH_PAGE, (stockPage + 1) * WH_PAGE)

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
      <div className="grid grid-cols-2 sm:grid-cols-3 gap-4">
        <StatCard title="Products in Stock" value={whStock.length} color="teal" />
        <StatCard title="Critical Items" value={criticalCount} color={criticalCount > 0 ? 'red' : 'green'} />
        <StatCard title="Low Stock" value={whStock.filter(s => s.status === 'WARNING').length} color="amber" />
      </div>

      {/* Stock Levels */}
      <div className="bg-white rounded-xl border border-gray-200">
        <div className="flex flex-wrap items-center justify-between gap-3 px-5 py-4 border-b border-gray-100">
          <h3 className="font-semibold text-gray-900">Stock Levels <span className="text-gray-400 font-normal text-sm">({filteredStock.length})</span></h3>
          <SearchBar value={stockSearch} onChange={v => { setStockSearch(v); setStockPage(0) }} placeholder="Search product or SKU…" />
        </div>
        <div className="overflow-x-auto">
          {stockLoading ? (
            <div className="py-12 text-center text-sm text-gray-400">Loading…</div>
          ) : (
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-gray-50 border-b border-gray-100">
                  {['Product', 'SKU', 'Unit', 'Stock', 'Threshold', 'Status'].map(h => (
                    <th key={h} className="text-left px-5 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wide">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50">
                {pagedStock.length === 0 ? (
                  <tr><td colSpan={6} className="px-5 py-8 text-center text-gray-400 text-sm">{stockSearch ? 'No matching stock items' : 'No stock recorded for this warehouse'}</td></tr>
                ) : pagedStock.map(s => (
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
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
        {stockPages > 1 && (
          <div className="flex items-center justify-between px-5 py-3 border-t border-gray-100">
            <span className="text-xs text-gray-400">Page {stockPage + 1} of {stockPages}</span>
            <div className="flex gap-1">
              <button disabled={stockPage === 0} onClick={() => setStockPage(p => p - 1)} className="px-2.5 py-1 rounded-lg text-xs border border-gray-200 text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed">Prev</button>
              <button disabled={stockPage >= stockPages - 1} onClick={() => setStockPage(p => p + 1)} className="px-2.5 py-1 rounded-lg text-xs border border-gray-200 text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed">Next</button>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

// ── Company Profile Panel ──────────────────────────────────────────────────────

function CompanyPanel() {
  const { companyName, companyLogo } = useSelector(s => s.auth)
  const dispatch = useDispatch()
  const [updateMyCompany] = useUpdateMyCompanyMutation()
  const fileRef = useRef()
  const [uploading, setUploading] = useState(false)

  const handleLogoChange = async (e) => {
    const file = e.target.files[0]
    e.target.value = ''
    if (!file) return
    if (file.size > 2 * 1024 * 1024) { toast.error('File must be under 2 MB'); return }
    setUploading(true)
    try {
      const url = await uploadToCloudinary(file)
      await updateMyCompany({ logoUrl: url })
      dispatch(setCompanyLogo(url))
      toast.success('Company logo updated')
    } catch {
      toast.error('Upload failed — please try again')
    } finally {
      setUploading(false)
    }
  }

  return (
    <div className="space-y-4 max-w-xl">
      <div className="bg-white rounded-xl border border-gray-200 p-6">
        <h3 className="font-semibold text-gray-900 mb-4">Company Branding</h3>
        <div className="flex items-center gap-6">
          <div className="shrink-0">
            {uploading ? (
              <div className="w-20 h-20 rounded-2xl bg-gray-100 flex items-center justify-center border border-gray-200">
                <svg className="w-6 h-6 text-teal-600 animate-spin" fill="none" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z" />
                </svg>
              </div>
            ) : companyLogo ? (
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
                disabled={uploading}
                className="px-3 py-1.5 bg-teal-600 text-white text-sm rounded-lg hover:bg-teal-700 font-medium disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {uploading ? 'Uploading…' : companyLogo ? 'Change Logo' : 'Upload Logo'}
              </button>
              {companyLogo && !uploading && (
                <button onClick={async () => { await updateMyCompany({ logoUrl: null }); dispatch(setCompanyLogo(null)); toast.info('Logo removed') }} className="px-3 py-1.5 border border-gray-300 text-gray-700 text-sm rounded-lg hover:bg-gray-50">
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

function ManageWarehouseModal({ wh, onClose }) {
  const [updateWarehouseMutation, { isLoading: isSaving }] = useUpdateWarehouseMutation()
  const [deactivateWarehouse] = useDeactivateWarehouseMutation()
  const [activateWarehouse] = useActivateWarehouseMutation()
  const [form, setForm] = useState({ name: wh.name, address: wh.address, latitude: wh.latitude ?? null, longitude: wh.longitude ?? null })
  const [confirm, setConfirm] = useState(null)
  const ch = e => setForm(f => ({ ...f, [e.target.name]: e.target.value }))

  const handleEdit = async e => {
    e.preventDefault()
    try {
      await updateWarehouseMutation({ id: wh.id, name: form.name, address: form.address, latitude: form.latitude, longitude: form.longitude }).unwrap()
      toast.success('Warehouse updated')
    } catch (err) {
      toast.error(err?.data?.message || 'Update failed')
    }
  }

  return (
    <>
      <Modal title={`Manage — ${wh.name}`} wide onClose={onClose}>
        <div className="space-y-5">
          <div>
            <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-3">Edit Details</p>
            <form onSubmit={handleEdit} className="space-y-3">
              <Field label="Name" name="name" value={form.name} onChange={ch} placeholder="Warehouse Alpha" required />
              <Field label="Address">
                <PlacesAutocompleteInput
                  value={form.address}
                  onChange={addr => setForm(f => ({ ...f, address: addr }))}
                  onPlaceSelect={({ address, latitude, longitude }) => setForm(f => ({ ...f, address, latitude, longitude }))}
                  placeholder="123 Street, City, State"
                  required
                />
              </Field>
              <button type="submit" disabled={isSaving} className="w-full py-2 bg-teal-600 text-white rounded-lg text-sm font-medium hover:bg-teal-700 disabled:opacity-70">
                {isSaving ? 'Saving…' : 'Save Changes'}
              </button>
            </form>
          </div>
          <div className="pt-4 border-t border-gray-100">
            <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-3">Status</p>
            <div className="flex items-center justify-between">
              <StatusBadge status={wh.isActive ? 'ACTIVE' : 'SUSPENDED'} />
              <button
                onClick={() => setConfirm({
                  action: async () => {
                    try {
                      if (wh.isActive) await deactivateWarehouse(wh.id).unwrap()
                      else await activateWarehouse(wh.id).unwrap()
                      toast.success(wh.isActive ? 'Warehouse deactivated' : 'Warehouse activated')
                      onClose()
                    } catch (err) {
                      toast.error(err?.data?.message || 'Action failed')
                    }
                  },
                  title: wh.isActive ? 'Deactivate Warehouse' : 'Activate Warehouse',
                  message: wh.isActive ? `Deactivate ${wh.name}? Users assigned here will lose warehouse access.` : `Activate ${wh.name}?`,
                  label: wh.isActive ? 'Deactivate' : 'Activate',
                  danger: wh.isActive,
                })}
                className={`px-4 py-2 rounded-lg text-sm font-semibold transition-colors ${wh.isActive ? 'bg-red-50 text-red-600 hover:bg-red-100' : 'bg-teal-50 text-teal-700 hover:bg-teal-100'}`}
              >
                {wh.isActive ? 'Deactivate' : 'Activate'}
              </button>
            </div>
          </div>
        </div>
      </Modal>
      {confirm && <ConfirmDialog title={confirm.title} message={confirm.message} danger={confirm.danger} confirmLabel={confirm.label} onConfirm={() => { confirm.action(); setConfirm(null) }} onCancel={() => setConfirm(null)} />}
    </>
  )
}

function WarehousesPanel() {
  const { data: warehouses = [], isLoading } = useGetWarehousesQuery()
  const [createWarehouse, { isLoading: isCreating }] = useCreateWarehouseMutation()
  const [form, setForm] = useState({ name: '', address: '', latitude: null, longitude: null })
  const [search, setSearch] = useState('')
  const [selectedWh, setSelectedWh] = useState(null)
  const [manageWh, setManageWh] = useState(null)
  const [showAdd, setShowAdd] = useState(false)
  const ch = e => setForm(f => ({ ...f, [e.target.name]: e.target.value }))

  if (selectedWh) {
    const live = warehouses.find(w => w.id === selectedWh.id) || selectedWh
    return <WarehouseDetail warehouse={live} onBack={() => setSelectedWh(null)} />
  }

  const filtered = warehouses.filter(w =>
    w.name.toLowerCase().includes(search.toLowerCase()) ||
    w.address.toLowerCase().includes(search.toLowerCase())
  )

  const openAdd = () => { setForm({ name: '', address: '', latitude: null, longitude: null }); setShowAdd(true) }

  const handleSubmit = async e => {
    e.preventDefault()
    if (form.latitude === null || form.longitude === null) {
      toast.error('Please select an address from the dropdown to set the location coordinates.')
      return
    }
    try {
      await createWarehouse({ name: form.name, address: form.address, latitude: form.latitude, longitude: form.longitude }).unwrap()
      toast.success('Warehouse added')
      setShowAdd(false)
    } catch (err) {
      toast.error(err?.data?.message || 'Failed to add warehouse')
    }
  }

  return (
    <div className="space-y-4">
      {warehouses.length > 0 && (
        <div className="grid grid-cols-2 sm:grid-cols-3 gap-4">
          <StatCard title="Total Warehouses" value={warehouses.length} color="teal" />
          <StatCard title="Active" value={warehouses.filter(w => w.isActive).length} color="teal" />
          <StatCard title="Inactive" value={warehouses.filter(w => !w.isActive).length} color="gray" />
        </div>
      )}
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
          {isLoading ? (
            <div className="py-12 text-center text-sm text-gray-400">Loading…</div>
          ) : !isLoading && warehouses.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-16 text-center px-8">
              <div className="w-12 h-12 rounded-2xl bg-teal-50 flex items-center justify-center mb-3">
                <svg className="w-6 h-6 text-teal-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
                </svg>
              </div>
              <h3 className="text-sm font-semibold text-gray-800 mb-1">No warehouses yet</h3>
              <p className="text-xs text-gray-400 mb-4 max-w-xs leading-relaxed">
                Create your first warehouse to start tracking inventory, assigning staff, and monitoring stock levels.
              </p>
              <button
                onClick={openAdd}
                className="flex items-center gap-1.5 px-4 py-2 bg-teal-600 text-white text-sm font-semibold rounded-lg hover:bg-teal-700 transition-colors"
              >
                <AddIcon /> Create Warehouse
              </button>
            </div>
          ) : (
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
                  <tr><td colSpan={4} className="px-5 py-8 text-center text-gray-400 text-sm">No warehouses match your search</td></tr>
                ) : filtered.map(wh => (
                  <tr key={wh.id} className="hover:bg-gray-50/60">
                    <td className="px-5 py-3 font-medium text-gray-900">{wh.name}</td>
                    <td className="px-5 py-3 text-gray-600">{wh.address}</td>
                    <td className="px-5 py-3"><StatusBadge status={wh.isActive ? 'ACTIVE' : 'SUSPENDED'} /></td>
                    <td className="px-5 py-3">
                      <div className="flex items-center gap-2">
                        <button onClick={() => setSelectedWh(wh)} className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg text-xs font-medium bg-teal-50 text-teal-700 border border-teal-100 hover:bg-teal-100 transition-colors">
                          <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" /><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" /></svg>
                          View
                        </button>
                        <button onClick={() => setManageWh(wh)} className="px-2.5 py-1.5 rounded-lg text-xs font-medium bg-gray-100 text-gray-700 border border-gray-200 hover:bg-gray-200 transition-colors">
                          Manage
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
      {showAdd && (
        <Modal title="Add Warehouse" onClose={() => setShowAdd(false)}>
          <form onSubmit={handleSubmit} className="space-y-3">
            <Field label="Warehouse Name" name="name" value={form.name} onChange={ch} placeholder="Warehouse Alpha" required />
            <Field label="Address">
              <PlacesAutocompleteInput
                value={form.address}
                onChange={addr => setForm(f => ({ ...f, address: addr, latitude: null, longitude: null }))}
                onPlaceSelect={({ address, latitude, longitude }) => setForm(f => ({ ...f, address, latitude, longitude }))}
                placeholder="15 Awolowo Road, Ikoyi, Lagos"
                required
              />
              {form.address && form.latitude === null && (
                <p className="text-xs text-amber-600 mt-1">Select an address from the dropdown to capture coordinates.</p>
              )}
            </Field>
            <BtnRow onClose={() => setShowAdd(false)} submitLabel={isCreating ? 'Adding…' : 'Add Warehouse'} />
          </form>
        </Modal>
      )}
      {manageWh && (
        <ManageWarehouseModal
          wh={warehouses.find(w => w.id === manageWh.id) || manageWh}
          onClose={() => setManageWh(null)}
        />
      )}
    </div>
  )
}

// ── Products Panel ────────────────────────────────────────────────────────────

function ProductsPanel() {
  const { data: products = [], isLoading } = useGetProductsQuery()
  const [createProduct, { isLoading: isCreating }] = useCreateProductMutation()
  const [updateProductMutation, { isLoading: isUpdating }] = useUpdateProductMutation()
  const [importProducts, { isLoading: isImporting }] = useImportProductsMutation()
  const [showAdd, setShowAdd] = useState(false)
  const [showBatch, setShowBatch] = useState(false)
  const [edit, setEdit] = useState(null)
  const [form, setForm] = useState({ name: '', sku: '', unitOfMeasure: '', defaultThreshold: '' })
  const [addViewMode, setAddViewMode] = useState('cards')
  const [search, setSearch] = useState('')
  const [csvFile, setCsvFile] = useState(null)
  const [csvPreview, setCsvPreview] = useState([])
  const [csvError, setCsvError] = useState('')
  const [confirm, setConfirm] = useState(null)
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

  const switchAddView = (mode) => {
    if (mode === 'table') {
      setRows(rs => { const p = [...rs]; while (p.length < 10) p.push({ ...EMPTY_ROW }); return p })
    } else {
      setRows(rs => { const ne = rs.filter(r => r.name || r.sku); return ne.length > 0 ? ne : [{ ...EMPTY_ROW }] })
    }
    setAddViewMode(mode)
  }

  const openAdd = () => { setRows([{ ...EMPTY_ROW }]); setAddViewMode('cards'); setEdit(null); setShowAdd(true) }
  const openEdit = p => { setForm({ name: p.name, sku: p.sku, unitOfMeasure: p.unitOfMeasure, defaultThreshold: p.defaultThreshold }); setEdit(p); setShowAdd(true) }

  const handleSubmit = async e => {
    e.preventDefault()
    if (edit) {
      try {
        await updateProductMutation({ id: edit.id, name: form.name, unitOfMeasure: form.unitOfMeasure, defaultThreshold: +form.defaultThreshold }).unwrap()
        toast.success('Product updated')
        setShowAdd(false)
      } catch (err) {
        toast.error(err?.data?.message || 'Failed to update product')
      }
    } else {
      const valid = rows.filter(r => r.name && r.sku)
      if (valid.length === 0) { toast.error('Add at least one product with a name and SKU'); return }
      try {
        await Promise.all(valid.map(r => createProduct({ name: r.name, sku: r.sku, unitOfMeasure: r.unitOfMeasure || 'units', defaultThreshold: +r.defaultThreshold || 0 }).unwrap()))
        toast.success(`${valid.length} product${valid.length !== 1 ? 's' : ''} added`)
        setShowAdd(false)
        setRows([{ ...EMPTY_ROW }])
      } catch (err) {
        toast.error(err?.data?.message || 'Failed to add products')
      }
    }
  }

  const handleCSVFile = (e) => {
    const file = e.target.files[0]
    if (!file) return
    setCsvFile(file)
    const reader = new FileReader()
    reader.onload = (ev) => {
      const parsed = parseCSV(ev.target.result)
      if (parsed.length === 0) { setCsvError('No valid rows found. Check the format.'); setCsvPreview([]) }
      else { setCsvError(''); setCsvPreview(parsed) }
    }
    reader.readAsText(file)
  }

  const handleBatchImport = async () => {
    if (!csvFile) return
    try {
      const result = await importProducts(csvFile).unwrap()
      toast.success(`${result.length} products imported successfully`)
      setShowBatch(false)
      setCsvPreview([])
      setCsvFile(null)
    } catch (err) {
      toast.error(err?.data?.message || 'Import failed')
    }
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
          {isLoading ? (
            <div className="py-12 text-center text-sm text-gray-400">Loading…</div>
          ) : (
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
                      <button onClick={() => openEdit(p)} className="px-2.5 py-1 rounded-lg text-xs font-medium bg-blue-50 text-blue-600 border border-blue-100 hover:bg-blue-100 transition-colors">
                        Edit
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
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
        <Modal title="Add Products" wide onClose={() => { setShowAdd(false); setRows([{ ...EMPTY_ROW }]); setAddViewMode('cards') }}>
          <form onSubmit={handleSubmit}>
            <div className="flex items-center justify-between mb-3">
              <p className="text-xs text-gray-500">{rows.length} product{rows.length !== 1 ? 's' : ''} to add</p>
              <ViewToggle mode={addViewMode} onChange={switchAddView} />
            </div>

            {addViewMode === 'table' ? (
              <div className="rounded-lg border border-gray-300 overflow-visible mb-4">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="bg-gray-100">
                      <th className="w-8 border-b-2 border-b-gray-300 border-r border-r-gray-300 py-2.5 text-center text-[11px] text-gray-500 font-medium select-none">#</th>
                      <th className="px-3 py-2.5 text-left text-[11px] font-semibold text-gray-600 uppercase tracking-wide border-b-2 border-b-gray-300 border-r border-r-gray-300">Name</th>
                      <th className="px-3 py-2.5 text-left text-[11px] font-semibold text-gray-600 uppercase tracking-wide border-b-2 border-b-gray-300 border-r border-r-gray-300 w-32">SKU</th>
                      <th className="px-3 py-2.5 text-left text-[11px] font-semibold text-gray-600 uppercase tracking-wide border-b-2 border-b-gray-300 border-r border-r-gray-300 w-28">Unit</th>
                      <th className="px-3 py-2.5 text-left text-[11px] font-semibold text-gray-600 uppercase tracking-wide border-b-2 border-b-gray-300 w-24">Threshold</th>
                    </tr>
                  </thead>
                  <tbody>
                    {rows.map((row, i) => (
                      <tr key={i} className="border-b border-gray-200 last:border-b-0">
                        <td className="w-8 bg-gray-50 border-r border-gray-200 text-center text-[11px] text-gray-400 select-none py-1 tabular-nums">{i + 1}</td>
                        <td className="p-0 border-r border-gray-200 focus-within:bg-blue-50/50">
                          <input
                            value={row.name} onChange={e => updateRow(i, 'name', e.target.value)}
                            placeholder="Industrial Laptop"
                            className="w-full px-3 py-2.5 bg-transparent border-0 focus:outline-none text-sm placeholder:text-gray-300"
                          />
                        </td>
                        <td className="p-0 border-r border-gray-200 focus-within:bg-blue-50/50">
                          <input
                            value={row.sku} onChange={e => updateRow(i, 'sku', e.target.value)}
                            placeholder="LAP-001"
                            className="w-full px-3 py-2.5 bg-transparent border-0 focus:outline-none text-sm font-mono placeholder:text-gray-300"
                          />
                        </td>
                        <td className="p-0 border-r border-gray-200 focus-within:bg-blue-50/50">
                          <input
                            value={row.unitOfMeasure} onChange={e => updateRow(i, 'unitOfMeasure', e.target.value)}
                            placeholder="units"
                            className="w-full px-3 py-2.5 bg-transparent border-0 focus:outline-none text-sm placeholder:text-gray-300"
                          />
                        </td>
                        <td className="p-0 focus-within:bg-blue-50/50">
                          <input
                            type="number" min="0" value={row.defaultThreshold}
                            onChange={e => updateRow(i, 'defaultThreshold', e.target.value)}
                            placeholder="20"
                            className="w-full px-3 py-2.5 bg-transparent border-0 focus:outline-none text-sm placeholder:text-gray-300"
                          />
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                <div className="border-t border-gray-200 bg-gray-50 px-4 py-2 flex items-center justify-between">
                  <span className="text-xs text-gray-400">{rows.filter(r => r.name || r.sku).length} of {rows.length} rows filled</span>
                  <button type="button" onClick={() => setRows(rs => { const a = [...rs]; for (let j = 0; j < 10; j++) a.push({ ...EMPTY_ROW }); return a })} className="text-xs font-medium text-teal-600 hover:text-teal-700 transition-colors">
                    + Add 10 more rows
                  </button>
                </div>
              </div>
            ) : (
              <>
                <div className="space-y-3 mb-4 max-h-96 overflow-y-auto pr-1">
                  {rows.map((row, i) => (
                    <div key={i} className="bg-gray-50 border border-gray-200 rounded-xl p-4 relative">
                      <div className="flex items-center justify-between mb-3">
                        <span className="text-xs font-bold text-gray-400 uppercase tracking-widest">Product {String(i + 1).padStart(2, '0')}</span>
                        <button type="button" onClick={() => removeRow(i)} disabled={rows.length === 1} className="w-7 h-7 flex items-center justify-center rounded-lg text-gray-300 hover:text-red-500 hover:bg-red-50 disabled:opacity-0 disabled:pointer-events-none transition-colors">
                          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /></svg>
                        </button>
                      </div>
                      <div className="grid grid-cols-2 gap-3">
                        <div>
                          <label className="block text-xs font-medium text-gray-500 mb-1">Name <span className="text-red-400">*</span></label>
                          <input value={row.name} onChange={e => updateRow(i, 'name', e.target.value)} placeholder="e.g. Industrial Laptop" required className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm bg-white focus:outline-none focus:ring-2 focus:ring-teal-600 focus:border-transparent" />
                        </div>
                        <div>
                          <label className="block text-xs font-medium text-gray-500 mb-1">SKU <span className="text-red-400">*</span></label>
                          <input value={row.sku} onChange={e => updateRow(i, 'sku', e.target.value)} placeholder="e.g. LAP-001" required className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm bg-white font-mono focus:outline-none focus:ring-2 focus:ring-teal-600 focus:border-transparent" />
                        </div>
                        <div>
                          <label className="block text-xs font-medium text-gray-500 mb-1">Unit of Measure</label>
                          <input value={row.unitOfMeasure} onChange={e => updateRow(i, 'unitOfMeasure', e.target.value)} placeholder="units / kg / boxes" className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm bg-white focus:outline-none focus:ring-2 focus:ring-teal-600 focus:border-transparent" />
                        </div>
                        <div>
                          <label className="block text-xs font-medium text-gray-500 mb-1">Default Threshold</label>
                          <input type="number" min="0" value={row.defaultThreshold} onChange={e => updateRow(i, 'defaultThreshold', e.target.value)} placeholder="e.g. 20" className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm bg-white focus:outline-none focus:ring-2 focus:ring-teal-600 focus:border-transparent" />
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
                <button type="button" onClick={() => setRows(rs => [...rs, { ...EMPTY_ROW }])} className="flex items-center gap-2 w-full py-2.5 border-2 border-dashed border-gray-300 rounded-xl text-sm text-gray-500 hover:border-teal-400 hover:text-teal-600 hover:bg-teal-50/40 transition-colors justify-center font-medium mb-4">
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" /></svg>
                  Add another product
                </button>
              </>
            )}

            <BtnRow onClose={() => { setShowAdd(false); setRows([{ ...EMPTY_ROW }]); setAddViewMode('cards') }} submitLabel={`Add ${rows.filter(r => r.name).length > 1 ? `${rows.filter(r => r.name).length} Products` : 'Product'}`} />
          </form>
        </Modal>
      )}

      {showBatch && (
        <Modal title="Batch Import Products" wide onClose={() => { setShowBatch(false); setCsvPreview([]); setCsvError(''); setCsvFile(null) }}>
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
              <p className="text-sm text-gray-600 font-medium">{csvFile ? csvFile.name : 'Click to upload CSV file'}</p>
              <p className="text-xs text-gray-400 mt-1">{csvFile ? 'Click to change file' : 'or drag and drop'}</p>
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
              <button type="button" onClick={() => { setShowBatch(false); setCsvPreview([]); setCsvError(''); setCsvFile(null) }} className="flex-1 py-2 border border-gray-300 text-gray-700 rounded-lg text-sm hover:bg-gray-50">Cancel</button>
              <button
                type="button"
                onClick={handleBatchImport}
                disabled={!csvFile || isImporting}
                className="flex-1 py-2 bg-teal-600 text-white rounded-lg text-sm font-medium hover:bg-teal-700 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {isImporting ? 'Importing…' : csvPreview.length > 0 ? `Import ${csvPreview.length} Products` : 'Import Products'}
              </button>
            </div>
          </div>
        </Modal>
      )}
      {confirm && <ConfirmDialog title={confirm.title} message={confirm.message} danger={confirm.danger} confirmLabel={confirm.label} onConfirm={() => { confirm.action(); setConfirm(null) }} onCancel={() => setConfirm(null)} />}
    </div>
  )
}

// ── Setup Checklist ───────────────────────────────────────────────────────────

function SetupChecklist({ warehouses, users, onGoToWarehouses, onGoToUsers }) {
  const { companyName } = useSelector(s => s.auth)
  const [dismissed, setDismissed] = useState(() =>
    localStorage.getItem('inventalert_setup_dismissed') === 'true'
  )

  const hasWarehouse = warehouses.length > 0
  const hasNonAdminUser = users.some(u => u.role !== 'ADMIN')
  const allDone = hasWarehouse && hasNonAdminUser

  if (dismissed || allDone) return null

  const steps = [
    { label: 'Create your first warehouse', done: hasWarehouse },
    { label: 'Invite your first team member', done: hasNonAdminUser, blocked: !hasWarehouse },
  ]
  const doneCount = steps.filter(s => s.done).length
  const pct = Math.round((doneCount / steps.length) * 100)
  const nextAction = !hasWarehouse
    ? { label: 'Create Warehouse', fn: onGoToWarehouses }
    : { label: 'Add Team Member', fn: onGoToUsers }

  return (
    <div className="relative bg-gradient-to-br from-teal-600 to-teal-700 rounded-2xl p-6 mb-5 overflow-hidden">
      <div className="absolute top-0 right-0 w-56 h-56 bg-white/5 rounded-full -translate-y-1/2 translate-x-1/3 pointer-events-none" />
      <div className="absolute bottom-0 right-20 w-28 h-28 bg-white/5 rounded-full translate-y-1/2 pointer-events-none" />

      <div className="relative flex items-start justify-between gap-6">
        <div className="flex-1 min-w-0">
          <p className="text-teal-200 text-xs font-semibold uppercase tracking-widest mb-1">Getting Started</p>
          <h2 className="text-xl font-bold text-white mb-1">
            Welcome{companyName ? ` to ${companyName}` : ''}
          </h2>
          <p className="text-teal-100 text-sm mb-5">
            {doneCount === 0
              ? "Complete 2 quick steps to activate your workspace."
              : "You're almost there — one step left."}
          </p>

          <div className="mb-5">
            <div className="flex items-center justify-between mb-1.5">
              <span className="text-xs text-teal-200 font-medium">Setup progress</span>
              <span className="text-xs text-white font-bold">{pct}%</span>
            </div>
            <div className="h-1.5 bg-white/20 rounded-full overflow-hidden">
              <div
                className="h-full bg-white rounded-full transition-all duration-500"
                style={{ width: `${pct}%` }}
              />
            </div>
          </div>

          <div className="flex flex-col gap-2 mb-5">
            {steps.map((step, i) => (
              <div key={i} className="flex items-center gap-2.5">
                <div className={`w-5 h-5 rounded-full flex items-center justify-center shrink-0 ${step.done ? 'bg-white text-teal-600' : 'border-2 border-white/40'}`}>
                  {step.done ? (
                    <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M5 13l4 4L19 7" />
                    </svg>
                  ) : (
                    <span className="w-1.5 h-1.5 rounded-full bg-white/50 block" />
                  )}
                </div>
                <span className={`text-sm ${step.done ? 'text-teal-200 line-through' : step.blocked ? 'text-teal-300/60' : 'text-white font-medium'}`}>
                  {step.label}
                  {step.blocked && <span className="ml-2 text-xs text-teal-300/50 font-normal">complete step 1 first</span>}
                </span>
              </div>
            ))}
          </div>

          <button
            onClick={nextAction.fn}
            className="inline-flex items-center gap-2 px-5 py-2.5 bg-white text-teal-700 text-sm font-bold rounded-xl hover:bg-teal-50 transition-colors shadow-md"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
            </svg>
            {nextAction.label}
          </button>
        </div>

        <button
          onClick={() => { setDismissed(true); localStorage.setItem('inventalert_setup_dismissed', 'true') }}
          className="text-white/30 hover:text-white/70 p-1 shrink-0 transition-colors"
          title="Dismiss"
        >
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>
    </div>
  )
}

// ── Users Panel ───────────────────────────────────────────────────────────────

function ManageUserModal({ user: u, onClose }) {
  const { data: warehouses = [] } = useGetWarehousesQuery()
  const { data: assignments = [] } = useGetUserAssignmentsQuery(u.id)
  const [updateUserRoleMutation] = useUpdateUserRoleMutation()
  const [assignToWarehouse] = useAssignToWarehouseMutation()
  const [removeAssignmentMutation] = useRemoveAssignmentMutation()
  const [deactivateUserMutation] = useDeactivateUserMutation()
  const [reactivateUserMutation] = useReactivateUserMutation()
  const [role, setRole] = useState(u.role)
  const [addWh, setAddWh] = useState('')
  const [confirm, setConfirm] = useState(null)

  const displayName = u.email.split('@')[0]
  const assignmentsWithName = assignments.map(a => ({
    ...a,
    warehouseName: warehouses.find(w => w.id === a.warehouseId)?.name || a.warehouseId,
  }))
  const alreadyAssigned = assignments.length > 0

  const saveRole = async () => {
    const result = await updateUserRoleMutation({ id: u.id, role })
    if (result.data) toast.success('Role updated')
    else toast.error(result.error?.data?.message || 'Failed to update role')
  }

  const doAssign = async e => {
    e.preventDefault()
    if (!addWh) return
    const result = await assignToWarehouse({ id: u.id, warehouseId: addWh })
    if (result.data) { toast.success('Warehouse assigned'); setAddWh('') }
    else toast.error(result.error?.data?.message || 'Assignment failed')
  }

  return (
    <>
      <Modal title={`Manage — ${displayName}`} wide onClose={onClose}>
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
              {assignmentsWithName.length === 0 ? (
                <p className="text-sm text-gray-400 italic">No warehouses assigned yet.</p>
              ) : (
                <div className="flex flex-wrap gap-1.5">
                  {assignmentsWithName.map(a => (
                    <span key={a.id} className="inline-flex items-center gap-1.5 bg-teal-50 text-teal-700 text-xs px-2.5 py-1 rounded-full border border-teal-200 font-medium">
                      {a.warehouseName}
                      <button
                        onClick={() => setConfirm({ action: async () => { await removeAssignmentMutation({ userId: u.id, assignmentId: a.id }); toast.info('Assignment removed') }, title: 'Remove Assignment', message: `Remove ${displayName} from ${a.warehouseName}?`, label: 'Remove', danger: true })}
                        className="text-teal-400 hover:text-red-500 leading-none font-bold"
                      >×</button>
                    </span>
                  ))}
                </div>
              )}
              {alreadyAssigned ? (
                <p className="text-xs text-amber-700 bg-amber-50 border border-amber-200 rounded-lg px-3 py-2 mt-2">
                  Remove the current warehouse assignment before assigning a new one.
                </p>
              ) : (
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
              )}
            </div>
          </div>

          {/* Account status */}
          <div className="pt-4 border-t border-gray-100 flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-700">{displayName}</p>
              <p className="text-xs text-gray-400">{u.email}</p>
            </div>
            <button
              onClick={() => setConfirm({ action: async () => { u.isActive ? await deactivateUserMutation(u.id) : await reactivateUserMutation(u.id); toast.success(u.isActive ? 'User deactivated' : 'User reactivated'); onClose() }, title: u.isActive ? 'Deactivate Account' : 'Reactivate Account', message: u.isActive ? `Deactivate ${displayName}? They will lose access to the system.` : `Reactivate ${displayName}? They will regain system access.`, label: u.isActive ? 'Deactivate' : 'Reactivate', danger: u.isActive })}
              className={`px-4 py-2 rounded-lg text-sm font-semibold transition-colors ${u.isActive ? 'bg-red-50 text-red-600 hover:bg-red-100' : 'bg-teal-50 text-teal-700 hover:bg-teal-100'}`}
            >
              {u.isActive ? 'Deactivate Account' : 'Reactivate Account'}
            </button>
          </div>
        </div>
      </Modal>
      {confirm && <ConfirmDialog title={confirm.title} message={confirm.message} danger={confirm.danger} confirmLabel={confirm.label} onConfirm={() => { confirm.action(); setConfirm(null) }} onCancel={() => setConfirm(null)} />}
    </>
  )
}

function UsersPanel({ onGoToWarehouses }) {
  const { data: users = [], isLoading } = useGetUsersQuery()
  const { data: warehouses = [], isLoading: isLoadingWarehouses } = useGetWarehousesQuery()
  const { user: me } = useSelector(s => s.auth)
  const [createUser, { isLoading: isCreating }] = useCreateUserMutation()
  const [showAdd, setShowAdd] = useState(false)
  const [showTempPass, setShowTempPass] = useState(false)
  const [manageUser, setManageUser] = useState(null)
  const [form, setForm] = useState({ email: '', role: 'MANAGER', password: '', warehouseId: '' })
  const [search, setSearch] = useState('')
  const ch = e => setForm(f => ({ ...f, [e.target.name]: e.target.value }))

  const filtered = users.filter(u =>
    u.email.toLowerCase().includes(search.toLowerCase())
  )

  const handleAdd = async e => {
    e.preventDefault()
    try {
      await createUser({ email: form.email, role: form.role, password: form.password, warehouseId: form.warehouseId || null }).unwrap()
      toast.success('User created — they must set a new password on first login')
      setShowAdd(false)
      setShowTempPass(false)
      setForm({ email: '', role: 'MANAGER', password: '', warehouseId: '' })
    } catch (err) {
      toast.error(err?.data?.message || 'Failed to create user')
    }
  }

  const roleCount = r => users.filter(u => u.role === r).length

  if (!isLoadingWarehouses && warehouses.length === 0) {
    return (
      <div className="bg-white rounded-xl border border-gray-200 flex flex-col items-center justify-center text-center py-20 px-8">
        <div className="w-14 h-14 rounded-full bg-amber-50 flex items-center justify-center mb-4">
          <svg className="w-7 h-7 text-amber-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
          </svg>
        </div>
        <h3 className="text-base font-semibold text-gray-900 mb-1">No warehouses yet</h3>
        <p className="text-sm text-gray-500 mb-5 max-w-xs">
          You need at least one warehouse before adding managers or staff. Create a warehouse first, then come back to add your team.
        </p>
        <button
          onClick={onGoToWarehouses}
          className="flex items-center gap-2 px-4 py-2 bg-teal-600 text-white text-sm font-semibold rounded-lg hover:bg-teal-700 transition-colors"
        >
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
          </svg>
          Create a Warehouse
        </button>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        <StatCard title="Total Users" value={users.length} color="blue" />
        <StatCard title="Active" value={users.filter(u => u.isActive).length} color="teal" />
        <StatCard title="Staff" value={roleCount('WAREHOUSE_STAFF')} color="amber" />
        <StatCard title="Inactive" value={users.filter(u => !u.isActive).length} color="gray" />
      </div>
      <div className="bg-white rounded-xl border border-gray-200">
        <div className="flex flex-wrap items-center justify-between gap-3 px-5 py-4 border-b border-gray-100">
          <h3 className="font-semibold text-gray-900">Team Members</h3>
          <div className="flex items-center gap-2 flex-wrap">
            <SearchBar value={search} onChange={setSearch} placeholder="Search members…" />
            <button onClick={() => setShowAdd(true)} className="flex items-center gap-1.5 px-3 py-1.5 bg-teal-600 text-white text-sm font-medium rounded-lg hover:bg-teal-700">
              <AddIcon /> Add User
            </button>
          </div>
        </div>
        <div className="overflow-x-auto">
          {isLoading ? (
            <div className="py-12 text-center text-sm text-gray-400">Loading…</div>
          ) : (
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-gray-50 border-b border-gray-100">
                  {['Name', 'Email', 'Role', 'Joined', 'Status', 'Actions'].map(h => (
                    <th key={h} className="text-left px-5 py-3 font-semibold text-gray-600 text-xs uppercase tracking-wide">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50">
                {filtered.map(u => {
                  const displayName = u.email.split('@')[0]
                  return (
                    <tr key={u.id} className={`hover:bg-gray-50/60 ${!u.isActive ? 'opacity-60' : ''}`}>
                      <td className="px-5 py-3">
                        <div className="flex items-center gap-3">
                          <div className="w-8 h-8 rounded-full bg-teal-100 flex items-center justify-center text-teal-700 font-semibold text-sm shrink-0">
                            {displayName[0].toUpperCase()}
                          </div>
                          <span className="font-medium text-gray-900">{displayName}</span>
                        </div>
                      </td>
                      <td className="px-5 py-3 text-gray-600">{u.email}</td>
                      <td className="px-5 py-3"><StatusBadge status={u.role} /></td>
                      <td className="px-5 py-3 text-gray-400 text-xs whitespace-nowrap">{u.createdAt ? fmtDate(u.createdAt) : '—'}</td>
                      <td className="px-5 py-3"><StatusBadge status={u.isActive ? 'ACTIVE' : 'SUSPENDED'} /></td>
                      <td className="px-5 py-3">
                        {u.id !== me?.id ? (
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
                  )
                })}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {showAdd && (
        <Modal title="Add Team Member" onClose={() => setShowAdd(false)}>
          <form onSubmit={handleAdd} className="space-y-3">
            <Field label="Email" name="email" type="email" value={form.email} onChange={ch} placeholder="jane@company.com" required />
            <Field label="Role">
              <select name="role" value={form.role} onChange={ch} className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-teal-600">
                <option value="MANAGER">Manager</option>
                <option value="WAREHOUSE_STAFF">Warehouse Staff</option>
                <option value="PROCUREMENT_OFFICER">Procurement Officer</option>
              </select>
            </Field>
            <Field label="Assign to Warehouse">
              <select name="warehouseId" value={form.warehouseId} onChange={ch} className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-teal-600">
                <option value="">No warehouse (assign later)</option>
                {warehouses.filter(w => w.isActive).map(w => (
                  <option key={w.id} value={w.id}>{w.name}</option>
                ))}
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
            <BtnRow onClose={() => setShowAdd(false)} submitLabel={isCreating ? 'Adding…' : 'Add User'} />
          </form>
        </Modal>
      )}

      {manageUser && <ManageUserModal user={manageUser} onClose={() => setManageUser(null)} />}
    </div>
  )
}

// ── Feedback & Support Panel ──────────────────────────────────────────────────

function FeedbackPanel() {
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
      dispatch(submitComplaint({ subject: form.subject, priority: form.priority, message: form.message, submittedBy: me?.email, email: me?.email, companyName, companyId }))
      setLoading(false)
      setSubmitted(true)
    }, 500)
  }

  if (submitted) {
    return (
      <div className="bg-white rounded-xl border border-gray-200 p-6">
        <div className="flex flex-col items-center justify-center py-16 gap-4">
          <div className="w-14 h-14 rounded-full bg-teal-50 flex items-center justify-center">
            <svg className="w-7 h-7 text-teal-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" /></svg>
          </div>
          <h3 className="text-lg font-bold text-gray-900">Feedback Submitted</h3>
          <p className="text-sm text-gray-500 text-center max-w-sm">Your feedback has been submitted and will be reviewed by the SuperAdmin team.</p>
          <button onClick={() => { setSubmitted(false); setForm({ subject: '', priority: 'MEDIUM', message: '' }) }} className="px-5 py-2 bg-teal-600 text-white text-sm font-semibold rounded-xl hover:bg-teal-700 transition-colors">Submit Another</button>
        </div>
      </div>
    )
  }

  return (
    <div className="bg-white rounded-xl border border-gray-200 p-6">
      <h2 className="text-xl font-bold text-gray-900 mb-0.5">Feedback & Support</h2>
      <p className="text-sm text-gray-500 mb-5">Submit a complaint, inquiry, or suggestion to the SuperAdmin team.</p>
      <form onSubmit={handleSubmit} className="space-y-4">
        <Field label="Subject" name="subject" value={form.subject} onChange={ch} placeholder="Brief description of your feedback" required />
        <Field label="Priority">
          <select name="priority" value={form.priority} onChange={ch} className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-teal-600">
            <option value="HIGH">High</option>
            <option value="MEDIUM">Medium</option>
            <option value="LOW">Low</option>
          </select>
        </Field>
        <Field label="Message">
          <textarea name="message" value={form.message} onChange={ch} rows={6} className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-teal-600 resize-none" placeholder="Describe your feedback in detail…" required />
        </Field>
        <button type="submit" disabled={loading} className="px-6 py-2.5 bg-teal-600 text-white font-semibold rounded-xl hover:bg-teal-700 transition-colors flex items-center gap-2 disabled:opacity-70">
          {loading && <svg className="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" /><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" /></svg>}
          Submit Feedback
        </button>
      </form>
    </div>
  )
}

// ── Main ──────────────────────────────────────────────────────────────────────

export default function AdminDashboard() {
  const [activeTab, setActiveTab] = useState('warehouses')
  const { data: users = [] } = useGetUsersQuery()
  const { data: warehouses = [] } = useGetWarehousesQuery()

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
      id: 'users', label: 'Users', badge: users.length,
      icon: <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" /></svg>,
    },
    {
      id: 'feedback', label: 'Feedback & Support', badge: 0,
      icon: <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" /></svg>,
    },
  ]

  return (
    <Layout title="Admin Dashboard" navItems={navItems} activeTab={activeTab} onTabChange={setActiveTab}>
      <SetupChecklist
        warehouses={warehouses}
        users={users}
        onGoToWarehouses={() => setActiveTab('warehouses')}
        onGoToUsers={() => setActiveTab('users')}
      />
      {activeTab === 'company' && <CompanyPanel />}
      {activeTab === 'warehouses' && <WarehousesPanel />}
      {activeTab === 'products' && <ProductsPanel />}
      {activeTab === 'users' && <UsersPanel onGoToWarehouses={() => setActiveTab('warehouses')} />}
      {activeTab === 'feedback' && <FeedbackPanel />}
    </Layout>
  )
}
