package gn.lacaeducation.expenses.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Une ligne de recette au sein d'une {@link MonthlyExpenseSheet} — pertinente seulement pour les
 * fiches de budget d'événement/sortie/vente (kermesse, sortie scolaire...), indépendante des recettes
 * de frais scolaires classiques (module {@code billing}), jamais mélangée avec elles.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueLine {
    private Long id;
    private Long tenantId;
    private Long sheetId;
    private String label;
    private BigDecimal amount;
    private LocalDate revenueDate;
    private LocalDateTime createdAt;
}
