# KẾ HOẠCH KIỂM THỬ & CHECKLIST REVIEW BẢO MẬT
**Issue Jira (project DUAN):** DUAN-65 (bộ Test Cases + kiểm thử tích hợp API), DUAN-63 (CI chạy test), kiểm thử bảo mật do Vinh phụ trách · **Người chính:** Lê Hoàng Xuân Vinh (B24DTCN227) — DevOps & QA Lead · **Hạn:** 14/11/2026 (Sprint 4)

Mọi ca kiểm thử (TC) của hệ thống **Budglee** gắn với use case trong `docs/SRS.md` và tiêu chí chấp nhận AC. Cột "Cách chạy" chỉ ra lệnh thật để chạy lại được.

## 1. Layers kiểm thử
| Layer | Phạm vi | Công cụ | Chạy ở đâu |
|---|---|---|---|
| Unit | Logic service: tính tổng, tỷ trọng, ngưỡng ngân sách, quy đổi ngày | JUnit 5 + Mockito | `./gradlew test` + CI job Backend |
| API/Contract | payload, mã lỗi, phân quyền | `scripts/api-smoke.sh` (curl + jq) | CI job Compose smoke |
| Tích hợp end-to-end | FE gọi BE qua Docker network | thao tác tay trên `http://localhost:5173` | máy từng thành viên + video demo |
| Phi chức năng | NFR-01 (phản hồi < 2s), NFR-05 (index + cache Redis L2/Caffeine L1), NFR-07 (backup) | k6 hoặc `ab`, đồng hồ + ảnh | trong Sprint 4, trước 14/11/2026 |

## 2. Bộ ca kiểm thử chính (Must-have, phải pass hết mới được demo)
| TC | UC / AC | Kịch bản | Kỳ vọng | Cách chạy |
|---|---|---|---|---|
| TC-01 | UC-01 / AC-01 | Đăng ký email mới, họ chưa có | 201, user + family OWNER, access+refresh token | form Đăng ký |
| TC-02 | UC-01 | Đăng ký email đã tồn tại | 409 `EMAIL_EXISTS` | `curl -X POST /auth/register` |
| TC-03 | UC-02 / AC-02 | Đăng nhập sai mật khẩu 5 lần | vẫn 401 cùng một thông báo, không lộ email tồn tại | `api-smoke.sh` bước 4 |
| TC-04 | UC-02 | Đăng nhập đúng | 200 + `last_login_at` được cập nhật | `SELECT last_login_at FROM users` |
| TC-05 | UC-02 | Refresh token dùng lại sau khi rotate | 401, token cũ `revoked_at != NULL` | `api-smoke.sh` bước 10 |
| TC-06 | UC-02 | Access token hết hạn 15 phút | 401, FE tự refresh rồi retry 1 lần | đổi `JWT_ACCESS_TOKEN_MIN=1`, chờ |
| TC-07 | UC-03 / AC-04 | Thêm giao dịch CHI, amount = 250000 | 201, ví trừ đúng 250000 | form Thêm giao dịch |
| TC-08 | UC-03 | amount = 0 hoặc âm | 400 `VALIDATION_ERROR` kèm field `amount` | `api-smoke.sh` bước 7 |
| TC-09 | UC-03 | TRANSFER giữa 2 ví | 1 dòng, `transfer_wallet_id` đúng, tổng tài sản không đổi | SQL: `select sum(...)` |
| TC-10 | UC-03 | TRANSFER mà 2 ví giống nhau | 400 | form |
| TC-11 | UC-03 | Giao dịch dùng danh mục sai kind (CHI dùng danh mục THU) | 400 | `curl` |
| TC-12 | UC-04 / AC-05 | Sửa rồi xoá giao dịch | xoá là mềm: `deleted_at` có giá trị, API GET trả 404 | `api-smoke.sh` bước 9 |
| TC-13 | UC-05 | Lọc theo tháng `?month=2026-10` | chỉ trả bản ghi trong tháng, phân trang đúng `totalElements` | UI + SQL count |
| TC-14 | UC-07 / AC-06 | Báo cáo tháng: tổng thu, tổng chi, kết dư, top danh mục | trùng kết quả `GROUP BY` chạy tay trong `docs/ERD.md` | `api-smoke.sh` bước 8 |
| TC-15 | UC-08 | Ngân sách 3.000.000, chi 2.500.000 | status WARNING (>= 80%) | UI thanh tiến độ |
| TC-16 | UC-08 | Chi vượt 100% | status OVER + màu đỏ + xuất hiện trong cảnh báo Dashboard | UI |
| TC-17 | UC-08 | Tạo trùng ngân sách cùng (family, category, period_start) | 409 `DUPLICATE` | `curl` 2 lần |
| TC-18 | UC-09 | Nạp vào mục tiêu tiết kiệm | `saved_amount` tăng đúng, `status=COMPLETED` khi đạt | UI |
| TC-19 | UC-11 / AC-07 | Member thường thử xoá thành viên | 403 `FORBIDDEN` | đăng nhập bằng user MEMBER |
| TC-20 | UC-11 | OWNER mời user đã ở hộ khác vẫn được, nhưng không hạ quyền chính mình | 200 / 400 khi tự hạ OWNER | UI |
| TC-21 | AC-08 / BR-01 | **Cách ly dữ liệu**: user A (family 1) đọc giao dịch id của family 2 | 404, không phải 200 và không phải 403-leak | `curl` đổi id |
| TC-22 | BR-01 | `GET /transactions?familyId=<của hộ khác>` | tham số familyId bị bỏ qua, luôn dùng family của token | `curl` |
| TC-23 | NFR-03 | Không có token ở mọi endpoint `/api/v1/**` | 401, không có redirect login HTML | `api-smoke.sh` bước 5 |
| TC-24 | NFR-05, NFR-13 (DUAN-57) | Gọi 2 lần `/reports/monthly?month=...` | lần 2 trả từ cache Redis L2 (log `RedisCacheManager`/`CacheInterceptor`), kết quả giống hệt; tắt container `ffa-redis` rồi gọi lại → API vẫn 200, log warn `cache GET lỗi ... gọi thẳng xuống nguồn` (`LoggingCacheErrorHandler`) | `logging.level.org.springframework.cache=DEBUG` + `docker stop ffa-redis` |
| TC-25 | NFR-07 | Backup rồi restore trên DB khác | `select count(*) from transactions` bằng nhau | `scripts/backup-prod.sh` + mục 6 `docs/DEPLOY.md` |
| TC-26 | — | `docker compose up` lại sau khi `down` (không purge) | dữ liệu seed và giao dịch test vẫn còn | `scripts/dev-down.sh` rồi `dev-up.sh` |
| TC-27 | UX | Tháng không có dữ liệu | Dashboard hiện empty state, không NaN/infinity | UI 375px + desktop |
| TC-28 | NFR-01 | 20 bản ghi/trang, bảng giao dịch | phản hồi < 2s ở LAN | đồng hồ + ảnh |

## 3. Checklist review bảo mật (làm trong Sprint 4, trước 14/11/2026, có người thứ 2 ký)
Chạy: `grep` trong repo + thử thủ công. Ghi "Đạt/Không" + người review vào cột cuối.

| # | Nội dung | Lệnh kiểm tra | Kết quả |
|---|---|---|---|
| S1 | Không có secret trong git (mật khẩu DB, JWT, PAT, SSH key) | `git log --all -p -S "password" -- backend/src/main/resources` và `gitleaks detect` | |
| S2 | `.env`, `.env.prod` không bị commit | `git check-ignore -v .env` | |
| S3 | Mật khẩu lưu bcrypt cost >= 10, không bao giờ trả `passwordHash` ra API | `grep -rn "passwordHash" backend/src/main/java/vn/ptit/ffa/web` phải rỗng | |
| S4 | Không có SQL động nối chuỗi (chỉ JPQL/parameter binding) | `grep -rn '" + ' backend/src/main/java --include=*Repository.java` | |
| S5 | Không có `@CrossOrigin("*")` | `grep -rn 'CrossOrigin' backend/src/main/java` | |
| S6 | CSRF tắt hợp lệ (API stateless, không cookie) | đọc `SecurityConfig` | |
| S7 | Endpoint công khai chỉ: health, swagger, register/login/refresh | đọc `SecurityConfig.requestMatchers` | |
| S8 | Mọi query nghiệp vụ đều có `family_id = ?` | `grep -rn "familyId" backend/src/main/java/vn/ptit/ffa/repository` | |
| S9 | JWT secret >= 32 byte, fail-fast khi ngắn hơn | test `JwtServiceTest` | |
| S10 | Refresh token lưu hash, không lưu明文 | `SELECT token_hash FROM refresh_tokens LIMIT 1` | |
| S11 | Log không in token/mật khẩu | `grep -rn "log.*token" backend/src/main/java` | |
| S12 | Thông báo lỗi 500 không lộ stack trace/SQL | `curl` gây lỗi rồi xem body | |
| S13 | Upload chứng từ (nếu bật) chặn file thực thi, giới hạn 5MB | `application.yml multipart` | |
| S14 | Port DB/Adminer không expose ở prod | `docker compose -f docker-compose.prod.yml ps` | |
| S15 | UFW chỉ 22/80/443 | `sudo ufw status` | |

## 4. Minh chứng cần nộp (issue DUAN-65)
1. Ảnh CI 3 job xanh + ảnh `api-smoke.sh` in "tất cả bước PASS".
2. Ảnh `docker compose ps` 4 service healthy.
3. Ảnh Dashboard có biểu đồ + ảnh cảnh báo ngân sách OVER.
4. Ảnh bảng nghiệm thu trong file Excel đã đổi "Đạt".
5. Ảnh run "release" + ảnh image trên GHCR.
6. Video demo 3–5 phút: đăng ký → thêm giao dịch → báo cáo → ngân sách → đăng nhập bằng hộ khác chứng minh cách ly dữ liệu (TC-21).
