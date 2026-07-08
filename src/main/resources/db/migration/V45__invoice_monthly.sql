-- Facturation par mois : une facture peut être rattachée à une échéance (mois) d'un frais MENSUALITE.
-- installment_id identifie le mois facturé ; period_label en porte le libellé lisible (ex. « Octobre »).
-- Permet au parent de payer plusieurs mois choisis, chacun avec son propre reçu.
ALTER TABLE invoices ADD COLUMN installment_id BIGINT;
ALTER TABLE invoices ADD COLUMN period_label VARCHAR(60);
CREATE INDEX idx_invoices_student_installment ON invoices (student_id, installment_id);
