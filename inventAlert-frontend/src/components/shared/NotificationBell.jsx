import { useState } from 'react'
import { useSelector, useDispatch } from 'react-redux'
import NotificationDrawer from './NotificationDrawer'
import { markAllAsRead } from '../../store/slices/notificationsSlice'

export default function NotificationBell() {
  const [open, setOpen] = useState(false)
  const dispatch = useDispatch()
  const { user } = useSelector(s => s.auth)
  const { notifications, wsConnected } = useSelector(s => s.notifications)

  const mine = notifications.filter(n => n.userId === user?.id)
  const unread = mine.filter(n => !n.isRead).length

  const handleMarkAll = () => dispatch(markAllAsRead(user?.id))

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
        {unread > 0 && (
          <span className="absolute -top-0.5 -right-0.5 min-w-4.5 h-4.5 flex items-center justify-center rounded-full bg-red-500 text-white text-[10px] font-bold px-1">
            {unread > 9 ? '9+' : unread}
          </span>
        )}
        {wsConnected && (
          <span className="absolute bottom-1 right-1 w-1.5 h-1.5 rounded-full bg-green-500" title="Live" />
        )}
      </button>

      <NotificationDrawer
        open={open}
        onClose={() => setOpen(false)}
        notifications={mine}
        onMarkAll={handleMarkAll}
      />
    </>
  )
}
