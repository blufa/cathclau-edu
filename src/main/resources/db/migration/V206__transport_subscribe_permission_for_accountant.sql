-- Le comptable n'a jamais eu transport:manage (configuration des lignes/arrêts, réservée à l'école) —
-- mais abonner un élève au bus est un geste courant à l'inscription/l'encaissement (choisir le bus sur
-- la fiche élève), pas une configuration. Nouveau privilège plus étroit, pour ce seul geste
-- (TransportSubscriptionController#subscribe accepte désormais transport:manage OU celui-ci).

INSERT INTO permissions (name, code) VALUES
    ('Abonner un élève au transport', 'abonnement_transport:create')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ACCOUNTANT' AND p.code = 'abonnement_transport:create'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- SUPER_ADMIN/SCHOOL_ADMIN ont déjà transport:manage (donc déjà l'accès) ; ajouté ici par cohérence
-- avec la convention déjà suivie (V189/V204) pour qu'un nouveau code reste toujours visible/cochable
-- dans l'écran Rôles & permissions pour ces rôles.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('SUPER_ADMIN', 'SCHOOL_ADMIN')
  AND p.code = 'abonnement_transport:create'
ON CONFLICT (role_id, permission_id) DO NOTHING;
