-- Un frais TRANSPORT se rattache à une ligne de bus (§billing), pas à une classe : des élèves de
-- classes différentes peuvent partager la même ligne, et le tarif dépend de la ligne (distance), pas
-- du niveau. class_id reste utilisé tel quel pour les autres types de frais.
ALTER TABLE school_fees ADD COLUMN IF NOT EXISTS route_id BIGINT;
