import { useEffect, useRef } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { setWsConnected } from '../store/slices/notificationsSlice'
import { inventAlertApi } from '../apis/inventAlertApi'

function getSockUrl() {
  const apiBase = import.meta.env.VITE_API_BASE_URL
  if (apiBase === 'http://localhost' || apiBase === 'https://localhost') {
    return `${apiBase}/ws`
  }
  return 'http://localhost:8083/ws'
}

export function useNotificationSocket() {
  const dispatch = useDispatch()
  const { token, user, companyId } = useSelector(s => s.auth)
  const clientRef = useRef(null)

  useEffect(() => {
    if (!token || !user?.id || !companyId) return

    const client = new Client({
      webSocketFactory: () => new SockJS(getSockUrl()),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      onConnect: () => {
        dispatch(setWsConnected(true))
        client.subscribe(`/topic/notifications/${companyId}/${user.id}`, ({ body }) => {
          try {
            const raw = JSON.parse(body)
            const notification = { ...raw, id: raw.notificationId, isRead: raw.read }
            dispatch(
              inventAlertApi.util.updateQueryData('getNotifications', undefined, draft => {
                if (!draft.find(n => n.id === notification.id)) {
                  draft.unshift(notification)
                }
              }),
            )
          } catch { /* ignore malformed messages */ }
        })
      },
      onDisconnect: () => dispatch(setWsConnected(false)),
      onStompError: () => dispatch(setWsConnected(false)),
    })

    client.activate()
    clientRef.current = client

    return () => {
      clientRef.current?.deactivate()
      dispatch(setWsConnected(false))
    }
  }, [token, user?.id, companyId, dispatch])
}
