-- Giai đoạn 5: Pomodoro / Time-tracking
-- Mỗi bản ghi là một phiên làm việc (stopwatch tự do hoặc một phiên focus Pomodoro).
-- ended_at NULL = phiên đang chạy; duration_seconds tính khi dừng.
CREATE TABLE time_entries (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    task_id          BIGINT REFERENCES tasks (id) ON DELETE CASCADE,
    source           VARCHAR(12) NOT NULL,        -- STOPWATCH / POMODORO
    started_at       TIMESTAMPTZ NOT NULL,
    ended_at         TIMESTAMPTZ,                 -- NULL = đang chạy
    duration_seconds INT,                         -- NULL khi đang chạy
    note             VARCHAR(255),
    created_at       TIMESTAMPTZ NOT NULL,
    updated_at       TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_time_entries_user ON time_entries (user_id);
CREATE INDEX idx_time_entries_task ON time_entries (task_id);
-- Tra cứu nhanh phiên đang chạy của một user (tối đa 1 phiên active mỗi user)
CREATE INDEX idx_time_entries_active ON time_entries (user_id) WHERE ended_at IS NULL;
