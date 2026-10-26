import { useState } from 'react'
import { categoryApi } from '../lib/endpoints'
import { useFetch } from '../hooks/useFetch'
import { useAuthStore } from '../store/auth'
import { canManageFamily } from '../store/auth'
import type { CategoryKind, CategoryPayload } from '../types/api'
import { Button, Card, Empty, ErrorNote, Field, Input, Loading, Modal, Select } from '../components/ui'

export default function Categories() {
  const role = useAuthStore((state) => state.family?.myRole)
  const canEdit = canManageFamily(role)
  const list = useFetch(() => categoryApi.list(), [])
  const [form, setForm] = useState<CategoryPayload | null>(null)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  async function submit(event: React.FormEvent) {
    event.preventDefault()
    if (!form) return
    setSaving(true)
    setFormError(null)
    try {
      if (editingId) await categoryApi.update(editingId, form)
      else await categoryApi.create(form)
      setForm(null)
      list.reload()
    } catch (error) {
      setFormError(error instanceof Error ? error.message : 'Không lưu được danh mục.')
    } finally {
      setSaving(false)
    }
  }

  async function remove(id: number) {
    if (!window.confirm('Xoá danh mục này?')) return
    try {
      await categoryApi.remove(id)
      list.reload()
    } catch (error) {
      window.alert(error instanceof Error ? error.message : 'Không xoá được danh mục.')
    }
  }

  const groups: CategoryKind[] = ['EXPENSE', 'INCOME']

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-lg font-semibold text-brand-700">Danh mục thu chi</h1>
        {canEdit && (
          <Button onClick={() => {
            setEditingId(null)
            setFormError(null)
            setForm({ name: '', kind: 'EXPENSE', parentId: null, icon: '', color: '#1A365D' })
          }}>+ Thêm danh mục</Button>
        )}
      </div>

      {list.error && <ErrorNote message={list.error} code={list.errorCode} />}
      {list.loading && !list.data && <Loading />}

      {groups.map((kind) => {
        const items = (list.data ?? []).filter((item) => item.kind === kind)
        return (
          <Card key={kind} title={kind === 'EXPENSE' ? 'Danh mục chi' : 'Danh mục thu'}>
            {items.length === 0
              ? <Empty message="Chưa có danh mục nào." />
              : (
                <ul className="divide-y divide-gray-100 text-sm">
                  {items.map((item) => (
                    <li key={item.id} className="flex items-center justify-between py-2">
                      <span className="flex items-center gap-2">
                        <span className="inline-block h-3 w-3 rounded-full" style={{ background: item.color ?? '#CBD5E0' }} />
                        {item.icon ? <span aria-hidden>{item.icon}</span> : null}
                        {item.name}
                        {item.isSystem && <span className="rounded bg-gray-100 px-1 text-xs text-gray-500">hệ thống</span>}
                      </span>
                      {canEdit && !item.isSystem && (
                        <span className="space-x-2">
                          <button type="button" className="text-brand-600 hover:underline" onClick={() => {
                            setEditingId(item.id)
                            setForm({ name: item.name, kind: item.kind, parentId: item.parentId, icon: item.icon ?? '', color: item.color ?? '' })
                          }}>Sửa</button>
                          <button type="button" className="text-red-600 hover:underline" onClick={() => remove(item.id)}>Xoá</button>
                        </span>
                      )}
                    </li>
                  ))}
                </ul>
              )}
          </Card>
        )
      })}

      {form && (
        <Modal title={editingId ? 'Sửa danh mục' : 'Thêm danh mục'} onClose={() => setForm(null)}>
          <form onSubmit={submit} className="space-y-3">
            {formError && <ErrorNote message={formError} />}
            <Field label="Tên danh mục">
              <Input value={form.name} required maxLength={100} onChange={(event) => setForm({ ...form, name: event.target.value })} />
            </Field>
            <Field label="Loại">
              <Select value={form.kind} onChange={(event) => setForm({ ...form, kind: event.target.value as CategoryKind })}>
                <option value="EXPENSE">Chi</option>
                <option value="INCOME">Thu</option>
              </Select>
            </Field>
            <div className="grid grid-cols-2 gap-3">
              <Field label="Biểu tượng">
                <Input value={form.icon ?? ''} maxLength={4} onChange={(event) => setForm({ ...form, icon: event.target.value })} placeholder="🍜" />
              </Field>
              <Field label="Màu">
                <Input type="color" value={form.color || '#1A365D'} onChange={(event) => setForm({ ...form, color: event.target.value })} />
              </Field>
            </div>
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
