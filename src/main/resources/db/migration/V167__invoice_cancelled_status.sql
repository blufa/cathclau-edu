-- Nouveau statut CANCELLED (facture annulée mais conservée pour l'historique — ex. transfert de
-- classe en cours d'année : les mensualités de l'ancienne classe sont annulées, pas supprimées, pour
-- garder la trace de ce qui a été facturé/payé avant le transfert). Distinct de la suppression
-- physique existante (DELETE /invoices/{id}/cancel, jamais renommée pour ne pas casser son usage
-- actuel).
ALTER TABLE invoices DROP CONSTRAINT IF EXISTS invoices_status_check;
ALTER TABLE invoices ADD CONSTRAINT invoices_status_check CHECK (status IN ('UNPAID','PARTIAL','PAID','CANCELLED'));
