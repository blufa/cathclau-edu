-- Certaines écoles séparent la période "cours" (interrogations/devoirs, ex. "1er Semestre" :
-- octobre-décembre) de la période "composition" correspondante (ex. "Compo 1er Semestre" : janvier) —
-- deux enregistrements distincts dans `periods`, mais UN SEUL bulletin (§retour fondateur, école Notre
-- Mère). Ce lien optionnel permet au moteur de notes de fusionner les évaluations des deux périodes
-- lors de la génération d'un bulletin, sans rien changer pour les écoles qui n'utilisent pas cette
-- convention (colonne nulle par défaut, comportement inchangé).
ALTER TABLE periods ADD COLUMN composition_period_id BIGINT REFERENCES periods (id);
