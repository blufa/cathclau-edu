package gn.lacaeducation.grading.domain;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Grade {
    private Long id;
    private Long assessmentId;
    private Long studentId;
    /** Toujours la note finale utilisée partout en aval (moyennes, bulletins) — moyenne de
     * {@link #valueA}/{@link #valueB} quand la saisie se fait en deux cases (Orale/Écrite, ou
     * Dictée-Question/Rédaction pour le Français), sinon la valeur saisie directement. */
    private BigDecimal value;
    /** Renseignées uniquement quand l'évaluation se saisit en deux cases (voir
     * {@code componentLabels} côté frontend) — null sinon. Jamais lues par les moteurs de calcul
     * de moyenne (semestre, bulletin mensuel), qui ne connaissent que {@link #value}. */
    private BigDecimal valueA;
    private BigDecimal valueB;
    private Boolean validated;
    private LocalDateTime enteredAt;
}
