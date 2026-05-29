CREATE TYPE password_reset_status AS ENUM ('PENDING', 'VERIFIED', 'USED');

CREATE TABLE password_resets (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    code         VARCHAR(4) NOT NULL,
    reset_token  UUID,
    status       password_reset_status NOT NULL DEFAULT 'PENDING',
    attempts     INT NOT NULL DEFAULT 3,
    expires_at   TIMESTAMP NOT NULL,
    created_at   TIMESTAMP NOT NULL DEFAULT now()
);
