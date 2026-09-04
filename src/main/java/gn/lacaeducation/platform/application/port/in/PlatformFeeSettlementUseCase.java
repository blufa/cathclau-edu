package gn.lacaeducation.platform.application.port.in;

import com.fasterxml.jackson.core.JsonProcessingException;
import gn.lacaeducation.platform.domain.PlatformFeeSettlement;

import java.math.BigDecimal;
import java.util.List;

/**
 * École (SCHOOL_ADMIN/DIRECTOR/ACCOUNTANT) : consulte le solde dû à la plateforme et le règle via
 * Djomy (bouton « Verser »). Même mécanique de redirection/confirmation que l'abonnement SaaS
 * ({@code PaymentUseCase.initiate}/{@code confirmViaDjomy}), appliquée ici au solde des frais
 * plateforme plutôt qu'à une facture d'abonnement.
 */
public interface PlatformFeeSettlementUseCase {

    /** Solde actuellement dû par le tenant courant (somme du registre − règlements réussis). */
    BigDecimal currentBalance();

    /** Initie le règlement du solde dû (Djomy) : 422 si le solde est nul ou négatif. */
    PlatformFeeSettlement initiate(String payerNumber) throws JsonProcessingException;

    /** Vérifie le statut Djomy à la demande et confirme si SUCCESS (idempotent). */
    PlatformFeeSettlement confirmViaDjomy(Long settlementId) throws JsonProcessingException;

    List<PlatformFeeSettlement> findByTenantId(Long tenantId);
}
