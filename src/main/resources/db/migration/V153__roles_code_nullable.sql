-- Bug bloquant depuis la création de la fonctionnalité « rôle personnalisé » : roles.code était
-- NOT NULL (V3__iam.sql) mais RoleService.create() enregistre volontairement un rôle personnalisé
-- sans code (seuls les rôles système en ont un, voir RoleService.update/delete : "code != null" =>
-- rôle système non modifiable/supprimable). Toute création de rôle personnalisé échouait donc avec
-- une violation de contrainte NOT NULL. PostgreSQL autorise plusieurs valeurs NULL sous une
-- contrainte UNIQUE (elles ne sont jamais considérées égales entre elles), donc UNIQUE reste valide.
ALTER TABLE roles ALTER COLUMN code DROP NOT NULL;
