# InventAlert Frontend

React 19 single-page application for the InventAlert platform. Runs on port **5173** (Vite dev server).

---

## Prerequisites

| Tool | Version |
|------|---------|
| Node.js | 18+ |
| npm | 9+ |

No backend services are required to run the frontend in its current state — it uses mock data for all screens.

---

## Setup

```bash
cd inventAlert-frontend
npm install
```

### Environment variables

Create a `.env` file in the `inventAlert-frontend` directory:

```env
# Cloudinary — upload preset must be set to "Unsigned" in Cloudinary > Settings > Upload
VITE_CLOUDINARY_CLOUD_NAME=your_cloud_name
VITE_CLOUDINARY_UPLOAD_PRESET=InventAlert

# Google Maps (optional — used for warehouse distance features)
VITE_GOOGLE_MAPS_API_KEY=your_google_maps_key
```

> Do not commit `.env`. It is listed in `.gitignore`.

---

## Running

```bash
npm run dev
```

App starts at `http://localhost:5173`.

---

## Available scripts

| Command | Description |
|---------|-------------|
| `npm run dev` | Start the Vite development server with hot reload |
| `npm run build` | Compile and bundle for production (output: `dist/`) |
| `npm run preview` | Serve the production build locally |
| `npm run lint` | Run ESLint |

---

## Mock credentials

The frontend currently uses mock data and does not call the backend APIs. Use these credentials on the login page:

| Role | Email | Password |
|------|-------|----------|
| Admin | `admin@techwave.com` | `password123` |
| Manager | `manager@techwave.com` | `password123` |
| Warehouse Staff | `staff.a@techwave.com` | `password123` |
| Warehouse Staff | `staff.b@techwave.com` | `password123` |
| Procurement Officer | `procurement@techwave.com` | `password123` |
| Super Admin | `superadmin@inventalert.com` | `superadmin123` |

---

## Role-based routes

| Path | Allowed role |
|------|-------------|
| `/admin` | ADMIN |
| `/manager` | MANAGER |
| `/staff` | WAREHOUSE_STAFF |
| `/procurement` | PROCUREMENT_OFFICER |
| `/superadmin` | SUPERADMIN |

`/dashboard` redirects automatically to the correct role path after login.

---

## Tech stack

| Library | Version | Purpose |
|---------|---------|---------|
| React | 19 | UI framework |
| Vite | 8 | Build tool and dev server |
| Redux Toolkit | 2 | Global state management |
| React Router | 7 | Client-side routing |
| Tailwind CSS | 4 | Utility-first styling |
| React Toastify | 11 | Toast notifications |
| Cloudinary | — | Image upload (company logos) |

---

## Connecting to the backend

When backend integration is added, set the API base URL in `.env`:

```env
VITE_API_BASE_URL=http://localhost:8081
VITE_INVENTORY_API_URL=http://localhost:8082
VITE_NOTIFICATION_API_URL=http://localhost:8083
```

The backend services that need to be running:
- **Identity service** on `localhost:8081` — auth, users, companies
- **Inventory service** on `localhost:8082` — warehouses, products, stock, transfers
- **Notification service** on `localhost:8083` — in-app notifications and WebSocket push

See `docker-compose.yml` in the project root for starting all infrastructure and services together.
