import { useEffect, useState } from 'react'

type Health = {
  status: string
  app: string
  version: string
  db: { connected: boolean; tables?: number; flywayVersion?: string; error?: string }
}

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? '/api'

export default function App() {
  const [health, setHealth] = useState<Health | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    fetch(`${API_BASE}/health`)
      .then((r) => r.json() as Promise<Health>)
      .then(setHealth)
      .catch((e: Error) => setError(e.message))
  }, [])

  return (
    <main className="mx-auto max-w-2xl p-8">
      <h1 className="text-2xl font-bold text-brand-700">Family Finance Manager</h1>
      <p className="mt-1 text-sm text-gray-500">
        Đề tài 10 — Quản lý thu chi cá nhân &amp; hộ gia đình · Sprint 1 (khởi tạo môi trường)
      </p>

      <section className="mt-6 rounded-lg border border-gray-200 bg-white p-5 shadow-sm">
        <h2 className="font-semibold">Trạng thái hệ thống</h2>
        {error && <p className="mt-2 text-red-600">Không gọi được API: {error}</p>}
        {health && (
          <ul className="mt-3 space-y-1 text-sm">
            <li>Backend: <span className="font-medium text-green-600">{health.status}</span> ({health.app} v{health.version})</li>
            <li>MySQL: {health.db.connected ? `kết nối OK — ${health.db.tables} bảng, Flyway ${health.db.flywayVersion}` : `lỗi: ${health.db.error}`}</li>
          </ul>
        )}
      </section>

      <p className="mt-6 text-xs text-gray-400">
        Khung UI (sidebar, dashboard, trang giao dịch) sẽ được Kỳ dựng ở Sprint 2.
      </p>
    </main>
  )
}
