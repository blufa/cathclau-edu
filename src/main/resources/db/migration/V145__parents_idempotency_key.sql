-- Clé d'idempotence fournie par le client (mode hors-ligne, §students) : rejouer une création de parent
-- avec la même clé après une coupure réseau renvoie le parent déjà créé au lieu d'en créer un second.
-- Même pattern que students.idempotency_key (V143) et teachers.idempotency_key (V144).
ALTER TABLE parents ADD COLUMN idempotency_key VARCHAR(100);
CREATE UNIQUE INDEX uq_parents_idempotency_key ON parents (idempotency_key)
    WHERE idempotency_key IS NOT NULL;
