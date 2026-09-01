-- Clé d'idempotence fournie par le client (mode hors-ligne, §payments) : rejouer un paiement groupé
-- avec la même clé après une coupure réseau renvoie le paiement déjà créé au lieu d'en créer un second.
-- Même pattern que payment_webhooks.external_event_id (V15).
ALTER TABLE payments ADD COLUMN idempotency_key VARCHAR(100);
CREATE UNIQUE INDEX uq_payments_idempotency_key ON payments (idempotency_key)
    WHERE idempotency_key IS NOT NULL;
