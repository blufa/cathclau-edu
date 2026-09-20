package gn.lacaeducation.billing.domain;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Exonération / réduction exceptionnelle accordée à un élève (§8). Traçable et archivée : motif,
 * bénéficiaire, pourcentage ou montant, durée, responsable ayant validé. Jamais supprimée
 * physiquement (§10/§14) — seulement révoquée ({@code active = false}).
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Exemption {
    private Long id;
    private Long tenantId;
    private Long studentId;
    private ExemptionType type;
    /** Pourcentage [0,100] pour TOTAL/PARTIAL (TOTAL = 100). */
    private BigDecimal percentage;
    /** Montant fixe pour une remise EXCEPTIONAL. */
    private BigDecimal amount;
    /** Frais concernés — un seul, plusieurs, ou vide/null = tous les frais. */
    private List<FeeType> feeTypes;
    private String reason;
    private LocalDate startDate;
    private LocalDate endDate;
    /** Utilisateur (direction) ayant validé l'exonération. */
    private Long validatedBy;
    private boolean active;
    private LocalDateTime createdAt;
}
