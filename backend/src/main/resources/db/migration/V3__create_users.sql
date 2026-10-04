CREATE TABLE users (
    id             BIGSERIAL PRIMARY KEY,
    email          VARCHAR(255) NOT NULL,
    password_hash  VARCHAR(100) NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_users_email UNIQUE (email)
);

-- Each expense belongs to one user. Nullable only because expenses created in Stage 1/2
-- have no owner; they stay in the table but no user can see them. The service always sets it
-- for new rows. Categories stay shared by all users.
ALTER TABLE expenses ADD COLUMN user_id BIGINT REFERENCES users (id);
CREATE INDEX idx_expenses_user_id ON expenses (user_id);
