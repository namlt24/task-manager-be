-- Giai đoạn 6: mốc thời gian hoàn thành để thống kê năng suất (việc xong theo ngày, streak)
ALTER TABLE tasks ADD COLUMN completed_at TIMESTAMPTZ;

-- Backfill gần đúng cho dữ liệu cũ: việc đã xong lấy theo updated_at
UPDATE tasks SET completed_at = updated_at WHERE completed = true AND completed_at IS NULL;

CREATE INDEX idx_tasks_completed_at ON tasks (completed_at) WHERE completed_at IS NOT NULL;
