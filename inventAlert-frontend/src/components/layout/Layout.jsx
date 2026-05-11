import Navbar from './Navbar'
import Sidebar from './Sidebar'

export default function Layout({ children, title, navItems, activeTab, onTabChange }) {
  return (
    <div className="flex h-screen bg-gray-50 overflow-hidden">
      <Sidebar navItems={navItems} activeTab={activeTab} onTabChange={onTabChange} />
      <div className="flex-1 flex flex-col min-w-0 overflow-hidden">
        <Navbar title={title} />
        <main className="flex-1 overflow-y-auto p-6">
          {children}
        </main>
      </div>
    </div>
  )
}
