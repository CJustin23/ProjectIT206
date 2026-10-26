import type {
  AdminUserRow,
  Budget,
  BudgetAlert,
  BudgetStatus,
  Category,
  CategoryKind,
  DashboardSlice,
  Family,
  FamilyMember,
  GoalContribution,
  GoalStatus,
  MonthlyReportRow,
  ReportSummary,
  Role,
  SavingsGoal,
  Transaction,
  TransactionType,
  TrendPoint,
  User,
  Wallet,
  WalletFlowRow,
  WalletType,
} from '../types/api'
import { clampPercent, ratioPercent, toSafeNumber } from './format'

/**
 * Backend được dựng song song nên DTO có thể thiếu field / đổi tên / trả null.
 * Mọi hàm normalize ở đây phải: (1) không throw, (2) không sinh NaN, (3) trung tính với
 * cả kiểu snake_case lẫn camelCase.
 */

type Loose = Record<string, unknown>

export function asObject(value: unknown): Loose {
  if (value && typeof value === 'object' && !Array.isArray(value)) return value as Loose
  return {}
}

export function asList(value: unknown): unknown[] {
  return Array.isArray(value) ? value : []
}

function pick(obj: Loose, keys: string[]): unknown {
  for (const key of keys) {
    const value = obj[key]
    if (value !== undefined && value !== null) return value
  }
  return undefined
}

export function toStr(value: unknown, fallback = ''): string {
  if (typeof value === 'string') return value
  if (typeof value === 'number' && Number.isFinite(value)) return String(value)
  return fallback
}

export function toNum(value: unknown, fallback = 0): number {
  if (typeof value === 'number') return Number.isFinite(value) ? value : fallback
  if (typeof value === 'string') {
    const n = Number(value.replace(/,/g, '.'))
    return Number.isFinite(n) ? n : fallback
  }
  return fallback
}

export function toBool(value: unknown, fallback = false): boolean {
  if (typeof value === 'boolean') return value
  if (typeof value === 'number') return value !== 0
  if (typeof value === 'string') return ['true', '1', 'yes'].includes(value.toLowerCase())
  return fallback
}

function toOptStr(value: unknown): string | null {
  const s = toStr(value)
  return s === '' ? null : s
}

function toOptNum(value: unknown): number | null {
  if (value === undefined || value === null) return null
  const n = toNum(value, Number.NaN)
  return Number.isFinite(n) ? n : null
}

/** yyyy-MM-dd từ 'yyyy-MM-dd', ISO datetime hoặc Date-ish string */
function toDay(value: unknown, fallback = ''): string {
  const raw = toStr(value, fallback)
  if (!raw) return fallback
  const match = /^(\d{4})-(\d{2})-(\d{2})/.exec(raw)
  return match ? `${match[1]}-${match[2]}-${match[3]}` : raw
}

const ROLES: Role[] = ['OWNER', 'ADMIN', 'MEMBER', 'VIEWER']
const KINDS: CategoryKind[] = ['INCOME', 'EXPENSE']
const TX_TYPES: TransactionType[] = ['INCOME', 'EXPENSE', 'TRANSFER']
const WALLET_TYPES: WalletType[] = ['CASH', 'BANK', 'E_WALLET', 'CREDIT_CARD', 'SAVINGS']
const GOAL_STATUSES: GoalStatus[] = ['ACTIVE', 'COMPLETED', 'CANCELLED']

function enumOf<T extends string>(value: unknown, allowed: T[], fallback: T): T {
  const raw = toStr(value).toUpperCase()
  return (allowed as string[]).includes(raw) ? (raw as T) : fallback
}

export function normalizeUser(raw: unknown): User {
  const o = asObject(raw)
  const nested = asObject(o.user ?? o.profile ?? o.account)
  const merged = { ...nested, ...o }
  return {
    id: toNum(pick(merged, ['id', 'userId'])),
    email: toStr(pick(merged, ['email', 'username'])),
    fullName: toStr(pick(merged, ['fullName', 'fullname', 'name', 'displayName']), 'Người dùng'),
    phone: toOptStr(pick(merged, ['phone', 'phoneNumber'])),
    avatarUrl: toOptStr(pick(merged, ['avatarUrl', 'avatar'])),
    isAdmin: toBool(pick(merged, ['isAdmin', 'admin'])),
    role: merged.role === undefined && merged.myRole === undefined ? null : enumOf(pick(merged, ['role', 'myRole']), ROLES, 'MEMBER'),
  }
}

export function normalizeFamily(raw: unknown): Family {
  const o = asObject(raw)
  return {
    id: toNum(pick(o, ['id', 'familyId'])),
    name: toStr(pick(o, ['name', 'familyName']), 'Hộ gia đình'),
    baseCurrency: toStr(pick(o, ['baseCurrency', 'currency']), 'VND'),
    fiscalMonthStartDay: toNum(pick(o, ['fiscalMonthStartDay', 'fiscalStartDay']), 1),
    myRole: o.myRole === undefined && o.role === undefined ? null : enumOf(pick(o, ['myRole', 'role']), ROLES, 'MEMBER'),
    memberCount: toNum(pick(o, ['memberCount', 'totalMembers', 'members']), 0),
  }
}

export function normalizeMember(raw: unknown): FamilyMember {
  const o = asObject(raw)
  const user = asObject(pick(o, ['user', 'member', 'account']))
  return {
    id: toNum(pick(o, ['id', 'membershipId'])),
    userId: toNum(pick(o, ['userId']) ?? pick(user, ['id']), 0),
    email: toStr(pick(o, ['email']) ?? pick(user, ['email'])),
    fullName: toStr(pick(o, ['fullName', 'name']) ?? pick(user, ['fullName', 'name']), 'Thành viên'),
    role: enumOf(pick(o, ['role', 'familyRole']), ROLES, 'MEMBER'),
    joinedAt: toOptStr(toDay(pick(o, ['joinedAt', 'createdAt']))),
    active: toBool(pick(o, ['active', 'isEnabled']), true),
  }
}

export function normalizeCategory(raw: unknown, familyId = 0): Category {
  const o = asObject(raw)
  const children = asList(pick(o, ['children', 'subCategories', 'childs'])).map((c) => normalizeCategory(c, familyId))
  return {
    id: toNum(pick(o, ['id', 'categoryId'])),
    familyId: toNum(pick(o, ['familyId']), familyId),
    name: toStr(pick(o, ['name', 'label']), 'Danh mục'),
    kind: enumOf(pick(o, ['kind', 'type']), KINDS, 'EXPENSE'),
    parentId: toOptNum(pick(o, ['parentId', 'parent'])),
    icon: toOptStr(pick(o, ['icon'])),
    color: toOptStr(pick(o, ['color'])),
    isSystem: toBool(pick(o, ['isSystem', 'system']) ?? o.is_system),
    hidden: toBool(pick(o, ['hidden', 'isHidden', 'archived'])),
    children,
  }
}

/** trả về cây 2 cấp; mảng phẳng có parentId sẽ được ráp con */
export function normalizeCategories(raw: unknown, familyId = 0): Category[] {
  const source = Array.isArray(raw)
    ? asList(raw)
    : asList(pick(asObject(raw), ['categories', 'content', '_embedded', 'items', 'data']))
  const flat = source.map((item) => normalizeCategory(item, familyId))
  const byId = new Map<number, Category>()
  flat.forEach((c) => byId.set(c.id, c))

  const roots: Category[] = []
  flat.forEach((c) => {
    if (c.parentId !== null && c.parentId !== c.id) {
      const parent = byId.get(c.parentId)
      if (parent) {
        if (!parent.children.some((child) => child.id === c.id)) parent.children.push(c)
        return
      }
    }
    if (!roots.some((r) => r.id === c.id)) roots.push(c)
  })
  return roots
}

export function flattenCategories(tree: Category[]): Category[] {
  const out: Category[] = []
  const walk = (nodes: Category[]): void => {
    nodes.forEach((node) => {
      out.push(node)
      walk(node.children)
    })
  }
  walk(tree)
  return out
}

export function normalizeWallet(raw: unknown, familyId = 0): Wallet {
  const o = asObject(raw)
  const opening = toNum(pick(o, ['openingBalance', 'initialBalance']) ?? o.opening_balance)
  return {
    id: toNum(pick(o, ['id', 'walletId'])),
    familyId: toNum(pick(o, ['familyId']), familyId),
    name: toStr(pick(o, ['name']), 'Ví'),
    type: enumOf(pick(o, ['type', 'walletType']), WALLET_TYPES, 'CASH'),
    openingBalance: opening,
    balance: toNum(pick(o, ['balance', 'currentBalance', 'computedBalance']), opening),
    isArchived: toBool(pick(o, ['isArchived', 'archived']) ?? o.is_archived),
  }
}

export function normalizeTransaction(raw: unknown): Transaction {
  const o = asObject(raw)
  const createdBy = asObject(pick(o, ['createdBy', 'creator', 'author']))
  const transferRaw = pick(o, ['transferWalletId', 'toWalletId', 'targetWalletId'])
  return {
    id: toNum(pick(o, ['id', 'transactionId'])),
    familyId: toNum(pick(o, ['familyId'])),
    walletId: toNum(pick(o, ['walletId', 'fromWalletId'])),
    categoryId: toOptNum(pick(o, ['categoryId'])),
    transferWalletId: toOptNum(transferRaw),
    type: enumOf(pick(o, ['type', 'transactionType']), TX_TYPES, 'EXPENSE'),
    amount: Math.abs(toNum(pick(o, ['amount']))),
    transactionDate: toDay(pick(o, ['transactionDate', 'date'])),
    note: toOptStr(pick(o, ['note', 'description'])),
    createdBy: createdBy.id !== undefined && pick(createdBy, ['id']) !== undefined
      ? { id: toNum(pick(createdBy, ['id', 'userId'])), fullName: toStr(pick(createdBy, ['fullName', 'name']), '—') }
      : toStr(pick(o, ['createdByName']))
        ? { id: 0, fullName: toStr(pick(o, ['createdByName'])) }
        : null,
    walletBalanceAfter: toOptNum(pick(o, ['walletBalanceAfter', 'balanceAfter'])),
    createdAt: toOptStr(pick(o, ['createdAt'])),
    version: toNum(pick(o, ['version']), 0),
    walletName: toOptStr(pick(o, ['walletName', 'wallet'])),
    transferWalletName: toOptStr(pick(o, ['transferWalletName', 'toWalletName'])),
    categoryName: toOptStr(pick(o, ['categoryName', 'category'])),
    isRecurring: toBool(pick(o, ['isRecurring', 'recurring']) ?? o.is_recurring),
  }
}

export function budgetStatus(percent: number): BudgetStatus {
  if (percent >= 100) return 'OVER'
  if (percent >= 80) return 'WARNING'
  return 'SAFE'
}

export function normalizeBudget(raw: unknown, fallbackPeriodStart = ''): Budget {
  const o = asObject(raw)
  const limit = Math.abs(toNum(pick(o, ['limitAmount', 'limit', 'amountLimit', 'budgetAmount', 'amount'])))
  const used = Math.abs(toNum(pick(o, ['used', 'usedAmount', 'spent', 'actualAmount', 'expenseAmount'])))
  const rawPercent = toOptNum(pick(o, ['percent', 'usagePercent', 'usedPercent']))
  const percent = rawPercent !== null && Number.isFinite(rawPercent) ? rawPercent : ratioPercent(used, limit)
  const statusRaw = toStr(pick(o, ['status', 'state'])).toUpperCase()
  const periodStart = toDay(pick(o, ['periodStart', 'start', 'from']), fallbackPeriodStart)
  return {
    id: toNum(pick(o, ['id', 'budgetId'])),
    familyId: toNum(pick(o, ['familyId'])),
    categoryId: toNum(pick(o, ['categoryId'])),
    categoryName: toOptStr(pick(o, ['categoryName', 'category'])),
    periodStart,
    periodEnd: toDay(pick(o, ['periodEnd', 'end', 'to']), periodStart),
    limitAmount: limit,
    used,
    percent: Number.isFinite(percent) ? Math.max(0, percent) : 0,
    status: (['SAFE', 'WARNING', 'OVER'] as string[]).includes(statusRaw)
      ? (statusRaw as BudgetStatus)
      : budgetStatus(percent),
    alertThresholdPercent: toNum(pick(o, ['alertThresholdPercent', 'alertThreshold']), 80),
    version: toNum(pick(o, ['version']), 0),
  }
}

function normalizeContribution(raw: unknown, goalId: number): GoalContribution {
  const o = asObject(raw)
  return {
    id: toNum(pick(o, ['id', 'contributionId'])),
    goalId: toNum(pick(o, ['goalId']), goalId),
    walletId: toOptNum(pick(o, ['walletId'])),
    amount: Math.abs(toNum(pick(o, ['amount', 'contributed']))),
    contributedOn: toDay(pick(o, ['contributedOn', 'date', 'createdAt'])),
  }
}

export function normalizeGoal(raw: unknown): SavingsGoal {
  const o = asObject(raw)
  const target = Math.abs(toNum(pick(o, ['targetAmount', 'target'])))
  const saved = Math.abs(toNum(pick(o, ['savedAmount', 'currentAmount', 'saved', 'progressAmount'])))
  const rawPercent = toOptNum(pick(o, ['percent', 'progressPercent', 'completionPercent']))
  const contributions = asList(pick(o, ['contributions', 'history'])).map((c) => normalizeContribution(c, toNum(pick(o, ['id', 'goalId']))))
  return {
    id: toNum(pick(o, ['id', 'goalId'])),
    familyId: toNum(pick(o, ['familyId'])),
    name: toStr(pick(o, ['name', 'title']), 'Mục tiêu'),
    targetAmount: target,
    savedAmount: saved || contributions.reduce((sum, c) => sum + c.amount, 0),
    deadline: toOptStr(toDay(pick(o, ['deadline', 'targetDate', 'dueDate']))),
    status: enumOf(pick(o, ['status', 'state']), GOAL_STATUSES, 'ACTIVE'),
    percent: clampPercent(rawPercent !== null ? rawPercent : ratioPercent(saved, target)),
    contributions,
  }
}

function normalizeTrend(raw: unknown, index: number): TrendPoint {
  const o = asObject(raw)
  const income = Math.abs(toNum(pick(o, ['income', 'totalIncome', 'in'])))
  const expense = Math.abs(toNum(pick(o, ['expense', 'totalExpense', 'out'])))
  const rawNet = toOptNum(pick(o, ['net', 'balance', 'surplus']))
  return {
    month: toStr(pick(o, ['month', 'period', 'label', 'yyyymm']), `#${index + 1}`),
    income,
    expense,
    net: rawNet !== null ? rawNet : income - expense,
  }
}

export function normalizeMonthly(raw: unknown): MonthlyReportRow[] {
  const list = Array.isArray(raw) ? asList(raw) : asList(pick(asObject(raw), ['months', 'rows', 'content', 'data', 'items']))
  return list.map((row, i) => normalizeTrend(row, i))
}

function normalizeSlice(raw: unknown): DashboardSlice {
  const o = asObject(raw)
  const amount = Math.abs(toNum(pick(o, ['amount', 'total', 'spent', 'value', 'sum'])))
  return {
    id: toOptNum(pick(o, ['categoryId', 'id'])),
    name: toStr(pick(o, ['name', 'categoryName', 'label', 'key']), 'Khác'),
    amount,
    percent: toNum(pick(o, ['percent', 'ratio'])),
    color: toOptStr(pick(o, ['color'])),
  }
}

function normalizeAlert(raw: unknown): BudgetAlert {
  const o = asObject(raw)
  const limit = Math.abs(toNum(pick(o, ['limitAmount', 'limit'])))
  const used = Math.abs(toNum(pick(o, ['used', 'usedAmount', 'spent'])))
  const rawPercent = toOptNum(pick(o, ['percent', 'usagePercent']))
  const percent = rawPercent !== null ? rawPercent : ratioPercent(used, limit)
  const statusRaw = toStr(pick(o, ['status'])).toUpperCase()
  return {
    budgetId: toOptNum(pick(o, ['budgetId', 'id'])),
    categoryId: toOptNum(pick(o, ['categoryId'])),
    categoryName: toStr(pick(o, ['categoryName', 'name']), 'Ngân sách'),
    limitAmount: limit,
    used,
    percent,
    status: (['SAFE', 'WARNING', 'OVER'] as string[]).includes(statusRaw) ? (statusRaw as BudgetStatus) : budgetStatus(percent),
  }
}

/** GET /reports/dashboard — chấp nhận cả response thiếu field */
export function normalizeDashboard(raw: unknown, month: string, sizeFallback = 20): ReportSummary {
  const o = asObject(raw)
  const income = Math.abs(toNum(pick(o, ['income', 'totalIncome', 'in'])))
  const expense = Math.abs(toNum(pick(o, ['expense', 'totalExpense', 'out'])))
  const rawNet = toOptNum(pick(o, ['net', 'balance', 'remaining', 'surplus']))
  const countRaw = toOptNum(pick(o, ['transactionCount', 'count', 'totalTransactions', 'size']))

  const categories = asList(pick(o, ['topCategories', 'categories', 'byCategory', 'expenseByCategory'])).map(normalizeSlice)
  const categoryTotal = categories.reduce((sum, c) => sum + c.amount, 0)
  categories.forEach((c) => {
    if (!c.percent) c.percent = ratioPercent(c.amount, categoryTotal || expense)
  })

  const trend = asList(pick(o, ['trend12Months', 'trend', 'monthly', 'last12Months'])).map((t, i) => normalizeTrend(t, i))
  const wallets = asList(pick(o, ['wallets', 'walletBalances'])).map((w) => {
    const n = asObject(w)
    return {
      id: toNum(pick(n, ['id', 'walletId'])),
      name: toStr(pick(n, ['name', 'walletName']), 'Ví'),
      balance: toNum(pick(n, ['balance', 'amount', 'currentBalance'])),
    }
  })

  return {
    month: toStr(pick(o, ['month', 'period']), month),
    income,
    expense,
    net: rawNet !== null ? rawNet : income - expense,
    transactionCount: countRaw !== null ? countRaw : toNum(pick(o, ['_size', 'size']), sizeFallback === 20 ? 0 : sizeFallback),
    wallets,
    topCategories: categories,
    trend12Months: trend,
    budgetAlerts: asList(pick(o, ['budgetAlerts', 'alerts', 'notifications'])).map(normalizeAlert),
  }
}

export function normalizeWalletFlows(raw: unknown): WalletFlowRow[] {
  const list = Array.isArray(raw) ? asList(raw) : asList(pick(asObject(raw), ['wallets', 'rows', 'content', 'data']))
  return list.map((item) => {
    const o = asObject(item)
    return {
      walletId: toNum(pick(o, ['walletId', 'id'])),
      name: toStr(pick(o, ['name', 'walletName']), 'Ví'),
      income: Math.abs(toNum(pick(o, ['income', 'totalIncome']))),
      expense: Math.abs(toNum(pick(o, ['expense', 'totalExpense']))),
      transferIn: Math.abs(toNum(pick(o, ['transferIn', 'in']))),
      transferOut: Math.abs(toNum(pick(o, ['transferOut', 'out']))),
    }
  })
}

export function normalizeAdminUsers(raw: unknown): AdminUserRow[] {
  const list = Array.isArray(raw) ? asList(raw) : asList(pick(asObject(raw), ['content', 'users', 'data', 'rows']))
  return list.map((item) => {
    const o = asObject(item)
    return {
      id: toNum(pick(o, ['id', 'userId'])),
      email: toStr(pick(o, ['email'])),
      fullName: toStr(pick(o, ['fullName', 'name']), '—'),
      active: toBool(pick(o, ['active', 'isEnabled']) ?? o.is_active, true),
      isAdmin: toBool(pick(o, ['isAdmin', 'admin']) ?? o.is_admin),
      lastLoginAt: toOptStr(pick(o, ['lastLoginAt'])),
    }
  })
}

export function percentOf(percent: number): number {
  return clampPercent(toSafeNumber(percent))
}
