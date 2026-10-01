-- RBAC (§retour utilisateur) : les permissions billing "manage" (frais:manage, echeance:manage)
-- étaient trop vagues — elles accordaient d'un bloc la création, la modification ET la suppression,
-- empêchant par exemple de donner au comptable un accès lecture seule aux écrans de configuration
-- (Frais scolaires, Règles de facturation) tout en gardant la main sur les écrans opérationnels
-- (Factures : création + lecture seulement, jamais modification/suppression). On les remplace par
-- des permissions create/read/update/delete distinctes, onglet par onglet.
--
-- Onglet "Frais scolaires" (SchoolFeeController + InstallmentController/échéances) -> frais_scolaire:*
-- Onglet "Règles de facturation" (types d'exonération, exonérations accordées, remise paiement
-- annuel complet, réduction familiale) -> regle_facturation:*
-- Prise en charge enfants du personnel (badges liste élèves, pas encore d'écran dédié) -> avantage_personnel:*
-- Onglet "Factures élèves" : facture:create/facture:read/facture:delete existaient déjà ; on ajoute
-- facture:update pour les actions qui modifiaient une facture existante (remise, recouvrement,
-- annulation) mais étaient jusqu'ici rattachées par erreur à facture:create.

-- 1) Nouveaux codes de permission (idempotent sur `code`)
INSERT INTO permissions (name, code) VALUES
    ('Consulter les frais scolaires', 'frais_scolaire:read'),
    ('Créer un frais scolaire', 'frais_scolaire:create'),
    ('Modifier un frais scolaire', 'frais_scolaire:update'),
    ('Supprimer un frais scolaire', 'frais_scolaire:delete'),
    ('Consulter les règles de facturation (exonérations, remises)', 'regle_facturation:read'),
    ('Créer une règle de facturation (exonération, remise)', 'regle_facturation:create'),
    ('Modifier une règle de facturation', 'regle_facturation:update'),
    ('Supprimer/révoquer une règle de facturation', 'regle_facturation:delete'),
    ('Modifier une facture (remise, recouvrement, annulation)', 'facture:update'),
    ('Consulter la prise en charge des enfants du personnel', 'avantage_personnel:read'),
    ('Créer une politique de prise en charge des enfants du personnel', 'avantage_personnel:create'),
    ('Modifier une politique de prise en charge des enfants du personnel', 'avantage_personnel:update'),
    ('Supprimer une politique de prise en charge des enfants du personnel', 'avantage_personnel:delete')
ON CONFLICT (code) DO NOTHING;

-- 2) SUPER_ADMIN / SCHOOL_ADMIN : nouveaux codes (le CROSS JOIN initial de V16 ne rattrape pas les
-- codes ajoutés après coup — convention déjà suivie en V189).
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('SUPER_ADMIN', 'SCHOOL_ADMIN')
  AND p.code IN ('frais_scolaire:read','frais_scolaire:create','frais_scolaire:update','frais_scolaire:delete',
                 'regle_facturation:read','regle_facturation:create','regle_facturation:update','regle_facturation:delete',
                 'facture:update',
                 'avantage_personnel:read','avantage_personnel:create','avantage_personnel:update','avantage_personnel:delete')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 3) DIRECTOR : avait frais:manage/echeance:manage (donc accès complet) -> préserve la capacité
-- actuelle avec les nouveaux codes granulaires (full CRUD).
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'DIRECTOR'
  AND p.code IN ('frais_scolaire:read','frais_scolaire:create','frais_scolaire:update','frais_scolaire:delete',
                 'regle_facturation:read','regle_facturation:create','regle_facturation:update','regle_facturation:delete',
                 'facture:update',
                 'avantage_personnel:read','avantage_personnel:create','avantage_personnel:update','avantage_personnel:delete')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 4) SECRETARY : avait frais:manage/echeance:manage uniquement par effet de bord (aucun écran de
-- configuration ne lui est accessible, cf. navigation.ts — ces onglets sont réservés à SCHOOL_ADMIN/
-- ACCOUNTANT) ; son vrai besoin est la LECTURE des frais et des types d'exonération à l'inscription
-- (listes déroulantes). Lecture seule, jamais de création/modification/suppression de configuration.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'SECRETARY'
  AND p.code IN ('frais_scolaire:read','regle_facturation:read')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 5) ACCOUNTANT (comptable) : politique explicite demandée — lecture seule sur tout ce qui est
-- configuré par l'administrateur école (Frais scolaires, Règles de facturation, Avantage personnel) ;
-- sur l'écran opérationnel Factures, création + lecture uniquement, jamais modification/suppression
-- (facture:update/facture:delete restent réservés à SCHOOL_ADMIN/DIRECTOR).
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ACCOUNTANT'
  AND p.code IN ('frais_scolaire:read','regle_facturation:read','avantage_personnel:read')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 6) Retrait des anciennes permissions "manage", devenues orphelines (plus aucun contrôleur ne les
-- vérifie) : d'abord les attributions aux rôles (tous rôles, y compris personnalisés créés par une
-- école), puis les codes eux-mêmes. Note : un rôle PERSONNALISÉ qui avait frais:manage/echeance:manage
-- coché perd cet accès sans remplacement automatique — à recocher manuellement avec les nouveaux
-- codes granulaires via l'écran Rôles & permissions.
DELETE FROM role_permissions WHERE permission_id IN (
    SELECT id FROM permissions WHERE code IN ('frais:manage', 'echeance:manage')
);
DELETE FROM permissions WHERE code IN ('frais:manage', 'echeance:manage');
