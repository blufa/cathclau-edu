-- Paiement groupé (plusieurs mois / année en une fois) : un paiement couvre N factures-mois.
-- invoice_id devient nullable (un paiement groupé ne cible pas une facture unique).
ALTER TABLE payments ALTER COLUMN invoice_id DROP NOT NULL;

-- Couverture imprimée sur le reçu groupé + répartition différée (cas Djomy en ligne) :
-- group_lines stocke en JSON les parts [{invoiceId, amount}] à appliquer au succès du paiement.
ALTER TABLE payments ADD COLUMN covered_months VARCHAR(500);
ALTER TABLE payments ADD COLUMN months_count INTEGER;
ALTER TABLE payments ADD COLUMN remaining_after NUMERIC(15, 2);
ALTER TABLE payments ADD COLUMN group_lines TEXT;
