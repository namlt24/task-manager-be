-- Notes (ghi chú markdown) — thuộc 1 user, có thể gắn tuỳ chọn tới 1 task
CREATE TABLE notes (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    task_id    BIGINT REFERENCES tasks (id) ON DELETE CASCADE,
    title      VARCHAR(200) NOT NULL,
    content    TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_notes_user ON notes (user_id);
CREATE INDEX idx_notes_task ON notes (task_id);
