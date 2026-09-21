-- Ajoute le motif SALARY_PAID (SMS de confirmation de paie envoyé à l'enseignant/employé) à la liste
-- autorisée pour reason — même mécanisme que V158/V160. Sans ceci, le paiement d'une fiche de paie
-- réussit mais l'envoi de la notification échoue avec une violation de contrainte CHECK.
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT con.conname
        FROM pg_constraint con
        JOIN pg_class rel ON rel.oid = con.conrelid
        WHERE rel.relname = 'communications' AND con.contype = 'c'
          AND pg_get_constraintdef(con.oid) LIKE '%reason%'
    LOOP
        EXECUTE format('ALTER TABLE communications DROP CONSTRAINT %I', r.conname);
    END LOOP;
END $$;
ALTER TABLE communications ADD CONSTRAINT communications_reason_check
    CHECK (reason IN ('LATE','ABSENCE','PAYMENT_DUE','RESULT','MEETING','EXAM','GENERAL','ACCOUNT_CREATED','PASSWORD_RESET','SALARY_PAID'));

DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT con.conname
        FROM pg_constraint con
        JOIN pg_class rel ON rel.oid = con.conrelid
        WHERE rel.relname = 'message_templates' AND con.contype = 'c'
          AND pg_get_constraintdef(con.oid) LIKE '%reason%'
    LOOP
        EXECUTE format('ALTER TABLE message_templates DROP CONSTRAINT %I', r.conname);
    END LOOP;
END $$;
ALTER TABLE message_templates ADD CONSTRAINT message_templates_reason_check
    CHECK (reason IN ('LATE','ABSENCE','PAYMENT_DUE','RESULT','MEETING','EXAM','GENERAL','ACCOUNT_CREATED','PASSWORD_RESET','SALARY_PAID'));
