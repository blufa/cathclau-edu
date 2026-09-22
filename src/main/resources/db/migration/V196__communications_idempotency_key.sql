-- Clé d'idempotence fournie par le client pour un envoi de communication (diffusion de masse ou
-- composition libre) : rejouer avec la même clé (ex. double-clic, requête réseau dupliquée) renvoie la
-- communication déjà créée au lieu d'en créer une seconde. Même pattern que payments.idempotency_key
-- (V142).
ALTER TABLE communications ADD COLUMN idempotency_key VARCHAR(100);
CREATE UNIQUE INDEX uq_communications_idempotency_key ON communications (idempotency_key)
    WHERE idempotency_key IS NOT NULL;
