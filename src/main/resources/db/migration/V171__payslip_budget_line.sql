-- Rattache optionnellement une fiche de paie à une ligne de budget annuel (§expenses) : au paiement,
-- le réalisé de la ligne s'incrémente automatiquement (comme pour une dépense), et la trésorerie
-- enregistre une sortie de caisse "Salaires" — jusqu'ici la paie était invisible du budget/trésorerie.
ALTER TABLE payslips ADD COLUMN IF NOT EXISTS budget_line_id BIGINT;
