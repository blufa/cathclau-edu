-- Corrige deux régressions introduites par V204 (remplacement de frais:manage par des permissions
-- granulaires) et un oubli préexistant, remontés par le retour utilisateur immédiat :
--
-- 1. applyEarlyPaymentDiscount/removeEarlyPaymentDiscount (facture:update en V204) bloquaient le
--    comptable : ce n'est pas une correction administrative d'une facture existante, mais une étape
--    normale de l'encaissement (appliquée juste avant un paiement groupé couvrant toute l'année) —
--    remis sur facture:create côté contrôleur (aucun nouveau code de permission nécessaire ici).
--
-- 2. Accorder une exonération à un élève (ExemptionController#grant/list/byStudent/revoke) était
--    rattaché à regle_facturation:* (catalogue de configuration, lecture seule pour le comptable) —
--    alors que l'octroi d'une exonération à l'inscription est une action courante du comptable/
--    secrétaire, pas une configuration. Nouveau code dédié exoneration:create/read/delete, distinct
--    du catalogue des types (ExemptionRuleController, inchangé sur regle_facturation:*).
--
-- 3. Le comptable n'avait jamais eu enseignant:read (jamais nécessaire jusqu'ici) : sans lui, le nom
--    de l'enseignant ne s'affiche jamais sur l'emploi du temps qu'elle consulte (repli sur "#<id>").

INSERT INTO permissions (name, code) VALUES
    ('Accorder une exonération à un élève', 'exoneration:create'),
    ('Consulter les exonérations accordées', 'exoneration:read'),
    ('Révoquer une exonération accordée', 'exoneration:delete')
ON CONFLICT (code) DO NOTHING;

-- SUPER_ADMIN / SCHOOL_ADMIN / DIRECTOR : octroi ET révocation (déjà accès complet avant via
-- frais:manage).
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('SUPER_ADMIN', 'SCHOOL_ADMIN', 'DIRECTOR')
  AND p.code IN ('exoneration:create', 'exoneration:read', 'exoneration:delete')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ACCOUNTANT / SECRETARY : octroi + lecture (action courante à l'inscription/l'encaissement), jamais
-- la révocation (correction réservée à l'admin école/direction) ; ACCOUNTANT gagne aussi enseignant:read.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('ACCOUNTANT', 'SECRETARY')
  AND p.code IN ('exoneration:create', 'exoneration:read')
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ACCOUNTANT' AND p.code = 'enseignant:read'
ON CONFLICT (role_id, permission_id) DO NOTHING;
