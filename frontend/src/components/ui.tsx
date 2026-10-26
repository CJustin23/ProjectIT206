import type { ReactNode } from 'react'

export function Card({ title, action, children, className = '' }: {
  title?: string
  action?: ReactNode
  children: ReactNode
  className?: string
}) {
  return (
    <section className={`rounded-lg border border-gray-200 bg-white p-4 shadow-sm ${className}`}>
      {(title || action) && (
        <header className="mb-3 flex items-center justify-between gap-2">
          {title && <h2 className="text-sm font-semibold text-brand-700">{title}</h2>}
          {action}
        </header>
      )}
      {children}
    </section>
  )
}

export function StatCard({ label, value, tone = 'default' }: {
  label: string
  value: string
  tone?: 'default' | 'income' | 'expense'
}) {
  const toneClass = tone === 'income' ? 'text-green-600' : tone === 'expense' ? 'text-red-600' : 'text-brand-700'
  return (
    <div className="rounded-lg border border-gray-200 bg-white p-4 shadow-sm">
      <p className="text-xs uppercase tracking-wide text-gray-500">{label}</p>
      <p className={`mt-1 text-xl font-bold ${toneClass}`}>{value}</p>
    </div>
  )
}

export function Button({ children, onClick, type = 'button', variant = 'primary', disabled, className = '' }: {
  children: ReactNode
  onClick?: () => void
  type?: 'button' | 'submit'
  variant?: 'primary' | 'ghost' | 'danger'
  disabled?: boolean
  className?: string
}) {
  const styles = {
    primary: 'bg-brand-600 text-white hover:bg-brand-700',
    ghost: 'border border-gray-300 bg-white text-gray-700 hover:bg-gray-50',
    danger: 'bg-red-600 text-white hover:bg-red-700',
  } as const
  return (
    <button
      type={type}
      onClick={onClick}
      disabled={disabled}
      className={`rounded-md px-3 py-2 text-sm font-medium transition disabled:opacity-50 ${styles[variant]} ${className}`}
    >
      {children}
    </button>
  )
}

export function Field({ label, error, children }: { label: string; error?: string; children: ReactNode }) {
  return (
    <label className="block text-sm">
      <span className="mb-1 block font-medium text-gray-700">{label}</span>
      {children}
      {error && <span className="mt-1 block text-xs text-red-600">{error}</span>}
    </label>
  )
}

const inputClass = 'w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none'

export function Input(props: React.InputHTMLAttributes<HTMLInputElement>) {
  return <input {...props} className={`${inputClass} ${props.className ?? ''}`} />
}

export function Select(props: React.SelectHTMLAttributes<HTMLSelectElement>) {
  return <select {...props} className={`${inputClass} ${props.className ?? ''}`} />
}

export function Empty({ message }: { message: string }) {
  return <p className="py-6 text-center text-sm text-gray-500">{message}</p>
}

export function Loading({ message = 'Đang tải dữ liệu…' }: { message?: string }) {
  return <p className="py-6 text-center text-sm text-gray-500">{message}</p>
}

export function ErrorNote({ message, code }: { message: string; code?: string | null }) {
  return (
    <div className="rounded-md border border-red-200 bg-red-50 p-3 text-sm text-red-700">
      {message}
      {code && <span className="ml-2 text-xs text-red-500">({code})</span>}
    </div>
  )
}

export function Modal({ title, onClose, children }: { title: string; onClose: () => void; children: ReactNode }) {
  return (
    <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/40 p-4" role="dialog" aria-modal="true">
      <div className="w-full max-w-lg rounded-lg bg-white p-5 shadow-lg">
        <header className="mb-4 flex items-center justify-between">
          <h3 className="text-base font-semibold text-brand-700">{title}</h3>
          <button type="button" onClick={onClose} className="text-gray-400 hover:text-gray-700" aria-label="Đóng">✕</button>
        </header>
        {children}
      </div>
    </div>
  )
}

export function ProgressBar({ percent, status }: { percent: number; status: string }) {
  const width = Math.min(Math.max(percent, 0), 100)
  const color = status === 'OVER' ? 'bg-red-500' : status === 'WARNING' ? 'bg-amber-400' : 'bg-green-500'
  return (
    <div className="h-2 w-full overflow-hidden rounded-full bg-gray-100">
      <div className={`h-full ${color}`} style={{ width: `${width}%` }} />
    </div>
  )
}
