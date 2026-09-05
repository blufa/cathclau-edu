-- Créneau d'emploi du temps sans matière formelle (« heure creuse » : étude, permanence, TD...) :
-- un libellé libre sert d'intitulé d'affichage quand subject_id est nul.
ALTER TABLE schedules ADD COLUMN IF NOT EXISTS label VARCHAR(255);
