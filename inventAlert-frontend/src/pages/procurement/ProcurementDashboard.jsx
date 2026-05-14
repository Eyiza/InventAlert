import { useState } from 'react'
import { useSelector, useDispatch } from 'react-redux'
import { toast } from 'react-toastify'
import Layout from '../../components/layout/Layout'
import ConfirmDialog from '../../components/shared/ConfirmDialog'
import StatusBadge from '../../components/shared/StatusBadge'
import StatCard from '../../components/shared/StatCard'
import { submitComplaint } from '../../store/slices/superadminSlice'
import {
  useGetAlertsQuery,
  useAcknowledgeAlertMutation,
  useMarkAlertOrderPlacedMutation,
  useResolveAlertMutation,
  useGetProductsQuery,
  useGetWarehousesQuery,
  useGetStockByWarehouseQuery,
  useGetAlertSummaryQuery,
  useGetAlertsByWarehouseQuery,
} from '../../apis/inventAlertApi'

const fmtDT = d => new Date(d).toLocaleString('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
const fmtDate = d => new Date(d).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })

function dateRange(days) {
  const toDate = new Date().toISOString().split('T')[0]
  const fromDate = new Date(Date.now() - days * 86400000).toISOString().split('T')[0]
  return {
    from: new Date(fromDate).toISOString(),
    to: new Date(toDate + 'T23:59:59.999Z').toISOString(),
  }
}

// ── Shared ────────────────────────────────────────────────────────────────────

function SearchBar({ value, onChange, placeholder }) {
  return (
    <div className="relative">
      <svg className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
      </svg>
      <input
        type="text" value={value} onChange={e => onChange(e.target.value)} placeholder={placeholder}
        className="pl-9 pr-4 py-1.5 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-teal-600 w-64"
      />
    </div>
  )
}

function Spinner() {
  return (
    <div className="flex items-center justify-center py-16">
      <svg className="w-8 h-8 text-teal-600 animate-spin" fill="none" viewBox="0 0 24 24">
        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z" />
      </svg>
    </div>
  )
}

// ── Alert Pipeline ────────────────────────────────────────────────────────────

const PIPELINE_STAGES = [
  { status: 'OPEN',         label: 'Open',         color: 'bg-amber-50 border-amber-200',   dot: 'bg-amber-500',  hdr: 'text-amber-700'  },
  { status: 'ACKNOWLEDGED', label: 'Acknowledged',  color: 'bg-blue-50 border-blue-200',     dot: 'bg-blue-500',   hdr: 'text-blue-700'   },
  { status: 'ORDER_PLACED', label: 'Order Placed',  color: 'bg-indigo-50 border-indigo-200', dot: 'bg-indigo-500', hdr: 'text-indigo-700' },
  { status: 'RESOLVED',     label: 'Resolved',      color: 'bg-green-50 border-green-200',   dot: 'bg-green-500',  hdr: 'text-green-700'  },
]

function AlertCard({ alert, productName, warehouseName, onAction, busy }) {
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
      {onAction && status !== 'RESOLVED' && (
        <button
          onClick={onAction}
          disabled={busy}
          className="w-full py-1.5 bg-teal-600 hover:bg-teal-700 disabled:opacity-50 text-white text-xs font-semibold rounded-lg transition-colors flex items-center justify-center gap-1.5"
        >
          {busy && <svg className="w-3 h-3 animate-spin" fill="none" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" /><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z" /></svg>}
          {status === 'OPEN' && 'Acknowledge'}
          {status === 'ACKNOWLEDGED' && 'Mark Order Placed'}
          {status === 'ORDER_PLACED' && 'Mark Resolved'}
        </button>
      )}
    </div>
  )
}

function AlertPipelinePanel() {
  const { data: alerts = [], isLoading } = useGetAlertsQuery(undefined, { pollingInterval: 15000 })
  const { data: products = [] } = useGetProductsQuery()
  const { data: warehouses = [] } = useGetWarehousesQuery()
  const [acknowledge] = useAcknowledgeAlertMutation()
  const [placeOrder] = useMarkAlertOrderPlacedMutation()
  const [resolve] = useResolveAlertMutation()
  const [busyId, setBusyId] = useState(null)

  const getAction = (alert) => {
    if (alert.status === 'RESOLVED') return null
    return async () => {
      setBusyId(alert.id)
      try {
        if (alert.status === 'OPEN') {
          await acknowledge(alert.id).unwrap()
          toast.success('Alert acknowledged')
        } else if (alert.status === 'ACKNOWLEDGED') {
          await placeOrder(alert.id).unwrap()
          toast.success('Order marked as placed')
        } else if (alert.status === 'ORDER_PLACED') {
          await resolve(alert.id).unwrap()
          toast.success('Alert resolved')
        }
      } catch (err) {
        toast.error(err?.data?.message || 'Action failed')
      } finally {
        setBusyId(null)
      }
    }
  }

  const openCount = alerts.filter(a => a.status === 'OPEN').length
  const resolvedAlerts = alerts
    .filter(a => a.status === 'RESOLVED')
    .sort((a, b) => new Date(b.updatedAt || b.createdAt) - new Date(a.updatedAt || a.createdAt))
  const recentResolved = resolvedAlerts.slice(0, 4)

  if (isLoading) return <Spinner />

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        <StatCard title="Open Alerts"   value={alerts.filter(a => a.status === 'OPEN').length}         color={openCount > 0 ? 'amber' : 'green'} />
        <StatCard title="Acknowledged"  value={alerts.filter(a => a.status === 'ACKNOWLEDGED').length}  color="blue"   />
        <StatCard title="Order Placed"  value={alerts.filter(a => a.status === 'ORDER_PLACED').length}  color="purple" />
        <StatCard title="Resolved"      value={resolvedAlerts.length}                                    color="green"  />
      </div>

      {alerts.length === 0 && (
        <div className="bg-green-50 border border-green-200 rounded-xl p-5 text-sm text-green-800">
          No restock alerts. All products are above their thresholds.
        </div>
      )}

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {PIPELINE_STAGES.map(stage => {
          const isResolved = stage.status === 'RESOLVED'
          const allStageAlerts = alerts.filter(a => a.status === stage.status)
          const stageAlerts = isResolved ? recentResolved : allStageAlerts
          return (
            <div key={stage.status} className={`rounded-xl border-2 p-3 ${stage.color}`}>
              <div className="flex items-center gap-2 mb-3">
                <div className={`w-2.5 h-2.5 rounded-full ${stage.dot}`} />
                <h4 className={`text-sm font-semibold ${stage.hdr}`}>{stage.label}</h4>
                <span className={`ml-auto text-xs font-bold px-2 py-0.5 rounded-full ${stage.dot} text-white`}>{isResolved ? stageAlerts.length : allStageAlerts.length}</span>
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
                    busy={busyId === alert.id}
                  />
                ))}
                {isResolved && allStageAlerts.length > 4 && (
                  <p className="text-xs text-green-600 text-center pt-1">
                    +{allStageAlerts.length - 4} more — see Purchase Orders tab
                  </p>
                )}
              </div>
            </div>
          )
        })}
      </div>

    </div>
  )
}

// ── Stock Overview ────────────────────────────────────────────────────────────

function StockOverviewPanel({ warehouseId }) {
  const { data: stockLevels = [], isLoading } = useGetStockByWarehouseQuery(warehouseId, { skip: !warehouseId })
  const { data: products = [] } = useGetProductsQuery()
  const { data: warehouses = [] } = useGetWarehousesQuery()
  const [filter, setFilter] = useState('ALL')
  const [search, setSearch] = useState('')

  const myWarehouse = warehouses.find(w => w.id === warehouseId)

  const rows = stockLevels
    .map(sl => ({
      ...sl,
      productName: products.find(p => p.id === sl.productId)?.name || sl.productId,
      sku: products.find(p => p.id === sl.productId)?.sku || '',
      status: sl.currentStock < sl.threshold ? 'CRITICAL' : sl.currentStock < sl.threshold * 1.25 ? 'WARNING' : 'OK',
    }))
    .filter(r => filter === 'ALL' || r.status === filter)
    .filter(r => !search || r.productName.toLowerCase().includes(search.toLowerCase()) || r.sku.toLowerCase().includes(search.toLowerCase()))
    .sort((a, b) => (a.daysUntilEmpty ?? 999) - (b.daysUntilEmpty ?? 999))

  const criticalCount = rows.filter(r => r.status === 'CRITICAL').length
  const warningCount  = rows.filter(r => r.status === 'WARNING').length

  if (!warehouseId) {
    return (
      <div className="bg-amber-50 border border-amber-200 rounded-xl p-5 text-sm text-amber-800">
        You are not assigned to a warehouse. Ask your admin to assign you to a warehouse so stock levels appear here.
      </div>
    )
  }

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        <StatCard title="Products Tracked" value={stockLevels.length}            color="blue"  />
        <StatCard title="Critical"          value={criticalCount}                 color={criticalCount > 0 ? 'red' : 'green'} />
        <StatCard title="Warning"           value={warningCount}                  color={warningCount > 0 ? 'amber' : 'green'} />
        <StatCard title="Healthy"           value={rows.filter(r => r.status === 'OK').length} color="green" />
      </div>

      <div className="bg-white rounded-xl border border-gray-200">
        <div className="flex flex-wrap items-center justify-between gap-3 px-5 py-4 border-b border-gray-100">
          <h3 className="font-semibold text-gray-900">Stock — {myWarehouse?.name || warehouseId}</h3>
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
          {isLoading ? <Spinner /> : (
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-gray-50 border-b border-gray-100">
                  {['Product', 'SKU', 'Stock', 'Threshold', 'Status', 'Days Until Empty'].map(h => (
                    <th key={h} className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wide">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50">
                {rows.length === 0 ? (
                  <tr><td colSpan={6} className="px-4 py-8 text-center text-gray-400 text-sm">No stock data</td></tr>
                ) : rows.map(r => (
                  <tr key={r.id} className="hover:bg-gray-50/60">
                    <td className="px-4 py-3 font-medium text-gray-900">{r.productName}</td>
                    <td className="px-4 py-3 font-mono text-xs text-gray-500">{r.sku}</td>
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
          )}
        </div>
      </div>
    </div>
  )
}

// ── In-Progress Orders (ORDER_PLACED alerts) ──────────────────────────────────

function InProgressOrdersPanel() {
  const { data: allAlerts = [], isLoading } = useGetAlertsQuery({ status: 'ORDER_PLACED' })
  const { data: resolvedData = [] } = useGetAlertsQuery({ status: 'RESOLVED' })
  const orders = allAlerts.filter(o => o.status === 'ORDER_PLACED')
  const resolvedAlerts = resolvedData
    .filter(a => a.status === 'RESOLVED')
    .sort((a, b) => new Date(b.updatedAt || b.createdAt) - new Date(a.updatedAt || a.createdAt))
  const { data: products = [] } = useGetProductsQuery()
  const { data: warehouses = [] } = useGetWarehousesQuery()
  const [resolve] = useResolveAlertMutation()
  const [confirm, setConfirm] = useState(null)
  const [busyId, setBusyId] = useState(null)
  const [subView, setSubView] = useState('orders')

  const handleResolve = async (id) => {
    setBusyId(id)
    try {
      await resolve(id).unwrap()
      toast.success('Order resolved — alert closed')
    } catch (err) {
      toast.error(err?.data?.message || 'Failed to resolve')
    } finally {
      setBusyId(null)
      setConfirm(null)
    }
  }

  if (isLoading) return <Spinner />

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-3 gap-4">
        <StatCard title="In-Progress Orders" value={orders.length} color="indigo" />
        <StatCard title="Unique Products"     value={new Set(orders.map(o => o.productId)).size} color="blue"  />
        <StatCard title="Resolved Alerts"     value={resolvedAlerts.length} color="green" />
      </div>

      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        <div className="flex items-center justify-between px-5 py-4 border-b border-gray-100">
          <div className="flex gap-1 p-1 bg-gray-100 rounded-lg">
            <button
              onClick={() => setSubView('orders')}
              className={`px-4 py-1.5 rounded-md text-sm font-medium transition-colors ${subView === 'orders' ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-500 hover:text-gray-700'}`}
            >
              Orders in Progress
              {orders.length > 0 && (
                <span className={`ml-1.5 text-xs font-bold px-1.5 py-0.5 rounded-full ${subView === 'orders' ? 'bg-indigo-100 text-indigo-700' : 'bg-gray-200 text-gray-600'}`}>{orders.length}</span>
              )}
            </button>
            <button
              onClick={() => setSubView('resolved')}
              className={`px-4 py-1.5 rounded-md text-sm font-medium transition-colors ${subView === 'resolved' ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-500 hover:text-gray-700'}`}
            >
              Resolved History
              {resolvedAlerts.length > 0 && (
                <span className={`ml-1.5 text-xs font-bold px-1.5 py-0.5 rounded-full ${subView === 'resolved' ? 'bg-green-100 text-green-700' : 'bg-gray-200 text-gray-600'}`}>{resolvedAlerts.length}</span>
              )}
            </button>
          </div>
        </div>

        {subView === 'orders' && (
          <>
            {orders.length === 0 ? (
              <div className="px-5 py-10 text-center">
                <svg className="w-10 h-10 text-gray-300 mx-auto mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                </svg>
                <p className="text-sm text-gray-400">No orders in progress. Acknowledge alerts and mark them as "Order Placed" to see them here.</p>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="bg-gray-50 border-b border-gray-100">
                      {['Product', 'Warehouse', 'Stock at Alert', 'Threshold', 'Date', 'Action'].map(h => (
                        <th key={h} className="text-left px-5 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wide">{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-50">
                    {orders.map(o => (
                      <tr key={o.id} className="hover:bg-gray-50/60">
                        <td className="px-5 py-3 font-medium text-gray-900">{products.find(p => p.id === o.productId)?.name || o.productId}</td>
                        <td className="px-5 py-3 text-gray-600">{warehouses.find(w => w.id === o.warehouseId)?.name || o.warehouseId}</td>
                        <td className="px-5 py-3"><span className="font-semibold text-red-600">{o.stockAtAlert}</span></td>
                        <td className="px-5 py-3 text-gray-600">{o.threshold}</td>
                        <td className="px-5 py-3 text-gray-500 text-xs">{fmtDate(o.createdAt)}</td>
                        <td className="px-5 py-3">
                          <button
                            onClick={() => setConfirm({ id: o.id, productName: products.find(p => p.id === o.productId)?.name || o.productId })}
                            disabled={busyId === o.id}
                            className="px-3 py-1 bg-green-50 text-green-700 border border-green-100 rounded-lg text-xs font-semibold hover:bg-green-100 disabled:opacity-50 transition-colors"
                          >
                            {busyId === o.id ? 'Resolving…' : 'Mark Resolved'}
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </>
        )}

        {subView === 'resolved' && (
          <>
            {resolvedAlerts.length === 0 ? (
              <div className="px-5 py-10 text-center">
                <svg className="w-10 h-10 text-gray-300 mx-auto mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                <p className="text-sm text-gray-400">No resolved alerts yet.</p>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="bg-gray-50 border-b border-gray-100">
                      {['Product', 'Warehouse', 'Stock at Alert', 'Threshold', 'Resolved'].map(h => (
                        <th key={h} className="text-left px-5 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wide">{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-50">
                    {resolvedAlerts.map(a => (
                      <tr key={a.id} className="hover:bg-gray-50/60">
                        <td className="px-5 py-3 font-medium text-gray-900">{products.find(p => p.id === a.productId)?.name || a.productId}</td>
                        <td className="px-5 py-3 text-gray-600">{warehouses.find(w => w.id === a.warehouseId)?.name || a.warehouseId}</td>
                        <td className="px-5 py-3"><span className="font-semibold text-gray-500">{a.stockAtAlert}</span></td>
                        <td className="px-5 py-3 text-gray-600">{a.threshold}</td>
                        <td className="px-5 py-3 text-gray-400 text-xs">{fmtDate(a.updatedAt || a.createdAt)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </>
        )}
      </div>

      {confirm && (
        <ConfirmDialog
          title="Resolve Order"
          message={`Mark the order for "${confirm.productName}" as resolved? This will close the alert.`}
          danger={false}
          confirmLabel="Resolve Order"
          onConfirm={() => handleResolve(confirm.id)}
          onCancel={() => setConfirm(null)}
        />
      )}
    </div>
  )
}

// ── Alert Frequency ───────────────────────────────────────────────────────────

function AlertFrequencyPanel() {
  const [days, setDays] = useState(30)
  const dateParams = dateRange(days)
  const { data: summary }     = useGetAlertSummaryQuery(dateParams)
  const { data: byWarehouse = [] } = useGetAlertsByWarehouseQuery(dateParams)
  const { data: warehouses = [] } = useGetWarehousesQuery()

  const maxTotal = Math.max(...byWarehouse.map(b => b.total ?? 0), 1)

  const alertsByMonth = summary?.alertsByMonth ?? []
  const totalAlerts   = summary?.totalAlerts ?? 0

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div className="grid grid-cols-3 gap-4 flex-1">
          <StatCard title="Total Alerts"      value={totalAlerts}          color={totalAlerts > 0 ? 'amber' : 'green'} />
          <StatCard title="Warehouses Affected" value={byWarehouse.length} color="blue"  />
          <StatCard title="Months w/ Alerts"  value={alertsByMonth.length} color="purple" />
        </div>
        <div className="flex gap-1 ml-4">
          {[7, 30, 90].map(d => (
            <button key={d} onClick={() => setDays(d)} className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors ${days === d ? 'bg-teal-600 text-white' : 'text-gray-500 border border-gray-200 hover:bg-gray-50'}`}>
              {d}d
            </button>
          ))}
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <div className="bg-white rounded-xl border border-gray-200">
          <div className="px-5 py-4 border-b border-gray-100">
            <h3 className="font-semibold text-gray-900">Alerts by Warehouse</h3>
            <p className="text-xs text-gray-500 mt-0.5">Which warehouses trigger the most restock alerts</p>
          </div>
          <div className="p-5 space-y-4">
            {byWarehouse.length === 0 ? (
              <p className="text-sm text-gray-400 text-center py-4">No alert data for this period.</p>
            ) : byWarehouse.map((b, i) => {
              const warehouseName = warehouses.find(w => w.id === b.warehouseId)?.name || b.warehouseId || 'Unknown'
              const pct = Math.round((b.total / maxTotal) * 100)
              return (
                <div key={i} className="space-y-1">
                  <div className="flex items-center justify-between">
                    <p className="text-sm font-medium text-gray-900 truncate max-w-45">{warehouseName}</p>
                    <p className="text-sm font-bold text-amber-700 shrink-0 ml-2">{b.total} alert{b.total !== 1 ? 's' : ''}</p>
                  </div>
                  <div className="w-full bg-gray-100 rounded-full h-2">
                    <div className="bg-amber-500 h-2 rounded-full transition-all" style={{ width: `${pct}%` }} />
                  </div>
                </div>
              )
            })}
          </div>
        </div>

        <div className="bg-white rounded-xl border border-gray-200">
          <div className="px-5 py-4 border-b border-gray-100">
            <h3 className="font-semibold text-gray-900">Monthly Alert Trend</h3>
            <p className="text-xs text-gray-500 mt-0.5">Number of restock alerts per month</p>
          </div>
          <div className="p-5">
            {alertsByMonth.length === 0 ? (
              <p className="text-sm text-gray-400 text-center py-4">No monthly data for this period.</p>
            ) : (
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-gray-100">
                    <th className="text-left pb-2 text-xs font-semibold text-gray-500 uppercase">Month</th>
                    <th className="text-right pb-2 text-xs font-semibold text-gray-500 uppercase">Alerts</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-50">
                  {alertsByMonth.map((m, i) => {
                    const ym = String(m.month)
                    const label = ym.length === 6
                      ? new Date(`${ym.slice(0, 4)}-${ym.slice(4, 6)}-01`).toLocaleDateString('en-US', { month: 'short', year: 'numeric' })
                      : m.month
                    return (
                      <tr key={i} className="hover:bg-gray-50/60">
                        <td className="py-2.5 text-gray-700">{label}</td>
                        <td className="py-2.5 text-right font-semibold text-amber-700">{m.total}</td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            )}
          </div>
        </div>
      </div>

      <div className="bg-amber-50 border border-amber-200 rounded-xl p-5">
        <h4 className="text-sm font-semibold text-amber-800 mb-1">Procurement Insight</h4>
        <p className="text-sm text-amber-700">
          Warehouses with recurring alerts may benefit from standing supply agreements or higher safety stock levels to reduce restocking frequency.
        </p>
      </div>
    </div>
  )
}

// ── Complaints ────────────────────────────────────────────────────────────────

function ComplaintsPanel() {
  const { companyId, companyName } = useSelector(s => s.auth)
  const dispatch = useDispatch()
  const [form, setForm] = useState({ subject: '', priority: 'MEDIUM', message: '' })
  const [submitted, setSubmitted] = useState(false)
  const ch = e => setForm(f => ({ ...f, [e.target.name]: e.target.value }))

  const handleSubmit = e => {
    e.preventDefault()
    dispatch(submitComplaint({ subject: form.subject, priority: form.priority, message: form.message, companyName: companyName || '', companyId }))
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
  const { warehouseId } = useSelector(s => s.auth)
  const { data: alerts = [] } = useGetAlertsQuery()
  const { data: warehouses = [] } = useGetWarehousesQuery()
  const [activeTab, setActiveTab] = useState('alerts')

  const myWarehouse = warehouses.find(w => w.id === warehouseId)
  const openAlerts      = alerts.filter(a => a.status === 'OPEN').length
  const ordersInFlight  = alerts.filter(a => a.status === 'ORDER_PLACED').length

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
      id: 'orders', label: 'Purchase Orders', badge: ordersInFlight,
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
      {myWarehouse ? (
        <div className="flex items-center gap-2 mb-4 px-4 py-2.5 bg-teal-50 border border-teal-200 rounded-xl">
          <svg className="w-4 h-4 text-teal-600 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
          </svg>
          <span className="text-sm text-teal-800 font-medium">{myWarehouse.name}</span>
          <span className="text-xs text-teal-600">{myWarehouse.address}</span>
        </div>
      ) : (
        <div className="flex items-center gap-2 mb-4 px-4 py-2.5 bg-amber-50 border border-amber-200 rounded-xl">
          <svg className="w-4 h-4 text-amber-600 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
          <span className="text-sm text-amber-800">You are not assigned to a warehouse. Contact your admin.</span>
        </div>
      )}
      {activeTab === 'alerts'     && <AlertPipelinePanel />}
      {activeTab === 'stock'      && <StockOverviewPanel warehouseId={warehouseId} />}
      {activeTab === 'orders'     && <InProgressOrdersPanel />}
      {activeTab === 'frequency'  && <AlertFrequencyPanel />}
      {activeTab === 'complaints' && <ComplaintsPanel />}
    </Layout>
  )
}
