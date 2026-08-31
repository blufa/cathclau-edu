package gn.lacaeducation.platform.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Montant du frais d'usage de la plateforme, défini par SUPER_ADMIN et appliqué à chaque nouvelle
 * facture de mensualité (frais scolaire type MONTHLY) générée par une école. Versionné par
 * insertion (jamais de mise à jour en place) : changer le montant n'affecte que les factures futures,
 * les factures déjà émises gardent le montant figé au moment de leur génération
 * ({@code Invoice.platformFeeAmount}).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformFeeSetting {
    private Long id;
    private BigDecimal amount;
    private boolean active;
    private Long createdByUserId;
    private LocalDateTime createdAt;
}
