-- P10: autoriser DJOMY comme fournisseur de webhook entrant (callback de paiement).
ALTER TABLE payment_webhooks DROP CONSTRAINT payment_webhooks_provider_check;
ALTER TABLE payment_webhooks ADD CONSTRAINT payment_webhooks_provider_check
    CHECK (provider IN ('ORANGE_MONEY','MTN_MONEY','WAVE','STRIPE','DJOMY'));
