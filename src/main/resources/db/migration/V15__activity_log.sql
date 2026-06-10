-- Mở rộng M5: Nhật ký hoạt động của workspace (ai làm gì) — ghi bởi ActivityConsumer từ Kafka.
CREATE TABLE activity_log (
    id             BIGSERIAL PRIMARY KEY,
    workspace_id   BIGINT NOT NULL REFERENCES workspaces (id) ON DELETE CASCADE,
    actor_id       BIGINT REFERENCES users (id) ON DELETE SET NULL,
    type           VARCHAR(40) NOT NULL,          -- TASK_ASSIGNED / TASK_COMPLETED ...
    task_id        BIGINT,
    target_user_id BIGINT,
    message        TEXT NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_activity_ws ON activity_log (workspace_id, id DESC);
