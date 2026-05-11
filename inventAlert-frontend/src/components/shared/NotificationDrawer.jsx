import { useDispatch } from 'react-redux'
import { markAsRead } from '../../store/slices/notificationsSlice'

const TYPE_ICONS = {
  RESTOCK_ALERT: { bg: 'bg-amber-100', text: 'text-amber-600', icon: '⚠' },
  TRANSFER_SUGGESTION: { bg: 'bg-blue-100', text: 'text-blue-600', icon: '↔' },
  TRANSFER_APPROVED: { bg: 'bg-green-100', text: 'text-green-600', icon: '✓' },
  RECONCILIATION_REQUESTED: { bg: 'bg-purple-100', text: 'text-purple-600', icon: '≡' },
  TRANSFER_REJECTED: { bg: 'bg-red-100', text: 'text-red-600', icon: '✕' },
}

function timeAgo(dateStr) {
  const diff = Date.now() - new Date(dateStr).getTime()
  const mins = Math.floor(diff / 60000)
  if (mins < 60) return `${mins}m ago`
  const hrs = Math.floor(mins / 60)
  if (hrs < 24) return `${hrs}h ago`
  return `${Math.floor(hrs / 24)}d ago`
}

export default function NotificationDrawer({ open, onClose, notifications, onMarkAll }) {
  const dispatch = useDispatch()
  const unreadCount = notifications.filter(n => !n.isRead).length

  if (!open) return null

  return (
    <>
      <div className="fixed inset-0 z-40 bg-black/20" onClick={onClose} />
      <div className="fixed right-0 top-0 h-full w-96 bg-white shadow-xl z-50 flex flex-col">
        <div className="flex items-center justify-between px-5 py-4 border-b border-gray-200">
          <div>
            <h2 className="font-semibold text-gray-900">Notifications</h2>
            {unreadCount > 0 && (
              <p className="text-xs text-gray-500">{unreadCount} unread</p>
            )}
          </div>
          <div className="flex items-center gap-2">
            {unreadCount > 0 && (
              <button
                onClick={onMarkAll}
                className="text-xs text-green-600 hover:underline font-medium"
              >
                Mark all read
              </button>
            )}
            <button
              onClick={onClose}
              className="p-1.5 rounded-lg text-gray-400 hover:text-gray-600 hover:bg-gray-100 transition-colors"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
        </div>

        <div className="flex-1 overflow-y-auto divide-y divide-gray-100">
          {notifications.length === 0 ? (
            <div className="flex flex-col items-center justify-center h-48 text-gray-400">
              <svg className="w-10 h-10 mb-2 opacity-40" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
                  d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
              </svg>
              <p className="text-sm">No notifications</p>
            </div>
          ) : (
            notifications
              .slice()
              .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
              .map(n => {
                const meta = TYPE_ICONS[n.type] || { bg: 'bg-gray-100', text: 'text-gray-600', icon: '•' }
                return (
                  <div
                    key={n.id}
                    className={`flex gap-3 px-5 py-4 cursor-pointer hover:bg-gray-50 transition-colors ${!n.isRead ? 'bg-green-50/40' : ''}`}
                    onClick={() => !n.isRead && dispatch(markAsRead(n.id))}
                  >
                    <div className={`shrink-0 w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold ${meta.bg} ${meta.text}`}>
                      {meta.icon}
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className={`text-sm leading-snug ${!n.isRead ? 'text-gray-900 font-medium' : 'text-gray-600'}`}>
                        {n.message}
                      </p>
                      <p className="text-xs text-gray-400 mt-1">{timeAgo(n.createdAt)}</p>
                    </div>
                    {!n.isRead && (
                      <div className="shrink-0 mt-1.5">
                        <div className="w-2 h-2 rounded-full bg-green-500" />
                      </div>
                    )}
                  </div>
                )
              })
          )}
        </div>
      </div>
    </>
  )
}
