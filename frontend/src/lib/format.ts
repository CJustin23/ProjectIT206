import dayjs from 'dayjs'

/**
 * Format dùng chung toàn app (NFR-11: tiếng Việt + VND).
 * Một instance duy nhất để tránh tạo lại Intl.NumberFormat mỗi lần render.
 */
const vndFormatter = new Intl.NumberFormat('vi-VN', {
  style: 'currency',
  currency: 'VND',
  maximumFractionDigits: 0,
})



export const DATE_FORMAT = 'DD/MM/YYYY'
export const MONTH_KEY_FORMAT = 'YYYY-MM'

/** 185000 -> "185.000 ₫"; null/NaN -> "0 ₫" (không bao giờ trả NaN) */
export function formatVnd(value: number | null | undefined): string {
  const n = toSafeNumber(value)
  return vndFormatter.format(n)
}

/** Số thường, dùng cho số lượng/tỷ lệ */
export function formatNumber(value: number | null | undefined, digits = 0): string {
  const n = toSafeNumber(value)
  return new Intl.NumberFormat('vi-VN', { maximumFractionDigits: digits }).format(n)
}

/** 42.857 -> "42.9%"; NaN/Infinity -> "0%" */
export function formatPercent(value: number | null | undefined, digits = 1): string {
  const n = toSafeNumber(value)
  return `${n.toFixed(digits)}%`
}

export function formatDate(value: string | null | undefined, fallback = '—'): string {
  if (!value) return fallback
  const d = dayjs(value)
  return d.isValid() ? d.format(DATE_FORMAT) : fallback
}

/** '2026-10' -> '10/2026' */
export function formatMonthKey(value: string | null | undefined, fallback = '—'): string {
  if (!value) return fallback
  const d = dayjs(`${value}-01`)
  return d.isValid() ? d.format('MM/YYYY') : fallback
}

export function formatDateTime(value: string | null | undefined, fallback = '—'): string {
  if (!value) return fallback
  const d = dayjs(value)
  return d.isValid() ? d.format(`DD/MM/YYYY HH:mm`) : fallback
}

export function todayIso(): string {
  return dayjs().format('YYYY-MM-DD')
}

export function currentMonthKey(): string {
  return dayjs().format(MONTH_KEY_FORMAT)
}

export function currentYear(): number {
  return dayjs().year()
}

/** '2026-10' -> { from: '2026-10-01', to: '2026-10-31' } */
export function monthRange(monthKey: string): { from: string; to: string; periodStart: string } {
  const base = dayjs(`${monthKey}-01`)
  const safe = base.isValid() ? base : dayjs()
  return {
    from: safe.startOf('month').format('YYYY-MM-DD'),
    to: safe.endOf('month').format('YYYY-MM-DD'),
    periodStart: safe.startOf('month').format('YYYY-MM-DD'),
  }
}

export function firstOfMonth(monthKey: string): string {
  return monthRange(monthKey).periodStart
}

/** monthKey dịch n tháng so với tháng hiện tại (monthKey 'yyyy-MM') */
export function shiftMonth(monthKey: string, delta: number): string {
  const base = dayjs(`${monthKey}-01`)
  return (base.isValid() ? base : dayjs()).add(delta, 'month').format(MONTH_KEY_FORMAT)
}

/** '1.234.567 ₫' | '1,234,567' | '1234567' -> 1234567 */
export function parseAmount(input: string): number {
  if (!input) return 0
  const cleaned = input.replace(/[^\d,.-]/g, '').replace(/\s/g, '')
  if (!cleaned) return 0
  // dấu , là phân cách hàng nghìn theo vi-VN -> bỏ; chỉ giữ dấu thập phân nếu có
  const normalized = cleaned.includes(',') && cleaned.lastIndexOf(',') > cleaned.lastIndexOf('.')
    ? cleaned.replace(/\./g, '').replace(',', '.')
    : cleaned.replace(/,/g, '')
  const n = Number(normalized)
  return Number.isFinite(n) ? n : 0
}

/** an toàn cho chia 0 / dữ liệu thiếu từ backend */
export function ratioPercent(part: number | null | undefined, total: number | null | undefined): number {
  const p = toSafeNumber(part)
  const t = toSafeNumber(total)
  if (t <= 0) return 0
  return round1((p / t) * 100)
}

export function clampPercent(value: number): number {
  if (!Number.isFinite(value)) return 0
  return Math.min(100, Math.max(0, value))
}

export function toSafeNumber(value: number | string | null | undefined): number {
  if (typeof value === 'number') return Number.isFinite(value) ? value : 0
  if (typeof value === 'string') {
    const n = Number(value.replace(/,/g, '.'))
    return Number.isFinite(n) ? n : 0
  }
  return 0
}

function round1(value: number): number {
  return Math.round(value * 10) / 10
}
