# Budglee

**Hệ thống Quản lý thu chi cá nhân và gia đình (Budglee – Personal & Family Finance Management)** — đồ án **Đề tài 10**, môn Phát triển Dự án Phần mềm.
Nhóm 03 · Lớp HN-KS24-CNTT3 · Giảng viên hướng dẫn: **ThS. Hồ Xuân Hùng** · Hạn nộp báo cáo đề cương: **26/10/2026**

**Liên kết:** GitHub repo [CJustin23/ProjectIT206](https://github.com/CJustin23/ProjectIT206) · Jira board **DUAN** (https://thenng.atlassian.net/jira/core/projects/DUAN/board — danh sách 38 issue DUAN-30…DUAN-67 xuất tại [`docs/jira-board.csv`](docs/jira-board.csv)) · **Lark wiki (đặc tả SRS của nhóm)** — bản đặc tả được nhóm duy trì song song với [`docs/SRS.md`](docs/SRS.md).

## 1. Thành viên

| STT | Họ và tên | MSV | Vai trò | Phạm vi chính |
|-----|-----------|-----|---------|----------------|
| 1 | Nguyễn Trường An (An) | B24DTCN181 | Backend Lead | Kiến trúc hệ thống, RESTful API cốt lõi (giao dịch, ngân sách, mục tiêu tiết kiệm), bảo mật & phân quyền JWT/RBAC, điều phối tiến độ |
| 2 | Nguyễn Tiến Thành (Thành) | B24DTCN283 | Frontend & Backend Lead | Thiết kế UI/UX trên Figma (theo mẫu giao diện Budglee), Dashboard & biểu đồ (React/Next.js), bộ lọc giao dịch, cảnh báo vượt ngân sách, phát triển API thống kê |
| 3 | Bàng Trọng Tú (Tú) | B24DTCN444 | Database & Data Lead | ERD (Sổ quỹ, Giao dịch, Danh mục, Ngân sách gia đình), chuẩn hóa CSDL, index tối ưu truy vấn báo cáo tháng, Redis caching, migration |
| 4 | Lê Hoàng Xuân Vinh (Vinh) | B24DTCN227 | DevOps & QA Lead | Docker/Compose, CI/CD GitHub Actions, Nginx + VPS, backup tự động, test cases + kiểm thử bảo mật |

## 2. Công nghệ

| Tầng | Lựa chọn |
|------|----------|
| Frontend | ReactJS/NextJS + TypeScript + Tailwind CSS + Zustand/Redux Toolkit + Axios + Recharts. Bản đang chạy trong repo: React 18 + Vite 6 + TypeScript + Tailwind + Zustand + Axios + Recharts (Next.js là hướng nêu trong báo cáo đề cương, chưa migrate) |
| Backend | Spring Boot (Java 21), Spring Security + JWT, Spring Data JPA, RESTful API, Flyway migration |
| Database | **MySQL 8.0** — báo cáo đề cương ghi "PostgreSQL / MySQL"; nhóm chọn và đã hiện thực **MySQL** cho toàn bộ môi trường dev/staging/prod. **Redis** cho caching số liệu thống kê dashboard (DUAN-57) và hàng đợi thông báo; Caffeine là cache L1 in-process |
| DevOps | Docker + Docker Compose, Nginx reverse proxy + HTTPS, GitHub Actions CI/CD, backup DB tự động (cron), Ubuntu VPS |

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
| Redis (`ffa-redis`) | chỉ trong `ffa-net`, cổng 6379 nội bộ — không expose ra host |

Dừng môi trường: `docker compose down` (giữ dữ liệu). Xóa cả volume DB: `docker compose down -v`.

## 4. Cấu trúc thư mục

```
ProjectIT206/
├── backend/                Spring Boot API (Gradle 8.14 wrapper, Java 21)
│   ├── gradlew             ./gradlew build — không cần cài Gradle
│   └── src/main/resources/db/migration/   Flyway V1 schema · V2 seed · V3 version
├── frontend/               React 18 + Vite 6 + TypeScript + Tailwind
├── docker-compose.yml      môi trường dev: mysql · redis · app · web · adminer
├── docker-compose.prod.yml môi trường VPS: db · redis · app · web · nginx edge (HTTPS)
├── deploy/                 nginx.ffa.conf · web.Dockerfile · env.prod.example
├── .github/workflows/      ci.yml (build+test+smoke) · release.yml (GHCR) · deploy.yml (SSH)
├── .env.example            biến môi trường dev (copy sang .env, không commit)
├── docs/
│   ├── SRS.md                 19 use case, quy tắc nghiệp vụ, NFR (bản đồng bộ với Lark wiki của nhóm)
│   ├── ERD.md                 Sơ đồ quan hệ + từ điển dữ liệu
│   ├── api.md                 Hợp đồng API — đóng băng schema 02/11
│   ├── architecture.md        Sơ đồ triển khai + luồng JWT + chiến lược cache
│   ├── sprint1-checklist.md   Đầu việc + cách chạy + tiêu chí nghiệm thu
│   ├── TEST-PLAN.md           28 ca kiểm thử + checklist review bảo mật
│   ├── DEPLOY.md              Runbook VPS: UFW, HTTPS, GHCR, backup/restore, rollback
│   ├── backlog.csv            Backlog mức epic theo đề cương
│   └── jira-board.csv         Bản xuất 38 issue Jira DUAN-30…DUAN-67 (owner, trạng thái, hạn)
└── scripts/                dev-up.sh · dev-down.sh · smoke-test.sh · api-smoke.sh · backup-db.sh · backup-prod.sh
```

## 5. Quy ước nhánh & commit

- `main` = bản phát hành ổn định, `develop` = nhánh tích hợp. Tính năng: `feat/<mã-thẻ>-<mô-tả>`.
- Conventional Commits: `feat:`, `fix:`, `docs:`, `refactor:`, `chore:`, `test:`.
- Mọi merge vào `develop` bắt buộc qua Pull Request, tối thiểu 1 reviewer approve và CI xanh.
- Chi tiết trong [`CONTRIBUTING.md`](CONTRIBUTING.md).

## 6. Liên kết dự án

- Repository: https://github.com/CJustin23/ProjectIT206  *(nhánh `main` + `develop`)*
- Jira board (project **DUAN**): https://thenng.atlassian.net/jira/core/projects/DUAN/board · bản xuất issue: [`docs/jira-board.csv`](docs/jira-board.csv)
- Figma (DUAN-30, DUAN-31 — UI/UX & wireframe, Thành): https://www.figma.com/file/&lt;file-id&gt;
- Đặc tả SRS: duy trì trên **Lark wiki (đặc tả SRS của nhóm)**; [`docs/SRS.md`](docs/SRS.md) là bản đồng bộ trong repo.

## 7. Trạng thái theo sprint *(tính đến 26/10/2026)*

| Sprint | Nội dung | Tình trạng |
|--------|----------|------------|
| 1 (20/10–26/10/2026) | DUAN-30…DUAN-37: UI/UX Figma, wireframe báo cáo, ERD & chuẩn hóa CSDL, git flow + branch, Docker Compose dev (MySQL + Redis), khởi tạo Jira board DUAN, họp nhóm | ✅ **Done** toàn bộ 8 issue |
| 2 (27/10–02/11/2026) | DUAN-38…DUAN-47: Entity/Repository/JPA, API đăng ký–đăng nhập + JWT/RBAC, khởi tạo React, layout, Axios interceptor, Flyway migration + seed data, biến môi trường | 🟡 DUAN-38 (An), DUAN-41 (Thành), DUAN-44 (Tú) **In Progress**; các issue còn lại To Do |
| 3 (03/11–09/11/2026) | DUAN-48…DUAN-57: API ví/danh mục/giao dịch, màn hình React, ngân sách + cảnh báo vượt, biểu đồ Recharts, index báo cáo tháng, Redis caching số liệu dashboard | ⚪ To Do (file `ci.yml` đã có sẵn trong repo từ sớm; issue DUAN-63 vẫn để Sprint 4) |
| 4 (10/11–14/11/2026) | DUAN-58…DUAN-67: mục tiêu tiết kiệm, insight & xuất báo cáo, tối ưu SQL, CI/CD GitHub Actions, cron backup DB, test cases + kiểm thử bảo mật, Nginx + HTTPS, đóng gói & deploy VPS | ⚪ To Do (`scripts/backup-db.sh`, `scripts/backup-prod.sh` đã sẵn) |
