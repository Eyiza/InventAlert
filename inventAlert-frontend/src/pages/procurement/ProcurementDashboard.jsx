import { useState } from 'react'
import { useSelector, useDispatch } from 'react-redux'
import { toast } from 'react-toastify'
import Layout from '../../components/layout/Layout'
import StatusBadge from '../../components/shared/StatusBadge'
import StatCard from '../../components/shared/StatCard'
import { acknowledgeAlert, markOrderPlaced, resolveAlert } from '../../store/slices/alertsSlice'

const fmtDT = d => new Date(d).toLocaleString('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })

// ── Alert Pipeline (Kanban) ───────────────────────────────────────────────────

const PIPELINE_STAGES = [
  { status: 'OPEN', label: 'Open', color: 'bg-amber-50 border-amber-200', dot: 'bg-amber-500', hdr: 'text-amber-700' },
  { status: 'ACKNOWLEDGED', label: 'Acknowledged', color: 'bg-blue-50 border-blue-200', dot: 'bg-blue-500', hdr: 'text-blue-700' },
  { status: 'ORDER_PLACED', label: 'Order Placed', color: 'bg-indigo-50 border-indigo-200', dot: 'bg-indigo-500', hdr: 'text-indigo-700' },
  { status: 'RESOLVED', label: 'Resolved', color: 'bg-green-50 border-green-200', dot: 'bg-green-500', hdr: 'text-green-700' },
]

function AlertCard({ alert, productName, warehouseName, userId, onAction }) {
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
        <button
          onClick={onAction}
          className="w-full py-1.5 bg-green-500 hover:bg-teal-700 text-white text-xs font-semibold rounded-lg transition-colors"
        >
          {status === 'OPEN' && 'Acknowledge'}
          {status === 'ACKNOWLEDGED' && 'Mark Order Placed'}
          {status === 'ORDER_PLACED' && 'Mark Resolved'}
        </button>
      )}
    </div>
  )
}

function AlertPipelinePanel() {
  const { alerts } = useSelector(s => s.alerts)
  const { products, warehouses } = useSelector(s => s.stock)
  const { user } = useSelector(s => s.auth)
  const dispatch = useDispatch()

  const getAction = (alert) => {
    if (alert.status === 'OPEN') return () => { dispatch(acknowledgeAlert({ alertId: alert.id, userId: user.id })); toast.success('Alert acknowledged') }
    if (alert.status === 'ACKNOWLEDGED') return () => { dispatch(markOrderPlaced(alert.id)); toast.success('Order marked as placed') }
    if (alert.status === 'ORDER_PLACED') return () => { dispatch(resolveAlert(alert.id)); toast.success('Alert resolved') }
    return null
  }

  const openCount = alerts.filter(a => a.status === 'OPEN').length
  const resolvedCount = alerts.filter(a => a.status === 'RESOLVED').length

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        <StatCard title="Open Alerts" value={alerts.filter(a => a.status === 'OPEN').length} color={openCount > 0 ? 'red' : 'green'} />
        <StatCard title="Acknowledged" value={alerts.filter(a => a.status === 'ACKNOWLEDGED').length} color="blue" />
        <StatCard title="Order Placed" value={alerts.filter(a => a.status === 'ORDER_PLACED').length} color="purple" />
        <StatCard title="Resolved" value={resolvedCount} color="green" />
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {PIPELINE_STAGES.map(stage => {
          const stageAlerts = alerts.filter(a => a.status === stage.status)
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
                ) : (
                  stageAlerts.map(alert => (
                    <AlertCard
                      key={alert.id}
                      alert={alert}
                      productName={products.find(p => p.id === alert.productId)?.name || alert.productId}
                      warehouseName={warehouses.find(w => w.id === alert.warehouseId)?.name || alert.warehouseId}
                      userId={user.id}
                      onAction={getAction(alert)}
                    />
                  ))
                )}
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}

// ── Alert Frequency Panel ─────────────────────────────────────────────────────

function AlertFrequencyPanel() {
  const { alertFrequency } = useSelector(s => s.analytics)
  const maxCount = Math.max(...alertFrequency.map(a => a.alertCount), 1)

  return (
    <div className="space-y-4">
      <div className="bg-white rounded-xl border border-gray-200">
        <div className="px-5 py-4 border-b border-gray-100">
          <h3 className="font-semibold text-gray-900">Alert Frequency Analysis</h3>
          <p className="text-xs text-gray-500 mt-0.5">Products that trigger restock alerts most frequently</p>
        </div>
        <div className="p-5 space-y-4">
          {alertFrequency.map((a, i) => (
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
          Industrial Laptop at Warehouse Alpha has triggered <strong>5 restock alerts</strong> with an average of <strong>12.5 days</strong> between alerts.
          Consider negotiating a standing order with your supplier to reduce alert frequency.
        </p>
      </div>
    </div>
  )
}

// ── Main ──────────────────────────────────────────────────────────────────────

export default function ProcurementDashboard() {
  const [activeTab, setActiveTab] = useState('pipeline')
  const { alerts } = useSelector(s => s.alerts)

  const openCount = alerts.filter(a => a.status === 'OPEN').length

  const navItems = [
    {
      id: 'pipeline', label: 'Alert Pipeline', badge: openCount,
      icon: <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" /></svg>,
    },
    {
      id: 'frequency', label: 'Alert Frequency', badge: 0,
      icon: <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" /></svg>,
    },
  ]

  return (
    <Layout title="Procurement Dashboard" navItems={navItems} activeTab={activeTab} onTabChange={setActiveTab}>
      {activeTab === 'pipeline' && <AlertPipelinePanel />}
      {activeTab === 'frequency' && <AlertFrequencyPanel />}
    </Layout>
  )
}
