-- ====================================================
-- EXTENSIONS
-- ====================================================
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ====================================================
-- USER ROLES
-- ====================================================
CREATE TABLE user_roles (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_name VARCHAR NOT NULL UNIQUE
);

INSERT INTO user_roles (id, role_name) VALUES
    (gen_random_uuid(), 'USER'),
    (gen_random_uuid(), 'MANAGER'),
    (gen_random_uuid(), 'SUPER_ADMIN');

-- ====================================================
-- BRANCHES
-- ====================================================
CREATE TABLE branches (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    address              VARCHAR NOT NULL,
    personal_code_prefix VARCHAR NOT NULL,
    is_active            BOOLEAN   DEFAULT true,
    next_sequence        INT       DEFAULT 1,
    created_at           TIMESTAMP DEFAULT now(),
    updated_at           TIMESTAMP DEFAULT now()
);

-- ====================================================
-- USERS
-- ====================================================
CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR NOT NULL UNIQUE,
    password_hash VARCHAR NOT NULL,
    first_name    VARCHAR NOT NULL,
    last_name     VARCHAR NOT NULL,
    phone         VARCHAR,
    date_of_birth DATE,
    personal_code VARCHAR UNIQUE,
    branch_id     UUID REFERENCES branches(id),
    role_id       UUID NOT NULL REFERENCES user_roles(id),
    chat_banned   BOOLEAN   DEFAULT false,
    status        INT       DEFAULT 0,  -- 0=ACTIVE, 1=INACTIVE, 2=DELETED, 3=PENDING_DELETION
    created_at    TIMESTAMP DEFAULT now(),
    updated_at    TIMESTAMP DEFAULT now()
);

-- ====================================================
-- USER PERSONAL CODES (история смены филиалов)
-- ====================================================
CREATE TABLE user_personal_codes (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID NOT NULL REFERENCES users(id),
    personal_code VARCHAR NOT NULL,
    branch_id     UUID NOT NULL REFERENCES branches(id),
    is_active     BOOLEAN   DEFAULT false,
    created_at    TIMESTAMP DEFAULT now()
);

-- ====================================================
-- ORDERS
-- ====================================================
CREATE TABLE orders (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users(id),
    branch_id  UUID NOT NULL REFERENCES branches(id),
    price      NUMERIC(12, 2) DEFAULT 0,
    weight     NUMERIC(12, 3) DEFAULT 0,
    item_count INT            DEFAULT 0,
    status     VARCHAR NOT NULL,  -- PENDING_PICKUP / DELIVERED
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now()
);

-- ====================================================
-- PRODUCTS
-- ====================================================
CREATE TABLE products (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hatch      VARCHAR NOT NULL,
    user_id    UUID NOT NULL REFERENCES users(id),
    order_id   UUID REFERENCES orders(id),
    status     VARCHAR NOT NULL,  -- IN_CHINA, ON_THE_WAY, IN_KG, DELIVERED
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now()
);

-- ====================================================
-- PRODUCT HISTORIES
-- ====================================================
CREATE TABLE product_histories (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES products(id),
    status     VARCHAR   NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

-- ====================================================
-- NEWS
-- ====================================================
CREATE TABLE news (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cover      VARCHAR,
    title      VARCHAR NOT NULL,
    content    TEXT    NOT NULL,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now()
);

-- ====================================================
-- PUSH TOKENS
-- ====================================================
CREATE TABLE push_tokens (
    id         SERIAL PRIMARY KEY,
    user_id    UUID NOT NULL REFERENCES users(id),
    token      TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now()
);

-- ====================================================
-- REFRESH SESSIONS
-- ====================================================
CREATE TABLE refresh_sessions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id),
    jti         VARCHAR UNIQUE NOT NULL,
    fingerprint VARCHAR,
    ip          VARCHAR,
    user_agent  TEXT,
    created_at  TIMESTAMP DEFAULT now(),
    expires_at  TIMESTAMP,
    revoked_at  TIMESTAMP
);

-- ====================================================
-- CONFIRMATIONS
-- ====================================================
CREATE TABLE confirmations (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code                VARCHAR(12) NOT NULL,
    confirmation_status VARCHAR     NOT NULL,
    attempts            INT         DEFAULT 3,
    expires_at          TIMESTAMP,
    last_sent_at        TIMESTAMP,
    user_id             UUID NOT NULL REFERENCES users(id)
);

-- ====================================================
-- CHAT MESSAGES
-- ====================================================
CREATE TABLE chat_messages (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id),
    manager_id  UUID REFERENCES users(id),
    sender_type VARCHAR  NOT NULL,  -- USER / MANAGER
    message     TEXT     NOT NULL,
    is_read     BOOLEAN  DEFAULT false,
    created_at  TIMESTAMP DEFAULT now()
);

-- ====================================================
-- NOTIFICATION LOGS
-- ====================================================
CREATE TABLE notification_logs (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type           VARCHAR NOT NULL,  -- TEST / BROADCAST / BY_PERSONAL_CODE
    title          VARCHAR NOT NULL,
    body           TEXT    NOT NULL,
    data_json      JSON,
    code_prefix    VARCHAR,
    target_user_id UUID REFERENCES users(id),
    created_by_id  UUID NOT NULL REFERENCES users(id),
    created_at     TIMESTAMP DEFAULT now()
);

-- ====================================================
-- INDEXES
-- ====================================================
CREATE INDEX idx_users_personal_code ON users(personal_code);
CREATE INDEX idx_users_branch_id     ON users(branch_id);
CREATE INDEX idx_products_user_id    ON products(user_id);
CREATE INDEX idx_products_order_id   ON products(order_id);
CREATE INDEX idx_products_hatch      ON products(hatch);
CREATE INDEX idx_products_status     ON products(status);
CREATE INDEX idx_orders_user_id      ON orders(user_id);
CREATE INDEX idx_orders_branch_id    ON orders(branch_id);
CREATE INDEX idx_refresh_sessions_jti ON refresh_sessions(jti);
CREATE INDEX idx_chat_messages_user_id ON chat_messages(user_id);
