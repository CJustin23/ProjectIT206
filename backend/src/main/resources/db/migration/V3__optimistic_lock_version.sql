-- =============================================================================
--  V3__optimistic_lock_version.sql — thêm cột version cho optimistic locking
--  Người chịu trách nhiệm: An (Leader)
--  Lý do: docs/api.md mô tả `version` và header `If-Match` khi sửa giao dịch /
--  ngân sách (chống 2 người cùng sửa một bản ghi, rủi ro R05 trong kế hoạch).
--  V1 chưa có cột này nên bổ sung bằng migration mới, KHÔNG sửa V1/V2 đã chạy.
-- =============================================================================

ALTER TABLE transactions
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE budgets
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
