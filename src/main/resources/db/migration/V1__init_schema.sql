-- ====================================================
-- EXTENSIONS
-- ====================================================
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ====================================================
-- Переименовать конфликтующие таблицы старой Node.js БД
-- IF EXISTS — безопасно на чистой БД, просто ничего не сделает
-- ====================================================
ALTER TABLE IF EXISTS news             RENAME TO news_old;
ALTER TABLE IF EXISTS push_tokens      RENAME TO push_tokens_old;
ALTER TABLE IF EXISTS refresh_sessions RENAME TO refresh_sessions_old;

-- ====================================================
-- BRANCHES
-- ====================================================
CREATE TABLE IF NOT EXISTS branches (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    address              VARCHAR NOT NULL,
    personal_code_prefix VARCHAR NOT NULL,
    next_sequence        INT              DEFAULT 1,
    latitude             DOUBLE PRECISION,
    longitude            DOUBLE PRECISION,
    photo_url            VARCHAR(255),
    phone                VARCHAR(50),
    working_hours        VARCHAR(100),
    created_at           TIMESTAMP        DEFAULT now(),
    updated_at           TIMESTAMP        DEFAULT now()
);

-- ====================================================
-- MANAGERS
-- ====================================================
CREATE TABLE IF NOT EXISTS managers (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    login         VARCHAR NOT NULL UNIQUE,
    password_hash VARCHAR NOT NULL,
    first_name    VARCHAR NOT NULL,
    last_name     VARCHAR NOT NULL,
    phone         VARCHAR NOT NULL,
    role          VARCHAR NOT NULL,
    branch_id     UUID REFERENCES branches(id),
    created_at    TIMESTAMP DEFAULT now(),
    updated_at    TIMESTAMP DEFAULT now()
);

-- ====================================================
-- USERS
-- ====================================================
CREATE TABLE IF NOT EXISTS users (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    login                VARCHAR NOT NULL UNIQUE,
    email                VARCHAR NOT NULL UNIQUE,
    password_hash        VARCHAR NOT NULL,
    first_name           VARCHAR NOT NULL,
    last_name            VARCHAR NOT NULL,
    phone                VARCHAR NOT NULL,
    date_of_birth        DATE    NOT NULL,
    personal_code        VARCHAR UNIQUE,
    branch_id            UUID REFERENCES branches(id),
    chat_banned          BOOLEAN   DEFAULT false,
    status               VARCHAR   NOT NULL DEFAULT 'ACTIVE',
    smartpoint_synced_at TIMESTAMP,
    created_at           TIMESTAMP DEFAULT now(),
    updated_at           TIMESTAMP DEFAULT now()
);

-- ====================================================
-- USER PERSONAL CODES
-- ====================================================
CREATE TABLE IF NOT EXISTS user_personal_codes (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID    NOT NULL REFERENCES users(id),
    personal_code VARCHAR NOT NULL,
    branch_id     UUID    NOT NULL REFERENCES branches(id),
    is_active     BOOLEAN   DEFAULT false,
    created_at    TIMESTAMP DEFAULT now()
);

-- ====================================================
-- PRODUCTS
-- ====================================================
CREATE TABLE IF NOT EXISTS products (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hatch      VARCHAR NOT NULL,
    user_id    UUID    NOT NULL REFERENCES users(id),
    status     VARCHAR NOT NULL,
    price      NUMERIC(12, 2),
    weight     NUMERIC(12, 3),
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now()
);

-- ====================================================
-- PRODUCT HISTORIES
-- ====================================================
CREATE TABLE IF NOT EXISTS product_histories (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID      NOT NULL REFERENCES products(id),
    status     VARCHAR   NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

-- ====================================================
-- NEWS
-- ====================================================
CREATE TABLE IF NOT EXISTS news (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    image      VARCHAR,
    title      VARCHAR NOT NULL,
    content    TEXT    NOT NULL,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now()
);

-- ====================================================
-- PUSH TOKENS
-- ====================================================
CREATE TABLE IF NOT EXISTS push_tokens (
    id         SERIAL PRIMARY KEY,
    user_id    UUID NOT NULL REFERENCES users(id),
    token      TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now()
);

-- ====================================================
-- REFRESH SESSIONS (мобильный клиент)
-- ====================================================
CREATE TABLE IF NOT EXISTS refresh_sessions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID    NOT NULL REFERENCES users(id),
    jti         VARCHAR UNIQUE NOT NULL,
    fingerprint VARCHAR,
    ip          VARCHAR,
    user_agent  TEXT,
    created_at  TIMESTAMP DEFAULT now(),
    expires_at  TIMESTAMP,
    revoked_at  TIMESTAMP
);

-- ====================================================
-- MANAGER REFRESH SESSIONS (веб-клиент)
-- ====================================================
CREATE TABLE IF NOT EXISTS manager_refresh_sessions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    manager_id  UUID    NOT NULL REFERENCES managers(id),
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
CREATE TABLE IF NOT EXISTS confirmations (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code                VARCHAR(4) NOT NULL,
    confirmation_status VARCHAR    NOT NULL,
    attempts            INT        DEFAULT 3,
    expires_at          TIMESTAMP,
    last_sent_at        TIMESTAMP,
    user_id             UUID NOT NULL REFERENCES users(id)
);

-- ====================================================
-- PASSWORD RESETS
-- ====================================================
CREATE TABLE IF NOT EXISTS password_resets (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    code        VARCHAR(4)  NOT NULL,
    reset_token UUID,
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts    INT         NOT NULL DEFAULT 3,
    expires_at  TIMESTAMP   NOT NULL,
    created_at  TIMESTAMP   NOT NULL DEFAULT now()
);

-- ====================================================
-- CHAT ROOMS
-- ====================================================
CREATE TABLE IF NOT EXISTS chat_rooms (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users(id),
    branch_id  UUID NOT NULL REFERENCES branches(id),
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now(),
    UNIQUE (user_id, branch_id)
);

-- ====================================================
-- CHAT MESSAGES
-- ====================================================
CREATE TABLE IF NOT EXISTS chat_messages (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chat_room_id UUID    NOT NULL REFERENCES chat_rooms(id),
    sender_type  VARCHAR NOT NULL,
    sender_id    UUID    NOT NULL,
    content      TEXT    NOT NULL,
    is_read      BOOLEAN   DEFAULT false,
    created_at   TIMESTAMP DEFAULT now()
);

-- ====================================================
-- NOTIFICATION LOGS
-- ====================================================
CREATE TABLE IF NOT EXISTS notification_logs (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type           VARCHAR NOT NULL,
    title          VARCHAR NOT NULL,
    body           TEXT    NOT NULL,
    data_json      JSON,
    code_prefix    VARCHAR,
    target_user_id UUID REFERENCES users(id),
    created_by_id  UUID NOT NULL REFERENCES managers(id),
    created_at     TIMESTAMP DEFAULT now()
);

-- ====================================================
-- INDEXES
-- ====================================================
CREATE INDEX IF NOT EXISTS idx_users_personal_code      ON users(personal_code);
CREATE INDEX IF NOT EXISTS idx_users_branch_id          ON users(branch_id);
CREATE INDEX IF NOT EXISTS idx_managers_branch_id       ON managers(branch_id);
CREATE INDEX IF NOT EXISTS idx_products_user_id         ON products(user_id);
CREATE INDEX IF NOT EXISTS idx_products_hatch           ON products(hatch);
CREATE INDEX IF NOT EXISTS idx_products_status          ON products(status);
CREATE INDEX IF NOT EXISTS idx_refresh_sessions_jti     ON refresh_sessions(jti);
CREATE INDEX IF NOT EXISTS idx_mgr_refresh_sessions_jti ON manager_refresh_sessions(jti);

CREATE UNIQUE INDEX IF NOT EXISTS uq_password_resets_active
    ON password_resets(user_id)
    WHERE status IN ('PENDING', 'VERIFIED');

CREATE INDEX IF NOT EXISTS idx_chat_rooms_user_id    ON chat_rooms(user_id);
CREATE INDEX IF NOT EXISTS idx_chat_rooms_branch_id  ON chat_rooms(branch_id);
CREATE INDEX IF NOT EXISTS idx_chat_messages_room_id ON chat_messages(chat_room_id);
CREATE INDEX IF NOT EXISTS idx_chat_messages_created ON chat_messages(chat_room_id, created_at);