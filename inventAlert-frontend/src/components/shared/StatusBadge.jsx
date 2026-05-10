const STYLES = {
  ACTIVE: 'bg-green-100 text-green-800',
  SUSPENDED: 'bg-red-100 text-red-800',
  OPEN: 'bg-amber-100 text-amber-800',
  ACKNOWLEDGED: 'bg-blue-100 text-blue-800',
  ORDER_PLACED: 'bg-indigo-100 text-indigo-800',
  RESOLVED: 'bg-green-100 text-green-800',
  SUGGESTED: 'bg-amber-100 text-amber-800',
  APPROVED: 'bg-blue-100 text-blue-800',
  REJECTED: 'bg-red-100 text-red-800',
  IN_TRANSIT: 'bg-purple-100 text-purple-800',
  COMPLETED: 'bg-green-100 text-green-800',
  DELIVERY_REJECTED: 'bg-red-100 text-red-800',
  PENDING_APPROVAL: 'bg-amber-100 text-amber-800',
  CRITICAL: 'bg-red-100 text-red-800',
  WARNING: 'bg-amber-100 text-amber-800',
  OK: 'bg-green-100 text-green-800',
  ADMIN: 'bg-green-100 text-green-800',
  MANAGER: 'bg-blue-100 text-blue-800',
  WAREHOUSE_STAFF: 'bg-amber-100 text-amber-800',
  PROCUREMENT_OFFICER: 'bg-purple-100 text-purple-800',
  SUPERADMIN: 'bg-red-100 text-red-800',
  INTAKE: 'bg-green-100 text-green-800',
  OUTBOUND_SALE: 'bg-orange-100 text-orange-800',
  TRANSFER_OUT: 'bg-blue-100 text-blue-800',
  TRANSFER_IN: 'bg-indigo-100 text-indigo-800',
  RECONCILIATION: 'bg-gray-100 text-gray-800',
  GOOGLE_MAPS: 'bg-blue-100 text-blue-800',
  HAVERSINE: 'bg-gray-100 text-gray-700',
}

const LABELS = {
  WAREHOUSE_STAFF: 'Staff',
  PROCUREMENT_OFFICER: 'Procurement',
  PENDING_APPROVAL: 'Pending',
  ORDER_PLACED: 'Order Placed',
  OUTBOUND_SALE: 'Outbound Sale',
  TRANSFER_OUT: 'Transfer Out',
  TRANSFER_IN: 'Transfer In',
  DELIVERY_REJECTED: 'Del. Rejected',
  GOOGLE_MAPS: 'Google Maps',
}

export default function StatusBadge({ status, size = 'sm' }) {
  const cls = STYLES[status] || 'bg-gray-100 text-gray-700'
  const label = LABELS[status] || status?.replace(/_/g, ' ')
  const textSize = size === 'xs' ? 'text-xs px-1.5 py-0.5' : 'text-xs px-2 py-0.5'
  return (
    <span className={`inline-flex items-center rounded-full font-medium ${textSize} ${cls}`}>
      {label}
    </span>
  )
}
