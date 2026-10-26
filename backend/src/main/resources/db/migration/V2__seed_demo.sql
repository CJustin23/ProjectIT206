-- =============================================================================
--  V2__seed_demo.sql — Dữ liệu mẫu PHỤC VỤ DEMO/REPORT, không dùng cho production
--  Tài khoản demo: cuong@ffa.dev / mật khẩu: password  (hash bcrypt, dev-only)
--  Sinh viên: Thành (Database & Data Lead)
-- =============================================================================

INSERT INTO users (id, email, password_hash, full_name, phone, is_admin) VALUES
  (1, 'cuong@ffa.dev', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'Nguyễn Tiến Cường', '0900000001', 1),
  (2, 'thanh@ffa.dev', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'Lê Văn Thành', NULL, 0),
  (3, 'ky@ffa.dev',    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'Trần Thị Kỳ',    NULL, 0);

INSERT INTO families (id, name, base_currency, fiscal_month_start_day, created_by) VALUES
  (1, 'Gia đình An', 'VND', 1, 1);

INSERT INTO family_members (family_id, user_id, role) VALUES
  (1, 1, 'OWNER'), (1, 2, 'MEMBER'), (1, 3, 'MEMBER');

INSERT INTO wallets (id, family_id, name, type, opening_balance) VALUES
  (1, 1, 'Tiền mặt',       'CASH',        2000000),
  (2, 1, 'VCB chính',      'BANK',       25000000),
  (3, 1, 'Ví MoMo',        'E_WALLET',    1500000),
  (4, 1, 'Tiết kiệm 2026', 'SAVINGS',    10000000);

-- Danh mục chi
INSERT INTO categories (id, family_id, name, kind, icon, color, is_system, sort_order) VALUES
  (1,  1, 'Ăn uống',            'EXPENSE', 'utensils',  '#E53E3E', 1, 1),
  (2,  1, 'Đi lại',             'EXPENSE', 'car',       '#DD6B20', 1, 2),
  (3,  1, 'Hóa đơn (điện/nước)', 'EXPENSE', 'bolt',      '#D69E2E', 1, 3),
  (4,  1, 'Học tập',            'EXPENSE', 'book',      '#3182CE', 1, 4),
  (5,  1, 'Giải trí',           'EXPENSE', 'film',      '#805AD5', 1, 5),
  (6,  1, 'Sức khỏe',           'EXPENSE', 'heart',     '#38A169', 1, 6),
  (7,  1, 'Mua sắm',            'EXPENSE', 'bag',       '#319795', 1, 7);
-- Danh mục thu
INSERT INTO categories (id, family_id, name, kind, icon, color, is_system, sort_order) VALUES
  (20, 1, 'Lương',              'INCOME',  'wallet',    '#2B6CB0', 1, 1),
  (21, 1, 'Thưởng',             'INCOME',  'gift',      '#2F855A', 1, 2),
  (22, 1, 'Thu nhập phụ',       'INCOME',  'coins',     '#4A5568', 1, 3);

INSERT INTO transactions (family_id, wallet_id, category_id, transfer_wallet_id, type, amount, transaction_date, note, created_by) VALUES
  (1, 2, 20, NULL, 'INCOME',  22000000, '2026-10-03', 'Lương tháng 10', 1),
  (1, 1, 1,  NULL, 'EXPENSE',   185000, '2026-10-04', 'Cơm trưa văn phòng', 1),
  (1, 3, 7,  NULL, 'EXPENSE',   690000, '2026-10-05', 'Áo khoác online', 2),
  (1, 2, 3,  NULL, 'EXPENSE',  1450000, '2026-10-07', 'Tiền điện tháng 9', 1),
  (1, 1, 2,  NULL, 'EXPENSE',   120000, '2026-10-08', 'Xăng', 1),
  (1, 1, 1,  NULL, 'EXPENSE',   430000, '2026-10-09', 'Ăn tối gia đình', 3),
  (1, 2, 4,  NULL, 'EXPENSE',  2500000, '2026-10-10', 'Học phí khóa tiếng Anh', 3),
  (1, 1, 5,  NULL, 'EXPENSE',   320000, '2026-10-11', 'Xem phim cuối tuần', 2),
  (1, 3, 1,  NULL, 'EXPENSE',    96000, '2026-10-12', 'Cà phê', 1),
  (1, 2, 6,  NULL, 'EXPENSE',   780000, '2026-10-13', 'Khám răng', 1),
  (1, 2, NULL, 4,  'TRANSFER', 3000000, '2026-10-15', 'Chuyển sang tiết kiệm', 1);

INSERT INTO budgets (family_id, category_id, period_start, period_end, limit_amount) VALUES
  (1, 1, '2026-10-01', '2026-10-31', 3000000),
  (1, 2, '2026-10-01', '2026-10-31', 1000000),
  (1, 5, '2026-10-01', '2026-10-31', 800000),
  (1, 7, '2026-10-01', '2026-10-31', 2000000);

INSERT INTO savings_goals (id, family_id, name, target_amount, saved_amount, deadline) VALUES
  (1, 1, 'Mua laptop mới',  25000000,  3000000, '2026-12-31'),
  (2, 1, 'Du lịch Đà Nẵng', 15000000,        0, '2027-04-30');
