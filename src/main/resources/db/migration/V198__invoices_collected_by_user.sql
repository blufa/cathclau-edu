-- Utilisateur (comptable/caissier) ayant physiquement encaissé le dernier paiement appliqué à une
-- facture — affiché en pied de facture PDF ("Encaissé par"). Null tant qu'aucun paiement guichet n'a
-- encore été appliqué (§retour comptable, point 5).
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS collected_by_user_id BIGINT;
