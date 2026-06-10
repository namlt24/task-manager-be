-- Mở rộng M1: chuyển cô lập dữ liệu từ user → workspace.
-- Tạo 1 workspace "Cá nhân" cho mỗi user hiện có, gán dữ liệu cũ vào đó, user thành OWNER.

-- 1) Mỗi user → 1 workspace cá nhân
INSERT INTO workspaces (name, owner_id, created_at, updated_at)
SELECT 'Cá nhân', u.id, now(), now() FROM users u;

-- 2) Chủ workspace = OWNER
INSERT INTO workspace_members (workspace_id, user_id, role, created_at, updated_at)
SELECT w.id, w.owner_id, 'OWNER', now(), now() FROM workspaces w;

-- 3) Thêm workspace_id vào các bảng nghiệp vụ + backfill theo user_id (= chủ workspace cá nhân)
ALTER TABLE boards        ADD COLUMN workspace_id BIGINT REFERENCES workspaces (id) ON DELETE CASCADE;
ALTER TABLE board_columns ADD COLUMN workspace_id BIGINT REFERENCES workspaces (id) ON DELETE CASCADE;
ALTER TABLE categories    ADD COLUMN workspace_id BIGINT REFERENCES workspaces (id) ON DELETE CASCADE;
ALTER TABLE labels        ADD COLUMN workspace_id BIGINT REFERENCES workspaces (id) ON DELETE CASCADE;
ALTER TABLE notes         ADD COLUMN workspace_id BIGINT REFERENCES workspaces (id) ON DELETE CASCADE;
ALTER TABLE tasks         ADD COLUMN workspace_id BIGINT REFERENCES workspaces (id) ON DELETE CASCADE;

UPDATE boards        b SET workspace_id = (SELECT w.id FROM workspaces w WHERE w.owner_id = b.user_id);
UPDATE board_columns c SET workspace_id = (SELECT w.id FROM workspaces w WHERE w.owner_id = c.user_id);
UPDATE categories    c SET workspace_id = (SELECT w.id FROM workspaces w WHERE w.owner_id = c.user_id);
UPDATE labels        l SET workspace_id = (SELECT w.id FROM workspaces w WHERE w.owner_id = l.user_id);
UPDATE notes         n SET workspace_id = (SELECT w.id FROM workspaces w WHERE w.owner_id = n.user_id);
UPDATE tasks         t SET workspace_id = (SELECT w.id FROM workspaces w WHERE w.owner_id = t.user_id);

-- 4) Sau backfill → bắt buộc NOT NULL + index
ALTER TABLE boards        ALTER COLUMN workspace_id SET NOT NULL;
ALTER TABLE board_columns ALTER COLUMN workspace_id SET NOT NULL;
ALTER TABLE categories    ALTER COLUMN workspace_id SET NOT NULL;
ALTER TABLE labels        ALTER COLUMN workspace_id SET NOT NULL;
ALTER TABLE notes         ALTER COLUMN workspace_id SET NOT NULL;
ALTER TABLE tasks         ALTER COLUMN workspace_id SET NOT NULL;

CREATE INDEX idx_boards_ws     ON boards (workspace_id);
CREATE INDEX idx_board_cols_ws ON board_columns (workspace_id);
CREATE INDEX idx_categories_ws ON categories (workspace_id);
CREATE INDEX idx_labels_ws     ON labels (workspace_id);
CREATE INDEX idx_notes_ws      ON notes (workspace_id);
CREATE INDEX idx_tasks_ws      ON tasks (workspace_id);

-- 5) Tên danh mục/nhãn nay duy nhất theo WORKSPACE (thay vì theo user)
DROP INDEX IF EXISTS uq_categories_user_name;
DROP INDEX IF EXISTS uq_labels_user_name;
CREATE UNIQUE INDEX uq_categories_ws_name ON categories (workspace_id, LOWER(name));
CREATE UNIQUE INDEX uq_labels_ws_name ON labels (workspace_id, LOWER(name));
