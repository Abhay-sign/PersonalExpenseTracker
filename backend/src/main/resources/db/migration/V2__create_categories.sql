-- One category has many expenses; each expense has at most one category (optional).
CREATE TABLE categories (
    id    BIGSERIAL PRIMARY KEY,
    name  VARCHAR(50) NOT NULL,
    CONSTRAINT uq_categories_name UNIQUE (name)
);

ALTER TABLE expenses ADD COLUMN category_id BIGINT REFERENCES categories (id);
CREATE INDEX idx_expenses_category_id ON expenses (category_id);

INSERT INTO categories (name) VALUES ('Food'), ('Transport'), ('Rent'), ('Entertainment'), ('Other');
