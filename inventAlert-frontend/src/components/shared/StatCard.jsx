export default function StatCard({ title, value, subtitle, color = 'teal', icon }) {
  const colorMap = {
    teal: 'bg-teal-50 text-teal-700 border-teal-200',
    green: 'bg-teal-50 text-teal-700 border-teal-200',
    blue: 'bg-blue-50 text-blue-700 border-blue-200',
    amber: 'bg-amber-50 text-amber-700 border-amber-200',
    red: 'bg-red-50 text-red-700 border-red-200',
    purple: 'bg-purple-50 text-purple-700 border-purple-200',
    gray: 'bg-gray-50 text-gray-700 border-gray-200',
  }
  const iconBg = {
    teal: 'bg-teal-100 text-teal-600',
    green: 'bg-teal-100 text-teal-600',
    blue: 'bg-blue-100 text-blue-600',
    amber: 'bg-amber-100 text-amber-600',
    red: 'bg-red-100 text-red-600',
    purple: 'bg-purple-100 text-purple-600',
    gray: 'bg-gray-100 text-gray-600',
  }
  return (
    <div className={`rounded-xl border p-4 ${colorMap[color] || colorMap.gray}`}>
      <div className="flex items-center justify-between mb-2">
        <span className="text-sm font-medium opacity-80">{title}</span>
        {icon && (
          <div className={`w-8 h-8 rounded-lg flex items-center justify-center ${iconBg[color] || iconBg.gray}`}>
            {icon}
          </div>
        )}
      </div>
      <div className="text-2xl font-bold">{value}</div>
      {subtitle && <div className="text-xs mt-1 opacity-70">{subtitle}</div>}
    </div>
  )
}
