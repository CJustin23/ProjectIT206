import { useState } from 'react'
import { goalApi, walletApi } from '../lib/endpoints'
import { useFetch } from '../hooks/useFetch'
import { formatVnd, todayIso } from '../lib/format'
import type { GoalPayload } from '../types/api'
import { Button, Card, Empty, ErrorNote, Field, Input, Loading, Modal, ProgressBar, Select } from '../components/ui'

export default function Goals() {
  const list = useFetch(() => goalApi.list(), [])
  const wallets = useFetch(() => walletApi.list(false), [])
  const [form, setForm] = useState<GoalPayload | null>(null)
  const [contributing, setContributing] = useState<{ goalId: number; amount: string; walletId: string } | null>(null)
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  async function submit(event: React.FormEvent) {
    event.preventDefault()
    if (!form) return
    setSaving(true)
    setFormError(null)
    try {
      await goalApi.create(form)
      setForm(null)
      list.reload()
    } catch (error) {
      setFormError(error instanceof Error ? error.message : 'Không tạo được mục tiêu.')
    } finally {
      setSaving(false)
    }
  }

  async function contribute(event: React.FormEvent) {
    event.preventDefault()
    if (!contributing) return
    const amount = Number(contributing.amount.replace(/[^0-9.]/g, ''))
    if (!(amount > 0) || !contributing.walletId) return setFormError('Nhập số tiền và chọn ví nguồn.')
    setSaving(true)
    setFormError(null)
    try {
      await goalApi.contribute(contributing.goalId, { amount, walletId: Number(contributing.walletId), contributedOn: todayIso() })
      setContributing(null)
      list.reload()
    } catch (error) {
      setFormError(error instanceof Error ? error.message : 'Không nạp được tiền.')
    } finally {
      setSaving(false)
    }
  }

  const rows = list.data ?? []

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-lg font-semibold text-brand-700">Mục tiêu tiết kiệm</h1>
        <Button onClick={() => { setFormError(null); setForm({ name: '', targetAmount: 0, deadline: null }) }}>+ Thêm mục tiêu</Button>
      </div>

      {list.error && <ErrorNote message={list.error} code={list.errorCode} />}
      {list.loading && !list.data && <Loading />}
      {!list.loading && rows.length === 0 && <Empty message="Chưa có mục tiêu tiết kiệm nào." />}

      <div className="grid gap-4 md:grid-cols-2">
        {rows.map((goal) => (
          <Card key={goal.id} title={goal.name}>
            <p className="text-sm text-gray-600">
              Đã tích luỹ {formatVnd(goal.savedAmount)} / {formatVnd(goal.targetAmount)} · {Math.round(goal.percent)}%
            </p>
            <div className="my-2"><ProgressBar percent={goal.percent} status={goal.status === 'COMPLETED' ? 'SAFE' : 'WARNING'} /></div>
            <div className="flex items-center justify-between text-xs text-gray-500">
              <span>{goal.deadline ? `Hạn ${goal.deadline.split('-').reverse().join('/')}` : 'Không có hạn'} · {goal.status}</span>
              <button type="button" className="text-brand-600 hover:underline" onClick={() => {
                setFormError(null)
                setContributing({ goalId: goal.id, amount: '', walletId: wallets.data?.[0] ? String(wallets.data[0].id) : '' })
              }}>Nạp tiền</button>
            </div>
          </Card>
        ))}
      </div>

      {form && (
        <Modal title="Mục tiêu mới" onClose={() => setForm(null)}>
          <form onSubmit={submit} className="space-y-3">
            {formError && <ErrorNote message={formError} />}
            <Field label="Tên mục tiêu">
              <Input value={form.name} required maxLength={120} onChange={(event) => setForm({ ...form, name: event.target.value })} />
            </Field>
            <Field label="Số tiền mong muốn (VND)">
              <Input inputMode="numeric" value={String(form.targetAmount)} required
                onChange={(event) => setForm({ ...form, targetAmount: Number(event.target.value.replace(/[^0-9.]/g, '') || 0) })} />
            </Field>
            <Field label="Hạn hoàn thành (tuỳ chọn)">
              <Input type="date" value={form.deadline ?? ''} onChange={(event) => setForm({ ...form, deadline: event.target.value || null })} />
            </Field>
            <div className="flex justify-end gap-2 pt-2">
              <Button variant="ghost" onClick={() => setForm(null)}>Huỷ</Button>
              <Button type="submit" disabled={saving}>{saving ? 'Đang lưu…' : 'Lưu'}</Button>
            </div>
          </form>
        </Modal>
      )}

      {contributing && (
        <Modal title="Nạp vào mục tiêu" onClose={() => setContributing(null)}>
          <form onSubmit={contribute} className="space-y-3">
            {formError && <ErrorNote message={formError} />}
            <Field label="Số tiền (VND)">
              <Input inputMode="numeric" value={contributing.amount} required
                onChange={(event) => setContributing({ ...contributing, amount: event.target.value })} />
            </Field>
            <Field label="Trừ từ ví">
              <Select value={contributing.walletId} required onChange={(event) => setContributing({ ...contributing, walletId: event.target.value })}>
                <option value="">— chọn ví —</option>
                {(wallets.data ?? []).map((wallet) => <option key={wallet.id} value={wallet.id}>{wallet.name}</option>)}
              </Select>
            </Field>
            <div className="flex justify-end gap-2 pt-2">
              <Button variant="ghost" onClick={() => setContributing(null)}>Huỷ</Button>
              <Button type="submit" disabled={saving}>{saving ? 'Đang lưu…' : 'Nạp'}</Button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  )
}
