import { useSelector } from 'react-redux'

export default function Sidebar({ navItems = [], activeTab, onTabChange }) {
  const { companyName, companyLogo } = useSelector(s => s.auth)

  return (
    <aside className="w-60 bg-gray-900 flex flex-col h-full shrink-0">
      <div className="flex items-center gap-2.5 px-5 py-4 border-b border-gray-800">
        <div className="w-8 h-8 rounded-lg bg-teal-600 flex items-center justify-center shrink-0">
          <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
              d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
          </svg>
        </div>
        <span className="text-white font-bold text-lg tracking-tight">InventAlert</span>
      </div>

      {companyName && (
        <div className="flex items-center gap-2.5 px-5 py-2.5 border-b border-gray-800 bg-gray-800/40">
          {companyLogo ? (
            <img src={companyLogo} alt={companyName} className="w-6 h-6 rounded object-contain shrink-0" />
          ) : (
            <div className="w-6 h-6 rounded bg-teal-600 text-white text-xs font-bold flex items-center justify-center shrink-0">
              {companyName[0]}
            </div>
          )}
          <span className="text-gray-300 text-sm font-medium truncate">{companyName}</span>
        </div>
      )}

      <nav className="flex-1 px-3 py-4 space-y-0.5 overflow-y-auto">
        {navItems.map(item => (
          <button
            key={item.id}
            onClick={() => onTabChange(item.id)}
            className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-all duration-100 text-left ${
              activeTab === item.id
                ? 'bg-teal-600 text-white shadow-sm'
                : 'text-gray-400 hover:bg-gray-800 hover:text-white'
            }`}
          >
            <span className="shrink-0 w-4 h-4">{item.icon}</span>
            <span className="truncate">{item.label}</span>
            {item.badge > 0 && (
              <span className={`ml-auto min-w-5 h-5 flex items-center justify-center rounded-full text-xs font-bold ${
                activeTab === item.id ? 'bg-white/20 text-white' : 'bg-teal-600 text-white'
              }`}>
                {item.badge}
              </span>
            )}
          </button>
        ))}
      </nav>

      <div className="px-4 py-3 border-t border-gray-800">
        <p className="text-xs text-gray-500">InventAlert v1.0</p>
      </div>
    </aside>
  )
}
