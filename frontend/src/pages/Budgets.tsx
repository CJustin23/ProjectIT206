import { useState } from 'react'
import { budgetApi, categoryApi } from '../lib/endpoints'
import { useFetch } from '../hooks/useFetch'
import { useAuthStore } from '../store/auth'
import { currentMonthKey, firstOfMonth, formatVnd, monthRange } from '../lib/format'
import type { BudgetPayload } from '../types/api'
import { Button, Card, Empty, ErrorNote, Field, Input, Loading, Modal, ProgressBar, Select } from '../components/ui'

const STATUS_TEXT: Record<string, string> = { SAFE: 'An toàn', WARNING: 'Sắp vượt', OVER: 'Đã vượt' }

export default function Budgets() {
  const [month, setMonth] = useState(currentMonthKey())
  const periodStart = firstOfMonth(month)
  const familyId = useAuthStore((state) => state.family?.id ?? 0)
  const list = useFetch(() => budgetApi.list(periodStart), [periodStart])
  const categories = useFetch(() => categoryApi.list('EXPENSE'), [])
  const [form, setForm] = useState<{ categoryId: string; limitAmount: string; alertThresholdPercent: string } | null>(null)
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  async function submit(event: React.FormEvent) {
    event.preventDefault()
    if (!form) return
    const limit = Number(form.limitAmount.replace(/[^0-9.]/g, ''))
    if (!(limit > 0)) return setFormError('Hạn mức phải lớn hơn 0.')
    const range = monthRange(month)
    const payload: BudgetPayload = {
      familyId,
      categoryId: Number(form.categoryId),
      periodStart: range.periodStart,
      periodEnd: range.to,
      limitAmount: limit,
      alertThresholdPercent: Number(form.alertThresholdPercent || 80),
    }
    setSaving(true)
    setFormError(null)
    try {
      await budgetApi.upsert(payload)
      setForm(null)
      list.reload()
    } catch (error) {
      setFormError(error instanceof Error ? error.message : 'Không lưu được ngân sách.')
    } finally {
      setSaving(false)
    }
  }

  async function remove(id: number) {
    if (!window.confirm('Xoá ngân sách này?')) return
    await budgetApi.remove(id)
    list.reload()
  }

  const rows = list.data ?? []

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <h1 className="text-lg font-semibold text-brand-700">Ngân sách</h1>
        <div className="flex items-end gap-2">
          <Field label="Tháng">
            <Input type="month" value={month} onChange={(event) => setMonth(event.target.value)} />
          </Field>
          <Button onClick={() => { setFormError(null); setForm({ categoryId: '', limitAmount: '', alertThresholdPercent: '80' }) }}>
            + Đặt ngân sách
          </Button>
        </div>
      </div>

      <Card>
        {list.error && <ErrorNote message={list.error} code={list.errorCode} />}
        {list.loading && !list.data && <Loading />}
        {!list.loading && rows.length === 0 && <Empty message="Chưa đặt ngân sách cho tháng này." />}
        <ul className="space-y-4">
          {rows.map((budget) => (
            <li key={budget.id}>
              <div className="mb-1 flex items-center justify-between text-sm">
                <span className="font-medium">{budget.categoryName ?? `Danh mục #${budget.categoryId}`}</span>
                <span className="text-gray-500">
                  {formatVnd(budget.used)} / {formatVnd(budget.limitAmount)} ·{' '}
                  <span className={budget.status === 'OVER' ? 'text-red-600' : budget.status === 'WARNING' ? 'text-amber-600' : 'text-green-600'}>
                    {STATUS_TEXT[budget.status]} {budget.percent}%
                  </span>
                </span>
              </div>
              <ProgressBar percent={budget.percent} status={budget.status} />
              <div className="mt-1 text-right">
                <button type="button" className="text-xs text-red-600 hover:underline" onClick={() => remove(budget.id)}>Xoá</button>
              </div>
            </li>
          ))}
        </ul>
      </Card>

      {form && (
        <Modal title="Đặt ngân sách tháng" onClose={() => setForm(null)}>
          <form onSubmit={submit} className="space-y-3">
            {formError && <ErrorNote message={formError} />}
            <Field label="Danh mục chi">
              <Select value={form.categoryId} required onChange={(event) => setForm({ ...form, categoryId: event.target.value })}>
                <option value="">— chọn danh mục —</option>
                {(categories.data ?? []).map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}
              </Select>
            </Field>
            <Field label="Hạn mức (VND)">
              <Input inputMode="numeric" value={form.limitAmount} required onChange={(event) => setForm({ ...form, limitAmount: event.target.value })} />
            </Field>
            <Field label="Ngưỡng cảnh báo (%)">
              <Input inputMode="numeric" value={form.alertThresholdPercent} onChange={(event) => setForm({ ...form, alertThresholdPercent: event.target.value })} />
            </Field>
            <div className="flex justify-end gap-2 pt-2">
              <Button variant="ghost" onClick={() => setForm(null)}>Huỷ</Button>
              <Button type="submit" disabled={saving}>{saving ? 'Đang lưu…' : 'Lưu'}</Button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  )
}
