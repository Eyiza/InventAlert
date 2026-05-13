export default function StatCard({ title, value, subtitle, color = 'teal', icon }) {
  const iconColors = {
    teal:   'bg-teal-100 text-teal-600',
    green:  'bg-emerald-100 text-emerald-600',
    blue:   'bg-blue-100 text-blue-600',
    amber:  'bg-amber-100 text-amber-600',
    red:    'bg-red-100 text-red-600',
    purple: 'bg-purple-100 text-purple-600',
    gray:   'bg-gray-100 text-gray-500',
  }
  const valueColors = {
    teal:   'text-teal-700',
    green:  'text-emerald-700',
    blue:   'text-blue-700',
    amber:  'text-amber-700',
    red:    'text-red-700',
    purple: 'text-purple-700',
    gray:   'text-gray-700',
  }
  return (
    <div className="bg-white rounded-xl border border-gray-200 px-4 py-3.5 shadow-sm">
      <div className="flex items-center justify-between mb-2.5">
        <span className="text-xs font-semibold text-gray-400 uppercase tracking-wide">{title}</span>
        {icon && (
          <div className={`w-7 h-7 rounded-lg flex items-center justify-center ${iconColors[color] || iconColors.gray}`}>
            {icon}
          </div>
        )}
      </div>
      <div className={`text-2xl font-bold leading-none ${valueColors[color] || 'text-gray-900'}`}>{value}</div>
      {subtitle && <div className="text-xs text-gray-400 mt-1.5">{subtitle}</div>}
    </div>
  )
}
