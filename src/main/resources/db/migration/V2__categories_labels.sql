-- Categories (danh mục cá nhân) — thuộc 1 user
CREATE TABLE categories (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name       VARCHAR(100) NOT NULL,
    color      VARCHAR(20),
    icon       VARCHAR(50),
    position   INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_categories_user ON categories (user_id);
CREATE UNIQUE INDEX uq_categories_user_name ON categories (user_id, LOWER(name));

-- Labels (nhãn/tag màu) — thuộc 1 user
CREATE TABLE labels (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name       VARCHAR(50) NOT NULL,
    color      VARCHAR(20),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_labels_user ON labels (user_id);
CREATE UNIQUE INDEX uq_labels_user_name ON labels (user_id, LOWER(name));
