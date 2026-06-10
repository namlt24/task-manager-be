-- Mở rộng M3: Giao việc — gán task cho một thành viên trong workspace
ALTER TABLE tasks ADD COLUMN assignee_id BIGINT REFERENCES users (id) ON DELETE SET NULL;
CREATE INDEX idx_tasks_assignee ON tasks (assignee_id);
