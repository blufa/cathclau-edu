-- Retour comptable : le crédit reporté lors d'un transfert de classe (ce qui était déjà payé sur
-- l'ancienne classe, appliqué directement sur les nouvelles factures) augmentait paidAmount sans
-- aucune trace de pourquoi — le comptable voyait une facture déjà réglée sans paiement correspondant.
ALTER TABLE invoices ADD COLUMN credited_amount NUMERIC(15,2);
ALTER TABLE invoices ADD COLUMN credit_reason VARCHAR(255);
