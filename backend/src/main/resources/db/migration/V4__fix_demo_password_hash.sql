-- =============================================================================
--  V4__fix_demo_password_hash.sql — sửa mật khẩu demo của tài khoản seed
--  Người chịu trách nhiệm: An (Leader)
--  Lý do: hash trong V2__seed_demo.sql là chuỗi bcrypt mẫu KHÔNG tương ứng với
--  mật khẩu "password" nên mọi lần đăng nhập demo (và api-smoke trên CI) đều 401.
--  Không sửa V2 (đã áp dụng ở máy thành viên) — sửa bằng migration mới.
--  Tài khoản demo sau này: an@ffa.dev / password  (thanh@ffa.dev, tu@ffa.dev likewise)
-- =============================================================================

UPDATE users
SET    password_hash = '$2a$10$qtbi313Z4lrlBqdfKHJ7nekX0nJcEyxfoyzQqmAd01NDOJnoHkJQO'
WHERE  email IN ('an@ffa.dev', 'thanh@ffa.dev', 'tu@ffa.dev');
