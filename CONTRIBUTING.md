# Quy trình làm việc nhóm (Git Flow, Commit, PR, Standup)
Áp dụng cho cả 4 thành viên dự án **Budglee** từ Sprint 1. Đây là mục 7 của đề cương, bản chi tiết để "soi" khi review.
Board tiến độ là **Jira**, project **DUAN**: https://thenng.atlassian.net/jira/core/projects/DUAN/board — bản xuất 38 issue (DUAN-30…DUAN-67) nằm tại [`docs/jira-board.csv`](docs/jira-board.csv).

## 1. Nhánh
| Nhánh | Ý nghĩa | Ai được push trực tiếp |
|---|---|---|
| `main` | bản phát hành ổn định, deploy production | không ai — chỉ qua PR merge |
| `develop` | nhánh tích hợp, deploy staging | không ai — chỉ qua PR |
| `feat/DUAN-32-erd-schema` | nhánh tính năng | chủ thẻ |
| `fix/<mã-issue>-<mô-tả>` | sửa lỗi | chủ thẻ |
| `hotfix/<mô-tả>` | vá gấp từ `main` | An/Vinh |

Quy tắc tên nhánh: `loai/<mã-issue-jira>-<kebab-mo-tả>`, ví dụ `feat/DUAN-50-transaction-crud`.

## 2. Commit — Conventional Commits
```
feat:  tính năng mới          fix:   sửa lỗi
docs:  tài liệu               refactor: không đổi hành vi
test:  bổ sung test           chore: linh tinh (build, deps, config)
perf:  tối ưu hiệu năng
```
Định dạng: `<type>(<scope>): <mô tả ở thể mệnh lệnh, không có dấu chấm cuối>`, và **ghi mã issue Jira (project DUAN) ở phần thân**:
```
feat(transaction): them loc theo khoanh tien

Refs: DUAN-50 (Budglee)
```
Scope gợi ý: `auth, family, wallet, category, transaction, budget, goal, report, docker, ci, docs`.

## 3. Pull Request
1. Tạo PR hướng `develop`; tiêu đề cùng quy ước commit.
2. Template PR bắt buộc điền: What / Why / How tested / Screenshots / Linked issue (mã `DUAN-xx`).
3. Điều kiện merge: **≥ 1 approve** + CI xanh (build + test + lint) + không conflict.
4. Merge style: `Squash merge` để `develop` sạch, mỗi issue một commit.
5. Self-merge chỉ được phép với commit `docs:` hoặc `chore:` không ảnh hưởng runtime.

## 4. Kiểm tra trước khi push (local)
```bash
cd backend  && gradle test            # hoặc ./gradlew test khi đã có wrapper
cd frontend && npm run typecheck      # eslint sẽ bật ở Sprint 3 (issue DUAN-63)
bash scripts/smoke-test.sh            # khi đã đổi hạ tầng
```

## 5. Họp & tiến độ
- **Daily Standup 20:30 hằng ngày qua Google Meet** (issue DUAN-37), 15 phút, 3 câu: đã làm / sẽ làm / vướng.
- Cập nhật issue trên **Jira board DUAN** theo đúng thứ tự trạng thái: `Backlog → To Do → In Progress → Done` (bật thêm cột `Code Review` khi cần). Nhóm chuyển từ board Trello sang Jira project DUAN từ Sprint 1 — mọi tham chiếu board trong tài liệu là Jira, bản xuất issue mới nhất là [`docs/jira-board.csv`](docs/jira-board.csv).
- Mỗi issue có **assignee (người chính) + reporter/backstop (người dự phòng)**, đối chiếu sheet `Phan cong RACI` trong file Excel kế hoạch; issue quá 24h ở `In Progress` phải nêu ở standup.
- Lịch sprint: S1 20/10–26/10/2026 · S2 27/10–02/11/2026 · S3 03/11–09/11/2026 · S4 10/11–14/11/2026.
- Kết thúc sprint: phiên Review 30 phút quay màn hình + Retrospective 3 cột (Giữ / Bỏ / Thử), ghi kết luận vào Sprint Report của Jira.

## 6. Cam kết bảo mật
- Không commit `.env`, key, token, mật khẩu; không dán credential vào chat/PR.
- Secret dùng GitHub Secrets; file mẫu luôn là `.env.example`.
- Phát hiện lộ secret: báo An ngay, rotate trong ≤ 1 giờ, commit `chore(security): rotate ...`.
