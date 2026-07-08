-- Garde-fou : discount_amount NOT NULL sans valeur par défaut faisait échouer l'insertion de facture
-- (inscription élève). On backfill les éventuels NULL et on fixe un défaut 0.
UPDATE invoices SET discount_amount = 0 WHERE discount_amount IS NULL;
ALTER TABLE invoices ALTER COLUMN discount_amount SET DEFAULT 0;
