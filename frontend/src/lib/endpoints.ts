import { download, get, idempotencyKey, patch, post, put, remove, toPage } from './api'
import {
  budgetStatus,
  flattenCategories,
  normalizeAdminUsers,
  normalizeBudget,
  normalizeCategories,
  normalizeCategory,
  normalizeDashboard,
  normalizeFamily,
  normalizeGoal,
  normalizeMember,
  normalizeMonthly,
  normalizeTransaction,
  normalizeUser,
  normalizeWallet,
  normalizeWalletFlows,
  asList,
  asObject,
  toNum,
} from './normalize'
import type {
  AdminUserRow,
  Budget,
  BudgetPayload,
  Category,
  CategoryKind,
  CategoryPayload,
  ContributionPayload,
  DashboardSlice,
  Family,
  FamilyMember,
  GoalPayload,
  LoginPayload,
  LoginResponse,
  MeResponse,
  MonthlyReportRow,
  NotificationItem,
  Page,
  ReportSummary,
  Role,
  SavingsGoal,
  Transaction,
  TransactionFilter,
  TransactionPayload,
  User,
  Wallet,
  WalletFlowRow,
  WalletPayload,
} from '../types/api'
import { firstOfMonth, monthRange, ratioPercent } from './format'

/**
 * Toàn bộ lời gọi REST theo docs/api.md §2.
 * Mỗi hàm trả DTO đã normalize → UI không bao giờ gặp undefined/NaN.
 */

function qs(values: Record<string, string | number | boolean | null | undefined>): Record<string, string> {
  const out: Record<string, string> = {}
  Object.entries(values).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') return
    out[key] = String(value)
  })
  return out
}

/* ---------------- 2.1 xác thực ---------------- */

export const authApi = {
  async login(payload: LoginPayload): Promise<LoginResponse> {
    const raw = await post<unknown>('/auth/login', payload)
    return mapAuthResponse(raw)
  },
  async register(payload: LoginPayload & { fullName: string }): Promise<LoginResponse> {
    const raw = await post<unknown>('/auth/register', payload)
    return mapAuthResponse(raw)
  },
  async logout(): Promise<void> {
    await post<unknown>('/auth/logout').catch(() => undefined)
  },
  async me(): Promise<MeResponse> {
    const raw = asObject(await get<unknown>('/auth/me'))
    const user = normalizeUser(raw.user ?? raw)
    const families = normalizeFamilyList(raw.families ?? raw.memberships ?? [])
    const familyId = toNum(raw.familyId ?? raw.currentFamilyId, 0)
    const familyRaw = raw.family ?? raw.currentFamily ?? families.find((f) => f.id === familyId)
    const family = familyRaw ? normalizeFamily(familyRaw) : null
    return { user, family, families: family ? mergeFamily(families, family) : families }
  },
  async updateProfile(body: { fullName?: string; phone?: string; avatarUrl?: string; familyId?: number }): Promise<User> {
    const raw = await patch<unknown>('/users/me', body)
    return normalizeUser(raw)
  },
  async forgotPassword(email: string): Promise<void> {
    await post<unknown>('/auth/forgot-password', { email })
  },
}

function mergeFamily(list: Family[], family: Family): Family[] {
  return list.some((f) => f.id === family.id) ? list : [...list, family]
}

function normalizeFamilyList(raw: unknown): Family[] {
  return asList(raw).map((item) => normalizeFamily(item)).filter((f) => f.id > 0)
}

function mapAuthResponse(raw: unknown): LoginResponse {
  const body = asObject(raw)
  const accessToken = String(body.accessToken ?? body.access_token ?? body.token ?? '')
  const refreshToken = String(body.refreshToken ?? body.refresh_token ?? '')
  const user = normalizeUser(body.user ?? body)
  const families = normalizeFamilyList(body.families ?? body.memberships ?? [])
  const activeFamily = body.family ? normalizeFamily(body.family) : null
  return {
    accessToken,
    refreshToken: refreshToken || accessToken,
    user: activeFamily ? { ...user, role: user.role ?? activeFamily.myRole } : user,
    families: activeFamily ? mergeFamily(families, activeFamily) : families,
  }
}

/* ---------------- 2.2 hộ gia đình ---------------- */

function resolveFamilyId(explicit?: number | null): number {
  const id = explicit ?? readCurrentFamilyId()
  return id > 0 ? id : 0
}

/** tránh import store (vòng tròn): đọc familyId đang chọn do store ghi vào localStorage */
function readCurrentFamilyId(): number {
  try {
    return Number(window.localStorage.getItem('ffms_family_id') ?? 0)
  } catch {
    return 0
  }
}

export const familyApi = {
  async create(name: string): Promise<Family> {
    return normalizeFamily(await post<unknown>('/families', { name, baseCurrency: 'VND' }))
  },
  async get(familyId?: number | null): Promise<Family> {
    const id = resolveFamilyId(familyId)
    if (id <= 0) throw notReady('Chưa chọn hộ gia đình')
    return normalizeFamily(await get<unknown>(`/families/${id}`))
  },
  async members(familyId?: number | null): Promise<FamilyMember[]> {
    const id = resolveFamilyId(familyId)
    if (id <= 0) throw notReady('Chưa chọn hộ gia đình')
    const raw = await get<unknown>(`/families/${id}/members`)
    const body = asObject(raw)
    const list = Array.isArray(raw) ? asList(raw) : asList(body.content ?? body.members)
    return list.map((item) => normalizeMember(item))
  },
  async invite(email: string, role: Role, familyId?: number | null): Promise<void> {
    const id = resolveFamilyId(familyId)
    await post<unknown>(`/families/${id}/members/invite`, { email, role })
  },
  async updateRole(userId: number, role: Role, familyId?: number | null): Promise<void> {
    const id = resolveFamilyId(familyId)
    await patch<unknown>(`/families/${id}/members/${userId}`, { role })
  },
  async removeMember(userId: number, familyId?: number | null): Promise<void> {
    const id = resolveFamilyId(familyId)
    await remove<unknown>(`/families/${id}/members/${userId}`)
  },
}

/** báo hiệu "API/màn hình chưa khả dụng" cho UI hiển thị empty state thay vì crash */
export class BackendNotReady extends Error {
  constructor(reason: string) {
    super(reason)
    this.name = 'BackendNotReady'
  }
}

function notReady(reason: string): BackendNotReady {
  return new BackendNotReady(reason)
}

/* ---------------- 2.3 danh mục & ví ---------------- */

export const categoryApi = {
  async list(kind?: CategoryKind, familyId?: number | null): Promise<Category[]> {
    const id = resolveFamilyId(familyId)
    if (id <= 0) return []
    const raw = await get<unknown>(`/families/${id}/categories`, qs({ kind }))
    return normalizeCategories(raw, id)
  },
  async create(payload: CategoryPayload, familyId?: number | null): Promise<Category> {
    const id = resolveFamilyId(familyId)
    return normalizeCategory(await post<unknown>(`/families/${id}/categories`, payload), id)
  },
  async update(id: number, payload: Partial<CategoryPayload>, familyId?: number | null): Promise<Category> {
    const fid = resolveFamilyId(familyId)
    return normalizeCategory(await put<unknown>(`/families/${fid}/categories/${id}`, payload), fid)
  },
  async remove(id: number, familyId?: number | null): Promise<void> {
    const fid = resolveFamilyId(familyId)
    await remove<unknown>(`/families/${fid}/categories/${id}`)
  },
  /** danh mục lá dùng cho form giao dịch */
  leaves(tree: Category[]): Category[] {
    return flattenCategories(tree)
  },
}

export const walletApi = {
  async list(includeArchived = true, familyId?: number | null): Promise<Wallet[]> {
    const id = resolveFamilyId(familyId)
    if (id <= 0) return []
    const raw = await get<unknown>(`/families/${id}/wallets`)
    const list = Array.isArray(raw) ? asList(raw) : asList(asObject(raw).content)
    const wallets = list.map((item) => normalizeWallet(item, id))
    return includeArchived ? wallets : wallets.filter((w) => !w.isArchived)
  },
  async create(payload: WalletPayload, familyId?: number | null): Promise<Wallet> {
    const id = resolveFamilyId(familyId)
    return normalizeWallet(await post<unknown>(`/families/${id}/wallets`, payload), id)
  },
  async update(id: number, payload: Partial<WalletPayload>, familyId?: number | null): Promise<Wallet> {
    const fid = resolveFamilyId(familyId)
    return normalizeWallet(await patch<unknown>(`/families/${fid}/wallets/${id}`, payload), fid)
  },
  async setArchived(id: number, isArchived: boolean, familyId?: number | null): Promise<Wallet> {
    return walletApi.update(id, { isArchived }, familyId)
  },
}

/* ---------------- 2.4 giao dịch ---------------- */

export const transactionApi = {
  async list(filter: TransactionFilter, familyId?: number | null): Promise<Page<Transaction>> {
    const page = filter.page ?? 0
    const size = filter.size ?? 20
    const range = filter.month ? monthRange(filter.month) : null
    const params = qs({
      familyId: resolveFamilyId(familyId) || undefined,
      from: filter.from ?? range?.from,
      to: filter.to ?? range?.to,
      type: filter.type || undefined,
      categoryId: filter.categoryId === '' ? undefined : filter.categoryId,
      walletId: filter.walletId === '' ? undefined : filter.walletId,
      keyword: filter.keyword?.trim() || undefined,
      page,
      size,
      sort: 'transactionDate,desc',
    })
    const raw = await get<unknown>('/transactions', params)
    const mapped = toPage<unknown>(raw, { page, size })
    return {
      ...mapped,
      content: mapped.content.map((item) => normalizeTransaction(item)),
    }
  },
  async get(id: number): Promise<Transaction> {
    return normalizeTransaction(await get<unknown>(`/transactions/${id}`))
  },
  async create(payload: TransactionPayload): Promise<Transaction> {
    const raw = await post<unknown>('/transactions', payload, {
      headers: { 'Idempotency-Key': idempotencyKey() },
    })
    return normalizeTransaction(raw)
  },
  async update(id: number, payload: TransactionPayload, version?: number): Promise<Transaction> {
    const raw = await put<unknown>(`/transactions/${id}`, payload, {
      headers: version !== undefined ? { 'If-Match': String(version) } : undefined,
    })
    return normalizeTransaction(raw)
  },
  async remove(id: number): Promise<void> {
    await remove<unknown>(`/transactions/${id}`)
  },
  async exportCsv(from: string, to: string): Promise<void> {
    await download('/transactions/export', { from, to, format: 'csv' }, `ffms-giao-dịch-${from}_${to}.csv`)
  },
}

/* ---------------- 2.5 ngân sách & mục tiêu ---------------- */

export const budgetApi = {
  async list(periodStart: string, familyId?: number | null): Promise<Budget[]> {
    const params = qs({ periodStart, familyId: resolveFamilyId(familyId) || undefined })
    const raw = await get<unknown>('/budgets', params)
    const list = Array.isArray(raw) ? asList(raw) : asList(asObject(raw).content ?? asObject(raw).budgets)
    return list.map((item) => {
      const budget = normalizeBudget(item, periodStart)
      if (!Number.isFinite(budget.percent)) {
        const percent = ratioPercent(budget.used, budget.limitAmount)
        return { ...budget, percent, status: budgetStatus(percent) }
      }
      return budget
    })
  },
  /** PUT upsert theo (family, category, periodStart) + If-Match khi sửa (api.md §2.5) */
  async upsert(payload: BudgetPayload, version?: number): Promise<Budget> {
    const raw = await put<unknown>('/budgets', payload, {
      headers: version !== undefined && version > 0 ? { 'If-Match': String(version) } : undefined,
    })
    return normalizeBudget(raw, payload.periodStart)
  },
  async remove(id: number): Promise<void> {
    await remove<unknown>(`/budgets/${id}`)
  },
}

export const goalApi = {
  async list(status?: 'ACTIVE' | 'COMPLETED' | 'CANCELLED', familyId?: number | null): Promise<SavingsGoal[]> {
    const params = qs({ status, familyId: resolveFamilyId(familyId) || undefined })
    const raw = await get<unknown>('/goals', params)
    const list = Array.isArray(raw) ? asList(raw) : asList(asObject(raw).content ?? asObject(raw).goals)
    return list.map((item) => normalizeGoal(item))
  },
  async create(payload: GoalPayload, familyId?: number | null): Promise<SavingsGoal> {
    const raw = await post<unknown>('/goals', { ...payload, familyId: resolveFamilyId(familyId) || undefined })
    return normalizeGoal(raw)
  },
  async update(id: number, payload: Partial<GoalPayload>): Promise<SavingsGoal> {
    return normalizeGoal(await patch<unknown>(`/goals/${id}`, payload))
  },
  async remove(id: number): Promise<void> {
    await remove<unknown>(`/goals/${id}`)
  },
  async contribute(id: number, payload: ContributionPayload): Promise<SavingsGoal> {
    const raw = await post<unknown>(`/goals/${id}/contributions`, payload)
    const body = asObject(raw)
    if (body.id !== undefined || body.goalId !== undefined) return normalizeGoal(raw)
    return goalApi.get(id)
  },
  async get(id: number): Promise<SavingsGoal> {
    return normalizeGoal(await get<unknown>(`/goals/${id}`))
  },
}

/* ---------------- 2.6 báo cáo ---------------- */

export const reportApi = {
  async dashboard(month: string): Promise<ReportSummary> {
    const raw = await get<unknown>('/reports/dashboard', qs({ month, familyId: resolveFamilyId() || undefined }))
    return normalizeDashboard(raw, month)
  },
  async monthly(year: number): Promise<MonthlyReportRow[]> {
    const raw = await get<unknown>('/reports/monthly', qs({ year, familyId: resolveFamilyId() || undefined }))
    return normalizeMonthly(raw)
  },
  async byCategory(from: string, to: string, kind: CategoryKind = 'EXPENSE'): Promise<DashboardSlice[]> {
    const raw = await get<unknown>('/reports/by-category', qs({ from, to, kind, familyId: resolveFamilyId() || undefined }))
    const body = asObject(raw)
    const list = Array.isArray(raw) ? asList(raw) : asList(body.content ?? body.rows ?? body.categories)
    const rows = list.map((item) => {
      const o = asObject(item)
      return {
        id: o.categoryId === undefined ? (o.id === undefined ? null : toNum(o.id)) : toNum(o.categoryId),
        name: String(o.name ?? o.categoryName ?? o.label ?? 'Khác'),
        amount: Math.abs(toNum(o.amount ?? o.total ?? o.spent ?? o.value)),
        percent: toNum(o.percent ?? o.ratio, Number.NaN),
        color: o.color === undefined ? null : String(o.color),
      } satisfies DashboardSlice
    })
    const total = rows.reduce((sum, row) => sum + row.amount, 0)
    return rows.map((row) => ({
      ...row,
      percent: Number.isFinite(row.percent) ? row.percent : ratioPercent(row.amount, total),
    }))
  },
  async byWallet(from: string, to: string): Promise<WalletFlowRow[]> {
    const raw = await get<unknown>('/reports/by-wallet', qs({ from, to, familyId: resolveFamilyId() || undefined }))
    return normalizeWalletFlows(raw)
  },
}

export const notificationApi = {
  async list(unread = true): Promise<NotificationItem[]> {
    const raw = await get<unknown>('/notifications', qs({ unread }))
    const list = Array.isArray(raw) ? asList(raw) : asList(asObject(raw).content)
    return list.map((item) => {
      const o = asObject(item)
      return {
        id: toNum(o.id),
        type: String(o.type ?? 'BUDGET_ALERT'),
        message: String(o.message ?? o.content ?? 'Cảnh báo ngân sách'),
        readAt: o.readAt === undefined || o.readAt === null ? null : String(o.readAt),
        createdAt: o.createdAt === undefined || o.createdAt === null ? null : String(o.createdAt),
      } satisfies NotificationItem
    })
  },
}

/* ---------------- 2.7 quản trị ---------------- */

export const adminApi = {
  async users(keyword: string, page = 0, size = 20): Promise<Page<AdminUserRow>> {
    const raw = await get<unknown>('/admin/users', qs({ keyword: keyword.trim() || undefined, page, size }))
    const mapped = toPage<unknown>(raw, { page, size })
    return { ...mapped, content: normalizeAdminUsers(mapped.content.length > 0 ? mapped.content : raw) }
  },
  async setActive(userId: number, active: boolean): Promise<void> {
    await patch<unknown>(`/admin/users/${userId}/status`, { active, status: active ? 'ACTIVE' : 'LOCKED' })
  },
}

/** tiện dụng cho form: tháng hiện tại dạng yyyy-MM-01 */
export function periodStartOf(monthKey: string): string {
  return firstOfMonth(monthKey)
}
