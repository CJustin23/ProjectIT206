import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuthStore } from '../store/auth'
import { Button, ErrorNote, Field, Input } from '../components/ui'

export default function Login() {
  const login = useAuthStore((state) => state.login)
  const busy = useAuthStore((state) => state.busy)
  const error = useAuthStore((state) => state.error)
  const clearError = useAuthStore((state) => state.clearError)
  const navigate = useNavigate()
  const [email, setEmail] = useState('an@ffa.dev')
  const [password, setPassword] = useState('password')

  async function onSubmit(event: React.FormEvent) {
    event.preventDefault()
    const ok = await login({ email, password })
    if (ok) navigate('/', { replace: true })
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-gray-50 p-4">
      <form onSubmit={onSubmit} className="w-full max-w-sm space-y-4 rounded-lg border border-gray-200 bg-white p-6 shadow-sm">
        <header>
          <h1 className="text-xl font-bold text-brand-700">Budglee</h1>
          <p className="text-sm text-gray-500">Quản lý thu chi cá nhân &amp; hộ gia đình</p>
        </header>
        {error && <ErrorNote message={error} />}
        <Field label="Email">
          <Input type="email" value={email} required autoComplete="username"
            onChange={(event) => { setEmail(event.target.value); clearError() }} />
        </Field>
        <Field label="Mật khẩu">
          <Input type="password" value={password} required autoComplete="current-password"
            onChange={(event) => { setPassword(event.target.value); clearError() }} />
        </Field>
        <Button type="submit" disabled={busy} className="w-full">
          {busy ? 'Đang đăng nhập…' : 'Đăng nhập'}
        </Button>
        <p className="text-center text-sm text-gray-500">
          Chưa có tài khoản? <Link to="/register" className="font-medium text-brand-600 hover:underline">Đăng ký</Link>
        </p>
        <p className="rounded bg-gray-50 p-2 text-center text-xs text-gray-500">
          Tài khoản demo: an@ffa.dev / password
        </p>
      </form>
    </main>
  )
}
