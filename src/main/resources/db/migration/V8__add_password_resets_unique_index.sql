CREATE UNIQUE INDEX uq_password_resets_active
    ON password_resets(user_id)
    WHERE status IN ('PENDING', 'VERIFIED');
