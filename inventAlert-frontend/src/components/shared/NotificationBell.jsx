import { useState } from 'react'
import { useSelector } from 'react-redux'
import NotificationDrawer from './NotificationDrawer'
import { useNotificationSocket } from '../../hooks/useNotificationSocket'
import { useGetNotificationsQuery, useMarkNotificationReadMutation } from '../../apis/inventAlertApi'

export default function NotificationBell() {
  const [open, setOpen] = useState(false)
  const { token } = useSelector(s => s.auth)
  const { wsConnected } = useSelector(s => s.notifications)
  const [markNotificationRead] = useMarkNotificationReadMutation()

  useNotificationSocket()

  const { data: notifications = [] } = useGetNotificationsQuery(undefined, { skip: !token })

  const unreadItems = notifications.filter(n => !n.isRead)
  const unreadCount = unreadItems.length

  const handleMarkAll = () => {
    unreadItems.forEach(n => markNotificationRead(n.id))
  }

  return (
    <>
      <button
        onClick={() => setOpen(true)}
        className="relative p-2 rounded-lg text-gray-500 hover:text-gray-700 hover:bg-gray-100 transition-colors"
        title="Notifications"
      >
        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
            d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
        </svg>
        {unreadCount > 0 && (
          <span className="absolute -top-0.5 -right-0.5 min-w-4.5 h-4.5 flex items-center justify-center rounded-full bg-red-500 text-white text-[10px] font-bold px-1">
            {unreadCount > 9 ? '9+' : unreadCount}
          </span>
        )}
        {wsConnected && (
          <span className="absolute bottom-1 right-1 w-1.5 h-1.5 rounded-full bg-green-500" title="Live" />
        )}
      </button>

      <NotificationDrawer
        open={open}
        onClose={() => setOpen(false)}
        notifications={notifications}
        onMarkAll={handleMarkAll}
      />
    </>
  )
}
