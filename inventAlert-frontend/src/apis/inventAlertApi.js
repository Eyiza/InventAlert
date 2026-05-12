import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react'

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
    window.location.href = '/login'
  }
  return result
}

export const inventAlertApi = createApi({
  reducerPath: 'inventAlertApi',
  baseQuery: baseQueryWithAuthRedirect,
  tagTypes: ['User', 'Company', 'Warehouse', 'Product', 'Stock', 'Movement', 'Transfer', 'Reconciliation', 'Alert', 'Notification'],
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

  }),
})

export const {
  useLoginMutation,
  useSuperAdminLoginMutation,
  useSignupMutation,
  useForgotPasswordMutation,
  useResetPasswordMutation,
} = inventAlertApi
