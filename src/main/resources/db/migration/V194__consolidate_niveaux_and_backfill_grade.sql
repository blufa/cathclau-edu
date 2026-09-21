-- Migration de données : réduit le catalogue Niveau à 3 valeurs fixes (Maternelle/Primaire/Secondaire)
-- et bascule la granularité pédagogique fine sur school_classes.grade (colonne ajoutée par V193).
--
-- Ordre : (1) backfill school_classes.grade depuis l'ancien libellé de niveau de la classe, avant de
-- toucher aux niveaux ; (2) créer les 3 niveaux consolidés par tenant ; (3) repointer school_classes
-- et teacher_levels vers ces niveaux consolidés ; (4) supprimer les anciens niveaux devenus orphelins.

-- (1) Backfill du grade précis depuis le libellé (normalisé, insensible à la casse) de l'ancien
-- niveau de la classe — couvre les entrées connues de l'ancien catalogue (18 niveaux, voir l'ancien
-- level-catalog.tsx) et les libellés de grade eux-mêmes (au cas où une école aurait directement
-- nommé un niveau "CP1", "7ème"...). Les libellés non reconnus (nomenclature maison) laissent
-- grade=NULL : dégradation gracieuse, la classe reste utilisable, seulement sans grade exploitable
-- finement (calcul de remise fratrie, catalogue MEPUA).
WITH grade_map(norm_label, grade_code) AS (VALUES
    ('garderie', 'GARDERIE'),
    ('petite section', 'PETITE_SECTION'),
    ('moyenne section', 'MOYENNE_SECTION'),
    ('grande section', 'GRANDE_SECTION'),
    ('cours d''initiation (ci)', 'CP1'),
    ('cours préparatoire (cp)', 'CP1'),
    ('cours élémentaire 1 (ce1)', 'CE1'),
    ('cours élémentaire 2 (ce2)', 'CE2'),
    ('cours moyen 1 (cm1)', 'CM1'),
    ('cours moyen 2 (cm2)', 'CM2'),
    ('cp1', 'CP1'), ('cp2', 'CP2'), ('ce1', 'CE1'), ('ce2', 'CE2'), ('cm1', 'CM1'), ('cm2', 'CM2'),
    ('6ème', 'SIXIEME'), ('5ème', 'CINQUIEME'), ('4ème', 'QUATRIEME'), ('3ème', 'TROISIEME'),
    ('7ème', 'SEPTIEME'), ('8ème', 'HUITIEME'), ('9ème', 'NEUVIEME'), ('10ème', 'DIXIEME'),
    ('2nde', 'SECONDE'), ('1ère', 'PREMIERE'),
    ('11ème', 'ONZIEME'), ('12ème', 'DOUZIEME'), ('terminale', 'TERMINALE')
)
UPDATE school_classes sc
SET grade = gm.grade_code
FROM levels l
JOIN grade_map gm ON gm.norm_label = lower(trim(l.label))
WHERE sc.level_id = l.id
  AND sc.grade IS NULL;

-- (2) Créer les 3 niveaux consolidés par tenant, s'ils n'existent pas déjà. "Secondaire" stocke
-- cycle=COLLEGE par convention (fusion Collège+Lycée ; l'enum SchoolCycle reste à 6 valeurs pour les
-- périodes et le catalogue MEPUA, qui dérivent désormais Collège/Lycée du grade de la classe plutôt
-- que du niveau — voir Grade.mepuaCycle() côté academics).
INSERT INTO levels (tenant_id, label, sort_order, cycle)
SELECT DISTINCT t.tenant_id, v.label, v.sort_order, v.cycle
FROM (SELECT DISTINCT tenant_id FROM levels) t
CROSS JOIN (VALUES
    ('Maternelle', 1, 'MATERNELLE'),
    ('Primaire',   2, 'PRIMAIRE'),
    ('Secondaire', 3, 'COLLEGE')
) AS v(label, sort_order, cycle)
WHERE NOT EXISTS (
    SELECT 1 FROM levels l2 WHERE l2.tenant_id = t.tenant_id AND l2.label = v.label
);

-- (3a) Repointer school_classes vers le niveau consolidé du tenant, selon le cycle de l'ancien
-- niveau (GARDERIE+MATERNELLE→Maternelle, PRIMAIRE→Primaire, COLLEGE+LYCEE+SUPERIEUR→Secondaire —
-- SUPERIEUR n'a pas d'équivalent parmi les 3 valeurs retenues, rattaché à Secondaire par défaut,
-- cas rare en pratique). Les niveaux sans cycle renseigné (référentiel jamais mis à jour) sont
-- laissés tels quels, cohérent avec le comportement déjà lenient de ClasseService à leur égard.
UPDATE school_classes sc
SET level_id = new_l.id
FROM levels old_l
JOIN levels new_l ON new_l.tenant_id = old_l.tenant_id
    AND new_l.label = CASE
        WHEN old_l.cycle IN ('GARDERIE', 'MATERNELLE') THEN 'Maternelle'
        WHEN old_l.cycle = 'PRIMAIRE' THEN 'Primaire'
        WHEN old_l.cycle IN ('COLLEGE', 'LYCEE', 'SUPERIEUR') THEN 'Secondaire'
        ELSE NULL
    END
WHERE sc.level_id = old_l.id
  AND old_l.label NOT IN ('Maternelle', 'Primaire', 'Secondaire')
  AND new_l.id IS NOT NULL;

-- (3b) Idem pour teacher_levels (niveaux enseignés par un enseignant) — pas de contrainte FK vers
-- levels, mais une contrainte UNIQUE(teacher_id, level_id) : on insère les correspondances
-- dédupliquées vers le niveau consolidé avant de retirer les anciennes lignes, pour ne pas perdre
-- l'affectation d'un enseignant qui enseignait par exemple à la fois en 7ème et en 8ème (toutes
-- deux → Secondaire, ce qui produirait sinon un doublon rejeté par la contrainte unique).
INSERT INTO teacher_levels (tenant_id, teacher_id, level_id)
SELECT DISTINCT tl.tenant_id, tl.teacher_id, new_l.id
FROM teacher_levels tl
JOIN levels old_l ON old_l.id = tl.level_id
JOIN levels new_l ON new_l.tenant_id = old_l.tenant_id
    AND new_l.label = CASE
        WHEN old_l.cycle IN ('GARDERIE', 'MATERNELLE') THEN 'Maternelle'
        WHEN old_l.cycle = 'PRIMAIRE' THEN 'Primaire'
        WHEN old_l.cycle IN ('COLLEGE', 'LYCEE', 'SUPERIEUR') THEN 'Secondaire'
        ELSE NULL
    END
WHERE old_l.label NOT IN ('Maternelle', 'Primaire', 'Secondaire')
  AND new_l.id IS NOT NULL
ON CONFLICT (teacher_id, level_id) DO NOTHING;

DELETE FROM teacher_levels tl
USING levels old_l
WHERE tl.level_id = old_l.id
  AND old_l.label NOT IN ('Maternelle', 'Primaire', 'Secondaire');

-- (4) Supprimer les anciens niveaux devenus orphelins (plus aucune classe ni affectation enseignant
-- ne les référence).
DELETE FROM levels l
WHERE l.label NOT IN ('Maternelle', 'Primaire', 'Secondaire')
  AND NOT EXISTS (SELECT 1 FROM school_classes sc WHERE sc.level_id = l.id)
  AND NOT EXISTS (SELECT 1 FROM teacher_levels tl WHERE tl.level_id = l.id);
