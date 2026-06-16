-- P9: ajoute le fournisseur agrégateur Djomy à côté des autres (Orange/MTN/Wave/Stripe).
-- Djomy sert les deux flux (paiements scolaires + abonnements). Côté platform, le canal
-- de subscription_payment est un VARCHAR libre (sans CHECK) : aucune migration requise là-bas.

-- Canal de paiement scolaire : autoriser DJOMY.
ALTER TABLE payments DROP CONSTRAINT payments_channel_check;
ALTER TABLE payments ADD CONSTRAINT payments_channel_check
    CHECK (channel IN ('ORANGE_MONEY','MTN_MONEY','WAVE','BANK_CARD','DJOMY'));

-- Fournisseur de la transaction sortante : autoriser DJOMY.
ALTER TABLE payment_transactions DROP CONSTRAINT payment_transactions_provider_check;
ALTER TABLE payment_transactions ADD CONSTRAINT payment_transactions_provider_check
    CHECK (provider IN ('ORANGE_MONEY','MTN_MONEY','WAVE','STRIPE','DJOMY'));

-- URL de redirection renvoyée par create_payment_gateway (portail Djomy).
ALTER TABLE payment_transactions ADD COLUMN redirect_url VARCHAR(1024);
