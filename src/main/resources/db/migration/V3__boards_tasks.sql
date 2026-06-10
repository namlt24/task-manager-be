-- Boards (bảng Kanban) — thuộc 1 user
CREATE TABLE boards (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name       VARCHAR(120) NOT NULL,
    position   INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_boards_user ON boards (user_id);

-- Columns (cột trạng thái) trong 1 board
CREATE TABLE board_columns (
    id         BIGSERIAL PRIMARY KEY,
    board_id   BIGINT NOT NULL REFERENCES boards (id) ON DELETE CASCADE,
    user_id    BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name       VARCHAR(120) NOT NULL,
    position   INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_columns_board ON board_columns (board_id);

-- Tasks
CREATE TABLE tasks (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    board_id    BIGINT NOT NULL REFERENCES boards (id) ON DELETE CASCADE,
    column_id   BIGINT NOT NULL REFERENCES board_columns (id) ON DELETE CASCADE,
    category_id BIGINT REFERENCES categories (id) ON DELETE SET NULL,
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    priority    VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    due_date    TIMESTAMPTZ,
    remind_at   TIMESTAMPTZ,
    position    INTEGER NOT NULL DEFAULT 0,
    completed   BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_tasks_user ON tasks (user_id);
CREATE INDEX idx_tasks_board ON tasks (board_id);
CREATE INDEX idx_tasks_column ON tasks (column_id);

-- Task <-> Label (N-N)
CREATE TABLE task_labels (
    task_id  BIGINT NOT NULL REFERENCES tasks (id) ON DELETE CASCADE,
    label_id BIGINT NOT NULL REFERENCES labels (id) ON DELETE CASCADE,
    PRIMARY KEY (task_id, label_id)
);
