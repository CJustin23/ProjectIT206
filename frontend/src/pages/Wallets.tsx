import { useState } from 'react'
import { walletApi } from '../lib/endpoints'
import { useFetch } from '../hooks/useFetch'
import { formatVnd } from '../lib/format'
import { canManageFamily, useAuthStore } from '../store/auth'
import type { WalletPayload, WalletType } from '../types/api'
import { Button, Card, Empty, ErrorNote, Field, Input, Loading, Modal, Select } from '../components/ui'

const TYPES: { value: WalletType; label: string }[] = [
  { value: 'CASH', label: 'Tiền mặt' },
  { value: 'BANK', label: 'Ngân hàng' },
  { value: 'E_WALLET', label: 'Ví điện tử' },
  { value: 'CREDIT_CARD', label: 'Tín dụng' },
  { value: 'SAVINGS', label: 'Tiết kiệm' },
]

export default function Wallets() {
  const role = useAuthStore((state) => state.family?.myRole)
  const canEdit = canManageFamily(role)
  const list = useFetch(() => walletApi.list(true), [])
  const [form, setForm] = useState<WalletPayload | null>(null)
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  async function submit(event: React.FormEvent) {
    event.preventDefault()
    if (!form) return
    setSaving(true)
    setFormError(null)
    try {
      await walletApi.create(form)
      setForm(null)
      list.reload()
    } catch (error) {
      setFormError(error instanceof Error ? error.message : 'Không tạo được ví.')
    } finally {
      setSaving(false)
    }
  }

  async function toggleArchive(id: number, archived: boolean) {
    await walletApi.setArchived(id, !archived)
    list.reload()
  }

  const rows = list.data ?? []

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-lg font-semibold text-brand-700">Ví &amp; thẻ</h1>
        {canEdit && (
          <Button onClick={() => {
            setFormError(null)
            setForm({ name: '', type: 'CASH', openingBalance: 0 })
          }}>+ Thêm ví</Button>
        )}
      </div>

      <Card>
        {list.error && <ErrorNote message={list.error} code={list.errorCode} />}
        {list.loading && !list.data && <Loading />}
        {!list.loading && rows.length === 0 && <Empty message="Chưa có ví nào. Thêm ví để ghi nhận giao dịch." />}
        <ul className="divide-y divide-gray-100 text-sm">
          {rows.map((wallet) => (
            <li key={wallet.id} className="flex flex-wrap items-center justify-between gap-2 py-3">
              <span>
                <span className={`font-medium ${wallet.isArchived ? 'text-gray-400 line-through' : ''}`}>{wallet.name}</span>
                <span className="ml-2 text-xs text-gray-500">
                  {TYPES.find((item) => item.value === wallet.type)?.label ?? wallet.type} · dư {formatVnd(wallet.balance)}
                </span>
              </span>
              {canEdit && (
                <button type="button" className="text-brand-600 hover:underline" onClick={() => toggleArchive(wallet.id, wallet.isArchived)}>
                  {wallet.isArchived ? 'Bỏ ẩn' : 'Ẩn'}
                </button>
              )}
            </li>
          ))}
        </ul>
      </Card>

      {form && (
        <Modal title="Thêm ví" onClose={() => setForm(null)}>
          <form onSubmit={submit} className="space-y-3">
            {formError && <ErrorNote message={formError} />}
            <Field label="Tên ví">
              <Input value={form.name} required maxLength={100} onChange={(event) => setForm({ ...form, name: event.target.value })} />
            </Field>
            <Field label="Loại">
              <Select value={form.type} onChange={(event) => setForm({ ...form, type: event.target.value as WalletType })}>
                {TYPES.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}
              </Select>
            </Field>
            <Field label="Số dư ban đầu (VND)">
              <Input inputMode="numeric" value={String(form.openingBalance)} required
                onChange={(event) => setForm({ ...form, openingBalance: Number(event.target.value.replace(/[^0-9.]/g, '') || 0) })} />
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
