-- Mở rộng M4: Outbox pattern — ghi event cùng transaction nghiệp vụ, relay đẩy sang Kafka sau.
CREATE TABLE outbox_events (
    id             BIGSERIAL PRIMARY KEY,
    aggregate_type VARCHAR(40) NOT NULL,           -- ví dụ TASK
    aggregate_id   BIGINT,
    type           VARCHAR(40) NOT NULL,           -- TASK_ASSIGNED / TASK_COMPLETED / TASK_REMINDER
    payload        TEXT NOT NULL,                  -- JSON
    published      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ NOT NULL,
    published_at   TIMESTAMPTZ
);
-- Relay quét nhanh các event chưa publish theo thứ tự tạo.
CREATE INDEX idx_outbox_unpublished ON outbox_events (published, id);
