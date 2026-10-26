# HỢP ĐỒNG API — Family Finance Manager
**Thẻ backlog:** BE-11 · **Chịu trách nhiệm:** Tú (Backend Engineer) — review An · **Đóng băng schema:** 27/10/2026 (hết Sprint 2)

- Base URL dev: `http://localhost:8090/api/v1` · Swagger UI: `/swagger-ui.html` · OpenAPI JSON: `/v3/api-docs`
- Auth: `Authorization: Bearer <accessToken>` · Hết hạn → `POST /auth/refresh`
- Định dạng: JSON UTF-8; tiền là `number` (VND, không dấu phân cách); ngày `YYYY-MM-DD`; thời gian `2026-10-22T20:30:00+07:00`
- Phân trang: `?page=0&size=20&sort=transactionDate,desc` → trả `_page`, `_size`, `_total`
- Idempotency: `POST /transactions` chấp nhận header `Idempotency-Key` (UUID) để chống submit đúp

## 1. Cấu trúc lỗi chuẩn (FE switch theo `code`, không parse `message`)
```json
{
  "timestamp": "2026-10-22T20:30:00+07:00",
  "status": 422,
  "code": "TX_CATEGORY_MISMATCH",
  "message": "Danh mục 'Lương' thuộc loại THU, không dùng cho giao dịch CHI.",
  "details": [ { "field": "categoryId", "reason": "kind != EXPENSE" } ]
}
```

| HTTP | code | Khi nào |
|---|---|---|
| 400 | `VALIDATION_FAILED` | body sai kiểu, thiếu field |
| 401 | `AUTH_INVALID_CREDENTIALS` / `TOKEN_EXPIRED` | sai mật khẩu, JWT hỏng/hết hạn |
| 403 | `FORBIDDEN_ROLE` | Member gọi API của HeadOfFamily |
| 404 | `NOT_FOUND` | id không tồn tại **hoặc** thuộc hộ khác (cố ý gộp để dò not-found) |
| 409 | `BUDGET_CONFLICT` | version optimistic locking lỗi |
| 423 | `ACCOUNT_LOCKED` | 5 lần đăng nhập sai / 15 phút |
| 429 | `RATE_LIMITED` | vượt 100 request/phút/user |

## 2. Danh sách endpoint

### 2.1 Xác thực (`/auth`) — Sprint 2
| Method | Path | Quyền | Mô tả |
|---|---|---|---|
| POST | `/auth/register` | public | `{email, password, fullName}` → 201 + token |
| POST | `/auth/login` | public | → `{accessToken, refreshToken, user, families[]}` |
| POST | `/auth/refresh` | cookie Bearer | rotate refresh token |
| POST | `/auth/logout` | Member | revoke refresh token hiện tại |
| GET | `/auth/me` | Member | user + hộ đang chọn + vai trò |
| PATCH | `/users/me` | Member | đổi fullName, phone, avatar, familyId hiện tại |
| POST | `/auth/forgot-password` | public | gửi email OTP (dev: log ra console) |

### 2.2 Hộ gia đình (`/families`) — Sprint 2–3
| Method | Path | Quyền | Mô tả |
|---|---|---|---|
| POST | `/families` | Member | tạo hộ, người tạo tự thành `OWNER` |
| GET | `/families/{id}` | Member | thông tin hộ + vai trò của tôi |
| GET | `/families/{id}/members` | Member | danh sách thành viên (`role`, `joinedAt`) |
| POST | `/families/{id}/members/invite` | HeadOfFamily | mời theo email → 204 |
| PATCH | `/families/{id}/members/{userId}` | HeadOfFamily | đổi role |
| DELETE | `/families/{id}/members/{userId}` | HeadOfFamily | gỡ thành viên (BR-11) |

### 2.3 Danh mục & ví — Sprint 2
| Method | Path | Quyền | Mô tả |
|---|---|---|---|
| GET | `/families/{fid}/categories?kind=EXPENSE` | Member | cây 2 cấp |
| POST | `/families/{fid}/categories` | HeadOfFamily | `{name, kind, parentId, icon, color}` |
| PUT | `/families/{fid}/categories/{id}` | HeadOfFamily | sửa tên/màu/ẩn |
| DELETE | `/families/{fid}/categories/{id}` | HeadOfFamily | 409 nếu còn giao dịch (BR-12) |
| GET | `/families/{fid}/wallets` | Member | kèm `balance` tính suy ra |
| POST | `/families/{fid}/wallets` | HeadOfFamily | `{name, type, openingBalance}` |
| PATCH | `/families/{fid}/wallets/{id}` | HeadOfFamily | archive/unarchive |

### 2.4 Giao dịch — Sprint 2
```jsonc
// POST /api/v1/transactions
{
  "walletId": 2,
  "categoryId": 1,          // null khi type = TRANSFER
  "transferWalletId": null, // bắt buộc khi type = TRANSFER
  "type": "EXPENSE",        // INCOME | EXPENSE | TRANSFER
  "amount": 185000,
  "transactionDate": "2026-10-04",
  "note": "Cơm trưa văn phòng"
}
```
```jsonc
// 201 Created
{
  "id": 12, "familyId": 1, "walletId": 2, "categoryId": 1, "type": "EXPENSE",
  "amount": 185000, "transactionDate": "2026-10-04", "note": "Cơm trưa văn phòng",
  "createdBy": { "id": 1, "fullName": "Nguyễn Tiến Cường" },
  "walletBalanceAfter": 24635000,
  "createdAt": "2026-10-22T20:31:02+07:00", "version": 0
}
```
| Method | Path | Mô tả |
|---|---|---|
| GET | `/transactions?familyId&from&to&type&categoryId&walletId&keyword&page&size` | lọc + phân trang |
| GET | `/transactions/{id}` | chi tiết, kèm attachments |
| POST | `/transactions` | tạo |
| PUT | `/transactions/{id}` | sửa (Member chỉ sửa của mình — Q-01) |
| DELETE | `/transactions/{id}` | soft delete |
| POST | `/transactions/{id}/attachments` | multipart `file` ≤ 5MB |
| GET | `/transactions/export?from&to&format=csv` | stream CSV (BE-10) |

### 2.5 Ngân sách & mục tiêu — Sprint 3
| Method | Path | Mô tả |
|---|---|---|
| GET | `/budgets?periodStart=2026-10-01` | trả kèm `used`, `percent`, `status` = `SAFE/WARNING/OVER` |
| PUT | `/budgets` | upsert theo `(family, category, periodStart)`; header `If-Match` cho optimistic lock |
| DELETE | `/budgets/{id}` | xóa hạn mức |
| GET | `/goals?status=ACTIVE` | mục tiêu + % hoàn thành |
| POST | `/goals` · PATCH `/goals/{id}` · DELETE `/goals/{id}` | CRUD |
| POST | `/goals/{id}/contributions` | `{amount, walletId, contributedOn}` |

### 2.6 Báo cáo — Sprint 3
| Method | Path | Mô tả |
|---|---|---|
| GET | `/reports/dashboard?month=2026-10` | `{income, expense, net, wallets[], topCategories[], trend12Months[], budgetAlerts[]}` |
| GET | `/reports/monthly?year=2026` | 12 tháng: thu/chi/kết dư |
| GET | `/reports/by-category?from&to&kind=EXPENSE` | tỷ trọng theo danh mục |
| GET | `/reports/by-wallet?from&to` | dòng tiền theo ví |
| GET | `/notifications?unread=true` | cảnh báo ngân sách 80%/vượt |

### 2.7 Quản trị — Sprint 4
| Method | Path | Quyền | Mô tả |
|---|---|---|---|
| GET | `/admin/users?keyword&page` | Admin | tìm user |
| PATCH | `/admin/users/{id}/status` | Admin | khóa/mở |
| GET | `/admin/audit?familyId&from&to` | Admin | audit log |
| GET | `/api/health` | public | dùng cho healthcheck Docker (đã có từ Sprint 1) |

## 3. Quy ước đặt tên & versioning
- Path tài nguyên số nhiều, không dùng động từ: `GET /transactions` (không phải `getTransactions`).
- Filter dạng query string, không nhét vào path.
- Breaking change → thêm `/api/v2`, giữ `/v1` tối thiểu 1 sprint.
- Mọi response là DTO, **không serialize entity JPA** (tránh `lazy initialization` và lộ `passwordHash`).
