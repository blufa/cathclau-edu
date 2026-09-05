-- Ajoute le motif ACCOUNT_CREATED (identifiants envoyés par SMS à la création d'un compte, §2.2) à la
-- liste autorisée pour reason — et corrige au passage l'oubli de EXAM, utilisé par le code depuis la
-- planification d'évaluation (onAssessmentScheduled) mais jamais ajouté à ces contraintes CHECK.
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
    CHECK (reason IN ('LATE','ABSENCE','PAYMENT_DUE','RESULT','MEETING','EXAM','GENERAL','ACCOUNT_CREATED'));

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
    CHECK (reason IN ('LATE','ABSENCE','PAYMENT_DUE','RESULT','MEETING','EXAM','GENERAL','ACCOUNT_CREATED'));
