-- Mở rộng M2: Mời thành viên vào workspace qua email + chấp nhận bằng token
CREATE TABLE workspace_invitations (
    id           BIGSERIAL PRIMARY KEY,
    workspace_id BIGINT NOT NULL REFERENCES workspaces (id) ON DELETE CASCADE,
    email        VARCHAR(150) NOT NULL,
    role         VARCHAR(10) NOT NULL,            -- MANAGER / MEMBER (không mời OWNER)
    token        VARCHAR(64) NOT NULL,
    status       VARCHAR(10) NOT NULL,            -- PENDING / ACCEPTED / REVOKED
    invited_by   BIGINT REFERENCES users (id) ON DELETE SET NULL,
    expires_at   TIMESTAMPTZ NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_ws_invitation_token UNIQUE (token)
);
CREATE INDEX idx_ws_invitations_ws ON workspace_invitations (workspace_id);
CREATE INDEX idx_ws_invitations_email ON workspace_invitations (LOWER(email));
