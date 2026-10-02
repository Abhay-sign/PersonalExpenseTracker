-- Flyway runs this once on startup and records it in the flyway_schema_history table.
-- Never edit a migration after it has run; add V2__..., V3__... instead.
CREATE TABLE expenses (
    id            BIGSERIAL PRIMARY KEY,
    title         VARCHAR(100)   NOT NULL,
    amount        NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    expense_date  DATE           NOT NULL,
    note          VARCHAR(500),
    created_at    TIMESTAMPTZ    NOT NULL,
    updated_at    TIMESTAMPTZ
);

CREATE INDEX idx_expenses_expense_date ON expenses (expense_date);
