-- Rang (§censeur, "il y'a le rang sur le bulletin") : classement de l'élève dans sa classe pour le
-- mois, recalculé à chaque génération (voir RankingUtil / MonthlyReportCardService.updateRanks) —
-- même convention que report_cards.rank/class_size, mais aucune des autres colonnes (pas de
-- mention/décision/distinction : ce bulletin reste un diagnostic, pas un document de passage).
ALTER TABLE monthly_report_cards
    ADD COLUMN rank       INTEGER,
    ADD COLUMN class_size INTEGER;
