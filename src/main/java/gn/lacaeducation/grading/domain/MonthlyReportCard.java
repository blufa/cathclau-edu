package gn.lacaeducation.grading.domain;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;

/** Bulletin de contrôle mensuel (§censeur) — diagnostic de niveau en cours d'année, totalement
 * séparé du {@link ReportCard} officiel : pas de rang, mention, décision ni workflow de publication. */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class MonthlyReportCard {
    private Long id;
    private Long tenantId;
    private Long studentId;
    private Long classId;
    private YearMonth yearMonth;
    private BigDecimal overallAverage;
    /** Appréciation automatique (§censeur) calculée depuis le même barème que le bulletin officiel
     * (GradingScale.honorFor) — figée à la génération, voir MonthlyReportCardService. */
    private String autoAppreciation;
    /** Appréciation manuelle du censeur — éditable, jamais écrasée par une régénération. */
    private String comment;
    private LocalDateTime generatedAt;
}
