# SPRINT 1 — Checklist & hướng dẫn chạy (Budglee)
**Mốc:** Sprint 1 = 20/10 → 26/10/2026 · **Deliverables theo báo cáo đề cương:** đặc tả SRS, ERD, khởi tạo GitHub repo, môi trường Docker Compose dev (MySQL + Redis), **Jira board project DUAN**.
**Lịch sprint:** S1 20/10–26/10 · S2 27/10–02/11 · S3 03/11–09/11 · S4 10/11–14/11/2026.

## 0. Trạng thái issue Sprint 1 (board DUAN, xuất 26/10/2026 — [`jira-board.csv`](jira-board.csv))
| Issue | Nội dung | Người phụ trách | Trạng thái |
|---|---|---|---|
| DUAN-30 | Thiết kế giao diện UI/UX toàn bộ hệ thống trên Figma (Dashboard, Wallet, Budget) | Thành | ✅ Done |
| DUAN-31 | Xây dựng Wireframe các màn hình báo cáo, thống kê, biểu đồ | Thành | ✅ Done |
| DUAN-32 | Khảo sát nghiệp vụ thu chi cá nhân & thiết kế ERD | Tú | ✅ Done (`docs/ERD.md`) |
| DUAN-33 | Chuẩn hóa các bảng CSDL (User, Wallet, Category, Transaction, Budget, Goal) | Tú | ✅ Done |
| DUAN-34 | Khởi tạo Git Repository, phân nhánh main/develop & thiết lập Git Flow | Vinh | ✅ Done (`CONTRIBUTING.md` mục 1) |
| DUAN-35 | Cấu hình Docker Compose cho môi trường local (MySQL, Redis, Backend, Frontend) | Vinh | ✅ Done (`docker-compose.yml`) |
| DUAN-36 | Khởi tạo Jira Project Board, chia Epic/Sprint và gán task ban đầu | Vinh | ✅ Done (project DUAN) |
| DUAN-37 | Họp nhóm định kỳ qua Google Meet, thống nhất cấu trúc thư mục code | Team | ✅ Done |

Kiến trúc hệ thống + luồng JWT do An phụ trách được ghi tại [`architecture.md`](architecture.md) (liên quan DUAN-34/DUAN-35/DUAN-38).

**Đang chạy ở Sprint 2 (27/10–02/11/2026):** DUAN-38 — Entity/Repository/JPA (An), DUAN-41 — Khởi tạo ReactJS Vite + TypeScript + Tailwind (Thành), DUAN-44 — Kịch bản Flyway migration (Tú) = **In Progress**. Toàn bộ issue còn lại của S2–S4 vẫn **To Do**.

## 1. Repository (ĐÃ PUSH — 22/10/2026)
Repo thật của nhóm: **https://github.com/CJustin23/ProjectIT206** — đã có `main` + `develop` và commit `chore(sprint1): ...`.

Đã xong: **Gradle wrapper** (`backend/gradlew`, Gradle 8.14 — CI và máy thành viên đều dùng `./gradlew`, không phải cài Gradle).

Còn 1 việc phải làm tay trên repo này:
```bash
# 1.1 Branch protection cho main + develop (can gh da login)
gh auth login            # chon GitHub.com -> HTTPS -> Login with a web browser
gh api repos/CJustin23/ProjectIT206/branches/main/protection -X PUT \
  -f required_status_checks[strict]=true \
  -F required_status_checks[contexts][]=backend \
  -F required_status_checks[contexts][]=frontend \
  -f enforce_admins=true \
  -f required_pull_request_reviews[required_approving_review_count]=1 \
  -f restrictions=NULL
# lap lai voi /branches/develop/protection
```
Nếu không muốn dùng `gh`: Settings → Branches → Add branch protection rule cho `main` và `develop`, tick *Require a pull request before merging* (1 approval) + *Require status checks to pass* (backend, frontend).

**Đề cương yêu cầu:** vào **Settings → Collaborators and teams → Add people** và thêm tài khoản của Giảng viên (ThS. Hồ Xuân Hùng) để thầy review được lịch sử commit (bản Public thì thầy chỉ cần link, nhưng nên add để sau này bật Private vẫn xem được).

## 2. Board Jira — project DUAN (Vinh)
Board **đã có sẵn** và là nguồn tiến độ duy nhất của nhóm: https://thenng.atlassian.net/jira/core/projects/DUAN/board — 38 issue **DUAN-30…DUAN-67**, mỗi issue có owner, trạng thái và hạn; bản xuất mới nhất là [`jira-board.csv`](jira-board.csv) (commit trong repo để giảng viên xem được ngay cả khi không có quyền Jira).

Không còn việc "tạo board / import backlog": nhóm dùng Jira project DUAN (đã khởi tạo ở DUAN-36, Done). Phần việc board còn lại:
1. **Chuyển DUAN-38, DUAN-41, DUAN-44 sang `Done`** khi code tương ứng merge vào `develop` (kèm link PR ở phần nhận xét của issue).
2. **Tạo subtask cho Sprint 2 (27/10–02/11/2026)** dưới các issue DUAN-39…DUAN-47: mỗi subtask ≤ 1 ngày công, gán assignee đúng vai trò (An – API/JWT/RBAC, Thành – UI/React/thống kê, Tú – migration/seed/index/Redis, Vinh – env/CI/backup/test).
3. Kéo issue sang `In Progress` ngay khi bắt đầu và cập nhật tiến độ trong standup 20:30 — trạng thái trên board phải khớp `jira-board.csv` ở lần xuất kế tiếp.

## 3. Chạy môi trường dev (cả nhóm, mỗi người một máy)
```bash
cp .env.example .env
docker compose --env-file .env up -d --build      # lần đầu ~5-8 phút (tải image)
bash scripts/smoke-test.sh
```

**Tiêu chí chấp nhận Sprint 1 (đủ 6 dòng này là PASS):**
| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | `docker compose ps` | 5 service `running` (mysql · redis · app · web · adminer), `ffa-app` và `ffa-redis` = `healthy` |
| 2 | `curl http://localhost:8090/api/health` | `"status":"UP"`, `"connected":true`, `"tables":14`, `"flywayVersion":"4"` |
| 3 | Mở `http://localhost:5173` | trang hiển thị "Backend: UP" + "MySQL: kết nối OK — 14 bảng" |
| 4 | Adminer `http://localhost:8081` (server `mysql`, user/password trong `.env`) | thấy 13 bảng + 11 dòng `transactions` |
| 5 | Action `ci` trên GitHub | `ci.yml` chạy 3 job: backend build + test, frontend typecheck + build, compose smoke (`docker compose up -d --wait` rồi `/api/health` trả `"connected":true`) — issue DUAN-63 vẫn To Do tới Sprint 4 vì chưa có ảnh/minh chứng nộp kèm báo cáo |
| 6 | `docker compose down && docker compose up -d` | dữ liệu seed **vẫn còn** (volume hoạt động) |

Dòng 1–4 và 6 là kiểm tra trên máy từng thành viên (An chạy một lần rồi báo kết quả); dòng 5 cần một lượt chạy xanh để chụp ảnh minh chứng.

Lệnh dọn dẹp: `bash scripts/dev-down.sh` (giữ dữ liệu) · `bash scripts/dev-down.sh --purge` (xóa sạch, chạy lại migration từ đầu).

## 4. Figma — đã hoàn thành ở Sprint 1 (DUAN-30, DUAN-31 — Thành)
Bộ khung đã thiết kế theo mẫu giao diện Budglee, đối chiếu khi làm UI Sprint 2–3:
1. `Dashboard` — 4 card số dư + biểu đồ tròn cơ cấu chi + cột 12 tháng + danh sách cảnh báo.
2. `Giao dịch` — bảng + filter tháng + modal "Thêm giao dịch" (đúng form UC-07).
3. `Ngân sách` — thanh tiến độ 3 trạng thái SAFE/WARNING/OVER (AC-03).
4. `Hộ gia đình` — danh sách thành viên + role + nút mời.
Việc còn lại: **File → Share → Link (Anyone with the link can view)** → dán URL vào bảng mục 1 của file Word và vào mục "Liên kết" của `README.md`.

## 5. Việc còn tồn sau Sprint 1
| Mã | Việc | Ai | Hạn |
|---|---|---|---|
| S1-01 | Đối chiếu lại bìa Word với hồ sơ lớp: Nguyễn Trường An (B24DTCN181), Nguyễn Tiến Thành (B24DTCN283), Bàng Trọng Tú (B24DTCN444), Lê Hoàng Xuân Vinh (B24DTCN227) — Nhóm 03, GVHD ThS. Hồ Xuân Hùng | An | 26/10/2026 |
| S1-02 | Dán link repo + Jira board DUAN + Figma vào mục 1 file Word | An | 26/10/2026 |
| S1-03 | Nộp file Word + Excel + 3 link trên biểu mẫu của giảng viên | An | 26/10/2026 |
| S1-04 | Chụp 2 ảnh: `docker compose ps` healthy + dashboard Figma | Vinh, Tú | 30/10/2026 |
| S1-05 | Chốt Q-01, Q-02, Q-03 trong SRS mục 10.2 | An + GVHD | 26/10/2026 |
| S1-06 | Branch protection cho `main` + `develop` (mục 1.1) | An | 30/10/2026 |

## 6. Lỗi hay gặp khi chạy lần đầu
| Triệu chứng | Nguyên nhân | Cách xử lý |
|---|---|---|
| `port is already allocated` (8090/5173/3307) | máy khác/chạy đề thi đang chiếm | sửa `APP_PORT`, `WEB_PORT`, `DB_PORT_HOST` trong `.env` |
| `ffa-app` restart loop, log `Access denied for user` | `.env` đổi `MYSQL_PASSWORD` sau khi đã tạo volume | `bash scripts/dev-down.sh --purge` rồi up lại |
| `ffa-app` chờ lâu rồi mới lên | `app` `depends_on` cả `mysql` và `redis` ở trạng thái healthy | kiểm tra `docker compose ps ffa-redis`; Redis chỉ là cache, mất Redis API vẫn chạy (degrade về MySQL) |
| `flyway validate` lỗi `Schema `ffa_db` non-empty` | DB từng có bảng cũ | xóa volume (`--purge`), hoặc `spring.flyway.clean-disabled=false` khi dev |
| Vite 502 khi gọi `/api` | app chưa lên | chờ healthcheck `healthy` rồi reload; proxy trỏ `http://app:8080` |
| Windows: hot reload không bắt | mount volume cần polling | đã bật `usePolling: true` trong `vite.config.ts` |
| `npm ci` báo thiếu lock file | chưa commit `package-lock.json` | chạy `npm install` một lần rồi commit lock file |
