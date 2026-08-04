package gn.lacaeducation.grading.domain;

import lombok.*;

import java.util.List;

/**
 * Formule de calcul de la moyenne d'une matière, paramétrable par établissement et applicable à un
 * type de période donné (ex. « SEMESTRE », « TRIMESTRE » — cf. {@code academics.PeriodPublicInfo.type()}).
 * Composée d'étapes ordonnées ({@link GradingFormulaStep}) exécutées par {@link GradingFormulaEngine}.
 * Remplace l'ancien calcul plat codé en dur, qui reste représentable comme une formule à une seule
 * étape {@code GROUP} sans filtre (voir la migration des données existantes).
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class GradingFormula {
    private Long id;
    private Long tenantId;
    private String label;
    /** Type de période auquel s'applique cette formule (valeur de {@code PeriodPublicInfo.type()}). */
    private String periodType;
    private boolean active;
    private List<GradingFormulaStep> steps;
}
