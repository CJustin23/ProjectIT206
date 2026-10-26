import { useState } from 'react'
import { familyApi } from '../lib/endpoints'
import { useFetch } from '../hooks/useFetch'
import { canManageFamily, useAuthStore } from '../store/auth'
import type { Role } from '../types/api'
import { Button, Card, Empty, ErrorNote, Field, Input, Loading, Select } from '../components/ui'

const ROLES: Role[] = ['OWNER', 'ADMIN', 'MEMBER', 'VIEWER']
const ROLE_LABEL: Record<Role, string> = { OWNER: 'Chủ hộ', ADMIN: 'Quản trị', MEMBER: 'Thành viên', VIEWER: 'Chỉ xem' }

export default function Family() {
  const family = useAuthStore((state) => state.family)
  const me = useAuthStore((state) => state.user)
  const isManager = canManageFamily(family?.myRole)
  const members = useFetch(() => familyApi.members(), [])

  const [invite, setInvite] = useState({ email: '', role: 'MEMBER' as Role })
  const [message, setMessage] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function onInvite(event: React.FormEvent) {
    event.preventDefault()
    setBusy(true)
    setMessage(null)
    try {
      await familyApi.invite(invite.email, invite.role)
      setMessage(`Đã thêm ${invite.email} vào hộ.`)
      setInvite({ email: '', role: 'MEMBER' })
      members.reload()
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Không mời được thành viên.')
    } finally {
      setBusy(false)
    }
  }

  async function changeRole(userId: number, role: Role) {
    try {
      await familyApi.updateRole(userId, role)
      members.reload()
    } catch (error) {
      window.alert(error instanceof Error ? error.message : 'Không đổi được vai trò.')
    }
  }

  async function removeMember(userId: number) {
    if (!window.confirm('Gỡ thành viên khỏi hộ gia đình?')) return
    await familyApi.removeMember(userId)
    members.reload()
  }

  const rows = members.data ?? []

  return (
    <div className="space-y-4">
      <h1 className="text-lg font-semibold text-brand-700">Hộ gia đình · {family?.name ?? '—'}</h1>
      {members.error && <ErrorNote message={members.error} code={members.errorCode} />}
      {message && <p className="rounded-md bg-blue-50 p-3 text-sm text-blue-700">{message}</p>}

      <Card title="Thành viên">
        {members.loading && rows.length === 0 && <Loading />}
        {!members.loading && rows.length === 0 && <Empty message="Chưa tải được danh sách thành viên." />}
        <ul className="divide-y divide-gray-100 text-sm">
          {rows.map((row) => (
            <li key={row.userId} className="flex flex-wrap items-center justify-between gap-2 py-2">
              <span>
                {row.fullName} <span className="text-gray-500">· {row.email}</span>
                {me?.id === row.userId && <span className="ml-2 text-xs text-brand-600">bạn</span>}
              </span>
              {isManager && row.role !== 'OWNER' ? (
                <span className="flex items-center gap-2">
                  <Select value={row.role} className="w-40" onChange={(event) => changeRole(row.userId, event.target.value as Role)}>
                    {ROLES.filter((role) => role !== 'OWNER').map((role) => <option key={role} value={role}>{ROLE_LABEL[role]}</option>)}
                  </Select>
                  <button type="button" className="text-red-600 hover:underline" onClick={() => removeMember(row.userId)}>Gỡ</button>
                </span>
              ) : (
                <span className="text-gray-500">{ROLE_LABEL[row.role]}</span>
              )}
            </li>
          ))}
        </ul>
      </Card>

      {isManager && (
        <Card title="Thêm thành viên">
          <form onSubmit={onInvite} className="flex flex-wrap items-end gap-3">
            <div className="min-w-56 flex-1">
              <Field label="Email đã đăng ký">
                <Input type="email" value={invite.email} required placeholder="thanvien@email.vn"
                  onChange={(event) => setInvite({ ...invite, email: event.target.value })} />
              </Field>
            </div>
            <Field label="Vai trò">
              <Select value={invite.role} onChange={(event) => setInvite({ ...invite, role: event.target.value as Role })}>
                {ROLES.filter((role) => role !== 'OWNER').map((role) => <option key={role} value={role}>{ROLE_LABEL[role]}</option>)}
              </Select>
            </Field>
            <Button type="submit" disabled={busy}>{busy ? 'Đang thêm…' : 'Thêm'}</Button>
          </form>
        </Card>
      )}
    </div>
  )
}
