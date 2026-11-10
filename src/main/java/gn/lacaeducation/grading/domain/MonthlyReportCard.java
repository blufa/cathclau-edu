package gn.lacaeducation.grading.domain;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;

/** Bulletin de contrôle mensuel (§censeur) — diagnostic de niveau en cours d'année, totalement
 * séparé du {@link ReportCard} officiel : même gabarit visuel et rang (classement du mois, parmi
 * les élèves de la classe ayant une évaluation mensuelle ce mois-ci) que le bulletin officiel, mais
 * pas de mention, décision, distinction ni workflow de publication — ce n'est pas un document de
 * passage en classe supérieure. */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class MonthlyReportCard {
    private Long id;
    private Long tenantId;
    private Long studentId;
    private Long classId;
    private YearMonth yearMonth;
    private BigDecimal overallAverage;
    /** Rang dans la classe pour ce mois, recalculé à chaque régénération de la classe (même règle
     * d'ex æquo que {@link ReportCard#getRank()}, voir RankingUtil). */
    private Integer rank;
    private Integer classSize;
    /** Appréciation automatique (§censeur) calculée depuis le même barème que le bulletin officiel
     * (GradingScale.honorFor) — figée à la génération, voir MonthlyReportCardService. */
    private String autoAppreciation;
    /** Appréciation manuelle du censeur — éditable, jamais écrasée par une régénération. */
    private String comment;
    private LocalDateTime generatedAt;
}
