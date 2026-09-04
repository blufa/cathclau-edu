package gn.lacaeducation.platform.application.port.in;

import gn.lacaeducation.platform.domain.PlatformFeeSetting;

import java.math.BigDecimal;

/** SUPER_ADMIN : définit le montant du frais d'usage de la plateforme (§ frais plateforme). */
public interface PlatformFeeSettingUseCase {
    /** Montant actif courant (zéro si jamais configuré). */
    BigDecimal currentAmount();
    /** Remplace le montant actif : désactive l'ancien, insère le nouveau. N'affecte jamais les
     * factures déjà émises (montant figé sur chaque facture au moment de sa génération). */
    PlatformFeeSetting setAmount(BigDecimal amount, Long createdByUserId);
}
