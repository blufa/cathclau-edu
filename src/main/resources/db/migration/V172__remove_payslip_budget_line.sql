-- Retire le rattachement d'une fiche de paie à une ligne de budget annuel voté (introduit par
-- V171) : la masse salariale est toujours financée directement par les recettes de l'école (frais
-- scolaires), jamais par le budget annuel voté, qui couvre le fonctionnement courant (carburant,
-- kermesse, événements) — les deux enveloppes ne doivent jamais se confondre.
ALTER TABLE payslips DROP COLUMN IF EXISTS budget_line_id;
