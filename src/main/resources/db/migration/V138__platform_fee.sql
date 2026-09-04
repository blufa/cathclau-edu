-- Frais d'usage plateforme (SUPER_ADMIN) : montant ajouté aux factures de mensualité (frais type
-- MONTHLY uniquement), réservé à l'éditeur de la plateforme, réglé par chaque école via Djomy.
-- Distinct de l'abonnement SaaS (subscription/subscription_invoice/subscription_payment) : ce montant
-- est indexé sur l'encaissement réel des frais de scolarité, pas sur un abonnement fixe.

ALTER TABLE invoices ADD COLUMN platform_fee_amount NUMERIC(15,2);

-- Un seul montant actif à la fois. Le changer ne modifie jamais les factures déjà émises (le montant
-- est figé sur chaque facture au moment de sa génération) — seules les factures futures sont affectées.
CREATE TABLE platform_fee_settings (
    id                  BIGSERIAL PRIMARY KEY,
    amount              NUMERIC(15,2) NOT NULL,
    active              BOOLEAN       NOT NULL DEFAULT TRUE,
    created_by_user_id  BIGINT,
    created_at          TIMESTAMP     NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX idx_platform_fee_settings_one_active ON platform_fee_settings (active) WHERE active = true;

-- Registre d'append : une ligne par part de frais plateforme effectivement encaissée (au prorata de
-- chaque paiement appliqué à une facture qui porte un platform_fee_amount). Jamais modifié après coup.
CREATE TABLE platform_fee_ledger_entries (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT        NOT NULL,
    invoice_id  BIGINT        NOT NULL,
    amount      NUMERIC(15,2) NOT NULL,
    created_at  TIMESTAMP     NOT NULL DEFAULT now()
);
CREATE INDEX idx_platform_fee_ledger_tenant ON platform_fee_ledger_entries (tenant_id);

-- Règlement du solde dû par une école (bouton « Verser » → redirection Djomy), même mécanique que
-- SubscriptionPayment (statuts INITIATED/PENDING/SUCCESSFUL/FAILED/REFUNDED, canal DJOMY).
CREATE TABLE platform_fee_settlements (
    id                      BIGSERIAL PRIMARY KEY,
    tenant_id               BIGINT        NOT NULL,
    reference               VARCHAR(100)  NOT NULL UNIQUE,
    amount                  NUMERIC(15,2) NOT NULL,
    channel                 VARCHAR(20)   NOT NULL,
    status                  VARCHAR(20)   NOT NULL,
    external_transaction_id VARCHAR(150),
    redirect_url            VARCHAR(1024),
    created_at              TIMESTAMP     NOT NULL DEFAULT now(),
    confirmed_at            TIMESTAMP
);
CREATE INDEX idx_platform_fee_settlements_tenant ON platform_fee_settlements (tenant_id);
