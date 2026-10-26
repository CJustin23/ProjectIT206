import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuthStore } from '../store/auth'
import { Button, ErrorNote, Field, Input } from '../components/ui'

export default function Register() {
  const register = useAuthStore((state) => state.register)
  const busy = useAuthStore((state) => state.busy)
  const error = useAuthStore((state) => state.error)
  const clearError = useAuthStore((state) => state.clearError)
  const navigate = useNavigate()

  const [form, setForm] = useState({ fullName: '', email: '', password: '', familyName: '' })
  const [confirm, setConfirm] = useState('')
  const [localError, setLocalError] = useState<string | null>(null)

  const set = (key: keyof typeof form) => (event: React.ChangeEvent<HTMLInputElement>) => {
    setForm({ ...form, [key]: event.target.value })
    clearError()
  }

  async function onSubmit(event: React.FormEvent) {
    event.preventDefault()
    if (form.password.length < 8) return setLocalError('Mật khẩu tối thiểu 8 ký tự.')
    if (form.password !== confirm) return setLocalError('Mật khẩu nhập lại không khớp.')
    setLocalError(null)
    const ok = await register({ email: form.email, password: form.password, fullName: form.fullName })
    if (ok) navigate('/', { replace: true })
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-gray-50 p-4">
      <form onSubmit={onSubmit} className="w-full max-w-sm space-y-4 rounded-lg border border-gray-200 bg-white p-6 shadow-sm">
        <header>
          <h1 className="text-xl font-bold text-brand-700">Tạo tài khoản</h1>
          <p className="text-sm text-gray-500">Nhóm sẽ tự tạo hộ gia đình cho bạn</p>
        </header>
        {error && <ErrorNote message={error} />}
        {localError && <ErrorNote message={localError} />}
        <Field label="Họ và tên">
          <Input value={form.fullName} required onChange={set('fullName')} autoComplete="name" />
        </Field>
        <Field label="Email">
          <Input type="email" value={form.email} required onChange={set('email')} autoComplete="username" />
        </Field>
        <Field label="Mật khẩu (≥ 8 ký tự)">
          <Input type="password" value={form.password} required onChange={set('password')} autoComplete="new-password" />
        </Field>
        <Field label="Nhập lại mật khẩu">
          <Input type="password" value={confirm} required onChange={(event) => setConfirm(event.target.value)} autoComplete="new-password" />
        </Field>
        <Field label="Tên hộ gia đình (tuỳ chọn)">
          <Input value={form.familyName} onChange={set('familyName')} placeholder="Ví dụ: Gia đình An" />
        </Field>
        <Button type="submit" disabled={busy} className="w-full">
          {busy ? 'Đang tạo tài khoản…' : 'Đăng ký'}
        </Button>
        <p className="text-center text-sm text-gray-500">
          Đã có tài khoản? <Link to="/login" className="font-medium text-brand-600 hover:underline">Đăng nhập</Link>
        </p>
      </form>
    </main>
  )
}
