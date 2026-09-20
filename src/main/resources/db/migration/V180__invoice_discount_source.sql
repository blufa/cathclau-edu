-- Trace l'origine de la remise en % effectivement appliquée à une facture (réduction familiale,
-- avantage personnel ou exonération — la plus avantageuse des trois) — sert notamment à répercuter
-- automatiquement le solde non couvert par un avantage personnel PARTIEL sur la paie du parent
-- employé (§billing ↔ payroll).

ALTER TABLE invoices ADD COLUMN discount_source VARCHAR(20);
