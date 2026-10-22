-- Grille horaire du Primaire/Maternelle configurable par tenant (§3.2 cahier des charges) : avant ce
-- réglage, 08h00-18h00 par bandes fixes de 60 minutes était codé en dur côté frontend. Valeurs par
-- défaut identiques à l'ancien comportement codé en dur, pour ne rien changer tant que l'école n'a pas
-- explicitement configuré sa grille.
CREATE TABLE IF NOT EXISTS primary_timetable_settings (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    grid_start_time TIME NOT NULL DEFAULT '08:00',
    grid_end_time   TIME NOT NULL DEFAULT '18:00',
    band_minutes    INTEGER NOT NULL DEFAULT 60,
    CONSTRAINT uq_primary_timetable_settings_tenant UNIQUE (tenant_id)
);
