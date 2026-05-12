import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react'
import { logout } from '../store/slices/authSlice'

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081'

const baseQuery = fetchBaseQuery({
  baseUrl: BASE_URL,
  prepareHeaders: (headers, { getState }) => {
    const token = getState().auth.token
    if (token) headers.set('Authorization', `Bearer ${token}`)
    return headers
  },
})

const AUTH_ENDPOINTS = ['login', 'superAdminLogin', 'signup', 'forgotPassword', 'resetPassword']

const baseQueryWithAuthRedirect = async (args, api, extraOptions) => {
  const result = await baseQuery(args, api, extraOptions)
  if (result.error?.status === 401 && !AUTH_ENDPOINTS.includes(api.endpoint)) {
    // Soft logout — clears Redux + localStorage without a page reload.
    // ProtectedRoute will redirect to /login on the next render.
    api.dispatch(logout())
  }
  return result
}

export const inventAlertApi = createApi({
  reducerPath: 'inventAlertApi',
  baseQuery: baseQueryWithAuthRedirect,
  refetchOnFocus: true,
  refetchOnReconnect: true,
  tagTypes: ['User', 'Company', 'Warehouse', 'Product', 'Stock', 'Movement', 'Transfer', 'Reconciliation', 'Alert', 'Notification', 'Analytics'],
  endpoints: build => ({

    // ── Auth ──────────────────────────────────────────────────────────────────
    login: build.mutation({
      query: body => ({ url: '/api/auth/login', method: 'POST', body }),
    }),
    superAdminLogin: build.mutation({
      query: body => ({ url: '/api/auth/superadmin/login', method: 'POST', body }),
    }),
    signup: build.mutation({
      query: body => ({ url: '/api/auth/signup', method: 'POST', body }),
    }),
    forgotPassword: build.mutation({
      query: body => ({ url: '/api/auth/forgot-password', method: 'POST', body }),
    }),
    resetPassword: build.mutation({
      query: body => ({ url: '/api/auth/reset-password', method: 'POST', body }),
    }),

    // ── Company ───────────────────────────────────────────────────────────────
    getMyCompany: build.query({
      query: () => '/api/companies/me',
      providesTags: ['Company'],
    }),
    updateMyCompany: build.mutation({
      query: body => ({ url: '/api/companies/me', method: 'PATCH', body }),
      invalidatesTags: ['Company'],
    }),

    // ── Users ─────────────────────────────────────────────────────────────────
    getUsers: build.query({
      query: () => '/api/users',
      providesTags: ['User'],
    }),
    createUser: build.mutation({
      query: body => ({ url: '/api/users', method: 'POST', body }),
      invalidatesTags: ['User'],
    }),
    updateUserRole: build.mutation({
      query: ({ id, role }) => ({ url: `/api/users/${id}/role`, method: 'PATCH', body: { role } }),
      invalidatesTags: ['User'],
    }),
    deactivateUser: build.mutation({
      query: id => ({ url: `/api/users/${id}/deactivate`, method: 'PATCH' }),
      invalidatesTags: ['User'],
    }),
    reactivateUser: build.mutation({
      query: id => ({ url: `/api/users/${id}/reactivate`, method: 'PATCH' }),
      invalidatesTags: ['User'],
    }),
    getUserAssignments: build.query({
      query: id => `/api/users/${id}/assignments`,
      providesTags: (result, error, id) => [{ type: 'User', id: `${id}-assignments` }],
    }),
    assignToWarehouse: build.mutation({
      query: ({ id, warehouseId }) => ({ url: `/api/users/${id}/assign`, method: 'POST', body: { warehouseId } }),
      invalidatesTags: (result, error, { id }) => [{ type: 'User', id: `${id}-assignments` }],
    }),
    removeAssignment: build.mutation({
      query: ({ userId, assignmentId }) => ({ url: `/api/users/${userId}/assignments/${assignmentId}`, method: 'DELETE' }),
      invalidatesTags: (result, error, { userId }) => [{ type: 'User', id: `${userId}-assignments` }],
    }),

    // ── Warehouses ────────────────────────────────────────────────────────────
    getWarehouses: build.query({
      query: () => '/api/warehouses',
      providesTags: ['Warehouse'],
    }),
    createWarehouse: build.mutation({
      query: body => ({ url: '/api/warehouses', method: 'POST', body }),
      invalidatesTags: ['Warehouse'],
    }),
    updateWarehouse: build.mutation({
      query: ({ id, ...body }) => ({ url: `/api/warehouses/${id}`, method: 'PATCH', body }),
      invalidatesTags: ['Warehouse'],
    }),
    deactivateWarehouse: build.mutation({
      query: id => ({ url: `/api/warehouses/${id}/deactivate`, method: 'PATCH' }),
      invalidatesTags: ['Warehouse'],
    }),
    activateWarehouse: build.mutation({
      query: id => ({ url: `/api/warehouses/${id}/activate`, method: 'PATCH' }),
      invalidatesTags: ['Warehouse'],
    }),

    // ── Products ──────────────────────────────────────────────────────────────
    getProducts: build.query({
      query: () => '/api/products',
      providesTags: ['Product'],
    }),
    createProduct: build.mutation({
      query: body => ({ url: '/api/products', method: 'POST', body }),
      invalidatesTags: ['Product'],
    }),
    updateProduct: build.mutation({
      query: ({ id, ...body }) => ({ url: `/api/products/${id}`, method: 'PATCH', body }),
      invalidatesTags: ['Product'],
    }),
    importProducts: build.mutation({
      query: file => {
        const fd = new FormData()
        fd.append('file', file)
        return { url: '/api/products/import', method: 'POST', body: fd }
      },
      invalidatesTags: ['Product'],
    }),

    // ── Stock ─────────────────────────────────────────────────────────────────
    getStockByWarehouse: build.query({
      query: warehouseId => `/api/stock/${warehouseId}`,
      providesTags: (result, error, warehouseId) => [{ type: 'Stock', id: warehouseId }],
    }),

    // ── Movements ─────────────────────────────────────────────────────────────
    getMovements: build.query({
      query: (params = {}) => ({ url: '/api/movements', params }),
      providesTags: ['Movement'],
    }),
    recordMovement: build.mutation({
      query: body => ({ url: '/api/movements', method: 'POST', body }),
      invalidatesTags: (result, error, { warehouseId }) => ['Movement', { type: 'Stock', id: warehouseId }],
    }),

    // ── Transfers ─────────────────────────────────────────────────────────────
    getTransfers: build.query({
      query: () => '/api/transfers',
      transformResponse: res => res.content ?? res,
      providesTags: ['Transfer'],
    }),
    initiateTransfer: build.mutation({
      query: body => ({ url: '/api/transfers', method: 'POST', body }),
      invalidatesTags: ['Transfer'],
    }),
    approveTransfer: build.mutation({
      query: id => ({ url: `/api/transfers/${id}/approve`, method: 'PATCH' }),
      invalidatesTags: ['Transfer'],
    }),
    rejectTransfer: build.mutation({
      query: id => ({ url: `/api/transfers/${id}/reject`, method: 'PATCH' }),
      invalidatesTags: ['Transfer'],
    }),
    dispatchTransfer: build.mutation({
      query: id => ({ url: `/api/transfers/${id}/dispatch`, method: 'PATCH' }),
      invalidatesTags: ['Transfer', 'Stock'],
    }),
    acceptTransfer: build.mutation({
      query: id => ({ url: `/api/transfers/${id}/accept`, method: 'PATCH' }),
      invalidatesTags: ['Transfer', 'Stock'],
    }),
    rejectDelivery: build.mutation({
      query: id => ({ url: `/api/transfers/${id}/reject-delivery`, method: 'PATCH' }),
      invalidatesTags: ['Transfer'],
    }),

    // ── Notifications ─────────────────────────────────────────────────────────
    getNotifications: build.query({
      query: () => '/api/notifications',
      transformResponse: res => res.map(n => ({ ...n, id: n.notificationId, isRead: n.read })),
      providesTags: ['Notification'],
    }),
    markNotificationRead: build.mutation({
      query: id => ({ url: `/api/notifications/${id}/read`, method: 'PATCH' }),
      invalidatesTags: ['Notification'],
    }),

    // ── Reconciliations ───────────────────────────────────────────────────────
    getReconciliations: build.query({
      query: () => '/api/reconciliations',
      transformResponse: res => res.content ?? res,
      providesTags: ['Reconciliation'],
    }),
    submitReconciliation: build.mutation({
      query: body => ({ url: '/api/reconciliations', method: 'POST', body }),
      invalidatesTags: ['Reconciliation'],
    }),
    approveReconciliation: build.mutation({
      query: id => ({ url: `/api/reconciliations/${id}/approve`, method: 'PATCH' }),
      invalidatesTags: ['Reconciliation', 'Stock'],
    }),
    rejectReconciliation: build.mutation({
      query: id => ({ url: `/api/reconciliations/${id}/reject`, method: 'PATCH' }),
      invalidatesTags: ['Reconciliation'],
    }),

    // ── Analytics ─────────────────────────────────────────────────────────────
    getStockSummary: build.query({
      query: ({ from, to }) => ({ url: '/api/analytics/stock/summary', params: { from, to } }),
      providesTags: ['Analytics'],
    }),
    getMovementTrend: build.query({
      query: ({ from, to }) => ({ url: '/api/analytics/stock/movements/trend', params: { from, to } }),
      providesTags: ['Analytics'],
    }),
    getTransferSummary: build.query({
      query: ({ from, to }) => ({ url: '/api/analytics/transfers/summary', params: { from, to } }),
      providesTags: ['Analytics'],
    }),
    getAlertSummary: build.query({
      query: ({ from, to }) => ({ url: '/api/analytics/alerts/summary', params: { from, to } }),
      providesTags: ['Analytics'],
    }),
    getAlertsByWarehouse: build.query({
      query: ({ from, to }) => ({ url: '/api/analytics/alerts/by-warehouse', params: { from, to } }),
      providesTags: ['Analytics'],
    }),

  }),
})

export const {
  useLoginMutation,
  useSuperAdminLoginMutation,
  useSignupMutation,
  useForgotPasswordMutation,
  useResetPasswordMutation,
  useGetMyCompanyQuery,
  useUpdateMyCompanyMutation,
  useGetUsersQuery,
  useCreateUserMutation,
  useUpdateUserRoleMutation,
  useDeactivateUserMutation,
  useReactivateUserMutation,
  useGetUserAssignmentsQuery,
  useAssignToWarehouseMutation,
  useRemoveAssignmentMutation,
  useGetWarehousesQuery,
  useCreateWarehouseMutation,
  useUpdateWarehouseMutation,
  useDeactivateWarehouseMutation,
  useActivateWarehouseMutation,
  useGetProductsQuery,
  useCreateProductMutation,
  useUpdateProductMutation,
  useImportProductsMutation,
  useGetStockByWarehouseQuery,
  useGetNotificationsQuery,
  useMarkNotificationReadMutation,
  useGetMovementsQuery,
  useRecordMovementMutation,
  useGetTransfersQuery,
  useInitiateTransferMutation,
  useApproveTransferMutation,
  useRejectTransferMutation,
  useDispatchTransferMutation,
  useAcceptTransferMutation,
  useRejectDeliveryMutation,
  useGetReconciliationsQuery,
  useSubmitReconciliationMutation,
  useApproveReconciliationMutation,
  useRejectReconciliationMutation,
  useGetStockSummaryQuery,
  useGetMovementTrendQuery,
  useGetTransferSummaryQuery,
  useGetAlertSummaryQuery,
  useGetAlertsByWarehouseQuery,
} = inventAlertApi
