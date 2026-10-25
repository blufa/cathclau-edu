-- Permission self-service élève : ajouter un parent (prospect) lié uniquement à lui-même, depuis son
-- espace élève (§retour utilisateur : "donner le privilège aux élèves d'ajouter les parents
-- prospect"). Volontairement PAS assignée par défaut à STUDENT ici — l'établissement l'active lui-même
-- via Rôles & permissions (PermissionMatrix, déjà 100% piloté par les données).
INSERT INTO permissions (name, code) VALUES
    ('Créer un parent prospect pour soi-même (self-service élève)', 'parent:create_own')
ON CONFLICT (code) DO NOTHING;
