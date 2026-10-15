-- Second school phone number, distinct from the single `phone` column which in practice often ends
-- up holding the director's personal/login number by mistake (nothing distinguishes it on the
-- create/edit forms) — lets a school print two numbers on badges/receipts without conflating either
-- with a person's private line. Nullable: most schools only need one number.
ALTER TABLE tenant ADD COLUMN phone2 VARCHAR(255);
