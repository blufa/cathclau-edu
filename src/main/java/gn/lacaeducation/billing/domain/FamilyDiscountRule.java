package gn.lacaeducation.billing.domain;

import lombok.*;

import java.math.BigDecimal;

/**
 * Règle de réduction familiale paramétrable (§5) : « si une famille a entre {@code minChildren} et
 * {@code maxChildren} enfants inscrits, appliquer {@code percentage} % sur le frais {@code feeType} ».
 * Aucun montant/seuil codé en dur : tout est configuré par l'établissement.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class FamilyDiscountRule {
    private Long id;
    private Long tenantId;
    private String label;
    /** Nombre minimum d'enfants de la famille pour déclencher la règle. */
    private Integer minChildren;
    /** Nombre maximum d'enfants (null = pas de borne supérieure, ex. « plus de 5 »). */
    private Integer maxChildren;
    /** Pourcentage de réduction [0, 100] (100 = exonération totale). */
    private BigDecimal percentage;
    /** Frais concerné (null = tous les frais). */
    private FeeType feeType;
    /** true = ne s'applique qu'au plus jeune enfant de la famille ; false = à tous les enfants éligibles. */
    private boolean appliesToYoungest;
    private boolean active;
}
