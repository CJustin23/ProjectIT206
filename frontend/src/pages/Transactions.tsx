import { useMemo, useState } from 'react'
import { categoryApi, transactionApi, walletApi } from '../lib/endpoints'
import { useFetch } from '../hooks/useFetch'
import { currentMonthKey, formatVnd, monthRange, todayIso } from '../lib/format'
import type { Transaction, TransactionPayload, TransactionType } from '../types/api'
import { Button, Card, Empty, ErrorNote, Field, Input, Loading, Modal, Select } from '../components/ui'

const TYPE_LABEL: Record<TransactionType, string> = { INCOME: 'Thu', EXPENSE: 'Chi', TRANSFER: 'Chuyển' }

const EMPTY_FORM = {
  type: 'EXPENSE' as TransactionType,
  amount: '',
  transactionDate: todayIso(),
  categoryId: '',
  walletId: '',
  transferWalletId: '',
  note: '',
}

export default function Transactions() {
  const [month, setMonth] = useState(currentMonthKey())
  const [type, setType] = useState<'' | TransactionType>('')
  const [page, setPage] = useState(0)
  const range = useMemo(() => monthRange(month), [month])

  const categories = useFetch(() => categoryApi.list(), [])
  const wallets = useFetch(() => walletApi.list(false), [])
  const list = useFetch(
    () => transactionApi.list({
      from: range.from, to: range.to,
      type: type || undefined,
      page, size: 20,
    }),
    [range.from, range.to, type, page],
  )

  const [form, setForm] = useState<typeof EMPTY_FORM | null>(null)
  const [editing, setEditing] = useState<Transaction | null>(null)
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  const categoryById = new Map((categories.data ?? []).map((item) => [item.id, item]))
  const walletById = new Map((wallets.data ?? []).map((item) => [item.id, item]))

  function openCreate() {
    setEditing(null)
    setFormError(null)
    setForm({ ...EMPTY_FORM, walletId: wallets.data?.[0] ? String(wallets.data[0].id) : '' })
  }

  function openEdit(row: Transaction) {
    setEditing(row)
    setFormError(null)
    setForm({
      type: row.type,
      amount: String(row.amount),
      transactionDate: row.transactionDate,
      categoryId: row.categoryId ? String(row.categoryId) : '',
      walletId: String(row.walletId),
      transferWalletId: row.transferWalletId ? String(row.transferWalletId) : '',
      note: row.note ?? '',
    })
  }

  async function submit(event: React.FormEvent) {
    event.preventDefault()
    if (!form) return
    const amount = Number(form.amount.replace(/[^0-9.]/g, ''))
    if (!(amount > 0)) return setFormError('Số tiền phải lớn hơn 0.')
    const payload: TransactionPayload = {
      type: form.type,
      amount,
      transactionDate: form.transactionDate,
      walletId: Number(form.walletId),
      categoryId: form.type === 'TRANSFER' || !form.categoryId ? null : Number(form.categoryId),
      transferWalletId: form.type === 'TRANSFER' && form.transferWalletId ? Number(form.transferWalletId) : null,
      note: form.note || null,
    }
    setSaving(true)
    setFormError(null)
    try {
      if (editing) await transactionApi.update(editing.id, payload, editing.version)
      else await transactionApi.create(payload)
      setForm(null)
      list.reload()
      wallets.reload()
    } catch (error) {
      setFormError(error instanceof Error ? error.message : 'Không lưu được giao dịch.')
    } finally {
      setSaving(false)
    }
  }

  async function remove(row: Transaction) {
    if (!window.confirm(`Xoá giao dịch ${formatVnd(row.amount)} ngày ${row.transactionDate}?`)) return
    try {
      await transactionApi.remove(row.id)
      list.reload()
    } catch (error) {
      window.alert(error instanceof Error ? error.message : 'Không xoá được giao dịch.')
    }
  }

  const rows = list.data?.content ?? []
  const totalPages = list.data ? Math.max(list.data.totalPages, 1) : 1

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <h1 className="text-lg font-semibold text-brand-700">Giao dịch</h1>
        <div className="flex flex-wrap items-end gap-2">
          <Field label="Tháng">
            <Input type="month" value={month} onChange={(event) => { setMonth(event.target.value); setPage(0) }} />
          </Field>
          <Field label="Loại">
            <Select value={type} onChange={(event) => { setType(event.target.value as '' | TransactionType); setPage(0) }}>
              <option value="">Tất cả</option>
              <option value="INCOME">Thu</option>
              <option value="EXPENSE">Chi</option>
              <option value="TRANSFER">Chuyển khoản</option>
            </Select>
          </Field>
          <Button onClick={openCreate}>+ Thêm giao dịch</Button>
        </div>
      </div>

      <Card>
        {list.error && <ErrorNote message={list.error} code={list.errorCode} />}
        {list.loading && !list.data && <Loading />}
        {!list.loading && rows.length === 0 && <Empty message="Không có giao dịch nào trong tháng này." />}
        {rows.length > 0 && (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="text-left text-xs uppercase text-gray-500">
                <tr>
                  <th className="py-2">Ngày</th>
                  <th>Loại</th>
                  <th>Danh mục</th>
                  <th>Ví</th>
                  <th className="text-right">Số tiền</th>
                  <th>Ghi chú</th>
                  <th />
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {rows.map((row) => (
                  <tr key={row.id}>
                    <td className="py-2 whitespace-nowrap">{row.transactionDate.split('-').reverse().join('/')}</td>
                    <td>{TYPE_LABEL[row.type]}</td>
                    <td>{row.categoryName ?? (row.categoryId ? categoryById.get(row.categoryId)?.name : '—') ?? '—'}</td>
                    <td>
                      {walletById.get(row.walletId)?.name ?? `#${row.walletId}`}
                      {row.transferWalletId ? ` → ${walletById.get(row.transferWalletId)?.name ?? `#${row.transferWalletId}`}` : ''}
                    </td>
                    <td className={`text-right font-medium ${row.type === 'EXPENSE' ? 'text-red-600' : row.type === 'INCOME' ? 'text-green-600' : ''}`}>
                      {formatVnd(row.amount)}
                    </td>
                    <td className="max-w-[16rem] truncate text-gray-500">{row.note ?? ''}</td>
                    <td className="space-x-2 whitespace-nowrap text-right">
                      <button type="button" className="text-brand-600 hover:underline" onClick={() => openEdit(row)}>Sửa</button>
                      <button type="button" className="text-red-600 hover:underline" onClick={() => remove(row)}>Xoá</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            <div className="mt-3 flex items-center justify-between text-sm text-gray-500">
              <span>Trang {page + 1}/{totalPages} · {list.data?.totalElements ?? 0} bản ghi</span>
              <span className="space-x-2">
                <button type="button" disabled={page === 0} className="rounded border px-2 py-1 disabled:opacity-40" onClick={() => setPage(page - 1)}>Trước</button>
                <button type="button" disabled={page + 1 >= totalPages} className="rounded border px-2 py-1 disabled:opacity-40" onClick={() => setPage(page + 1)}>Sau</button>
              </span>
            </div>
          </div>
        )}
      </Card>

      {form && (
        <Modal title={editing ? 'Sửa giao dịch' : 'Thêm giao dịch'} onClose={() => setForm(null)}>
          <form onSubmit={submit} className="space-y-3">
            {formError && <ErrorNote message={formError} />}
            <Field label="Loại giao dịch">
              <Select value={form.type} onChange={(event) => setForm({ ...form, type: event.target.value as TransactionType })}>
                <option value="EXPENSE">Chi</option>
                <option value="INCOME">Thu</option>
                <option value="TRANSFER">Chuyển khoản</option>
              </Select>
            </Field>
            <Field label="Số tiền (VND)">
              <Input inputMode="numeric" value={form.amount} required onChange={(event) => setForm({ ...form, amount: event.target.value })} />
            </Field>
            <Field label="Ngày">
              <Input type="date" value={form.transactionDate} required onChange={(event) => setForm({ ...form, transactionDate: event.target.value })} />
            </Field>
            {form.type !== 'TRANSFER' && (
              <Field label="Danh mục">
                <Select value={form.categoryId} required onChange={(event) => setForm({ ...form, categoryId: event.target.value })}>
                  <option value="">— chọn danh mục —</option>
                  {(categories.data ?? [])
                    .filter((item) => item.kind === form.type)
                    .map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}
                </Select>
              </Field>
            )}
            <Field label={form.type === 'TRANSFER' ? 'Ví gửi' : 'Ví'}>
              <Select value={form.walletId} required onChange={(event) => setForm({ ...form, walletId: event.target.value })}>
                <option value="">— chọn ví —</option>
                {(wallets.data ?? []).map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}
              </Select>
            </Field>
            {form.type === 'TRANSFER' && (
              <Field label="Ví nhận">
                <Select value={form.transferWalletId} required onChange={(event) => setForm({ ...form, transferWalletId: event.target.value })}>
                  <option value="">— chọn ví nhận —</option>
                  {(wallets.data ?? [])
                    .filter((item) => String(item.id) !== form.walletId)
                    .map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}
                </Select>
              </Field>
            )}
            <Field label="Ghi chú">
              <Input value={form.note} maxLength={255} onChange={(event) => setForm({ ...form, note: event.target.value })} />
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
