-- DEV/TEST : école de test prête à l'emploi pour Postman (modules tenant-scopés).
--   SCHOOL_ADMIN : admin@laca.gn / Test@1234
-- ⚠️ Données de TEST : à supprimer avant la production.

-- 1) Tenant de test (statut ACTIVE).
INSERT INTO tenant (school_name, email, director, status, created_at)
SELECT 'École Test', 'contact@ecole-test.gn', 'Directeur Test', 'ACTIVE', NOW()
WHERE NOT EXISTS (SELECT 1 FROM tenant WHERE school_name = 'École Test');

-- 2) SCHOOL_ADMIN lié à ce tenant (même hash BCrypt que le super-admin : Test@1234).
INSERT INTO users (tenant_id, first_name, last_name, email, password_hash, active, created_at)
SELECT t.id, 'Admin', 'École', 'admin@laca.gn',
       '$2a$10$uHqjCLYFZPTIX4kwSzCjwu4TT64os9ANG9xo9U6avitgFkVxPlX9e', TRUE, NOW()
FROM tenant t
WHERE t.school_name = 'École Test'
  AND NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@laca.gn');

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.code = 'SCHOOL_ADMIN'
WHERE u.email = 'admin@laca.gn'
ON CONFLICT (user_id, role_id) DO NOTHING;

-- 3) Année scolaire courante (EN_COURS → saisie de notes/inscriptions autorisées).
INSERT INTO academic_years (tenant_id, label, start_date, end_date, status, is_current, created_at)
SELECT t.id, '2025-2026', DATE '2025-09-01', DATE '2026-07-31', 'EN_COURS', TRUE, NOW()
FROM tenant t
WHERE t.school_name = 'École Test'
ON CONFLICT (tenant_id, label) DO NOTHING;

-- 4) Une période (1er trimestre, ouverte).
INSERT INTO periods (academic_year_id, label, type, sort_order, start_date, end_date, closed)
SELECT ay.id, '1er trimestre', 'TRIMESTRE', 1, DATE '2025-09-01', DATE '2025-12-15', FALSE
FROM academic_years ay
JOIN tenant t ON t.id = ay.tenant_id
WHERE t.school_name = 'École Test' AND ay.label = '2025-2026'
  AND NOT EXISTS (
      SELECT 1 FROM periods p WHERE p.academic_year_id = ay.id AND p.label = '1er trimestre'
  );
