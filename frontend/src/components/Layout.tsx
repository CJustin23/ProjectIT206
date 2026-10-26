import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuthStore } from '../store/auth'

const NAV = [
  { to: '/', label: 'Tổng quan', icon: '◧' },
  { to: '/transactions', label: 'Giao dịch', icon: '⇄' },
  { to: '/budgets', label: 'Ngân sách', icon: '▤' },
  { to: '/goals', label: 'Mục tiêu', icon: '◎' },
  { to: '/wallets', label: 'Ví & thẻ', icon: '▣' },
  { to: '/categories', label: 'Danh mục', icon: '☰' },
  { to: '/family', label: 'Hộ gia đình', icon: '⌂' },
]

export default function Layout() {
  const user = useAuthStore((state) => state.user)
  const family = useAuthStore((state) => state.family)
  const logout = useAuthStore((state) => state.logout)
  const navigate = useNavigate()

  async function onLogout() {
    await logout()
    navigate('/login', { replace: true })
  }

  const linkClass = ({ isActive }: { isActive: boolean }) =>
    `flex items-center gap-2 rounded-md px-3 py-2 text-sm ${
      isActive ? 'bg-brand-50 font-semibold text-brand-700' : 'text-gray-600 hover:bg-gray-100'
    }`

  return (
    <div className="min-h-screen bg-gray-50 text-gray-800 md:flex">
      <aside className="border-b border-gray-200 bg-white p-3 md:w-60 md:border-b-0 md:border-r">
        <div className="mb-4 px-2">
          <p className="text-lg font-bold text-brand-700">Budglee</p>
          <p className="truncate text-xs text-gray-500">{family?.name ?? 'Chưa có hộ gia đình'}</p>
        </div>
        <nav className="flex gap-1 overflow-x-auto md:flex-col">
          {NAV.map((item) => (
            <NavLink key={item.to} to={item.to} end={item.to === '/'} className={linkClass}>
              <span aria-hidden>{item.icon}</span>
              {item.label}
            </NavLink>
          ))}
        </nav>
      </aside>

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex items-center justify-between border-b border-gray-200 bg-white px-4 py-3">
          <p className="text-sm text-gray-600">
            Xin chào <span className="font-semibold">{user?.fullName ?? 'bạn'}</span>
            {user?.email ? ` · ${user.email}` : ''}
          </p>
          <button type="button" onClick={onLogout} className="rounded-md border border-gray-300 px-3 py-1.5 text-sm hover:bg-gray-50">
            Đăng xuất
          </button>
        </header>
        <main className="flex-1 space-y-4 p-4">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
