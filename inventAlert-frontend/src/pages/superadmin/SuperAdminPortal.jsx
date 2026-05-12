import { useState } from 'react'
import { useSelector, useDispatch } from 'react-redux'
import { toast } from 'react-toastify'
import Layout from '../../components/layout/Layout'
import StatusBadge from '../../components/shared/StatusBadge'
import StatCard from '../../components/shared/StatCard'
import { suspendCompany, reactivateCompany, resolveComplaint, reviewComplaint } from '../../store/slices/superadminSlice'

const fmtDate = d => new Date(d).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
const fmtDT = d => new Date(d).toLocaleString('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })

function SearchBar({ value, onChange, placeholder }) {
  return (
    <div className="relative">
      <svg className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
      </svg>
      <input
        type="text" value={value} onChange={e => onChange(e.target.value)} placeholder={placeholder}
        className="pl-9 pr-4 py-1.5 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-teal-600 w-52"
      />
    </div>
  )
}

const PRIORITY_COLOR = {
  HIGH: 'bg-red-100 text-red-700 border-red-200',
  MEDIUM: 'bg-amber-100 text-amber-700 border-amber-200',
  LOW: 'bg-gray-100 text-gray-600 border-gray-200',
}

const COMPLAINT_STATUS_COLOR = {
  OPEN: 'bg-red-50 text-red-600 border-red-200',
  IN_REVIEW: 'bg-blue-50 text-blue-600 border-blue-200',
  RESOLVED: 'bg-teal-50 text-teal-600 border-teal-200',
}

// ── Companies Panel ───────────────────────────────────────────────────────────

function CompaniesPanel() {
  const { companies } = useSelector(s => s.superadmin)
  const dispatch = useDispatch()
  const [filter, setFilter] = useState('ALL')
  const [search, setSearch] = useState('')
  const [confirm, setConfirm] = useState(null)

  const filtered = companies
    .filter(c => filter === 'ALL' || c.status === filter)
    .filter(c => !search || c.companyName.toLowerCase().includes(search.toLowerCase()) || c.adminEmail.toLowerCase().includes(search.toLowerCase()))

  const handleAction = (company) => {
    if (company.status === 'ACTIVE') setConfirm({ company, action: 'suspend' })
    else { dispatch(reactivateCompany(company.id)); toast.success(`${company.companyName} reactivated`) }
  }

  const confirmSuspend = () => {
    dispatch(suspendCompany(confirm.company.id))
    toast.warning(`${confirm.company.companyName} suspended`)
    setConfirm(null)
  }

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-2 sm:grid-cols-3 gap-4">
        <StatCard title="Total Companies" value={companies.length} color="blue" />
        <StatCard title="Active" value={companies.filter(c => c.status === 'ACTIVE').length} color="teal" />
        <StatCard title="Suspended" value={companies.filter(c => c.status === 'SUSPENDED').length} color="red" />
      </div>

      <div className="bg-white rounded-xl border border-gray-200">
        <div className="flex flex-wrap items-center justify-between gap-3 px-5 py-4 border-b border-gray-100">
          <h3 className="font-semibold text-gray-900">Registered Companies</h3>
          <div className="flex items-center gap-2 flex-wrap">
            <SearchBar value={search} onChange={setSearch} placeholder="Search name or email…" />
            <div className="flex gap-1">
              {['ALL', 'ACTIVE', 'SUSPENDED'].map(f => (
                <button key={f} onClick={() => setFilter(f)} className={`px-3 py-1 rounded-lg text-xs font-medium transition-colors ${filter === f ? 'bg-teal-600 text-white' : 'text-gray-500 hover:bg-gray-100'}`}>{f}</button>
              ))}
            </div>
          </div>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-100">
                {['Company', 'Admin Email', 'Status', 'Registered', 'Actions'].map(h => (
                  <th key={h} className="text-left px-5 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wide">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {filtered.map(c => (
                <tr key={c.id} className={`hover:bg-gray-50/60 ${c.status === 'SUSPENDED' ? 'opacity-70' : ''}`}>
                  <td className="px-5 py-4">
                    <div className="flex items-center gap-3">
                      {c.logo ? (
                        <img src={c.logo} alt={c.companyName} className="w-8 h-8 rounded-lg object-contain border border-gray-200 bg-gray-50 shrink-0" />
                      ) : (
                        <div className="w-8 h-8 rounded-lg bg-teal-100 flex items-center justify-center text-teal-700 font-bold text-sm shrink-0">
                          {c.companyName[0]}
                        </div>
                      )}
                      <span className="font-medium text-gray-900">{c.companyName}</span>
                    </div>
                  </td>
                  <td className="px-5 py-4 text-gray-600">{c.adminEmail}</td>
                  <td className="px-5 py-4"><StatusBadge status={c.status} /></td>
                  <td className="px-5 py-4 text-gray-500 text-xs">{fmtDate(c.createdAt)}</td>
                  <td className="px-5 py-4">
                    <button
                      onClick={() => handleAction(c)}
                      className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors ${c.status === 'ACTIVE' ? 'bg-red-50 text-red-600 hover:bg-red-100' : 'bg-teal-50 text-teal-700 hover:bg-teal-100'}`}
                    >
                      {c.status === 'ACTIVE' ? 'Suspend' : 'Reactivate'}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {confirm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4" onClick={() => setConfirm(null)}>
          <div className="bg-white rounded-2xl shadow-xl p-6 w-full max-w-sm" onClick={e => e.stopPropagation()}>
            <div className="text-center mb-4">
              <div className="w-12 h-12 rounded-full bg-red-100 flex items-center justify-center mx-auto mb-3">
                <svg className="w-6 h-6 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                </svg>
              </div>
              <h3 className="font-semibold text-gray-900">Suspend Company?</h3>
              <p className="text-sm text-gray-500 mt-1">
                <strong>{confirm.company.companyName}</strong> users will immediately lose access to the platform.
              </p>
            </div>
            <div className="flex gap-2">
              <button onClick={() => setConfirm(null)} className="flex-1 py-2 border border-gray-300 text-gray-700 rounded-lg text-sm hover:bg-gray-50">Cancel</button>
              <button onClick={confirmSuspend} className="flex-1 py-2 bg-red-500 text-white rounded-lg text-sm font-semibold hover:bg-red-600">Suspend</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

// ── Complaints Panel ──────────────────────────────────────────────────────────

function ComplaintsPanel() {
  const { complaints } = useSelector(s => s.superadmin)
  const dispatch = useDispatch()
  const [filter, setFilter] = useState('ALL')
  const [search, setSearch] = useState('')
  const [companyFilter, setCompanyFilter] = useState('')
  const [expanded, setExpanded] = useState(null)

  const companies = [...new Set(complaints.map(c => c.companyName))].sort()
  const filtered = complaints
    .filter(c => filter === 'ALL' || c.status === filter)
    .filter(c => !companyFilter || c.companyName === companyFilter)
    .filter(c => !search || c.subject.toLowerCase().includes(search.toLowerCase()) || c.submittedBy.toLowerCase().includes(search.toLowerCase()) || c.companyName.toLowerCase().includes(search.toLowerCase()))
  const openCount = complaints.filter(c => c.status === 'OPEN').length

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        <StatCard title="Total Tickets" value={complaints.length} color="blue" />
        <StatCard title="Open" value={openCount} color={openCount > 0 ? 'red' : 'teal'} />
        <StatCard title="In Review" value={complaints.filter(c => c.status === 'IN_REVIEW').length} color="amber" />
        <StatCard title="Resolved" value={complaints.filter(c => c.status === 'RESOLVED').length} color="teal" />
      </div>

      <div className="bg-white rounded-xl border border-gray-200">
        <div className="px-5 py-4 border-b border-gray-100 space-y-3">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h3 className="font-semibold text-gray-900">Support & Complaints</h3>
              <p className="text-xs text-gray-500 mt-0.5">Feedback and issues submitted by company users</p>
            </div>
            <div className="flex gap-1">
              {['ALL', 'OPEN', 'IN_REVIEW', 'RESOLVED'].map(f => (
                <button key={f} onClick={() => setFilter(f)} className={`px-2.5 py-1 rounded-lg text-xs font-medium transition-colors ${filter === f ? 'bg-teal-600 text-white' : 'text-gray-500 hover:bg-gray-100'}`}>
                  {f === 'IN_REVIEW' ? 'In Review' : f}
                </button>
              ))}
            </div>
          </div>
          <div className="flex gap-2 flex-wrap">
            <SearchBar value={search} onChange={setSearch} placeholder="Search subject or company…" />
            <select
              value={companyFilter} onChange={e => setCompanyFilter(e.target.value)}
              className="px-3 py-1.5 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-teal-600"
            >
              <option value="">All Companies</option>
              {companies.map(name => <option key={name} value={name}>{name}</option>)}
            </select>
          </div>
        </div>
        <div className="divide-y divide-gray-100">
          {filtered.length === 0 ? (
            <div className="px-5 py-12 text-center text-gray-400">
              <svg className="w-10 h-10 text-gray-300 mx-auto mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
              </svg>
              <p className="text-sm">No tickets in this category.</p>
            </div>
          ) : filtered.map(c => (
            <div key={c.id} className="px-5 py-4">
              <div className="flex items-start justify-between gap-4">
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 flex-wrap mb-1">
                    <span className={`text-xs font-semibold px-2 py-0.5 rounded-full border ${PRIORITY_COLOR[c.priority]}`}>{c.priority}</span>
                    <span className={`text-xs font-medium px-2 py-0.5 rounded-full border ${COMPLAINT_STATUS_COLOR[c.status]}`}>{c.status.replace('_', ' ')}</span>
                    <span className="text-xs text-gray-400">{fmtDT(c.createdAt)}</span>
                  </div>
                  <p className="font-medium text-gray-900 text-sm">{c.subject}</p>
                  <p className="text-xs text-gray-500 mt-0.5">{c.submittedBy} · {c.companyName} · <a href={`mailto:${c.email}`} className="text-teal-600 hover:underline">{c.email}</a></p>
                  {expanded === c.id && (
                    <p className="text-sm text-gray-600 mt-2 bg-gray-50 rounded-lg px-3 py-2.5 border border-gray-100">{c.message}</p>
                  )}
                </div>
                <div className="flex items-center gap-2 shrink-0">
                  <button onClick={() => setExpanded(expanded === c.id ? null : c.id)} className="text-xs text-gray-400 hover:text-gray-600 px-2 py-1 rounded hover:bg-gray-100">
                    {expanded === c.id ? 'Collapse' : 'View'}
                  </button>
                  {c.status === 'OPEN' && (
                    <button onClick={() => { dispatch(reviewComplaint(c.id)); toast.info('Ticket marked as in review') }} className="px-2.5 py-1 bg-blue-50 text-blue-600 text-xs rounded-lg hover:bg-blue-100 font-medium">
                      Mark In Review
                    </button>
                  )}
                  {c.status !== 'RESOLVED' && (
                    <button onClick={() => { dispatch(resolveComplaint(c.id)); toast.success('Ticket resolved') }} className="px-2.5 py-1 bg-teal-50 text-teal-600 text-xs rounded-lg hover:bg-teal-100 font-medium">
                      Resolve
                    </button>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

// ── Main ──────────────────────────────────────────────────────────────────────

export default function SuperAdminPortal() {
  const [activeTab, setActiveTab] = useState('companies')
  const { companies, complaints } = useSelector(s => s.superadmin)

  const openComplaints = complaints.filter(c => c.status === 'OPEN').length

  const navItems = [
    {
      id: 'companies', label: 'Companies', badge: companies.filter(c => c.status === 'ACTIVE').length,
      icon: <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" /></svg>,
    },
    {
      id: 'complaints', label: 'Support & Complaints', badge: openComplaints,
      icon: <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" /></svg>,
    },
  ]

  return (
    <Layout title="Platform Admin Console" navItems={navItems} activeTab={activeTab} onTabChange={setActiveTab}>
      {activeTab === 'companies' && <CompaniesPanel />}
      {activeTab === 'complaints' && <ComplaintsPanel />}
    </Layout>
  )
}
