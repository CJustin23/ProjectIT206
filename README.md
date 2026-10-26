# Family Finance Manager

Hệ thống Quản lý Thu chi Cá nhân và Hộ gia đình — đồ án **Đề tài 10**, môn Phát triển Dự án Phần mềm.
Nhóm 05 · Giảng viên hướng dẫn: ThS. Bùi Thanh Hải · Hạn nộp proposal: 23/10/2026

## 1. Thành viên

| STT | Họ và tên | MSV | Vai trò | Phạm vi chính |
|-----|-----------|-----|---------|----------------|
| 1 | Nguyễn Tiến Cường (An) — Leader | B24DCCN181 | Backend Lead | Kiến trúc, REST API, Auth/JWT, màn hình hộ gia đình, điều phối |
| 2 | (Thành) | B24DCCN283 | Database & Data Lead | ERD, migration, index, dữ liệu mẫu |
| 3 | (Tú) | B24DCCN444 | Backend Engineer & Dashboard UI | Transaction, Budget, Report API, biểu đồ Recharts |
| 4 | (Vinh) | B24DCCN227 | DevOps, QA & Frontend Lead | Docker, CI/CD, VPS, kiểm thử, Figma + khung UI React |

## 2. Công nghệ

| Tầng | Lựa chọn |
|------|----------|
| Frontend | React 18 + Vite + TypeScript, Tailwind CSS, Recharts, Zustand, Axios |
| Backend | Spring Boot 3.5 (Java 21), Spring Security + JWT, Spring Data JPA, Flyway |
| Database | MySQL 8.0, Caffeine cache (Redis dự kiến ở sprint 4) |
| DevOps | Docker Compose, GitHub Actions, Nginx, Ubuntu VPS |

## 3. Chạy môi trường dev (Sprint 1)

```bash
cp .env.example .env          # sửa mật khẩu/ cổng nếu cần
docker compose up -d --build
docker compose ps
curl http://localhost:8090/api/health
```

| Dịch vụ | URL dev |
|---------|---------|
| Frontend (Vite, hot reload) | http://localhost:5173 |
| Backend API | http://localhost:8090/api |
| Spring Boot health | http://localhost:8090/api/health |
| Adminer (xem CSDL) | http://localhost:8081 |

Dừng môi trường: `docker compose down` (giữ dữ liệu). Xóa cả volume DB: `docker compose down -v`.

## 4. Cấu trúc thư mục

```
ProjectIT206/
├── backend/          Spring Boot API (Gradle, Java 21)
│   └── src/main/resources/db/migration/   Flyway — lịch sử schema
├── frontend/         React + Vite + TypeScript
├── docker-compose.yml
├── .env.example      Các biến môi trường (copy sang .env, .env không commit)
├── docs/
│   ├── SRS.md                 Đặc tả yêu cầu người dùng
│   ├── ERD.md                 Sơ đồ quan hệ + từ điển dữ liệu
│   ├── api.md                 Hợp đồng API (freeze ở Sprint 2)
│   ├── trello-board.csv       Nội dung khởi tạo board
│   └── sprint1-checklist.md   Đầu việc + mốc ngày Sprint 1
└── scripts/          dev-up.sh, dev-down.sh, seed.sh
```

## 5. Quy ước nhánh & commit

- `main` = bản phát hành ổn định, `develop` = nhánh tích hợp. Tính năng: `feat/<mã-thẻ>-<mô-tả>`.
- Conventional Commits: `feat:`, `fix:`, `docs:`, `refactor:`, `chore:`, `test:`.
- Mọi merge vào `develop` bắt buộc qua Pull Request, tối thiểu 1 reviewer approve và CI xanh.

## 6. Liên kết dự án

- Repository: https://github.com/CJustin23/ProjectIT206  *(nhánh `main` + `develop`)*
- Trello board: https://trello.com/b/&lt;board-id&gt;/family-finance-manager
- Figma / SRS: https://www.figma.com/file/&lt;file-id&gt;

## 7. Trạng thái theo sprint

| Sprint | Nội dung | Tình trạng (cập nhật 26/10/2026) |
|--------|----------|----------------------------------|
| 1 (20–23/10) | SRS, ERD, khởi tạo repo, Docker Compose dev, Trello board | ✅ Đã xong phần kỹ thuật — còn: import Trello, branch protection, Figma |
| 2 (25–27/10) | Auth, CRUD danh mục/ví/giao dịch, khung UI | 🟡 Đang triển khai |
| 3 (28–30/10) | Ngân sách, báo cáo, biểu đồ, CI tự động | 🟡 CI đã xong sớm (workflow `ci.yml`: backend build + test, frontend typecheck + build, compose smoke — cả 3 job xanh ở run #4 26/10); còn module báo cáo, GHCR, deploy SSH |
| 4 (31/10–01/11) | Kiểm thử, đóng gói, deploy VPS + HTTPS | ⚪ Chưa bắt đầu (scripts/backup-db.sh đã sẵn) |
