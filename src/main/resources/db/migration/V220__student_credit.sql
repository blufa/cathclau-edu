-- Trop-perçu conservé comme crédit disponible de l'élève (§4 cahier des charges) : jusqu'ici, un
-- paiement supérieur au solde dû d'une facture gonflait simplement paid_amount au-delà du total,
-- rendant remaining_balance négatif sans aucune trace exploitable ni moyen de réutiliser le surplus
-- sur une facture future.
CREATE TABLE student_credits (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    amount NUMERIC(14,2) NOT NULL,
    remaining_amount NUMERIC(14,2) NOT NULL,
    source_payment_id BIGINT,
    source_invoice_id BIGINT,
    reason VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT student_credits_remaining_nonnegative CHECK (remaining_amount >= 0),
    CONSTRAINT student_credits_remaining_le_amount CHECK (remaining_amount <= amount)
);
CREATE INDEX idx_student_credits_student ON student_credits(tenant_id, student_id);

-- Historique des utilisations (§4 : "date, auteur, paiement d'origine, facture source, facture
-- cible, montant et solde restant") — une ligne par application, même partielle.
CREATE TABLE student_credit_usages (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    credit_id BIGINT NOT NULL REFERENCES student_credits(id),
    target_invoice_id BIGINT NOT NULL,
    amount NUMERIC(14,2) NOT NULL,
    applied_by_user_id BIGINT,
    applied_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_student_credit_usages_student ON student_credit_usages(tenant_id, student_id);
CREATE INDEX idx_student_credit_usages_credit ON student_credit_usages(credit_id);
