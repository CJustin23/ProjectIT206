import { useEffect } from 'react'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import Layout from './components/Layout'
import Budgets from './pages/Budgets'
import Categories from './pages/Categories'
import Dashboard from './pages/Dashboard'
import Family from './pages/Family'
import Goals from './pages/Goals'
import Login from './pages/Login'
import Register from './pages/Register'
import Transactions from './pages/Transactions'
import Wallets from './pages/Wallets'
import { selectIsAuthenticated, selectIsReady, useAuthStore } from './store/auth'

function Bootstrap() {
  const ready = useAuthStore(selectIsReady)
  const bootstrap = useAuthStore((state) => state.bootstrap)
  useEffect(() => {
    if (!ready) void bootstrap()
  }, [ready, bootstrap])
  return null
}

function Protected({ children }: { children: React.ReactNode }) {
  const ready = useAuthStore(selectIsReady)
  const authed = useAuthStore(selectIsAuthenticated)
  if (!ready) return <p className="p-8 text-center text-sm text-gray-500">Đang kiểm tra phiên đăng nhập…</p>
  if (!authed) return <Navigate to="/login" replace />
  return <>{children}</>
}

function PublicOnly({ children }: { children: React.ReactNode }) {
  const ready = useAuthStore(selectIsReady)
  const authed = useAuthStore(selectIsAuthenticated)
  if (ready && authed) return <Navigate to="/" replace />
  return <>{children}</>
}

export default function App() {
  return (
    <BrowserRouter>
      <Bootstrap />
      <Routes>
        <Route path="/login" element={<PublicOnly><Login /></PublicOnly>} />
        <Route path="/register" element={<PublicOnly><Register /></PublicOnly>} />
        <Route element={<Protected><Layout /></Protected>}>
          <Route index element={<Dashboard />} />
          <Route path="transactions" element={<Transactions />} />
          <Route path="budgets" element={<Budgets />} />
          <Route path="goals" element={<Goals />} />
          <Route path="wallets" element={<Wallets />} />
          <Route path="categories" element={<Categories />} />
          <Route path="family" element={<Family />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
