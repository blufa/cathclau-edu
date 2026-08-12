-- Remise pour paiement annuel complet (§billing) : quand un parent règle en un seul paiement la
-- totalité des tranches restantes d'un frais, une remise en pourcentage s'applique automatiquement.
-- Aucun taux codé en dur : paramétrable par établissement, éventuellement ciblé sur un type de frais.
CREATE TABLE early_payment_discount_rules (
    id         BIGSERIAL PRIMARY KEY,
    tenant_id  BIGINT       NOT NULL,
    label      VARCHAR(255) NOT NULL,
    percentage NUMERIC(5,2) NOT NULL,
    fee_type   VARCHAR(20),
    active     BOOLEAN      NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_early_payment_discount_rules_tenant ON early_payment_discount_rules (tenant_id);
