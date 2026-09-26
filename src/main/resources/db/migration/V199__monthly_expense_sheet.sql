-- Refonte du budget (§retour comptable, point 31) : remplace le circuit d'approbation par catégorie/
-- fournisseur par une fiche de dépense mensuelle simple (libellé, année scolaire, financement) avec
-- deux tableaux de lignes (dépenses, recettes) et une clôture qui verrouille le mois.
--
-- Additive uniquement : aucune colonne ni ligne de la table "expense" n'est supprimée, pour préserver
-- l'historique de toute dépense déjà saisie via l'ancien circuit d'approbation (catégorie, fournisseur,
-- statut...) — ces anciennes colonnes restent en base, simplement ignorées par le code applicatif.
-- Les tables "expense_category", "supplier" et "expense_budget" (enveloppe par catégorie, retirée)
-- restent également en base intactes, sans code applicatif dessus.

CREATE TABLE IF NOT EXISTS monthly_expense_sheet (
    id                BIGSERIAL PRIMARY KEY,
    tenant_id         BIGINT NOT NULL,
    label             VARCHAR(160) NOT NULL,
    academic_year     VARCHAR(20) NOT NULL,
    financing_mode    VARCHAR(16),
    financing_amount  NUMERIC(15, 2),
    created_at        TIMESTAMP,
    created_by        BIGINT,
    closed_at         TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_monthly_expense_sheet_tenant ON monthly_expense_sheet (tenant_id);

CREATE TABLE IF NOT EXISTS revenue_line (
    id            BIGSERIAL PRIMARY KEY,
    tenant_id     BIGINT NOT NULL,
    sheet_id      BIGINT NOT NULL REFERENCES monthly_expense_sheet (id) ON DELETE CASCADE,
    label         VARCHAR(255) NOT NULL,
    amount        NUMERIC(15, 2) NOT NULL,
    revenue_date  DATE,
    created_at    TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_revenue_line_sheet ON revenue_line (sheet_id);

ALTER TABLE expense ADD COLUMN IF NOT EXISTS sheet_id BIGINT REFERENCES monthly_expense_sheet (id) ON DELETE CASCADE;
ALTER TABLE expense ADD COLUMN IF NOT EXISTS necessary BOOLEAN NOT NULL DEFAULT false;
CREATE INDEX IF NOT EXISTS idx_expense_sheet ON expense (sheet_id);
