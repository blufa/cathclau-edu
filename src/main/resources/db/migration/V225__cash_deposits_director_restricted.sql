-- Dépôts de fonds (§5) : strictement réservé au comptable et au fondateur (SCHOOL_ADMIN) — retire
-- l'accès accordé à DIRECTOR en V221 (convention V190 appliquée par erreur ici, le fondateur voulant
-- explicitement garder ce circuit comptable→fondateur fermé, sans intermédiaire).
DELETE FROM role_permissions WHERE role_id = (SELECT id FROM roles WHERE code = 'DIRECTOR')
    AND permission_id IN (SELECT id FROM permissions WHERE code IN ('depot:valider', 'depot:read'));
