-- Retour comptable : quand un transfert de classe laisse un surplus de crédit non affectable (le
-- parent avait déjà réglé plus que ce que coûtent les mensualités de la nouvelle classe), ce
-- montant doit être remboursé — jusqu'ici seulement logué/envoyé par SMS, jamais persisté nulle
-- part, donc impossible à rappeler sur un reçu ultérieur ni à suivre jusqu'au remboursement effectif.
CREATE TABLE class_transfer_refunds (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    old_class_id BIGINT,
    new_class_id BIGINT,
    amount NUMERIC(15,2) NOT NULL,
    reason VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    refunded_at TIMESTAMP
);
CREATE INDEX idx_class_transfer_refunds_student ON class_transfer_refunds(student_id);
