-- Clé d'idempotence fournie par le client (mode hors-ligne, §students) : rejouer une création d'élève
-- avec la même clé après une coupure réseau renvoie l'élève déjà créé au lieu d'en créer un second.
-- Même pattern que payments.idempotency_key (V142).
ALTER TABLE students ADD COLUMN idempotency_key VARCHAR(100);
CREATE UNIQUE INDEX uq_students_idempotency_key ON students (idempotency_key)
    WHERE idempotency_key IS NOT NULL;
