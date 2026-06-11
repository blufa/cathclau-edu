-- IAM: permission catalog (resource:action), role->permission mapping, per-tenant email uniqueness

-- 1) Email unique per tenant (was globally unique in V3)
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_email_key;
ALTER TABLE users ADD CONSTRAINT uk_users_tenant_email UNIQUE (tenant_id, email);

-- 2) Permission catalog (idempotent on the unique `code`)
INSERT INTO permissions (name, code) VALUES
    -- platform
    ('Create tenant', 'tenant:create'),
    ('Read tenant', 'tenant:read'),
    ('Update tenant', 'tenant:update'),
    ('Delete tenant', 'tenant:delete'),
    ('Manage plans', 'plan:manage'),
    ('Manage subscriptions', 'abonnement:manage'),
    ('Read subscription', 'abonnement:read'),
    ('View platform dashboard', 'dashboard:platform'),
    -- iam
    ('Create user', 'user:create'),
    ('Read user', 'user:read'),
    ('Update user', 'user:update'),
    ('Delete user', 'user:delete'),
    ('Create role', 'role:create'),
    ('Read role', 'role:read'),
    ('Update role', 'role:update'),
    ('Delete role', 'role:delete'),
    ('Assign roles to users', 'role:assign'),
    ('Assign permissions to roles', 'role:assign_permissions'),
    ('Read permission catalog', 'permission:read'),
    -- students
    ('Create student', 'eleve:create'),
    ('Read student', 'eleve:read'),
    ('Update student', 'eleve:update'),
    ('Delete student', 'eleve:delete'),
    ('Create enrollment', 'inscription:create'),
    ('Read enrollment', 'inscription:read'),
    ('Validate enrollment', 'inscription:validate'),
    ('Manage parents', 'parent:manage'),
    -- staff
    ('Create teacher', 'enseignant:create'),
    ('Read teacher', 'enseignant:read'),
    ('Update teacher', 'enseignant:update'),
    ('Delete teacher', 'enseignant:delete'),
    -- academics
    ('Manage academic years', 'annee:manage'),
    ('Manage periods', 'periode:manage'),
    ('Manage levels', 'niveau:manage'),
    ('Manage rooms', 'salle:manage'),
    ('Manage classes', 'classe:manage'),
    ('Read class', 'classe:read'),
    ('Manage subjects', 'matiere:manage'),
    ('Manage timetable', 'emploi:manage'),
    ('Read timetable', 'emploi:read'),
    -- attendance
    ('Create attendance', 'presence:create'),
    ('Read attendance', 'presence:read'),
    ('Update attendance', 'presence:update'),
    -- grading
    ('Create evaluation', 'evaluation:create'),
    ('Read evaluation', 'evaluation:read'),
    ('Update evaluation', 'evaluation:update'),
    ('Delete evaluation', 'evaluation:delete'),
    ('Create grade', 'note:create'),
    ('Read grade', 'note:read'),
    ('Update grade', 'note:update'),
    ('Validate grade', 'note:validate'),
    ('Manage exams', 'examen:manage'),
    ('Read report card', 'bulletin:read'),
    ('Generate report card', 'bulletin:generate'),
    ('Publish report card', 'bulletin:publish'),
    -- billing
    ('Manage school fees', 'frais:manage'),
    ('Manage installments', 'echeance:manage'),
    ('Create invoice', 'facture:create'),
    ('Read invoice', 'facture:read'),
    -- payments
    ('Create payment', 'paiement:create'),
    ('Read payment', 'paiement:read'),
    ('Refund payment', 'paiement:refund'),
    ('Read transaction', 'transaction:read'),
    ('Read receipt', 'recu:read'),
    -- webhooks
    ('Manage webhooks', 'webhook:manage'),
    -- communication
    ('Send communication', 'communication:send'),
    ('Read communication', 'communication:read'),
    -- transverse
    ('Read audit log', 'audit:read'),
    ('View dashboard', 'dashboard:read')
ON CONFLICT (code) DO NOTHING;

-- 3) Role -> permission mapping
-- SUPER_ADMIN: full platform scope (every permission)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'SUPER_ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- SCHOOL_ADMIN: every tenant-level permission (everything except platform tenant/plan mgmt)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'SCHOOL_ADMIN'
  AND p.code NOT IN ('tenant:create','tenant:update','tenant:delete','plan:manage','dashboard:platform')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- DIRECTOR: pedagogical oversight + read-heavy
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'DIRECTOR'
  AND p.code IN (
    'user:read','role:read','permission:read',
    'eleve:read','inscription:read','inscription:validate','parent:manage','enseignant:read',
    'annee:manage','periode:manage','niveau:manage','salle:manage','classe:manage','classe:read',
    'matiere:manage','emploi:manage','emploi:read',
    'presence:read','evaluation:read','note:read','examen:manage',
    'bulletin:read','bulletin:generate','bulletin:publish',
    'facture:read','paiement:read','communication:send','communication:read',
    'audit:read','dashboard:read')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ACCOUNTANT: finance
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ACCOUNTANT'
  AND p.code IN (
    'eleve:read','abonnement:read',
    'frais:manage','echeance:manage','facture:create','facture:read',
    'paiement:create','paiement:read','paiement:refund','transaction:read','recu:read',
    'communication:send','dashboard:read')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- SECRETARY: enrollments / administrative
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'SECRETARY'
  AND p.code IN (
    'eleve:create','eleve:read','eleve:update','parent:manage',
    'inscription:create','inscription:read','inscription:validate',
    'enseignant:read','classe:read','emploi:read',
    'presence:create','presence:read','presence:update',
    'facture:read','communication:send','communication:read','dashboard:read')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- TEACHER: grading + attendance
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'TEACHER'
  AND p.code IN (
    'eleve:read','classe:read','emploi:read',
    'presence:create','presence:read','presence:update',
    'evaluation:create','evaluation:read','evaluation:update','evaluation:delete',
    'note:create','note:read','note:update','note:validate',
    'examen:manage','bulletin:read','bulletin:generate','dashboard:read')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- PARENT: read own children's data
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'PARENT'
  AND p.code IN (
    'eleve:read','inscription:read','presence:read','note:read','bulletin:read',
    'facture:read','paiement:create','paiement:read','recu:read','communication:read','dashboard:read')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- STUDENT: read own data
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'STUDENT'
  AND p.code IN (
    'note:read','bulletin:read','presence:read','emploi:read','communication:read','dashboard:read')
ON CONFLICT (role_id, permission_id) DO NOTHING;
