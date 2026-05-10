import { useSelector } from 'react-redux'

const BoxIcon = ({ className = 'w-4 h-4' }) => (
  <svg className={className} fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
      d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
  </svg>
)

export default function Sidebar({ navItems = [], activeTab, onTabChange }) {
  const { companyName, companyLogo } = useSelector(s => s.auth)
  const isCompanyUser = !!companyName

  return (
    <aside className="w-60 bg-teal-50 flex flex-col h-full shrink-0 border-r border-teal-100">
      {/* Header — company branding or platform identity */}
      <div className="bg-teal-600 px-5 py-5 shrink-0">
        {isCompanyUser ? (
          <div className="flex flex-col items-center gap-2.5 text-center">
            {companyLogo ? (
              <img
                src={companyLogo}
                alt={companyName}
                className="w-14 h-14 rounded-2xl object-contain bg-white/90 p-1 shadow-md"
              />
            ) : (
              <div className="w-14 h-14 rounded-2xl bg-white/20 border border-white/30 flex items-center justify-center shadow-sm">
                <span className="text-2xl font-bold text-white leading-none">{companyName[0]}</span>
              </div>
            )}
            <div>
              <p className="text-white font-semibold text-sm leading-snug">{companyName}</p>
              <p className="text-teal-200 text-xs mt-0.5 font-medium tracking-wide">Workspace</p>
            </div>
          </div>
        ) : (
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-xl bg-white/20 border border-white/30 flex items-center justify-center shrink-0">
              <BoxIcon className="w-5 h-5 text-white" />
            </div>
            <div>
              <p className="text-white font-bold text-base leading-tight">InventAlert</p>
              <p className="text-teal-200 text-xs font-medium">Platform Admin</p>
            </div>
          </div>
        )}
      </div>

      {/* Nav items */}
      <nav className="flex-1 px-3 py-4 space-y-0.5 overflow-y-auto">
        {navItems.map(item => (
          <button
            key={item.id}
            onClick={() => onTabChange(item.id)}
            className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-all duration-100 text-left ${
              activeTab === item.id
                ? 'bg-teal-600 text-white shadow-sm'
                : 'text-teal-900 hover:bg-teal-100 hover:text-teal-800'
            }`}
          >
            <span className="shrink-0 w-4 h-4">{item.icon}</span>
            <span className="truncate">{item.label}</span>
            {item.badge > 0 && (
              <span className={`ml-auto min-w-5 h-5 flex items-center justify-center rounded-full text-xs font-bold px-1 ${
                activeTab === item.id ? 'bg-white/20 text-white' : 'bg-teal-600 text-white'
              }`}>
                {item.badge}
              </span>
            )}
          </button>
        ))}
      </nav>

      {/* Footer */}
      <div className="px-4 py-3 border-t border-teal-100 flex items-center gap-2 shrink-0">
        <div className="w-5 h-5 rounded-md bg-teal-600 flex items-center justify-center shrink-0">
          <BoxIcon className="w-3 h-3 text-white" />
        </div>
        <p className="text-xs text-teal-400 font-medium">InventAlert</p>
      </div>
    </aside>
  )
}
