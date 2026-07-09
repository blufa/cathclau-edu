-- Remises / bourses sur facture (règlement financier : bourse, remise d'ordre Ramadan, réduction
-- fratrie…). Le net dû = total - remise ; le reste à payer et le statut sont recalculés en conséquence.
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS discount_amount NUMERIC(15, 2) NOT NULL DEFAULT 0;
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS discount_reason VARCHAR(255);
