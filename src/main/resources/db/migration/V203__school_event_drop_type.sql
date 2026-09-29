-- Le jour férié (chômé et payé par l'État) existe déjà dans le calendrier académique
-- (academic_events, type HOLIDAY — voir CalendarDataProvider#paidHolidayDates) : le type HOLIDAY
-- ajouté ici en V202 était un doublon erroné. Un événement école (school_events) reste désormais
-- toujours un événement "autre" (kermesse, sortie...) dont la direction choisit les enseignants
-- crédités — voir school_event_credited_teachers, conservée.

ALTER TABLE school_events DROP COLUMN type;
