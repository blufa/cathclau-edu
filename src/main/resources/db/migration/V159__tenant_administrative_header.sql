-- Rattachement administratif de l'école (IRE / DPE / sous-préfecture) pour l'en-tête officiel des
-- documents pédagogiques (bulletins, fiches de note...), reproduisant le format des documents papier
-- guinéens (MEPU-A / IRE / DPE / DSSEE).
ALTER TABLE tenant ADD COLUMN IF NOT EXISTS ire VARCHAR(255);
ALTER TABLE tenant ADD COLUMN IF NOT EXISTS dpe_label VARCHAR(255);
ALTER TABLE tenant ADD COLUMN IF NOT EXISTS sous_prefecture_label VARCHAR(255);
