-- Isolation par année scolaire : factures et paiements n'avaient AUCUNE notion d'année, ce qui les
-- faisait réapparaître dans la nouvelle année après clôture. On ajoute la colonne, puis on rattache
-- les enregistrements déjà existants à l'année courante de leur tenant (données historiques : choix
-- pragmatique validé, faute de pouvoir déduire précisément l'année d'origine de chacun).
ALTER TABLE invoices ADD COLUMN academic_year_id BIGINT REFERENCES academic_years(id);
UPDATE invoices i SET academic_year_id = (
    SELECT ay.id FROM academic_years ay WHERE ay.tenant_id = i.tenant_id AND ay.is_current = true LIMIT 1
) WHERE academic_year_id IS NULL;
CREATE INDEX idx_invoices_academic_year ON invoices(academic_year_id);

ALTER TABLE payments ADD COLUMN academic_year_id BIGINT REFERENCES academic_years(id);
UPDATE payments p SET academic_year_id = (
    SELECT ay.id FROM academic_years ay WHERE ay.tenant_id = p.tenant_id AND ay.is_current = true LIMIT 1
) WHERE academic_year_id IS NULL;
CREATE INDEX idx_payments_academic_year ON payments(academic_year_id);
