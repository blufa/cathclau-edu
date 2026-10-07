-- Retour comptable : donner au comptable la possibilité de supprimer une facture générée par erreur
-- (ex. doublon) TANT QU'ELLE N'EST PAS ENCORE ENCAISSÉE — jusqu'ici facture:delete était réservé à
-- SCHOOL_ADMIN/DIRECTOR (V204 §5). Devient sûr maintenant qu'une garde serveur bloque la suppression
-- d'une facture déjà payée (InvoiceService#delete) — avant ce correctif, seul un contrôle côté client
-- (contournable) existait.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ACCOUNTANT' AND p.code = 'facture:delete'
ON CONFLICT (role_id, permission_id) DO NOTHING;
