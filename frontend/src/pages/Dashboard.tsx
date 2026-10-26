import { useState } from 'react'
import {
  Bar, BarChart, CartesianGrid, Cell, Legend, Pie, PieChart,
  ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts'
import { reportApi } from '../lib/endpoints'
import { useFetch } from '../hooks/useFetch'
import { currentMonthKey, formatMonthKey, formatVnd, shiftMonth } from '../lib/format'
import { Card, Empty, ErrorNote, Loading, ProgressBar, StatCard } from '../components/ui'

const COLORS = ['#1A365D', '#2B6CB0', '#4299E1', '#63B3ED', '#90CDF4', '#CBD5E0', '#E53E3E', '#DD6B20']

export default function Dashboard() {
  const [month, setMonth] = useState(currentMonthKey())
  const { data, loading, error, errorCode } = useFetch(() => reportApi.dashboard(month), [month])

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between gap-3">
        <h1 className="text-lg font-semibold text-brand-700">Tổng quan {formatMonthKey(month)}</h1>
        <div className="flex items-center gap-2">
          <button type="button" className="rounded border border-gray-300 px-2 py-1 text-sm" onClick={() => setMonth(shiftMonth(month, -1))}>←</button>
          <button type="button" className="rounded border border-gray-300 px-2 py-1 text-sm" onClick={() => setMonth(shiftMonth(month, 1))}>→</button>
        </div>
      </div>

      {error && <ErrorNote message={error} code={errorCode} />}
      {loading && !data && <Loading />}

      {data && (
        <>
          <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
            <StatCard label="Tổng thu" value={formatVnd(data.income)} tone="income" />
            <StatCard label="Tổng chi" value={formatVnd(data.expense)} tone="expense" />
            <StatCard label="Kết dư" value={formatVnd(data.net)} tone={data.net >= 0 ? 'income' : 'expense'} />
            <StatCard label="Giao dịch" value={String(data.transactionCount)} />
          </div>

          <div className="grid gap-4 lg:grid-cols-2">
            <Card title="Cơ cấu chi theo danh mục">
              {data.topCategories.length === 0
                ? <Empty message="Chưa có khoản chi nào trong tháng." />
                : (
                  <ResponsiveContainer width="100%" height={260}>
                    <PieChart>
                      <Pie data={data.topCategories} dataKey="amount" nameKey="name" outerRadius={90} label={false}>
                        {data.topCategories.map((slice, index) => (
                          <Cell key={slice.id ?? index} fill={slice.color ?? COLORS[index % COLORS.length]} />
                        ))}
                      </Pie>
                      <Tooltip formatter={(value: number) => formatVnd(value)} />
                      <Legend />
                    </PieChart>
                  </ResponsiveContainer>
                )}
            </Card>

            <Card title="Thu chi 12 tháng gần nhất">
              {data.trend12Months.every((point) => point.income === 0 && point.expense === 0)
                ? <Empty message="Chưa có dữ liệu theo tháng." />
                : (
                  <ResponsiveContainer width="100%" height={260}>
                    <BarChart data={data.trend12Months}>
                      <CartesianGrid strokeDasharray="3 3" stroke="#E2E8F0" />
                      <XAxis dataKey="month" tick={{ fontSize: 11 }} />
                      <YAxis tick={{ fontSize: 11 }} width={70} tickFormatter={(value: number) => `${Math.round(value / 1000)}k`} />
                      <Tooltip formatter={(value: number) => formatVnd(value)} />
                      <Legend />
                      <Bar dataKey="income" name="Thu" fill="#48BB78" />
                      <Bar dataKey="expense" name="Chi" fill="#E53E3E" />
                    </BarChart>
                  </ResponsiveContainer>
                )}
            </Card>
          </div>

          <div className="grid gap-4 lg:grid-cols-2">
            <Card title="Số dư theo ví">
              {data.wallets.length === 0
                ? <Empty message="Chưa có ví nào. Vào mục Ví &amp; thẻ để thêm." />
                : (
                  <ul className="divide-y divide-gray-100 text-sm">
                    {data.wallets.map((wallet) => (
                      <li key={wallet.id} className="flex items-center justify-between py-2">
                        <span>{wallet.name}</span>
                        <span className="font-medium">{formatVnd(wallet.balance)}</span>
                      </li>
                    ))}
                  </ul>
                )}
            </Card>

            <Card title="Cảnh báo ngân sách">
              {data.budgetAlerts.length === 0
                ? <Empty message="Chưa ngân sách nào chạm ngưỡng 80%." />
                : (
                  <ul className="space-y-3">
                    {data.budgetAlerts.map((alert, index) => (
                      <li key={alert.budgetId ?? index}>
                        <div className="mb-1 flex items-center justify-between text-sm">
                          <span>{alert.categoryName}</span>
                          <span className={alert.status === 'OVER' ? 'font-semibold text-red-600' : 'text-amber-600'}>
                            {Math.round(alert.percent)}%
                          </span>
                        </div>
                        <ProgressBar percent={alert.percent} status={alert.status} />
                        <p className="mt-1 text-xs text-gray-500">
                          Đã chi {formatVnd(alert.used)} / {formatVnd(alert.limitAmount)}
                        </p>
                      </li>
                    ))}
                  </ul>
                )}
            </Card>
          </div>
        </>
      )}
    </div>
  )
}
