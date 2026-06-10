-- Giai đoạn 4: Nhắc nhở & Lặp lại & Thông báo

-- 1) Recurrence + cờ chống nhắc lặp trên tasks
ALTER TABLE tasks ADD COLUMN recurrence_freq     VARCHAR(10);              -- null = không lặp; DAILY/WEEKLY/MONTHLY
ALTER TABLE tasks ADD COLUMN recurrence_interval INT;                     -- vd 2 = mỗi 2 đơn vị
ALTER TABLE tasks ADD COLUMN recurrence_until    TIMESTAMPTZ;             -- null = lặp vô hạn
ALTER TABLE tasks ADD COLUMN reminder_sent       BOOLEAN NOT NULL DEFAULT false;

-- Quét reminder nhanh: chỉ index task có remind_at
CREATE INDEX idx_tasks_remind ON tasks (remind_at) WHERE remind_at IS NOT NULL;

-- 2) Thông báo trong ứng dụng
CREATE TABLE notifications (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    task_id    BIGINT REFERENCES tasks (id) ON DELETE CASCADE,
    type       VARCHAR(20) NOT NULL,            -- REMINDER / RECURRENCE
    title      VARCHAR(255) NOT NULL,
    message    TEXT,
    read       BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_notifications_user_read ON notifications (user_id, read);
