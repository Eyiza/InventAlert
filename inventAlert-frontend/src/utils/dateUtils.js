const toUTC = d => {
  if (!d || typeof d !== 'string') return d
  if (d.endsWith('Z') || d.includes('+')) return d
  if (/^\d{4}-\d{2}-\d{2}$/.test(d)) return d + 'T12:00:00Z'
  return d + 'Z'
}

export const fmtDate = d => new Date(toUTC(d)).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
export const fmtDT = d => new Date(toUTC(d)).toLocaleString('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
export const relativeTime = dateStr => {
  const diff = Date.now() - new Date(toUTC(dateStr)).getTime()
  const mins = Math.floor(diff / 60000)
  if (mins < 1) return 'just now'
  if (mins < 60) return `${mins}m ago`
  const hrs = Math.floor(mins / 60)
  if (hrs < 24) return `${hrs}h ago`
  return `${Math.floor(hrs / 24)}d ago`
}
