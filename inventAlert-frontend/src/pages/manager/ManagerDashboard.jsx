import { useState } from 'react'
import { useSelector, useDispatch } from 'react-redux'
import { toast } from 'react-toastify'
import Layout from '../../components/layout/Layout'
import StatusBadge from '../../components/shared/StatusBadge'
import StatCard from '../../components/shared/StatCard'
import { approveTransfer, rejectTransfer } from '../../store/slices/transfersSlice'
import { submitComplaint } from '../../store/slices/superadminSlice'
import { approveReconciliation, rejectReconciliation } from '../../store/slices/reconciliationsSlice'
import { addUser, updateUserRole, assignWarehouse, deactivateUser, reactivateUser } from '../../store/slices/usersSlice'
import { registerLocalUser } from '../../store/slices/authSlice'
import ConfirmDialog from '../../components/shared/ConfirmDialog'

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
  const { stockLevels, products } = useSelector(s => s.stock)
  const { user: me } = useSelector(s => s.auth)
  const { warehouseAssignments } = useSelector(s => s.users)
  const [filter, setFilter] = useState('ALL')
  const [search, setSearch] = useState('')

  const myWarehouseIds = warehouseAssignments.filter(a => a.userId === me.id).map(a => a.warehouseId)

  const rows = stockLevels
    .filter(sl => myWarehouseIds.includes(sl.warehouseId))
    .map(sl => ({
      ...sl,
      productName: products.find(p => p.id === sl.productId)?.name || sl.productId,
      sku: products.find(p => p.id === sl.productId)?.sku || '',
      status: sl.currentStock < sl.threshold ? 'CRITICAL'
        : sl.currentStock < sl.threshold * 1.25 ? 'WARNING' : 'OK',
    }))
    .filter(r => filter === 'ALL' || r.status === filter)
    .filter(r => !search || r.productName.toLowerCase().includes(search.toLowerCase()) || r.sku.toLowerCase().includes(search.toLowerCase()))
    .sort((a, b) => (a.daysUntilEmpty ?? 999) - (b.daysUntilEmpty ?? 999))

  const criticalCount = rows.filter(r => r.status === 'CRITICAL').length

  const stockColor = status => ({
    CRITICAL: 'text-red-700 bg-red-50',
    WARNING: 'text-amber-700 bg-amber-50',
    OK: 'text-green-700 bg-green-50',
  }[status] || '')

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-2 sm:grid-cols-3 gap-4">
        <StatCard title="Stock Items" value={rows.length} color="blue" />
        <StatCard title="Below Threshold" value={criticalCount} color="red" />
        <StatCard title="Products Tracked" value={[...new Set(rows.map(r => r.productId))].length} color="gray" />
      </div>

      <div className="bg-white rounded-xl border border-gray-200">
        <div className="flex flex-wrap items-center justify-between gap-3 px-5 py-4 border-b border-gray-100">
          <h3 className="font-semibold text-gray-900">Stock Overview</h3>
          <div className="flex items-center gap-2 flex-wrap">
            <SearchBar value={search} onChange={setSearch} placeholder="Search product or SKU…" />
            <div className="flex gap-1">
              {['ALL', 'CRITICAL', 'WARNING', 'OK'].map(f => (
                <button key={f} onClick={() => setFilter(f)} className={`px-3 py-1 rounded-lg text-xs font-medium transition-colors ${filter === f ? 'bg-teal-600 text-white' : 'text-gray-500 hover:bg-gray-100'}`}>{f}</button>
              ))}
            </div>
          </div>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-100">
                {['Product', 'SKU', 'Stock', 'Threshold', 'Status', 'Velocity/day', 'Days Until Empty'].map(h => (
                  <th key={h} className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wide">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {rows.length === 0 ? (
                <tr><td colSpan={7} className="px-4 py-8 text-center text-gray-400 text-sm">
                  {myWarehouseIds.length === 0 ? 'You are not assigned to a warehouse.' : 'No stock items match.'}
                </td></tr>
              ) : rows.map(r => (
                <tr key={r.id} className="hover:bg-gray-50/60">
                  <td className="px-4 py-3 font-medium text-gray-900">{r.productName}</td>
                  <td className="px-4 py-3 font-mono text-xs text-gray-500">{r.sku}</td>
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
        className="pl-9 pr-4 py-1.5 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-teal-600 w-72"
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
  const [confirm, setConfirm] = useState(null)

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
                {['Product', 'From', 'To', 'Qty', 'Status', 'Requested', 'Actions'].map(h => (
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
                  <td className="px-4 py-3"><StatusBadge status={t.status} /></td>
                  <td className="px-4 py-3 text-gray-400 text-xs whitespace-nowrap">{fmtDT(t.createdAt)}</td>
                  <td className="px-4 py-3">
                    {t.status === 'SUGGESTED' ? (
                      <div className="flex gap-2">
                        <button
                          onClick={() => setConfirm({ action: () => { dispatch(approveTransfer({ id: t.id, userId: user.id })); toast.success('Transfer approved') }, title: 'Approve Transfer', message: `Approve transfer of ${t.quantity} units of ${t.productName} from ${t.fromName} to ${t.toName}?`, label: 'Approve' })}
                          className="px-2 py-1 bg-teal-600 text-white text-xs rounded-lg hover:bg-teal-700 font-medium"
                        >Approve</button>
                        <button
                          onClick={() => setConfirm({ action: () => { dispatch(rejectTransfer(t.id)); toast.info('Transfer rejected') }, title: 'Reject Transfer', message: `Reject the transfer request for ${t.quantity} units of ${t.productName}?`, label: 'Reject', danger: true })}
                          className="px-2 py-1 bg-red-50 text-red-600 text-xs rounded-lg hover:bg-red-100 font-medium"
                        >Reject</button>
                      </div>
                    ) : (
                      <span className="text-gray-300 text-xs">—</span>
                    )}
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

// ── Reconciliations Panel ─────────────────────────────────────────────────────

function ReconciliationsPanel() {
  const { reconciliations } = useSelector(s => s.reconciliations)
  const { products, warehouses } = useSelector(s => s.stock)
  const { users } = useSelector(s => s.users)
  const { user } = useSelector(s => s.auth)
  const dispatch = useDispatch()
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [search, setSearch] = useState('')
  const [confirm, setConfirm] = useState(null)

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
                {['Product', 'Warehouse', 'System', 'Physical', 'Discrepancy', 'Reason', 'Submitted', 'By', 'Status', 'Actions'].map(h => (
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
                    <td className="px-4 py-3 text-gray-500 text-xs leading-relaxed max-w-xs">{r.reason}</td>
                    <td className="px-4 py-3 text-gray-400 text-xs whitespace-nowrap">{r.createdAt ? fmtDT(r.createdAt) : '—'}</td>
                    <td className="px-4 py-3 text-gray-600">{r.createdByName}</td>
                    <td className="px-4 py-3"><StatusBadge status={r.status} /></td>
                    <td className="px-4 py-3">
                      {r.status === 'PENDING_APPROVAL' ? (
                        <div className="flex gap-2">
                          {selfApproval ? (
                            <span className="text-xs text-red-400 italic">Self-approval blocked</span>
                          ) : (
                            <>
                              <button
                                onClick={() => setConfirm({ action: () => { dispatch(approveReconciliation({ id: r.id, userId: user.id })); toast.success('Reconciliation approved') }, title: 'Approve Reconciliation', message: `Approve the stock count discrepancy of ${r.discrepancy > 0 ? '+' : ''}${r.discrepancy} for ${r.productName} at ${r.warehouseName}?`, label: 'Approve' })}
                                className="px-2 py-1 bg-teal-600 text-white text-xs rounded-lg hover:bg-teal-700 font-medium"
                              >Approve</button>
                              <button
                                onClick={() => setConfirm({ action: () => { dispatch(rejectReconciliation(r.id)); toast.info('Reconciliation rejected') }, title: 'Reject Reconciliation', message: `Reject this reconciliation request for ${r.productName}?`, label: 'Reject', danger: true })}
                                className="px-2 py-1 bg-red-50 text-red-600 text-xs rounded-lg hover:bg-red-100 font-medium"
                              >Reject</button>
                            </>
                          )}
                        </div>
                      ) : (
                        <span className="text-gray-300 text-xs">—</span>
                      )}
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      </div>
      {confirm && <ConfirmDialog title={confirm.title} message={confirm.message} danger={confirm.danger} confirmLabel={confirm.label} onConfirm={() => { confirm.action(); setConfirm(null) }} onCancel={() => setConfirm(null)} />}
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
                    <span className="text-sm font-semibold text-gray-700 ml-2 shrink-0">{s.velocityPerDay}/day</span>
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
  const { warehouses } = useSelector(s => s.stock)
  const dispatch = useDispatch()
  const [pendingRole, setPendingRole] = useState({})
  const [search, setSearch] = useState('')
  const [confirm, setConfirm] = useState(null)
  const [showAddUser, setShowAddUser] = useState(false)
  const [showTempPass, setShowTempPass] = useState(false)
  const [addForm, setAddForm] = useState({ name: '', email: '', role: 'WAREHOUSE_STAFF', password: '' })
  const chAdd = e => setAddForm(f => ({ ...f, [e.target.name]: e.target.value }))

  const myWarehouseIds = warehouseAssignments
    .filter(a => a.userId === me.id)
    .map(a => a.warehouseId)

  const myWarehouseId = myWarehouseIds[0] || null
  const myWarehouseName = warehouses.find(w => w.id === myWarehouseId)?.name || null

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

  const handleAddUser = e => {
    e.preventDefault()
    if (!myWarehouseId) { toast.error('You are not assigned to a warehouse'); return }
    const newId = `user-${Date.now()}`
    dispatch(addUser({ ...addForm, companyId, id: newId }))
    dispatch(registerLocalUser({ id: newId, name: addForm.name, email: addForm.email, password: addForm.password, role: addForm.role, companyId, companyName: null, warehouseId: myWarehouseId }))
    dispatch(assignWarehouse({ userId: newId, warehouseId: myWarehouseId, companyId }))
    toast.success(`${addForm.name} added to ${myWarehouseName}`)
    setShowAddUser(false)
    setShowTempPass(false)
    setAddForm({ name: '', email: '', role: 'WAREHOUSE_STAFF', password: '' })
  }

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-2 gap-4">
        <StatCard title="Team Members" value={teamUsers.length} color="teal" />
        <StatCard title="Active" value={teamUsers.filter(u => u.isActive).length} color="green" />
      </div>
      <div className="bg-white rounded-xl border border-gray-200">
        <div className="flex flex-wrap items-center justify-between gap-3 px-5 py-4 border-b border-gray-100">
          <div>
            <h3 className="font-semibold text-gray-900">Team Members</h3>
            <p className="text-xs text-gray-500 mt-0.5">Staff assigned to your warehouse. You can update their roles.</p>
          </div>
          <div className="flex items-center gap-2">
            <SearchBar value={search} onChange={setSearch} placeholder="Search members…" />
            {myWarehouseId && (
              <button
                onClick={() => setShowAddUser(true)}
                className="flex items-center gap-1.5 px-3 py-1.5 bg-teal-600 text-white text-sm font-medium rounded-lg hover:bg-teal-700"
              >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" /></svg>
                Add Member
              </button>
            )}
          </div>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-100">
                {['Member', 'Email', 'Status', 'Role', 'Actions'].map(h => (
                  <th key={h} className="text-left px-5 py-3 font-semibold text-gray-600 text-xs uppercase tracking-wide">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {teamUsers.length === 0 ? (
                <tr><td colSpan={5} className="px-5 py-8 text-center text-gray-400 text-sm">
                  {myWarehouseIds.length === 0 ? 'You are not assigned to any warehouses.' : 'No team members found.'}
                </td></tr>
              ) : teamUsers.map(u => {
                const isAdmin = u.role === 'ADMIN'
                const isMe = u.id === me.id
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
                      {isAdmin || isMe ? (
                        <div className="flex items-center gap-2">
                          <StatusBadge status={u.role} />
                          {isMe && <span className="text-xs text-gray-400">(you)</span>}
                        </div>
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
                    <td className="px-5 py-3">
                      {!isAdmin && !isMe && (
                        <button
                          onClick={() => setConfirm({
                            action: () => { u.isActive ? dispatch(deactivateUser(u.id)) : dispatch(reactivateUser(u.id)); toast.success(u.isActive ? `${u.name} deactivated` : `${u.name} reactivated`) },
                            title: u.isActive ? 'Deactivate Member' : 'Reactivate Member',
                            message: u.isActive ? `Deactivate ${u.name}? They will lose access to the system.` : `Reactivate ${u.name}? They will regain access to the system.`,
                            label: u.isActive ? 'Deactivate' : 'Reactivate',
                            danger: u.isActive,
                          })}
                          className={`px-3 py-1.5 text-xs font-medium rounded-lg transition-colors ${u.isActive ? 'bg-red-50 text-red-600 hover:bg-red-100' : 'bg-teal-50 text-teal-600 hover:bg-teal-100'}`}
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

      {showAddUser && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4" onClick={() => setShowAddUser(false)}>
          <div className="bg-white rounded-2xl shadow-xl p-6 w-full max-w-md" onClick={e => e.stopPropagation()}>
            <div className="flex items-center justify-between mb-5">
              <div>
                <h3 className="text-base font-semibold text-gray-900">Add Team Member</h3>
                {myWarehouseName && <p className="text-xs text-gray-400 mt-0.5">Will be assigned to {myWarehouseName}</p>}
              </div>
              <button onClick={() => setShowAddUser(false)} className="p-1 rounded text-gray-400 hover:text-gray-600 hover:bg-gray-100">
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /></svg>
              </button>
            </div>
            <form onSubmit={handleAddUser} className="space-y-3">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Full Name</label>
                <input name="name" value={addForm.name} onChange={chAdd} placeholder="Jane Doe" required className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-teal-600" />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
                <input name="email" type="email" value={addForm.email} onChange={chAdd} placeholder="jane@company.com" required className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-teal-600" />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Role</label>
                <select name="role" value={addForm.role} onChange={chAdd} className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-teal-600">
                  <option value="WAREHOUSE_STAFF">Warehouse Staff</option>
                  <option value="PROCUREMENT_OFFICER">Procurement Officer</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Temporary Password</label>
                <div className="relative">
                  <input
                    name="password" type={showTempPass ? 'text' : 'password'}
                    value={addForm.password} onChange={chAdd}
                    placeholder="Min 8 characters" required
                    className="w-full px-3 py-2 pr-10 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-teal-600"
                  />
                  <button type="button" onClick={() => setShowTempPass(s => !s)} tabIndex={-1} className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600">
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d={showTempPass ? "M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" : "M15 12a3 3 0 11-6 0 3 3 0 016 0zM2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"} /></svg>
                  </button>
                </div>
                <p className="text-xs text-gray-400 mt-1">The user will be prompted to change this on first login.</p>
              </div>
              <div className="flex gap-2 pt-2">
                <button type="button" onClick={() => setShowAddUser(false)} className="flex-1 py-2 border border-gray-300 text-gray-700 rounded-lg text-sm hover:bg-gray-50">Cancel</button>
                <button type="submit" className="flex-1 py-2 bg-teal-600 text-white rounded-lg text-sm font-medium hover:bg-teal-700">Add Member</button>
              </div>
            </form>
          </div>
        </div>
      )}
      {confirm && <ConfirmDialog title={confirm.title} message={confirm.message} danger={confirm.danger} confirmLabel={confirm.label} onConfirm={() => { confirm.action(); setConfirm(null) }} onCancel={() => setConfirm(null)} />}
    </div>
  )
}

// ── Complaints Panel ──────────────────────────────────────────────────────────

function ComplaintsPanel() {
  const { user: me, companyId, companyName } = useSelector(s => s.auth)
  const dispatch = useDispatch()
  const [form, setForm] = useState({ subject: '', priority: 'MEDIUM', message: '' })
  const [submitted, setSubmitted] = useState(false)
  const chForm = e => setForm(f => ({ ...f, [e.target.name]: e.target.value }))

  const handleSubmit = e => {
    e.preventDefault()
    dispatch(submitComplaint({ subject: form.subject, priority: form.priority, message: form.message, submittedBy: me.name, email: me.email, companyName, companyId }))
    setSubmitted(true)
    setForm({ subject: '', priority: 'MEDIUM', message: '' })
  }

  return (
    <div className="space-y-4">
      <SectionHeader title="Feedback & Support" subtitle="Submit a complaint, inquiry, or suggestion to the SuperAdmin team." />
      <div className="bg-white rounded-xl border border-gray-200 p-6">
        {submitted && (
          <div className="mb-4 px-4 py-3 bg-teal-50 border border-teal-200 rounded-lg text-teal-800 text-sm">
            Your feedback has been submitted successfully.
          </div>
        )}
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Subject</label>
            <input
              name="subject" type="text" value={form.subject} onChange={chForm}
              placeholder="Brief subject of your feedback"
              required
              className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-teal-600"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Priority</label>
            <select
              name="priority" value={form.priority} onChange={chForm}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-teal-600"
            >
              <option value="HIGH">High</option>
              <option value="MEDIUM">Medium</option>
              <option value="LOW">Low</option>
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Message</label>
            <textarea
              name="message" value={form.message} onChange={chForm}
              placeholder="Describe your feedback in detail…"
              rows={4}
              required
              className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-teal-600 resize-none"
            />
          </div>
          <div className="flex justify-end">
            <button type="submit" className="px-5 py-2 bg-teal-600 text-white text-sm font-medium rounded-lg hover:bg-teal-700 transition-colors">
              Submit Feedback
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

// ── Main ──────────────────────────────────────────────────────────────────────

export default function ManagerDashboard() {
  const [activeTab, setActiveTab] = useState('stock')
  const { transfers } = useSelector(s => s.transfers)
  const { reconciliations } = useSelector(s => s.reconciliations)
  const { stockLevels, warehouses } = useSelector(s => s.stock)
  const { user: me } = useSelector(s => s.auth)
  const { warehouseAssignments } = useSelector(s => s.users)

  const myWarehouseId = warehouseAssignments.find(a => a.userId === me?.id)?.warehouseId || null
  const myWarehouse = warehouses.find(w => w.id === myWarehouseId) || null

  const myWarehouseStockLevels = myWarehouseId
    ? stockLevels.filter(sl => sl.warehouseId === myWarehouseId)
    : []

  const navItems = [
    {
      id: 'stock', label: 'Stock Overview',
      badge: myWarehouseStockLevels.filter(sl => sl.currentStock < sl.threshold).length,
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
    {
      id: 'complaints', label: 'Feedback & Support', badge: 0,
      icon: <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" /></svg>,
    },
  ]

  return (
    <Layout title="Manager Dashboard" navItems={navItems} activeTab={activeTab} onTabChange={setActiveTab}>
      {myWarehouse && (
        <div className="flex items-center gap-2 mb-4 px-4 py-2.5 bg-teal-50 border border-teal-200 rounded-xl">
          <svg className="w-4 h-4 text-teal-600 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
          </svg>
          <span className="text-sm text-teal-800 font-medium">{myWarehouse.name}</span>
          <span className="text-xs text-teal-600">{myWarehouse.address}</span>
          {!myWarehouse.isActive && <span className="ml-auto text-xs bg-red-100 text-red-600 px-2 py-0.5 rounded-full font-medium">Inactive</span>}
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
      {activeTab === 'movements' && <MovementsPanel />}
      {activeTab === 'transfers' && <TransfersPanel />}
      {activeTab === 'reconciliations' && <ReconciliationsPanel />}
      {activeTab === 'analytics' && <AnalyticsPanel />}
      {activeTab === 'team' && <TeamPanel />}
      {activeTab === 'complaints' && <ComplaintsPanel />}
    </Layout>
  )
}
