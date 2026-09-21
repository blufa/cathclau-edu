-- Suite du nettoyage RBAC (V189) : les ~42 sites hasAnyRole(...) restants dans les modules plus petits
-- (communication, transport, academics, calendar, webhooks, studentcards, pettycash, payroll, health,
-- discipline, biometric, announcements, courses) migrés vers hasAuthority('resource:action'). Les seuls
-- hasAnyRole conservés le sont volontairement : DPE_ADMIN et REDACTEUR sont des identités système
-- (comme SUPER_ADMIN) jamais attribuables à un rôle personnalisé, et PARENT/STUDENT en libre-service
-- (achats de cours) restent en rôle car ce n'est pas une action déléguable à un rôle administratif.

-- 1) Nouveaux codes de permission
INSERT INTO permissions (name, code) VALUES
    ('Gérer les événements (calendrier académique, événements école)', 'evenement:manage'),
    ('Gérer la petite caisse (fonds, opérations)', 'petite_caisse:manage'),
    ('Enregistrer une opération de petite caisse', 'petite_caisse:enregistrer'),
    ('Supprimer un incident disciplinaire', 'discipline:delete'),
    ('Supprimer un passage à l''infirmerie', 'sante:delete'),
    ('Supprimer un cours payant', 'cours:delete')
ON CONFLICT (code) DO NOTHING;

-- 2) SUPER_ADMIN — rattrapage des nouveaux codes (convention déjà en usage, cf. V189).
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'SUPER_ADMIN'
  AND p.code IN ('evenement:manage','petite_caisse:manage','petite_caisse:enregistrer',
                 'discipline:delete','sante:delete','cours:delete')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 3) SCHOOL_ADMIN — idem (clause NOT IN de V16 ne rattrape pas les codes ajoutés après coup).
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'SCHOOL_ADMIN'
  AND p.code IN ('evenement:manage','petite_caisse:manage','petite_caisse:enregistrer',
                 'discipline:delete','sante:delete','cours:delete')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 4) DIRECTOR — nouveaux codes + rattrapage de transaction:read (webhook de paiement : déjà
-- accessible via l'ancien hasAnyRole, jamais retiré, juste enfin porté par la vraie permission).
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'DIRECTOR'
  AND p.code IN ('evenement:manage','petite_caisse:manage','petite_caisse:enregistrer',
                 'discipline:delete','sante:delete','cours:delete','transaction:read')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 5) ACCOUNTANT : petite caisse.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ACCOUNTANT' AND p.code IN ('petite_caisse:manage','petite_caisse:enregistrer')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 6) SECRETARY : événements, petite caisse (enregistrement), modèles de message (déjà accessible en
-- lecture via l'ancien hasAnyRole sur MessageTemplateController).
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'SECRETARY' AND p.code IN ('evenement:manage','petite_caisse:enregistrer','modele:manage')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 7) TEACHER : communication:send (déjà accessible via l'ancien hasAnyRole sur l'envoi de messages/
-- diffusions — n'incluait pas ce rôle dans le catalogue jusqu'ici, cf. commentaire navigation.ts).
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'TEACHER' AND p.code = 'communication:send'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 8) RESPONSABLE_PEDAGOGIQUE : événements, cours (suppression).
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'RESPONSABLE_PEDAGOGIQUE' AND p.code IN ('evenement:manage','cours:delete')
ON CONFLICT (role_id, permission_id) DO NOTHING;
