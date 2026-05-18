# WebSocket — Real-Time Notifications

The Notification Service exposes a STOMP-over-WebSocket endpoint for real-time notification delivery to the frontend.

## Connection Endpoint

```
ws://localhost:8083/ws
```

Via Nginx gateway: `ws://localhost:8080/notification/ws`

## Authentication

Pass the JWT as a query parameter on the initial WebSocket handshake (the STOMP `CONNECT` frame):

```javascript
const socket = new SockJS('/ws');
const client = new Client({
  webSocketFactory: () => socket,
  connectHeaders: {
    Authorization: `Bearer ${token}`,
  },
});
```

The `WebSocketAuthInterceptor` validates the token on the `CONNECT` frame and rejects invalid tokens with a `403`.

## Subscribing to Notifications

After connecting, subscribe to the user-specific topic:

```javascript
client.onConnect = () => {
  client.subscribe(`/topic/notifications/${userId}`, (message) => {
    const notification = JSON.parse(message.body);
    console.log('New notification:', notification);
  });
};
```

## Notification Payload

```json
{
  "id": "notif-uuid",
  "type": "RESTOCK_ALERT",
  "title": "Low stock: Paracetamol 500mg",
  "message": "Stock at Lagos Main has fallen below threshold (5 units remaining, threshold: 20).",
  "referenceId": "alert-uuid",
  "read": false,
  "createdAt": "2025-05-14T10:30:00Z"
}
```

**Notification types:**

| Type | Trigger |
|---|---|
| `RESTOCK_ALERT` | Stock falls below threshold and no transfer candidate available |
| `TRANSFER_SUGGESTED` | System auto-suggests an inter-warehouse transfer |
| `TRANSFER_APPROVED` | Manager approves a transfer |
| `TRANSFER_REJECTED` | Manager rejects a transfer suggestion |
| `TRANSFER_DISPATCHED` | Source warehouse dispatches goods |
| `TRANSFER_ACCEPTED` | Destination warehouse confirms receipt |
| `TRANSFER_DELIVERY_REJECTED` | Destination rejects delivery |
| `RECONCILIATION_SUBMITTED` | Staff submits a physical count |
| `RECONCILIATION_APPROVED` | Manager approves reconciliation |
| `RECONCILIATION_REJECTED` | Manager rejects reconciliation |
| `PASSWORD_RESET` | Password reset link sent |

## REST Fallback

For clients that cannot maintain a persistent WebSocket, the REST polling API at `/api/notifications` provides the same data. See [endpoints.md](endpoints.md).
