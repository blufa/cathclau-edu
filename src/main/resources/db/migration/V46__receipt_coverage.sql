-- Couverture d'un reçu groupé (paiement de plusieurs mois / année en une fois) :
-- liste des mois payés, leur nombre, et le reste à payer après ce règlement. Affiché sur le reçu PDF.
ALTER TABLE receipts ADD COLUMN covered_months VARCHAR(500);
ALTER TABLE receipts ADD COLUMN months_count INTEGER;
ALTER TABLE receipts ADD COLUMN remaining_after NUMERIC(15, 2);
