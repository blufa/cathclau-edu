-- Frais annexe d'accès à la plateforme (§billing) : le parent choisit à l'inscription si son enfant
-- doit être connecté à la plateforme. Si oui, le frais d'usage plateforme (platform_fee_settings)
-- est ajouté à chaque mensualité générée pour cet élève sur l'année ; sinon jamais. Par défaut à
-- FALSE : le frais plateforme n'est plus ajouté automatiquement à tous les élèves (comportement
-- précédent), seulement à ceux dont la famille a opté pour l'accès.
ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS platform_access_opt_in BOOLEAN NOT NULL DEFAULT FALSE;
