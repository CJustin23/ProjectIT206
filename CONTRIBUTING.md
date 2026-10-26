# Quy trình làm việc nhóm (Git Flow, Commit, PR, Standup)
Áp dụng cho cả 5 thành viên từ Sprint 1. Đây là mục 7 của đề cương, bản chi tiết để "soi" khi review.

## 1. Nhánh
| Nhánh | Ý nghĩa | Ai được push trực tiếp |
|---|---|---|
| `main` | bản phát hành ổn định, deploy production | không ai — chỉ qua PR merge |
| `develop` | nhánh tích hợp, deploy staging | không ai — chỉ qua PR |
| `feat/S1-DB-01-erd-schema` | nhánh tính năng | chủ thẻ |
| `fix/<mã-thẻ>-<mô-tả>` | sửa lỗi | chủ thẻ |
| `hotfix/<mô-tả>` | vá gấp từ `main` | An/Vinh |

Quy tắc tên nhánh: `loai/<sprint>-<mã-thẻ>-<kebab-mo-tả>`, ví dụ `feat/S2-BE-05-transaction-crud`.

## 2. Commit — Conventional Commits
```
feat:  tính năng mới          fix:   sửa lỗi
docs:  tài liệu               refactor: không đổi hành vi
test:  bổ sung test           chore: linh tinh (build, deps, config)
perf:  tối ưu hiệu năng
```
Định dạng: `<type>(<scope>): <mô tả ở thể mệnh lệnh, không có dấu chấm cuối>`, và **ghi mã thẻ Trello ở phần thân**:
```
feat(transaction): them loc theo khoanh tien

Refs: FFMS BE-05
```
Scope gợi ý: `auth, family, wallet, category, transaction, budget, goal, report, docker, ci, docs`.

## 3. Pull Request
1. Tạo PR hướng `develop`; tiêu đề cùng quy ước commit.
2. Template PR bắt buộc điền: What / Why / How tested / Screenshots / Linked card.
3. Điều kiện merge: **≥ 1 approve** + CI xanh (build + test + lint) + không conflict.
4. Merge style: `Squash merge` để `develop` sạch, mỗi thẻ một commit.
5. Self-merge chỉ được phép với commit `docs:` hoặc `chore:` không ảnh hưởng runtime.

## 4. Kiểm tra trước khi push (local)
```bash
cd backend  && gradle test            # hoặc ./gradlew test khi đã có wrapper
cd frontend && npm run typecheck      # eslint sẽ bật ở Sprint 3 (thẻ OPS-02)
bash scripts/smoke-test.sh            # khi đã đổi hạ tầng
```

## 5. Họp & tiến độ
- **Daily Standup 20:30 hằng ngày qua Google Meet**, 15 phút, 3 câu: đã làm / sẽ làm / vướng.
- Cập nhật thẻ trên Trello theo đúng thứ tự: `Backlog → To Do → In Progress → Review → Done`.
- Mỗi thẻ có **người chính + người dự phòng** (sheet `Phan cong RACI` trong file Excel); thẻ quá 24h ở `In Progress` phải nêu ở standup.
- Kết thúc sprint: phiên Review 30 phút quay màn hình + Retrospective 3 cột (Giữ / Bỏ / Thử).

## 6. Cam kết bảo mật
- Không commit `.env`, key, token, mật khẩu; không dán credential vào chat/PR.
- Secret dùng GitHub Secrets; file mẫu luôn là `.env.example`.
- Phát hiện lộ secret: báo An ngay, rotate trong ≤ 1 giờ, commit `chore(security): rotate ...`.
