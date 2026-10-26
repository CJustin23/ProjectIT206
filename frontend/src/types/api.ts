/**
 * FFMS — kiểu dữ liệu DTO theo docs/api.md + docs/ERD.md.
 * Tiền luôn là number (VND, DECIMAL(15,2) ở BE), ngày là chuỗi 'yyyy-MM-dd'.
 */

export type Role = 'OWNER' | 'ADMIN' | 'MEMBER' | 'VIEWER'
export type CategoryKind = 'INCOME' | 'EXPENSE'
export type TransactionType = 'INCOME' | 'EXPENSE' | 'TRANSFER'
export type WalletType = 'CASH' | 'BANK' | 'E_WALLET' | 'CREDIT_CARD' | 'SAVINGS'
export type BudgetStatus = 'SAFE' | 'WARNING' | 'OVER'
export type GoalStatus = 'ACTIVE' | 'COMPLETED' | 'CANCELLED'

export interface User {
  id: number
  email: string
  fullName: string
  phone: string | null
  avatarUrl: string | null
  isAdmin: boolean
  /** vai trò của user này trong hộ đang chọn */
  role: Role | null
}

export interface Family {
  id: number
  name: string
  baseCurrency: string
  fiscalMonthStartDay: number
  myRole: Role | null
  memberCount: number
}

export interface FamilyMember {
  id: number
  userId: number
  email: string
  fullName: string
  role: Role
  joinedAt: string | null
  active: boolean
}

export interface Category {
  id: number
  familyId: number
  name: string
  kind: CategoryKind
  parentId: number | null
  icon: string | null
  color: string | null
  isSystem: boolean
  hidden: boolean
  /** cây 2 cấp: BE trả parent kèm children */
  children: Category[]
}

export interface Wallet {
  id: number
  familyId: number
  name: string
  type: WalletType
  openingBalance: number
  /** số dư suy ra (ERD §5), luôn có sẵn sau normalize */
  balance: number
  isArchived: boolean
}

export interface PersonRef {
  id: number
  fullName: string
}

export interface Transaction {
  id: number
  familyId: number
  walletId: number
  categoryId: number | null
  transferWalletId: number | null
  type: TransactionType
  amount: number
  /** 'yyyy-MM-dd' */
  transactionDate: string
  note: string | null
  createdBy: PersonRef | null
  walletBalanceAfter: number | null
  createdAt: string | null
  version: number
  /** tên hiển thị — BE có thể trả kèm trong list DTO */
  walletName: string | null
  transferWalletName: string | null
  categoryName: string | null
  isRecurring: boolean
}

export interface Budget {
  id: number
  familyId: number
  categoryId: number
  categoryName: string | null
  periodStart: string
  periodEnd: string
  limitAmount: number
  used: number
  /** 0..n, có thể > 100 khi OVER */
  percent: number
  status: BudgetStatus
  alertThresholdPercent: number
  version: number
}

export interface GoalContribution {
  id: number
  goalId: number
  walletId: number | null
  amount: number
  contributedOn: string
}

export interface SavingsGoal {
  id: number
  familyId: number
  name: string
  targetAmount: number
  savedAmount: number
  deadline: string | null
  status: GoalStatus
  percent: number
  contributions: GoalContribution[]
}

export interface DashboardWallet {
  id: number
  name: string
  balance: number
}

export interface DashboardSlice {
  id: number | null
  name: string
  amount: number
  percent: number
  color: string | null
}

export interface TrendPoint {
  /** 'yyyy-MM' */
  month: string
  income: number
  expense: number
  net: number
}

export interface BudgetAlert {
  budgetId: number | null
  categoryId: number | null
  categoryName: string
  limitAmount: number
  used: number
  percent: number
  status: BudgetStatus
}

/** GET /reports/dashboard */
export interface ReportSummary {
  month: string
  income: number
  expense: number
  net: number
  transactionCount: number
  wallets: DashboardWallet[]
  topCategories: DashboardSlice[]
  trend12Months: TrendPoint[]
  budgetAlerts: BudgetAlert[]
}

export interface MonthlyReportRow {
  month: string
  income: number
  expense: number
  net: number
}

export interface WalletFlowRow {
  walletId: number
  name: string
  income: number
  expense: number
  transferIn: number
  transferOut: number
}

export interface NotificationItem {
  id: number
  type: string
  message: string
  readAt: string | null
  createdAt: string | null
}

export interface AdminUserRow {
  id: number
  email: string
  fullName: string
  active: boolean
  isAdmin: boolean
  lastLoginAt: string | null
}

/** Phân trang chuẩn FE dùng nội bộ (docs/api.md cho phép _page/_size/_total) */
export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface FieldIssue {
  field: string
  reason: string
}

export interface ApiErrorBody {
  timestamp?: string
  status?: number
  code?: string
  message?: string
  details?: FieldIssue[]
}

export interface AuthTokens {
  accessToken: string
  refreshToken: string
}

export interface LoginResponse extends AuthTokens {
  user: User
  families: Family[]
}

export interface MeResponse {
  user: User
  family: Family | null
  families: Family[]
}

/* ---------- request payloads ---------- */

export interface RegisterPayload {
  email: string
  password: string
  fullName: string
}

export interface LoginPayload {
  email: string
  password: string
}

export interface TransactionPayload {
  walletId: number
  categoryId: number | null
  transferWalletId: number | null
  type: TransactionType
  amount: number
  transactionDate: string
  note: string | null
}

export interface CategoryPayload {
  name: string
  kind: CategoryKind
  parentId: number | null
  icon: string | null
  color: string | null
  hidden?: boolean
}

export interface WalletPayload {
  name: string
  type: WalletType
  openingBalance: number
  isArchived?: boolean
}

export interface BudgetPayload {
  familyId: number
  categoryId: number
  periodStart: string
  periodEnd: string
  limitAmount: number
  alertThresholdPercent: number
}

export interface GoalPayload {
  name: string
  targetAmount: number
  deadline: string | null
  status?: GoalStatus
}

export interface ContributionPayload {
  amount: number
  walletId: number | null
  contributedOn: string
}

export interface TransactionFilter {
  month?: string
  from?: string
  to?: string
  type?: TransactionType | ''
  categoryId?: number | ''
  walletId?: number | ''
  keyword?: string
  page?: number
  size?: number
}
