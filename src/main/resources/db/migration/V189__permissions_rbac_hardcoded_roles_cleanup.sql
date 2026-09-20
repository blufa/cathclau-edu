-- RBAC (§2.3) : un rôle personnalisé créé par une école, même avec les bonnes permissions cochées,
-- restait bloqué sur ~250 méthodes de contrôleur qui vérifiaient encore un nom de rôle codé en dur
-- (@PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR',...)")) au lieu de la permission réellement
-- assignée (@PreAuthorize("hasAuthority('resource:action')")). Cette migration accompagne le nettoyage
-- de ces contrôleurs (modules accounting, grading, attendance, billing, students, payments, staff,
-- platform) : nouveaux codes de permission nécessaires + rattrapage des rôles qui avaient accès via
-- l'ancien contrôle par nom de rôle mais ne détenaient pas encore la permission correspondante.

-- 1) Nouveaux codes de permission (idempotent sur `code`)
INSERT INTO permissions (name, code) VALUES
    ('Gérer la comptabilité (journaux, écritures, plan comptable, trésorerie, rapprochements)', 'comptabilite:manage'),
    ('Clôturer/supprimer des données comptables (exercices, écritures, comptes, journaux)', 'comptabilite:approve'),
    ('Supprimer une facture', 'facture:delete'),
    ('Supprimer un paiement', 'paiement:delete'),
    ('Gérer le personnel non-enseignant', 'personnel:manage'),
    ('Consulter le personnel non-enseignant', 'personnel:read'),
    ('Gérer les disponibilités des enseignants', 'disponibilite:manage'),
    ('Gérer le profil de l''école (identité, coordonnées, image)', 'ecole:profil'),
    ('Déverrouiller une note validée', 'note:unlock'),
    ('Supprimer un bulletin', 'bulletin:delete'),
    ('Configurer le moteur de calcul des notes (barème, formules, règles de décision/élimination, sessions d''évaluation)', 'parametrage_notes:manage'),
    ('Supprimer un enregistrement de présence', 'presence:delete')
ON CONFLICT (code) DO NOTHING;

-- 2) SUPER_ADMIN garde toutes les permissions (convention déjà en usage, cf. V132/V137) : les nouveaux
-- codes ne sont pas rattrapés automatiquement par le CROSS JOIN initial de V16, il faut les accorder ici.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'SUPER_ADMIN'
  AND p.code IN ('comptabilite:manage','comptabilite:approve','facture:delete','paiement:delete',
                 'personnel:manage','personnel:read','disponibilite:manage','ecole:profil',
                 'note:unlock','bulletin:delete','parametrage_notes:manage','presence:delete')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 3) SCHOOL_ADMIN : là aussi, la clause NOT IN de V16 ne rattrape pas les codes ajoutés après coup.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'SCHOOL_ADMIN'
  AND p.code IN ('comptabilite:manage','comptabilite:approve','facture:delete','paiement:delete',
                 'personnel:manage','personnel:read','disponibilite:manage','ecole:profil',
                 'note:unlock','bulletin:delete','parametrage_notes:manage','presence:delete')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 4) DIRECTOR : nouveaux codes + rattrapage de codes existants qu'il n'avait pas encore (accès qu'il
-- avait déjà via l'ancien hasAnyRole, jamais retiré, juste enfin porté par la vraie permission).
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'DIRECTOR'
  AND p.code IN ('comptabilite:manage','comptabilite:approve','facture:delete','paiement:delete',
                 'personnel:manage','personnel:read','disponibilite:manage','ecole:profil',
                 'note:unlock','bulletin:delete','parametrage_notes:manage','presence:delete',
                 'frais:manage','echeance:manage','facture:create','paiement:refund','abonnement:manage')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 5) ACCOUNTANT : nouveaux codes comptables + personnel (lecture) + prise en charge de la clôture des
-- règlements plateforme (déjà accessible via hasAnyRole côté SubscriptionController/PlatformFeeSettlement).
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ACCOUNTANT'
  AND p.code IN ('comptabilite:manage','personnel:read','abonnement:manage')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 6) SECRETARY : rattrapage des permissions déjà accessibles via l'ancien hasAnyRole sur la
-- facturation/paiements/notes-lecture/examens/bulletins (jamais eu de code dédié jusqu'ici).
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'SECRETARY'
  AND p.code IN ('frais:manage','echeance:manage','facture:create','paiement:create',
                 'evaluation:read','note:read','examen:manage','bulletin:generate','bulletin:read')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 7) TEACHER : disponibilités (gestion de ses propres créneaux, déjà permis via hasAnyRole).
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'TEACHER' AND p.code = 'disponibilite:manage'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 8) RESPONSABLE_PEDAGOGIQUE : premier vrai rattachement de permissions pour les écrans de
-- configuration/consultation pédagogique qui étaient jusqu'ici uniquement sur hasAnyRole (ce rôle
-- n'avait dans le catalogue que 'annonce:manage', cf. V152).
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'RESPONSABLE_PEDAGOGIQUE'
  AND p.code IN ('parametrage_notes:manage','evaluation:read','note:read',
                 'bulletin:generate','bulletin:read','bulletin:publish')
ON CONFLICT (role_id, permission_id) DO NOTHING;
