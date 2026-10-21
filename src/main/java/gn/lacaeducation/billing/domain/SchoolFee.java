package gn.lacaeducation.billing.domain;
import lombok.*;
import java.math.BigDecimal;
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SchoolFee {
    private Long id;
    private Long tenantId;
    private Long classId;
    /** Ligne de transport (module transport) — utilisé à la place de {@link #classId} pour un frais de
     * type TRANSPORT, car une ligne dessert des élèves de classes différentes et le tarif dépend de la
     * ligne (distance), pas du niveau. Null pour les autres types de frais. */
    private Long routeId;
    private FeeType type;
    private BigDecimal amount;
    private String academicYear;
    /** Nom de la catégorie quand {@code type == OTHER} (ex. "Tenue scolaire", "Bus de transport"). */
    private String label;
    /** Facturé automatiquement à l'inscription (true, comportement historique) ou seulement sur
     * ajout explicite du comptable à l'élève concerné (false) — ex. un paquet de RAM facultatif
     * (§13 cahier des charges). Jamais null en base (défaut TRUE, voir migration). */
    private Boolean mandatory;
}
