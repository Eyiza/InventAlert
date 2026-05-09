import { useEffect } from 'react'
import { Link, useNavigate } from 'react-router'
import { useSelector } from 'react-redux'

const ROLE_HOME = {
  ADMIN: '/admin', MANAGER: '/manager', WAREHOUSE_STAFF: '/staff',
  PROCUREMENT_OFFICER: '/procurement', SUPERADMIN: '/superadmin',
}

const FEATURES = [
  {
    icon: (
      <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
      </svg>
    ),
    title: 'Real-time Alerts',
    desc: 'Get instant notifications when stock falls below thresholds. Never be caught off-guard by a stockout again.',
  },
  {
    icon: (
      <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
      </svg>
    ),
    title: 'Multi-Warehouse',
    desc: 'Manage multiple warehouses from one dashboard. Track stock levels, movements, and transfers across all locations.',
  },
  {
    icon: (
      <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" />
      </svg>
    ),
    title: 'Role-Based Access',
    desc: 'Admins, managers, warehouse staff, and procurement — each role sees exactly what they need, nothing more.',
  },
  {
    icon: (
      <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
      </svg>
    ),
    title: 'Smart Analytics',
    desc: 'Velocity tracking, reorder forecasting, transfer efficiency — data-driven decisions without the complexity.',
  },
  {
    icon: (
      <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4" />
      </svg>
    ),
    title: 'Smart Transfers',
    desc: 'AI-suggested stock transfers between warehouses. Approve, reject, or track in-transit movements in real time.',
  },
  {
    icon: (
      <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
      </svg>
    ),
    title: 'Reconciliation',
    desc: 'Staff submit physical counts, managers review discrepancies and approve adjustments — with full audit trails.',
  },
]

const STEPS = [
  { n: '01', title: 'Register your company', desc: 'Create an account, upload your logo, and set up your workspace in under two minutes.' },
  { n: '02', title: 'Add warehouses & team', desc: 'Invite staff, managers, and procurement officers. Assign them to specific warehouses.' },
  { n: '03', title: 'Track inventory live', desc: 'Record intakes, sales, and reconciliations. Get alerts the moment anything falls below threshold.' },
]

const STATS = [
  { value: '500+', label: 'Warehouses tracked' },
  { value: '2M+', label: 'Movements logged' },
  { value: '99.9%', label: 'Uptime guarantee' },
  { value: '<30s', label: 'Alert response time' },
]

function BoxIcon({ className = 'w-6 h-6' }) {
  return (
    <svg className={className} fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
        d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
    </svg>
  )
}

export default function LandingPage() {
  const { isAuthenticated, role } = useSelector(s => s.auth)
  const navigate = useNavigate()

  useEffect(() => {
    if (isAuthenticated) navigate(ROLE_HOME[role] || '/login', { replace: true })
  }, [isAuthenticated, role, navigate])

  return (
    <div className="min-h-screen bg-white text-gray-900 font-sans">

      {/* ── Navbar ── */}
      <header className="sticky top-0 z-50 bg-white/90 backdrop-blur border-b border-gray-100">
        <div className="max-w-6xl mx-auto px-6 h-16 flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-xl bg-teal-600 flex items-center justify-center">
              <BoxIcon className="w-5 h-5 text-white" />
            </div>
            <span className="font-bold text-lg text-gray-900">InventAlert</span>
          </div>
          <nav className="hidden sm:flex items-center gap-6 text-sm text-gray-500">
            <a href="#features" className="hover:text-gray-900 transition-colors">Features</a>
            <a href="#how-it-works" className="hover:text-gray-900 transition-colors">How it works</a>
          </nav>
          <div className="flex items-center gap-2">
            <Link to="/login" className="px-4 py-2 text-sm font-medium text-gray-700 hover:text-gray-900 hover:bg-gray-100 rounded-lg transition-colors">
              Sign In
            </Link>
            <Link to="/signup" className="px-4 py-2 text-sm font-semibold text-white bg-teal-600 hover:bg-teal-700 rounded-lg transition-colors">
              Get Started
            </Link>
          </div>
        </div>
      </header>

      {/* ── Hero ── */}
      <section className="relative overflow-hidden bg-linear-to-br from-teal-600 via-teal-700 to-teal-800 text-white">
        <div className="absolute inset-0 opacity-10">
          <div className="absolute top-10 left-10 w-72 h-72 rounded-full bg-white blur-3xl" />
          <div className="absolute bottom-0 right-20 w-96 h-96 rounded-full bg-teal-300 blur-3xl" />
        </div>
        <div className="relative max-w-6xl mx-auto px-6 py-28 text-center">
          <div className="inline-flex items-center gap-2 bg-white/15 border border-white/20 rounded-full px-4 py-1.5 text-sm font-medium mb-8">
            <span className="w-2 h-2 rounded-full bg-teal-300 animate-pulse" />
            Multi-warehouse support live
          </div>
          <h1 className="text-5xl sm:text-6xl font-extrabold leading-tight tracking-tight mb-6">
            Warehouse intelligence,
            <br />
            <span className="text-teal-200">finally simplified.</span>
          </h1>
          <p className="text-lg text-teal-100 max-w-2xl mx-auto mb-10 leading-relaxed">
            InventAlert gives your team real-time stock visibility, smart reorder alerts, and seamless multi-warehouse coordination — from intake to sale.
          </p>
          <div className="flex flex-col sm:flex-row items-center justify-center gap-3">
            <Link
              to="/signup"
              className="w-full sm:w-auto px-7 py-3.5 bg-white text-teal-700 font-bold rounded-xl hover:bg-teal-50 transition-colors shadow-lg shadow-teal-900/20 text-base"
            >
              Start for free
            </Link>
            <Link
              to="/login"
              className="w-full sm:w-auto px-7 py-3.5 bg-white/10 border border-white/25 text-white font-semibold rounded-xl hover:bg-white/20 transition-colors text-base"
            >
              Sign in →
            </Link>
          </div>
          <p className="text-teal-300 text-sm mt-5">No credit card required · Setup in 2 minutes</p>
        </div>
      </section>

      {/* ── Stats bar ── */}
      <section className="bg-gray-900 text-white">
        <div className="max-w-6xl mx-auto px-6 py-8 grid grid-cols-2 sm:grid-cols-4 gap-6 text-center">
          {STATS.map(s => (
            <div key={s.label}>
              <p className="text-3xl font-extrabold text-teal-400">{s.value}</p>
              <p className="text-sm text-gray-400 mt-1">{s.label}</p>
            </div>
          ))}
        </div>
      </section>

      {/* ── Features ── */}
      <section id="features" className="max-w-6xl mx-auto px-6 py-24">
        <div className="text-center mb-14">
          <p className="text-sm font-semibold text-teal-600 uppercase tracking-wider mb-2">Everything you need</p>
          <h2 className="text-3xl sm:text-4xl font-bold text-gray-900">Built for warehouse teams</h2>
          <p className="text-gray-500 mt-3 max-w-xl mx-auto">From the floor to the boardroom, every role has a purpose-built view.</p>
        </div>
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {FEATURES.map(f => (
            <div key={f.title} className="group p-6 rounded-2xl border border-gray-100 hover:border-teal-200 hover:shadow-lg hover:shadow-teal-50 transition-all duration-200 bg-white">
              <div className="w-12 h-12 rounded-xl bg-teal-50 text-teal-600 flex items-center justify-center mb-4 group-hover:bg-teal-100 transition-colors">
                {f.icon}
              </div>
              <h3 className="font-semibold text-gray-900 mb-2">{f.title}</h3>
              <p className="text-sm text-gray-500 leading-relaxed">{f.desc}</p>
            </div>
          ))}
        </div>
      </section>

      {/* ── How it works ── */}
      <section id="how-it-works" className="bg-gray-50 py-24">
        <div className="max-w-6xl mx-auto px-6">
          <div className="text-center mb-14">
            <p className="text-sm font-semibold text-teal-600 uppercase tracking-wider mb-2">Simple by design</p>
            <h2 className="text-3xl sm:text-4xl font-bold text-gray-900">Up and running in minutes</h2>
          </div>
          <div className="grid sm:grid-cols-3 gap-8">
            {STEPS.map((s, i) => (
              <div key={s.n} className="relative">
                {i < STEPS.length - 1 && (
                  <div className="hidden sm:block absolute top-7 left-[calc(50%+2.5rem)] w-full h-px border-t-2 border-dashed border-teal-200" />
                )}
                <div className="flex flex-col items-center text-center">
                  <div className="w-14 h-14 rounded-2xl bg-teal-600 text-white flex items-center justify-center text-xl font-extrabold mb-4 shadow-lg shadow-teal-200">
                    {s.n}
                  </div>
                  <h3 className="font-semibold text-gray-900 mb-2">{s.title}</h3>
                  <p className="text-sm text-gray-500 leading-relaxed">{s.desc}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── CTA ── */}
      <section className="max-w-6xl mx-auto px-6 py-24 text-center">
        <div className="bg-linear-to-br from-teal-600 to-teal-700 rounded-3xl px-8 py-16 text-white relative overflow-hidden">
          <div className="absolute inset-0 opacity-10">
            <div className="absolute -top-10 -right-10 w-64 h-64 rounded-full bg-white blur-3xl" />
          </div>
          <div className="relative">
            <h2 className="text-3xl sm:text-4xl font-bold mb-4">Ready to take control of your inventory?</h2>
            <p className="text-teal-200 mb-8 max-w-lg mx-auto">Join companies already using InventAlert to eliminate stockouts and streamline their warehouse operations.</p>
            <Link
              to="/signup"
              className="inline-block px-8 py-4 bg-white text-teal-700 font-bold rounded-xl hover:bg-teal-50 transition-colors text-base shadow-xl shadow-teal-900/20"
            >
              Create your free account
            </Link>
          </div>
        </div>
      </section>

      {/* ── Footer ── */}
      <footer className="border-t border-gray-100 bg-gray-50">
        <div className="max-w-6xl mx-auto px-6 py-8 flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-2">
            <div className="w-6 h-6 rounded-lg bg-teal-600 flex items-center justify-center">
              <BoxIcon className="w-4 h-4 text-white" />
            </div>
            <span className="font-semibold text-gray-700">InventAlert</span>
            {/* <span className="text-gray-400 text-sm ml-1">v1.0</span> */}
          </div>
          <div className="flex items-center gap-5 text-sm text-gray-500">
            <Link to="/login" className="hover:text-gray-900 transition-colors">Sign In</Link>
            <Link to="/signup" className="hover:text-gray-900 transition-colors">Register</Link>
          </div>
          <p className="text-xs text-gray-400">© 2026 InventAlert. All rights reserved.</p>
        </div>
      </footer>

    </div>
  )
}
