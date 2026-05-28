ALTER TABLE password_resets ALTER COLUMN status DROP DEFAULT;

ALTER TABLE password_resets ALTER COLUMN status TYPE VARCHAR(20) USING status::text;

DROP TYPE password_reset_status;

ALTER TABLE password_resets ALTER COLUMN status SET DEFAULT 'PENDING';
