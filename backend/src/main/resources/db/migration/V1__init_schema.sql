-- =============================================================================
--  V1__init_schema.sql  —  Skeleton CSDL cho Family Finance Manager (Đề tài 10)
--  Người chịu trách nhiệm: Thành (Database & Data Lead)
--  Nguyên tắc:
--    * Mọi bảng nghiệp vụ đều có family_id để lọc theo hộ gia đình (multi-tenant).
--    * Tiền: DECIMAL(15,2), không dùng FLOAT/DOUBLE.
--    * Thời gian: DATETIME lưu giờ địa phương (serverTimezone=Asia/Ho_Chi_Minh).
--    * Khóa mềm (soft delete) bằng deleted_at cho transactions.
--  Chạy tự động bởi Flyway khi container app khởi động lần đầu.
-- =============================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------- users
CREATE TABLE users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    email           VARCHAR(160) NOT NULL,
    password_hash   VARCHAR(100) NOT NULL,
    full_name       VARCHAR(120) NOT NULL,
    phone           VARCHAR(20)  NULL,
    avatar_url      VARCHAR(255) NULL,
    is_active       TINYINT(1)   NOT NULL DEFAULT 1,
    is_admin        TINYINT(1)   NOT NULL DEFAULT 0,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login_at   DATETIME     NULL,
    CONSTRAINT uq_users_email UNIQUE (email)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------- families (hộ gia đình)
CREATE TABLE families (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    name                    VARCHAR(120) NOT NULL,
    base_currency           CHAR(3)      NOT NULL DEFAULT 'VND',
    fiscal_month_start_day  TINYINT      NOT NULL DEFAULT 1,
    created_by              BIGINT       NOT NULL,
    created_at              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_families_name UNIQUE (name),
    CONSTRAINT fk_families_created_by FOREIGN KEY (created_by) REFERENCES users (id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------- family_members (phân quyền trong hộ)
CREATE TABLE family_members (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id   BIGINT      NOT NULL,
    user_id     BIGINT      NOT NULL,
    role        ENUM('OWNER','ADMIN','MEMBER','VIEWER') NOT NULL DEFAULT 'MEMBER',
    joined_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_family_member UNIQUE (family_id, user_id),
    CONSTRAINT fk_fm_family FOREIGN KEY (family_id) REFERENCES families (id) ON DELETE CASCADE,
    CONSTRAINT fk_fm_user   FOREIGN KEY (user_id)   REFERENCES users (id)   ON DELETE CASCADE,
    INDEX idx_fm_user (user_id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------- categories (danh mục thu/chi, có cây cha-con)
CREATE TABLE categories (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id   BIGINT      NOT NULL,
    parent_id   BIGINT      NULL,
    name        VARCHAR(100) NOT NULL,
    kind        ENUM('INCOME','EXPENSE') NOT NULL,
    icon        VARCHAR(40)  NULL,
    color       CHAR(7)      NULL,
    is_system   TINYINT(1)   NOT NULL DEFAULT 0,
    sort_order  SMALLINT     NOT NULL DEFAULT 0,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_category_name UNIQUE (family_id, kind, name),
    CONSTRAINT fk_cat_family FOREIGN KEY (family_id) REFERENCES families (id) ON DELETE CASCADE,
    CONSTRAINT fk_cat_parent FOREIGN KEY (parent_id) REFERENCES categories (id) ON DELETE SET NULL,
    INDEX idx_cat_family_kind (family_id, kind)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------- wallets (tiền mặt, ngân hàng, ví điện tử, tiết kiệm)
CREATE TABLE wallets (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id       BIGINT       NOT NULL,
    name            VARCHAR(100) NOT NULL,
    type            ENUM('CASH','BANK','E_WALLET','CREDIT_CARD','SAVINGS') NOT NULL DEFAULT 'CASH',
    currency        CHAR(3)      NOT NULL DEFAULT 'VND',
    opening_balance DECIMAL(15,2) NOT NULL DEFAULT 0,
    is_archived     TINYINT(1)   NOT NULL DEFAULT 0,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_wallet_name UNIQUE (family_id, name),
    CONSTRAINT fk_wallet_family FOREIGN KEY (family_id) REFERENCES families (id) ON DELETE CASCADE,
    INDEX idx_wallet_family (family_id, is_archived)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------- transactions
CREATE TABLE transactions (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id           BIGINT       NOT NULL,
    wallet_id           BIGINT       NOT NULL,
    category_id         BIGINT       NULL,
    transfer_wallet_id  BIGINT       NULL,
    type                ENUM('INCOME','EXPENSE','TRANSFER') NOT NULL,
    amount              DECIMAL(15,2) NOT NULL,
    transaction_date    DATE         NOT NULL,
    note                VARCHAR(255) NULL,
    is_recurring        TINYINT(1)   NOT NULL DEFAULT 0,
    recurring_rule      JSON         NULL,
    created_by          BIGINT       NOT NULL,
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at          DATETIME     NULL,
    CONSTRAINT chk_tx_amount CHECK (amount > 0),
    CONSTRAINT fk_tx_family   FOREIGN KEY (family_id)  REFERENCES families (id),
    CONSTRAINT fk_tx_wallet   FOREIGN KEY (wallet_id)  REFERENCES wallets (id),
    CONSTRAINT fk_tx_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE SET NULL,
    CONSTRAINT fk_tx_transfer FOREIGN KEY (transfer_wallet_id) REFERENCES wallets (id),
    CONSTRAINT fk_tx_creator  FOREIGN KEY (created_by) REFERENCES users (id),
    INDEX idx_tx_family_date (family_id, transaction_date),
    INDEX idx_tx_wallet_date (wallet_id, transaction_date),
    INDEX idx_tx_category_date (category_id, transaction_date)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------- transaction_attachments ( hóa đơn, ảnh chứng từ)
CREATE TABLE transaction_attachments (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id  BIGINT       NOT NULL,
    file_key        VARCHAR(255) NOT NULL,
    file_name       VARCHAR(160) NOT NULL,
    mime_type       VARCHAR(80)  NOT NULL,
    size_bytes      INT          NOT NULL,
    uploaded_by     BIGINT       NOT NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_att_tx FOREIGN KEY (transaction_id) REFERENCES transactions (id) ON DELETE CASCADE,
    INDEX idx_att_tx (transaction_id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------- budgets (ngân sách theo danh mục / chu kỳ)
CREATE TABLE budgets (
    id                          BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id                   BIGINT       NOT NULL,
    category_id                 BIGINT       NOT NULL,
    wallet_id                   BIGINT       NULL,
    period_start                DATE         NOT NULL,
    period_end                  DATE         NOT NULL,
    limit_amount                DECIMAL(15,2) NOT NULL,
    alert_threshold_percent     TINYINT      NOT NULL DEFAULT 80,
    created_at                  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_budget_period CHECK (period_end >= period_start),
    CONSTRAINT chk_budget_limit  CHECK (limit_amount > 0),
    CONSTRAINT uq_budget UNIQUE (family_id, category_id, period_start),
    CONSTRAINT fk_bg_family   FOREIGN KEY (family_id)   REFERENCES families (id) ON DELETE CASCADE,
    CONSTRAINT fk_bg_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE CASCADE,
    CONSTRAINT fk_bg_wallet   FOREIGN KEY (wallet_id)   REFERENCES wallets (id),
    INDEX idx_bg_family_period (family_id, period_start, period_end)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------- savings_goals (mục tiêu tiết kiệm)
CREATE TABLE savings_goals (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id     BIGINT       NOT NULL,
    name          VARCHAR(120) NOT NULL,
    target_amount DECIMAL(15,2) NOT NULL,
    saved_amount  DECIMAL(15,2) NOT NULL DEFAULT 0,
    deadline      DATE         NULL,
    status        ENUM('ACTIVE','COMPLETED','CANCELLED') NOT NULL DEFAULT 'ACTIVE',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_goal_target CHECK (target_amount > 0),
    CONSTRAINT fk_goal_family FOREIGN KEY (family_id) REFERENCES families (id) ON DELETE CASCADE,
    INDEX idx_goal_family (family_id, status)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------- goal_contributions
CREATE TABLE goal_contributions (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    goal_id      BIGINT        NOT NULL,
    wallet_id    BIGINT        NOT NULL,
    amount       DECIMAL(15,2) NOT NULL,
    contributed_on DATE        NOT NULL,
    created_by   BIGINT        NOT NULL,
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_contrib_amount CHECK (amount > 0),
    CONSTRAINT fk_gc_goal   FOREIGN KEY (goal_id)   REFERENCES savings_goals (id) ON DELETE CASCADE,
    CONSTRAINT fk_gc_wallet FOREIGN KEY (wallet_id) REFERENCES wallets (id),
    INDEX idx_gc_goal (goal_id, contributed_on)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------- refresh_tokens (JWT rotating)
CREATE TABLE refresh_tokens (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    token_hash  VARCHAR(128) NOT NULL,
    expires_at  DATETIME     NOT NULL,
    revoked_at  DATETIME     NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_rt_hash UNIQUE (token_hash),
    CONSTRAINT fk_rt_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_rt_user (user_id, expires_at)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------- notifications
CREATE TABLE notifications (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT   NOT NULL,
    family_id   BIGINT   NOT NULL,
    type        VARCHAR(40) NOT NULL,
    title       VARCHAR(160) NOT NULL,
    body        VARCHAR(500) NULL,
    read_at     DATETIME NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_nt_user   FOREIGN KEY (user_id)   REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_nt_family FOREIGN KEY (family_id) REFERENCES families (id) ON DELETE CASCADE,
    INDEX idx_nt_user (user_id, read_at)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------- audit_logs
CREATE TABLE audit_logs (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id   BIGINT      NULL,
    user_id     BIGINT      NULL,
    action      VARCHAR(60) NOT NULL,
    entity      VARCHAR(40) NOT NULL,
    entity_id   BIGINT      NULL,
    payload     JSON        NULL,
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_family (family_id, created_at),
    INDEX idx_audit_user (user_id, created_at)
) ENGINE=InnoDB;
