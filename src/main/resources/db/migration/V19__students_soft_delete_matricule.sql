-- Students: soft delete + per-tenant unique student number (matricule)
ALTER TABLE students ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
CREATE INDEX IF NOT EXISTS idx_students_deleted_at ON students (deleted_at);

-- Matricule unique per tenant (NULLs are allowed and do not collide in PostgreSQL)
ALTER TABLE students DROP CONSTRAINT IF EXISTS uk_students_tenant_number;
ALTER TABLE students ADD CONSTRAINT uk_students_tenant_number UNIQUE (tenant_id, student_number);
