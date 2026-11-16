-- Bulletin de contrôle mensuel (§censeur) : diagnostic de niveau en cours d'année, totalement
-- séparé du bulletin officiel (report_cards) — jamais la même table, donc aucun risque de collision
-- ni d'être compté dans l'agrégation annuelle (qui ne lit que report_cards). Volontairement plus
-- simple que report_cards : pas de rang, mention, décision, distinction ni workflow de publication.
CREATE TABLE monthly_report_cards (
    id               BIGSERIAL PRIMARY KEY,
    tenant_id        BIGINT      NOT NULL,
    student_id       BIGINT      NOT NULL,
    class_id         BIGINT      NOT NULL,
    year_month       VARCHAR(7)  NOT NULL, -- "2026-11"
    overall_average  NUMERIC(5, 2),
    -- Appréciation automatique (§censeur, "pour que son parent puisse savoir si il doit faire des
    -- révisions") calculée à la génération depuis le même barème que le bulletin officiel
    -- (GradingScale.honorFor) — figée ici pour rester fidèle à l'appréciation du moment, même si le
    -- barème de l'établissement change ensuite. `comment` : appréciation manuelle du censeur, éditable
    -- après génération, jamais écrasée par une régénération (voir MonthlyReportCardService).
    auto_appreciation VARCHAR(255),
    comment          TEXT,
    generated_at     TIMESTAMP   NOT NULL,
    UNIQUE (student_id, class_id, year_month)
);
CREATE INDEX idx_monthlyrc_tenant ON monthly_report_cards (tenant_id);
CREATE INDEX idx_monthlyrc_class_month ON monthly_report_cards (class_id, year_month);

CREATE TABLE monthly_report_card_lines (
    id                       BIGSERIAL PRIMARY KEY,
    monthly_report_card_id   BIGINT      NOT NULL REFERENCES monthly_report_cards (id) ON DELETE CASCADE,
    subject_id               BIGINT      NOT NULL,
    subject_average          NUMERIC(5, 2),
    coefficient              NUMERIC(4, 2),
    teacher_name              VARCHAR(255)
);
CREATE INDEX idx_monthlyrcline_card ON monthly_report_card_lines (monthly_report_card_id);
