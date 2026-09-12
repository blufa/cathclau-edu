-- Mois "YYYY-MM" effectivement réglés sur une facture multi-mois (tranche), même format
-- colonne-texte-séparée-par-virgules que installments.months (InstallmentMonthsConverter) — une
-- facture d'un seul mois n'en a pas besoin (son periodLabel suffit), reste NULL dans ce cas.
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS paid_months TEXT;
