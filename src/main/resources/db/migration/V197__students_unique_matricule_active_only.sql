-- Corrige le blocage de réinscription après archivage : la contrainte UNIQUE(tenant_id,
-- student_number) posée par V19 s'applique à TOUTES les lignes, y compris les élèves archivés
-- (deleted_at renseigné). `existsByTenantIdAndStudentNumber` respecte bien le filtre
-- @SQLRestriction("deleted_at IS NULL") de l'entité et considère donc un matricule archivé comme
-- disponible — mais l'INSERT échoue ensuite contre la contrainte inconditionnelle, qui ne sait rien
-- de l'archivage. Remplacée par un index unique PARTIEL, actif uniquement sur les élèves non archivés
-- : un matricule libéré par archivage redevient réellement réutilisable.
ALTER TABLE students DROP CONSTRAINT IF EXISTS uk_students_tenant_number;
CREATE UNIQUE INDEX uk_students_tenant_number ON students (tenant_id, student_number)
    WHERE deleted_at IS NULL;
