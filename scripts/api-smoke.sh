#!/usr/bin/env bash
# =============================================================================
# api-smoke.sh — đi qua luồng nghiệp vụ thật qua HTTP (dùng trong CI và khi demo)
# Cách chạy:
#   bash scripts/api-smoke.sh http://localhost:8090 an@ffa.dev password
# Mặc định trỏ container app ở cổng 8090 theo .env.example.
# Yêu cầu: curl, jq. Thoát mã != 0 nếu một bước sai.
# =============================================================================
set -uo pipefail

BASE="${1:-http://localhost:8090}"
EMAIL="${2:-an@ffa.dev}"
PASS="${3:-password}"
FAILED=0

step() { printf '\n=== %s ===\n' "$1"; }
check() {
  if [ "$1" = "0" ]; then echo "PASS: $2"; else echo "FAIL: $2"; FAILED=$((FAILED+1)); fi
}

command -v jq >/dev/null || { echo "thiếu jq"; exit 2; }

step "1. Health"
H=$(curl -fsS "$BASE/api/health")
echo "$H" | jq -e '.db.connected == true' >/dev/null; check $? "DB connected"
echo "$H" | jq -e '.db.tables >= 13' >/dev/null; check $? "đủ 13 bảng do Flyway tạo"

step "2. Đăng nhập bằng tài khoản demo"
LOGIN=$(curl -fsS -X POST "$BASE/api/v1/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASS\"}")
echo "login body: ${LOGIN:0:400}"
ACCESS=$(echo "$LOGIN" | jq -r '.accessToken // .access_token // empty')
 REFRESH=$(echo "$LOGIN" | jq -r '.refreshToken // .refresh_token // empty')
[ -n "$ACCESS" ]; check $? "nhận được accessToken"

AUTH="Authorization: Bearer $ACCESS"

step "3. Danh tính hiện tại"
ME=$(curl -fsS "$BASE/api/v1/auth/me" -H "$AUTH" || true)
echo "$ME" | jq -e '.user.email != null' >/dev/null; check $? "GET /auth/me trả user.email"

step "4. Sai mật khẩu phải bị chặn"
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/api/v1/auth/login" \
  -H 'Content-Type: application/json' -d "{\"email\":\"$EMAIL\",\"password\":\"sai-r12345\"}")
[ "$CODE" = "401" ]; check $? "login sai mật khẩu -> 401 (nhận $CODE)"

step "5. Không có token phải bị chặn"
CODE=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/api/v1/transactions")
[ "$CODE" = "401" ]; check $? "GET /transactions không token -> 401 (nhận $CODE)"

step "6. Tạo danh mục + giao dịch"
CATS=$(curl -fsS "$BASE/api/v1/families/1/categories" -H "$AUTH")
CAT_ID=$(echo "$CATS" | jq -r '.[0].id // .content[0].id // empty')
[ -n "$CAT_ID" ]; check $? "đọc được danh mục (id=$CAT_ID)"
WALLET_ID=$(curl -fsS "$BASE/api/v1/families/1/wallets" -H "$AUTH" | jq -r '.[0].id // .content[0].id // empty')
TX=$(curl -fsS -X POST "$BASE/api/v1/transactions" -H "$AUTH" -H 'Content-Type: application/json' \
  -d "{\"type\":\"EXPENSE\",\"amount\":250000,\"transactionDate\":\"$(date +%F)\",\"categoryId\":$CAT_ID,\"walletId\":$WALLET_ID,\"note\":\"smoke test\"}")
TX_ID=$(echo "$TX" | jq -r '.id')
[ -n "$TX_ID" ] && [ "$TX_ID" != "null" ]; check $? "tạo giao dịch id=$TX_ID"

step "7. Số tiền âm phải bị từ chối"
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/api/v1/transactions" -H "$AUTH" \
  -H 'Content-Type: application/json' \
  -d "{\"type\":\"EXPENSE\",\"amount\":-1,\"transactionDate\":\"$(date +%F)\",\"walletId\":$WALLET_ID}")
[ "$CODE" = "400" ]; check $? "amount<=0 -> 400 (nhận $CODE)"

step "8. Báo cáo tháng"
REPORT=$(curl -fsS "$BASE/api/v1/reports/dashboard?month=$(date +%Y-%m)" -H "$AUTH")
echo "$REPORT" | jq -e 'has("expense")' >/dev/null; check $? "dashboard có tổng chi trong tháng"

step "9. Xoá mềm giao dịch"
curl -fsS -X DELETE "$BASE/api/v1/transactions/$TX_ID" -H "$AUTH" >/dev/null; check $? "DELETE /transactions/{id}"
curl -fsS -o /dev/null "$BASE/api/v1/transactions/$TX_ID" -H "$AUTH" && \
  echo "LỖI: bản ghi đã xoá vẫn truy cập được" && FAILED=$((FAILED+1))

step "10. Refresh token quay vòng"
NEW=$(curl -fsS -X POST "$BASE/api/v1/auth/refresh" -H 'Content-Type: application/json' \
  -d "{\"refreshToken\":\"$REFRESH\"}" | jq -r '.accessToken // empty')
[ -n "$NEW" ] && [ "$NEW" != "$ACCESS" ]; check $? "refresh trả access token mới"
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/api/v1/auth/refresh" \
  -H 'Content-Type: application/json' -d "{\"refreshToken\":\"$REFRESH\"}")
[ "$CODE" = "401" ]; check $? "dùng lại refresh token cũ -> 401 (nhận $CODE)"

printf '\n========================================\n'
if [ "$FAILED" -eq 0 ]; then echo "API SMOKE: tất cả bước PASS"; else echo "API SMOKE: $FAILED bước FAIL"; fi
exit "$FAILED"
