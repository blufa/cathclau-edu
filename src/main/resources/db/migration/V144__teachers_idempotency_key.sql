-- Clé d'idempotence fournie par le client (mode hors-ligne, §staff) : rejouer une création d'enseignant
-- avec la même clé après une coupure réseau renvoie l'enseignant déjà créé au lieu d'en créer un second.
-- Même pattern que students.idempotency_key (V143) et payments.idempotency_key (V142).
ALTER TABLE teachers ADD COLUMN idempotency_key VARCHAR(100);
CREATE UNIQUE INDEX uq_teachers_idempotency_key ON teachers (idempotency_key)
    WHERE idempotency_key IS NOT NULL;
