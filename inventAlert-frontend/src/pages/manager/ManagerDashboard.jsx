import { useState } from 'react'
import { useSelector, useDispatch } from 'react-redux'
import { toast } from 'react-toastify'
import Layout from '../../components/layout/Layout'
import StatusBadge from '../../components/shared/StatusBadge'
import StatCard from '../../components/shared/StatCard'
import { approveTransfer, rejectTransfer } from '../../store/slices/transfersSlice'
import { approveReconciliation, rejectReconciliation } from '../../store/slices/reconciliationsSlice'
import { updateUserRole } from '../../store/slices/usersSlice'

const fmtDate = d => new Date(d).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
const fmtDT = d => new Date(d).toLocaleString('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })

function SectionHeader({ title, subtitle }) {
  return (
    <div className="mb-4">
      <h2 className="text-base font-semibold text-gray-900">{title}</h2>
      {subtitle && <p className="text-sm text-gray-500 mt-0.5">{subtitle}</p>}
    </div>
  )
}

// ── Stock Overview ────────────────────────────────────────────────────────────

function StockPanel() {
  const { stockLevels, products, warehouses } = useSelector(s => s.stock)
  const [filter, setFilter] = useState('ALL')

  const rows = stockLevels
    .map(sl => ({
      ...sl,
      productName: products.find(p => p.id === sl.productId)?.name || sl.productId,
      sku: products.find(p => p.id === sl.productId)?.sku || '',
      warehouseName: warehouses.find(w => w.id === sl.warehouseId)?.name || sl.warehouseId,
      status: sl.currentStock < sl.threshold ? 'CRITICAL'
        : sl.currentStock < sl.threshold * 1.25 ? 'WARNING' : 'OK',
    }))
    .filter(r => filter === 'ALL' || r.status === filter)
    .sort((a, b) => (a.daysUntilEmpty ?? 999) - (b.daysUntilEmpty ?? 999))

  const criticalCount = stockLevels.filter(sl => sl.currentStock < sl.threshold).length

  const stockColor = status => ({
    CRITICAL: 'text-red-700 bg-red-50',
    WARNING: 'text-amber-700 bg-amber-50',
    OK: 'text-green-700 bg-green-50',
  }[status] || '')

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        <StatCard title="Total Stock Items" value={stockLevels.length} color="blue" />
        <StatCard title="Below Threshold" value={criticalCount} color="red" />
        <StatCard title="Active Warehouses" value={warehouses.filter(w => w.isActive).length} color="green" />
        <StatCard title="Products" value={products.filter(p => p.isActive).length} color="gray" />
      </div>

      <div className="bg-white rounded-xl border border-gray-200">
        <div className="flex items-center justify-between px-5 py-4 border-b border-gray-100">
          <h3 className="font-semibold text-gray-900">Live Stock Overview</h3>
          <div className="flex gap-1">
            {['ALL', 'CRITICAL', 'WARNING', 'OK'].map(f => (
              <button key={f} onClick={() => setFilter(f)} className={`px-3 py-1 rounded-lg text-xs font-medium transition-colors ${filter === f ? 'bg-teal-600 text-white' : 'text-gray-500 hover:bg-gray-100'}`}>{f}</button>
            ))}
          </div>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-100">
                {['Product', 'SKU', 'Warehouse', 'Stock', 'Threshold', 'Status', 'Velocity/day', 'Days Until Empty'].map(h => (
                  <th key={h} className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wide">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {rows.map(r => (
                <tr key={r.id} className="hover:bg-gray-50/60">
                  <td className="px-4 py-3 font-medium text-gray-900">{r.productName}</td>
                  <td className="px-4 py-3 font-mono text-xs text-gray-500">{r.sku}</td>
                  <td className="px-4 py-3 text-gray-600">{r.warehouseName}</td>
                  <td className="px-4 py-3">
                    <span className={`px-2 py-0.5 rounded font-semibold text-sm ${stockColor(r.status)}`}>{r.currentStock}</span>
                  </td>
                  <td className="px-4 py-3 text-gray-600">{r.threshold}</td>
                  <td className="px-4 py-3"><StatusBadge status={r.status} /></td>
                  <td className="px-4 py-3 text-gray-600">{r.velocityPerDay?.toFixed(1) ?? '—'}</td>
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

// ── Movements Panel ───────────────────────────────────────────────────────────

function SearchBar({ value, onChange, placeholder }) {
  return (
    <div className="relative">
      <svg className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
      </svg>
      <input
        type="text" value={value} onChange={e => onChange(e.target.value)} placeholder={placeholder}
        className="pl-9 pr-4 py-1.5 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-teal-600 w-52"
      />
    </div>
  )
}

function MovementsPanel() {
  const { movements, products, warehouses } = useSelector(s => s.stock)
  const { users } = useSelector(s => s.users)
  const [typeFilter, setTypeFilter] = useState('ALL')
  const [search, setSearch] = useState('')

  const rows = movements
    .map(m => ({
      ...m,
      productName: products.find(p => p.id === m.productId)?.name || m.productId,
      warehouseName: warehouses.find(w => w.id === m.warehouseId)?.name || m.warehouseId,
      createdByName: users.find(u => u.id === m.createdBy)?.name || m.createdBy,
    }))
    .filter(m => typeFilter === 'ALL' || m.type === typeFilter)
    .filter(m => !search || m.productName.toLowerCase().includes(search.toLowerCase()) || m.warehouseName.toLowerCase().includes(search.toLowerCase()))

  const TYPES = ['ALL', 'INTAKE', 'OUTBOUND_SALE', 'TRANSFER_OUT', 'TRANSFER_IN', 'RECONCILIATION']

  return (
    <div className="space-y-4">
      <div className="bg-white rounded-xl border border-gray-200">
        <div className="flex flex-wrap items-center gap-2 px-5 py-4 border-b border-gray-100">
          <h3 className="font-semibold text-gray-900 mr-2">Movement History</h3>
          {TYPES.map(t => (
            <button key={t} onClick={() => setTypeFilter(t)} className={`px-2.5 py-1 rounded-lg text-xs font-medium transition-colors ${typeFilter === t ? 'bg-teal-600 text-white' : 'text-gray-500 hover:bg-gray-100'}`}>
              {t === 'ALL' ? 'All' : t.replace('_', ' ')}
            </button>
          ))}
          <div className="ml-auto">
            <SearchBar value={search} onChange={setSearch} placeholder="Search product or warehouse…" />
          </div>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-100">
                {['Product', 'Warehouse', 'Type', 'Quantity', 'Recorded By', 'Date'].map(h => (
                  <th key={h} className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wide">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {rows.map(m => (
                <tr key={m.id} className="hover:bg-gray-50/60">
                  <td className="px-4 py-3 font-medium text-gray-900">{m.productName}</td>
                  <td className="px-4 py-3 text-gray-600">{m.warehouseName}</td>
                  <td className="px-4 py-3"><StatusBadge status={m.type} /></td>
                  <td className="px-4 py-3 font-medium text-gray-900">{m.quantity}</td>
                  <td className="px-4 py-3 text-gray-600">{m.createdByName}</td>
                  <td className="px-4 py-3 text-gray-500 text-xs">{fmtDT(m.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}

// ── Transfers Panel ───────────────────────────────────────────────────────────

function TransfersPanel() {
  const { transfers } = useSelector(s => s.transfers)
  const { products, warehouses } = useSelector(s => s.stock)
  const { user } = useSelector(s => s.auth)
  const dispatch = useDispatch()
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [search, setSearch] = useState('')

  const rows = transfers
    .map(t => ({
      ...t,
      productName: products.find(p => p.id === t.productId)?.name || t.productId,
      fromName: warehouses.find(w => w.id === t.fromWarehouseId)?.name || t.fromWarehouseId,
      toName: warehouses.find(w => w.id === t.toWarehouseId)?.name || t.toWarehouseId,
    }))
    .filter(t => statusFilter === 'ALL' || t.status === statusFilter)
    .filter(t => !search || t.productName.toLowerCase().includes(search.toLowerCase()) || t.fromName.toLowerCase().includes(search.toLowerCase()) || t.toName.toLowerCase().includes(search.toLowerCase()))

  const pending = transfers.filter(t => t.status === 'SUGGESTED').length

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-3 gap-4">
        <StatCard title="Pending Approval" value={pending} color={pending > 0 ? 'amber' : 'green'} />
        <StatCard title="In Transit" value={transfers.filter(t => t.status === 'IN_TRANSIT').length} color="purple" />
        <StatCard title="Completed" value={transfers.filter(t => t.status === 'COMPLETED').length} color="green" />
      </div>

      <div className="bg-white rounded-xl border border-gray-200">
        <div className="flex flex-wrap items-center gap-2 px-5 py-4 border-b border-gray-100">
          <h3 className="font-semibold text-gray-900 mr-2">Transfer Suggestions</h3>
          {['ALL', 'SUGGESTED', 'APPROVED', 'IN_TRANSIT', 'COMPLETED', 'REJECTED'].map(s => (
            <button key={s} onClick={() => setStatusFilter(s)} className={`px-2.5 py-1 rounded-lg text-xs font-medium transition-colors ${statusFilter === s ? 'bg-teal-600 text-white' : 'text-gray-500 hover:bg-gray-100'}`}>
              {s === 'ALL' ? 'All' : s.replace('_', ' ')}
            </button>
          ))}
          <div className="ml-auto">
            <SearchBar value={search} onChange={setSearch} placeholder="Search product or warehouse…" />
          </div>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-100">
                {['Product', 'From', 'To', 'Qty', 'Distance', 'Source', 'Status', 'Actions'].map(h => (
                  <th key={h} className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wide">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {rows.map(t => (
                <tr key={t.id} className="hover:bg-gray-50/60">
                  <td className="px-4 py-3 font-medium text-gray-900">{t.productName}</td>
                  <td className="px-4 py-3 text-gray-600">{t.fromName}</td>
                  <td className="px-4 py-3 text-gray-600">{t.toName}</td>
                  <td className="px-4 py-3 font-medium text-gray-900">{t.quantity}</td>
                  <td className="px-4 py-3 text-gray-500">{t.distanceKm ? `${t.distanceKm} km` : '—'}</td>
                  <td className="px-4 py-3"><StatusBadge status={t.distanceSource} size="xs" /></td>
                  <td className="px-4 py-3"><StatusBadge status={t.status} /></td>
                  <td className="px-4 py-3">
                    {t.status === 'SUGGESTED' && (
                      <div className="flex gap-2">
                        <button
                          onClick={() => { dispatch(approveTransfer({ id: t.id, userId: user.id })); toast.success('Transfer approved') }}
                          className="px-2 py-1 bg-teal-600 text-white text-xs rounded-lg hover:bg-teal-700 font-medium"
                        >Approve</button>
                        <button
                          onClick={() => { dispatch(rejectTransfer(t.id)); toast.info('Transfer rejected') }}
                          className="px-2 py-1 bg-red-50 text-red-600 text-xs rounded-lg hover:bg-red-100 font-medium"
                        >Reject</button>
                      </div>
                    )}
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

// ── Reconciliations Panel ─────────────────────────────────────────────────────

function ReconciliationsPanel() {
  const { reconciliations } = useSelector(s => s.reconciliations)
  const { products, warehouses } = useSelector(s => s.stock)
  const { users } = useSelector(s => s.users)
  const { user } = useSelector(s => s.auth)
  const dispatch = useDispatch()
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [search, setSearch] = useState('')

  const rows = reconciliations
    .map(r => ({
      ...r,
      productName: products.find(p => p.id === r.productId)?.name || r.productId,
      warehouseName: warehouses.find(w => w.id === r.warehouseId)?.name || r.warehouseId,
      createdByName: users.find(u => u.id === r.createdBy)?.name || r.createdBy,
    }))
    .filter(r => statusFilter === 'ALL' || r.status === statusFilter)
    .filter(r => !search || r.productName.toLowerCase().includes(search.toLowerCase()) || r.warehouseName.toLowerCase().includes(search.toLowerCase()) || r.reason?.toLowerCase().includes(search.toLowerCase()))

  const pending = reconciliations.filter(r => r.status === 'PENDING_APPROVAL').length

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-3 gap-4">
        <StatCard title="Pending Approval" value={pending} color={pending > 0 ? 'amber' : 'green'} />
        <StatCard title="Approved" value={reconciliations.filter(r => r.status === 'APPROVED').length} color="green" />
        <StatCard title="Rejected" value={reconciliations.filter(r => r.status === 'REJECTED').length} color="red" />
      </div>

      <div className="bg-white rounded-xl border border-gray-200">
        <div className="flex flex-wrap items-center gap-2 px-5 py-4 border-b border-gray-100">
          <h3 className="font-semibold text-gray-900 mr-2">Reconciliation Requests</h3>
          {['ALL', 'PENDING_APPROVAL', 'APPROVED', 'REJECTED'].map(s => (
            <button key={s} onClick={() => setStatusFilter(s)} className={`px-2.5 py-1 rounded-lg text-xs font-medium transition-colors ${statusFilter === s ? 'bg-teal-600 text-white' : 'text-gray-500 hover:bg-gray-100'}`}>
              {s === 'ALL' ? 'All' : s.replace(/_/g, ' ')}
            </button>
          ))}
          <div className="ml-auto">
            <SearchBar value={search} onChange={setSearch} placeholder="Search product, warehouse…" />
          </div>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-100">
                {['Product', 'Warehouse', 'System', 'Physical', 'Discrepancy', 'Reason', 'By', 'Status', 'Actions'].map(h => (
                  <th key={h} className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wide">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {rows.map(r => {
                const selfApproval = r.createdBy === user.id
                return (
                  <tr key={r.id} className="hover:bg-gray-50/60">
                    <td className="px-4 py-3 font-medium text-gray-900">{r.productName}</td>
                    <td className="px-4 py-3 text-gray-600">{r.warehouseName}</td>
                    <td className="px-4 py-3 text-gray-900">{r.systemCount}</td>
                    <td className="px-4 py-3 text-gray-900">{r.physicalCount}</td>
                    <td className="px-4 py-3">
                      <span className={`font-semibold ${r.discrepancy < 0 ? 'text-red-600' : r.discrepancy > 0 ? 'text-green-600' : 'text-gray-500'}`}>
                        {r.discrepancy > 0 ? '+' : ''}{r.discrepancy}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-gray-500 max-w-xs truncate" title={r.reason}>{r.reason}</td>
                    <td className="px-4 py-3 text-gray-600">{r.createdByName}</td>
                    <td className="px-4 py-3"><StatusBadge status={r.status} /></td>
                    <td className="px-4 py-3">
                      {r.status === 'PENDING_APPROVAL' && (
                        <div className="flex gap-2">
                          {selfApproval ? (
                            <span className="text-xs text-red-400 italic">Self-approval blocked</span>
                          ) : (
                            <>
                              <button
                                onClick={() => { dispatch(approveReconciliation({ id: r.id, userId: user.id })); toast.success('Reconciliation approved') }}
                                className="px-2 py-1 bg-teal-600 text-white text-xs rounded-lg hover:bg-teal-700 font-medium"
                              >Approve</button>
                              <button
                                onClick={() => { dispatch(rejectReconciliation(r.id)); toast.info('Reconciliation rejected') }}
                                className="px-2 py-1 bg-red-50 text-red-600 text-xs rounded-lg hover:bg-red-100 font-medium"
                              >Reject</button>
                            </>
                          )}
                        </div>
                      )}
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}

// ── Analytics Panel ───────────────────────────────────────────────────────────

function AnalyticsPanel() {
  const { stockVelocity, lowStockForecast, reorderRecommendations, transferEfficiency, alertFrequency, movementSummary } = useSelector(s => s.analytics)
  const [section, setSection] = useState('velocity')

  const maxVelocity = Math.max(...stockVelocity.map(s => s.velocityPerDay))
  const maxAlertCount = Math.max(...alertFrequency.map(a => a.alertCount))
  const maxMovement = Math.max(...movementSummary.map(d => Math.max(d.totalIntake, d.totalOutboundSales)))

  const SECTIONS = [
    { id: 'velocity', label: 'Stock Velocity' },
    { id: 'forecast', label: 'Low-Stock Forecast' },
    { id: 'reorder', label: 'Reorder Recommendations' },
    { id: 'transfers', label: 'Transfer Efficiency' },
    { id: 'alerts', label: 'Alert Frequency' },
    { id: 'movements', label: 'Movement Summary' },
  ]

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap gap-1 bg-white rounded-xl border border-gray-200 p-2">
        {SECTIONS.map(s => (
          <button key={s.id} onClick={() => setSection(s.id)} className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${section === s.id ? 'bg-teal-600 text-white' : 'text-gray-500 hover:bg-gray-100'}`}>
            {s.label}
          </button>
        ))}
      </div>

      {section === 'velocity' && (
        <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
          <div className="px-5 py-4 border-b border-gray-100">
            <h3 className="font-semibold text-gray-900">Stock Velocity Rankings</h3>
            <p className="text-xs text-gray-500 mt-0.5">Products ranked by fastest depletion (units/day)</p>
          </div>
          <div className="p-5 space-y-3">
            {stockVelocity.map((s, i) => (
              <div key={i} className="flex items-center gap-3">
                <span className="w-4 text-xs text-gray-400 font-medium">{i + 1}</span>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between mb-1">
                    <span className="text-sm font-medium text-gray-900 truncate">{s.productName} <span className="text-gray-400 font-normal">· {s.warehouseName}</span></span>
                    <span className="text-sm font-semibold text-gray-700 ml-2 flex-shrink-0">{s.velocityPerDay}/day</span>
                  </div>
                  <div className="w-full bg-gray-100 rounded-full h-2">
                    <div className="bg-teal-600 h-2 rounded-full transition-all" style={{ width: `${(s.velocityPerDay / maxVelocity) * 100}%` }} />
                  </div>
                </div>
                <span className={`text-xs font-medium w-16 text-right ${s.daysUntilEmpty <= 7 ? 'text-red-600' : s.daysUntilEmpty <= 14 ? 'text-amber-600' : 'text-green-600'}`}>
                  {s.daysUntilEmpty}d left
                </span>
              </div>
            ))}
          </div>
        </div>
      )}

      {section === 'forecast' && (
        <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
          <div className="px-5 py-4 border-b border-gray-100">
            <h3 className="font-semibold text-gray-900">Low-Stock Forecast</h3>
            <p className="text-xs text-gray-500 mt-0.5">Products ranked by urgency</p>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead><tr className="bg-gray-50 border-b border-gray-100">
                {['Product', 'Warehouse', 'Current Stock', 'Threshold', 'Days Until Empty', 'Urgency'].map(h => (
                  <th key={h} className="text-left px-5 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wide">{h}</th>
                ))}
              </tr></thead>
              <tbody className="divide-y divide-gray-50">
                {lowStockForecast.map((r, i) => (
                  <tr key={i} className="hover:bg-gray-50/60">
                    <td className="px-5 py-3 font-medium text-gray-900">{r.productName}</td>
                    <td className="px-5 py-3 text-gray-600">{r.warehouseName}</td>
                    <td className="px-5 py-3 font-semibold text-gray-900">{r.currentStock}</td>
                    <td className="px-5 py-3 text-gray-600">{r.threshold}</td>
                    <td className="px-5 py-3">
                      <span className={`font-semibold ${r.daysUntilEmpty <= 7 ? 'text-red-600' : 'text-amber-600'}`}>{r.daysUntilEmpty}d</span>
                    </td>
                    <td className="px-5 py-3"><StatusBadge status={r.urgency} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {section === 'reorder' && (
        <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
          <div className="px-5 py-4 border-b border-gray-100">
            <h3 className="font-semibold text-gray-900">Reorder Recommendations</h3>
            <p className="text-xs text-gray-500 mt-0.5">Smart reorder quantities and dates based on 7-day velocity</p>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead><tr className="bg-gray-50 border-b border-gray-100">
                {['Product', 'Warehouse', 'Avg Velocity', 'Suggested Qty', 'Recommended Order Date'].map(h => (
                  <th key={h} className="text-left px-5 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wide">{h}</th>
                ))}
              </tr></thead>
              <tbody className="divide-y divide-gray-50">
                {reorderRecommendations.map((r, i) => (
                  <tr key={i} className="hover:bg-gray-50/60">
                    <td className="px-5 py-3 font-medium text-gray-900">{r.productName}</td>
                    <td className="px-5 py-3 text-gray-600">{r.warehouseName}</td>
                    <td className="px-5 py-3 text-gray-600">{r.avgVelocity}/day</td>
                    <td className="px-5 py-3 font-semibold text-gray-900">{r.suggestedQuantity} units</td>
                    <td className="px-5 py-3">
                      <span className="bg-amber-50 text-amber-700 text-xs font-medium px-2 py-0.5 rounded-full">{r.recommendedOrderDate}</span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {section === 'transfers' && (
        <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
          <div className="px-5 py-4 border-b border-gray-100">
            <h3 className="font-semibold text-gray-900">Transfer Efficiency</h3>
          </div>
          <div className="p-5 space-y-4">
            {transferEfficiency.map((t, i) => (
              <div key={i} className="border border-gray-100 rounded-lg p-4">
                <div className="flex items-center justify-between mb-3">
                  <span className="text-sm font-medium text-gray-900">{t.fromWarehouseName} → {t.toWarehouseName}</span>
                  <span className={`text-sm font-bold ${t.acceptanceRate >= 60 ? 'text-green-600' : 'text-amber-600'}`}>{t.acceptanceRate.toFixed(1)}%</span>
                </div>
                <div className="w-full bg-gray-100 rounded-full h-2 mb-2">
                  <div className={`h-2 rounded-full ${t.acceptanceRate >= 60 ? 'bg-green-500' : 'bg-amber-500'}`} style={{ width: `${t.acceptanceRate}%` }} />
                </div>
                <div className="flex gap-4 text-xs text-gray-500">
                  <span>Suggested: <strong className="text-gray-700">{t.totalSuggested}</strong></span>
                  <span>Accepted: <strong className="text-green-700">{t.totalAccepted}</strong></span>
                  <span>Rejected: <strong className="text-red-700">{t.totalRejected}</strong></span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {section === 'alerts' && (
        <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
          <div className="px-5 py-4 border-b border-gray-100">
            <h3 className="font-semibold text-gray-900">Alert Frequency</h3>
            <p className="text-xs text-gray-500 mt-0.5">Products that trigger restocking alerts most often</p>
          </div>
          <div className="p-5 space-y-3">
            {alertFrequency.map((a, i) => (
              <div key={i} className="flex items-center gap-3">
                <span className="w-4 text-xs text-gray-400">{i + 1}</span>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between mb-1">
                    <span className="text-sm font-medium text-gray-900">{a.productName} <span className="text-gray-400 font-normal">· {a.warehouseName}</span></span>
                    <span className="text-sm font-semibold text-amber-700 ml-2">{a.alertCount} alerts</span>
                  </div>
                  <div className="w-full bg-gray-100 rounded-full h-2">
                    <div className="bg-amber-500 h-2 rounded-full" style={{ width: `${(a.alertCount / maxAlertCount) * 100}%` }} />
                  </div>
                  <p className="text-xs text-gray-400 mt-0.5">Avg {a.avgDaysBetweenAlerts} days between alerts</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {section === 'movements' && (
        <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
          <div className="px-5 py-4 border-b border-gray-100">
            <h3 className="font-semibold text-gray-900">Movement Summary (Last 7 Days)</h3>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead><tr className="bg-gray-50 border-b border-gray-100">
                {['Date', 'Intake', 'Outbound Sales', 'Transfers In', 'Transfers Out', 'Visual'].map(h => (
                  <th key={h} className="text-left px-5 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wide">{h}</th>
                ))}
              </tr></thead>
              <tbody className="divide-y divide-gray-50">
                {movementSummary.map((d, i) => (
                  <tr key={i} className="hover:bg-gray-50/60">
                    <td className="px-5 py-3 font-medium text-gray-900">{fmtDate(d.summaryDate)}</td>
                    <td className="px-5 py-3 text-green-700 font-medium">{d.totalIntake || '—'}</td>
                    <td className="px-5 py-3 text-orange-700 font-medium">{d.totalOutboundSales || '—'}</td>
                    <td className="px-5 py-3 text-blue-700">{d.transfersIn || '—'}</td>
                    <td className="px-5 py-3 text-purple-700">{d.transfersOut || '—'}</td>
                    <td className="px-5 py-3">
                      <div className="flex gap-1 items-end h-6">
                        {d.totalIntake > 0 && <div className="bg-teal-400 rounded-sm w-3" style={{ height: `${(d.totalIntake / maxMovement) * 24}px` }} title={`Intake: ${d.totalIntake}`} />}
                        {d.totalOutboundSales > 0 && <div className="bg-orange-400 rounded-sm w-3" style={{ height: `${(d.totalOutboundSales / maxMovement) * 24}px` }} title={`Sales: ${d.totalOutboundSales}`} />}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  )
}

// ── Team Panel ────────────────────────────────────────────────────────────────

function TeamPanel() {
  const { users, warehouseAssignments } = useSelector(s => s.users)
  const { user: me, companyId } = useSelector(s => s.auth)
  const dispatch = useDispatch()
  const [pendingRole, setPendingRole] = useState({})
  const [search, setSearch] = useState('')

  const myWarehouseIds = warehouseAssignments
    .filter(a => a.userId === me.id)
    .map(a => a.warehouseId)

  const myWarehouseUserIds = warehouseAssignments
    .filter(a => myWarehouseIds.includes(a.warehouseId))
    .map(a => a.userId)

  const teamUsers = users.filter(u =>
    u.companyId === companyId &&
    myWarehouseUserIds.includes(u.id) &&
    u.role !== 'ADMIN' &&
    (u.name.toLowerCase().includes(search.toLowerCase()) || u.email.toLowerCase().includes(search.toLowerCase()))
  )

  const saveRole = (userId, role) => {
    dispatch(updateUserRole({ id: userId, role }))
    toast.success('Role updated')
    setPendingRole(r => { const n = { ...r }; delete n[userId]; return n })
  }

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-2 sm:grid-cols-3 gap-4">
        <StatCard title="Your Warehouses" value={myWarehouseIds.length} color="blue" />
        <StatCard title="Team Members" value={teamUsers.length} color="teal" />
        <StatCard title="Active" value={teamUsers.filter(u => u.isActive).length} color="green" />
      </div>
      <div className="bg-white rounded-xl border border-gray-200">
        <div className="flex flex-wrap items-center justify-between gap-3 px-5 py-4 border-b border-gray-100">
          <div>
            <h3 className="font-semibold text-gray-900">Team Members</h3>
            <p className="text-xs text-gray-500 mt-0.5">Staff assigned to your warehouses. You can update their roles.</p>
          </div>
          <SearchBar value={search} onChange={setSearch} placeholder="Search members…" />
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-100">
                {['Member', 'Email', 'Status', 'Role'].map(h => (
                  <th key={h} className="text-left px-5 py-3 font-semibold text-gray-600 text-xs uppercase tracking-wide">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {teamUsers.length === 0 ? (
                <tr><td colSpan={4} className="px-5 py-8 text-center text-gray-400 text-sm">
                  {myWarehouseIds.length === 0 ? 'You are not assigned to any warehouses.' : 'No team members found.'}
                </td></tr>
              ) : teamUsers.map(u => {
                const isAdmin = u.role === 'ADMIN'
                const pending = pendingRole[u.id]
                return (
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
                    <td className="px-5 py-3"><StatusBadge status={u.isActive ? 'ACTIVE' : 'SUSPENDED'} /></td>
                    <td className="px-5 py-3">
                      {isAdmin ? (
                        <StatusBadge status="ADMIN" />
                      ) : (
                        <div className="flex items-center gap-2">
                          <select
                            value={pending ?? u.role}
                            onChange={e => setPendingRole(r => ({ ...r, [u.id]: e.target.value }))}
                            className="px-2.5 py-1.5 border border-gray-300 rounded-lg text-xs focus:outline-none focus:ring-2 focus:ring-teal-600"
                          >
                            <option value="MANAGER">Manager</option>
                            <option value="WAREHOUSE_STAFF">Warehouse Staff</option>
                            <option value="PROCUREMENT_OFFICER">Procurement Officer</option>
                          </select>
                          {pending && pending !== u.role && (
                            <button
                              onClick={() => saveRole(u.id, pending)}
                              className="px-2.5 py-1.5 bg-teal-600 text-white text-xs font-medium rounded-lg hover:bg-teal-700"
                            >
                              Save
                            </button>
                          )}
                        </div>
                      )}
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}

// ── Main ──────────────────────────────────────────────────────────────────────

export default function ManagerDashboard() {
  const [activeTab, setActiveTab] = useState('stock')
  const { transfers } = useSelector(s => s.transfers)
  const { reconciliations } = useSelector(s => s.reconciliations)
  const { stockLevels } = useSelector(s => s.stock)

  const navItems = [
    {
      id: 'stock', label: 'Stock Overview',
      badge: stockLevels.filter(sl => sl.currentStock < sl.threshold).length,
      icon: <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 10h18M3 14h18M10 6v12M14 6v12M5 6h14a1 1 0 011 1v10a1 1 0 01-1 1H5a1 1 0 01-1-1V7a1 1 0 011-1z" /></svg>,
    },
    {
      id: 'movements', label: 'Movements', badge: 0,
      icon: <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 16V4m0 0L3 8m4-4l4 4M17 8v12m0 0l4-4m-4 4l-4-4" /></svg>,
    },
    {
      id: 'transfers', label: 'Transfers',
      badge: transfers.filter(t => t.status === 'SUGGESTED').length,
      icon: <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4" /></svg>,
    },
    {
      id: 'reconciliations', label: 'Reconciliations',
      badge: reconciliations.filter(r => r.status === 'PENDING_APPROVAL').length,
      icon: <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" /></svg>,
    },
    {
      id: 'analytics', label: 'Analytics', badge: 0,
      icon: <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" /></svg>,
    },
    {
      id: 'team', label: 'Team', badge: 0,
      icon: <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" /></svg>,
    },
  ]

  return (
    <Layout title="Manager Dashboard" navItems={navItems} activeTab={activeTab} onTabChange={setActiveTab}>
      {activeTab === 'stock' && <StockPanel />}
      {activeTab === 'movements' && <MovementsPanel />}
      {activeTab === 'transfers' && <TransfersPanel />}
      {activeTab === 'reconciliations' && <ReconciliationsPanel />}
      {activeTab === 'analytics' && <AnalyticsPanel />}
      {activeTab === 'team' && <TeamPanel />}
    </Layout>
  )
}
