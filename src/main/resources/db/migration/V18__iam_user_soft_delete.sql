-- IAM: soft delete for users (DELETE /users/{id} is a logical delete, never physical)
ALTER TABLE users ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_users_deleted_at ON users (deleted_at);
