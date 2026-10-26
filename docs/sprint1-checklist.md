# SPRINT 1 — Checklist & hướng dẫn chạy
**Mốc:** 20/10 → 23/10/2026 · **Deliverables theo đề cương:** đặc tả SRS, ERD, khởi tạo GitHub repo, môi trường Docker Compose dev, board Trello.

## 0. Trạng thái các thẻ Sprint 1
| Thẻ | Nội dung | Người chính | Trạng thái hiện tại |
|---|---|---|---|
| BE-01 | Repo + nhánh + branch protection + CONTRIBUTING | An | ✅ xong khung (còn mục 1–2 dưới đây) |
| BE-02 | Sơ đồ kiến trúc + luồng xác thực | An | ✅ `docs/architecture.md` |
| DB-01 | ERD + migration Flyway | Thành | ✅ `docs/ERD.md` + `V1__init_schema.sql` |
| OPS-01 | Dockerfile + docker-compose dev | Vinh | ✅ `docker-compose.yml`, 2 Dockerfile |
| DOC-01 | SRS + quy tắc nghiệp vụ + NFR | An | ✅ `docs/SRS.md` (19 UC, 12 BR, 12 NFR) |
| OPS-02 | Workflow CI (build + test + compose smoke) | Vinh | ✅ 3 job xanh ở run #4 26/10 — còn GHCR + deploy SSH (Sprint 3–4) |
| FE-01 | Prototype Figma 4 màn hình | Vinh | ⏳ **phải làm tay** (mục 4) |

## 1. Repository (ĐÃ PUSH — 22/10/2026)
Repo thật của nhóm: **https://github.com/CJustin23/ProjectIT206** — đã có `main` + `develop` và commit `chore(sprint1): ...`.

Còn 2 việc phải làm tay trên repo này:
```bash
# 1.1 Sinh Gradle wrapper để người khác không cần cài Gradle (rồi commit)
cd backend && gradle wrapper --gradle-version 8.14 && cd ..
git add gradlew gradlew.bat gradle/ backend/gradle/wrapper
git commit -m "chore(build): them gradle wrapper"
git push

# 1.2 Branch protection cho main + develop (can gh da login)
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

**Đề cương yêu cầu:** vào **Settings → Collaborators and teams → Add people** và thêm tài khoản của Giảng viên để thầy review được lịch sử commit (bản Public thì thầy chỉ cần link, nhưng nên add để sau này bật Private vẫn xem được).

## 2. Board Trello (Vinh — 10 phút)
1. Tạo board **Family Finance Manager** → Add members: 5 tài khoản nhóm.
2. Tạo 5 list đúng quy ước đề cương: `Backlog` · `To Do` · `In Progress` · `Review` · `Done`.
3. Import: Menu `...` → **Import and Export** → *Import from CSV* → chọn `docs/trello-board.csv` (30 thẻ, đã có tiêu đề/mô tả/list/label).
   - CSV không gán được member thật (Trello cần username): sau khi import, bật Power-Up **"Card Owner"** hoặc kéo tay thẻ về avatar — 30 thẻ ≈ 15 phút, chia cho Vinh + Tú.
4. Thêm **Labels** màu theo Epic: Hạ tầng, Kiến trúc, Cơ sở dữ liệu, Xác thực, Giao dịch, Ngân sách, Báo cáo, DevOps, Kiểm thử, Thiết kế.
5. Dán link board vào bảng "Tổng hợp liên kết" của file Word trước khi nộp.

## 3. Chạy môi trường dev (cả nhóm, mỗi người một máy)
```bash
cp .env.example .env
docker compose --env-file .env up -d --build      # lần đầu ~5-8 phút (tải image)
bash scripts/smoke-test.sh
```

**Tiêu chí chấp nhận Sprint 1 (đủ 5 dòng này là PASS):**
| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | `docker compose ps` | 4 service `running`, `ffa-app` = `healthy` |
| 2 | `curl http://localhost:8090/api/health` | `"status":"UP"`, `"connected":true`, `"tables":14`, `"flywayVersion":"2"` |
| 3 | Mở `http://localhost:5173` | trang hiển thị "Backend: UP" + "MySQL: kết nối OK — 14 bảng" |
| 4 | Adminer `http://localhost:8081` (server `mysql`, user/password trong `.env`) | thấy 13 bảng + 11 dòng `transactions` |
| 5 | Action `ci` trên GitHub | ✅ **ĐÃ ĐẠT** — run #4 (26/10/2026) 3 job xanh: backend build + test, frontend typecheck + build, compose smoke (`docker compose up -d --wait` rồi `/api/health` trả `"connected":true`) |
| 6 | `docker compose down && docker compose up -d` | dữ liệu seed **vẫn còn** (volume hoạt động) |

Dòng 1–4 và 6 là kiểm tra trên máy từng thành viên (An chạy một lần rồi báo kết quả); dòng 5 đã được chứng minh bằng CI nên môi trường được coi là chạy được trên máy sạch.

Lệnh dọn dẹp: `bash scripts/dev-down.sh` (giữ dữ liệu) · `bash scripts/dev-down.sh --purge` (xóa sạch, chạy lại migration từ đầu).

## 4. Figma 4 màn hình (Vinh — 6 giờ, hạn 27/10)
Frame cần có để khớp đề cương & SRS mục 6:
1. `Dashboard` — 4 card số dư + biểu đồ tròn cơ cấu chi + cột 12 tháng + danh sách cảnh báo.
2. `Giao dịch` — bảng + filter tháng + modal "Thêm giao dịch" (đúng form UC-07).
3. `Ngân sách` — thanh tiến độ 3 trạng thái SAFE/WARNING/OVER (AC-03).
4. `Hộ gia đình` — danh sách thành viên + role + nút mời.
Thiết kế xong: **File → Share → Link (Anyone with the link can view)** → dán URL vào bảng mục 1 của file Word.

## 5. Việc còn tồn của Sprint 1
| Mã | Việc | Ai | Hạn |
|---|---|---|---|
| S1-01 | Đối chiếu lại bìa Word với hồ sơ lớp: Nguyễn Trường An (181), Nguyễn Tiến Thành (283), Bàng Trọng Tú (444), Lê Hoàng Xuân Vinh (227) | An | 27/10 |
| S1-02 | Dán link repo + board + Figma vào mục 1 file Word | An | 27/10 |
| S1-03 | Nộp file Word + Excel + 3 link trên biểu mẫu của giảng viên | An | 27/10 |
| S1-04 | Chụp 2 ảnh: `docker compose ps` healthy + dashboard Figma | Vinh, Tú | 27/10 |
| S1-05 | Quyết định Q-01, Q-02, Q-03 trong SRS mục 10.2 | An +GVHD | 24/10 |

## 6. Lỗi hay gặp khi chạy lần đầu
| Triệu chứng | Nguyên nhân | Cách xử lý |
|---|---|---|
| `port is already allocated` (8090/5173/3307) | máy khác/chạy đề thi đang chiếm | sửa `APP_PORT`, `WEB_PORT`, `DB_PORT_HOST` trong `.env` |
| `ffa-app` restart loop, log `Access denied for user` | `.env` đổi `MYSQL_PASSWORD` sau khi đã tạo volume | `bash scripts/dev-down.sh --purge` rồi up lại |
| `flyway validate` lỗi `Schema `ffa_db` non-empty` | DB từng có bảng cũ | xóa volume (`--purge`), hoặc `spring.flyway.clean-disabled=false` khi dev |
| Vite 502 khi gọi `/api` | app chưa lên | chờ healthcheck `healthy` rồi reload; proxy trỏ `http://app:8080` |
| Windows: hot reload không bắt | mount volume cần polling | đã bật `usePolling: true` trong `vite.config.ts` |
| `npm ci` báo thiếu lock file | chưa commit `package-lock.json` | chạy `npm install` một lần rồi commit lock file |
