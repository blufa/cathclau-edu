package gn.lacaeducation.platform.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.payments.DjomyGatewayApi;
import gn.lacaeducation.platform.application.port.in.PlatformFeeSettlementUseCase;
import gn.lacaeducation.platform.application.port.out.PlatformFeeLedgerRepository;
import gn.lacaeducation.platform.application.port.out.PlatformFeeSettlementRepository;
import gn.lacaeducation.platform.application.port.out.TenantRepository;
import gn.lacaeducation.platform.domain.PaymentChannel;
import gn.lacaeducation.platform.domain.PaymentStatus;
import gn.lacaeducation.platform.domain.PlatformFeeSettlement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Solde des frais plateforme dû par l'école courante et son règlement via Djomy (bouton « Verser »).
 * Même mécanique de redirection/confirmation que {@code AbonnementService} pour l'abonnement SaaS,
 * appliquée ici au solde cumulé du registre plutôt qu'à une facture d'abonnement unique.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PlatformFeeSettlementService implements PlatformFeeSettlementUseCase {

    private final PlatformFeeLedgerRepository ledgerRepository;
    private final PlatformFeeSettlementRepository settlementRepository;
    private final DjomyGatewayApi djomyGatewayApi;
    private final TenantRepository tenantRepository;

    @Override
    @Transactional(readOnly = true)
    public BigDecimal currentBalance() {
        BigDecimal collected = ledgerRepository.sumForCurrentTenant();
        BigDecimal settled = settlementRepository.sumSucceededForCurrentTenant();
        BigDecimal balance = collected.subtract(settled);
        return balance.signum() < 0 ? BigDecimal.ZERO : balance;
    }

    @Override
    public PlatformFeeSettlement initiate(String payerNumber) throws JsonProcessingException {
        BigDecimal balance = currentBalance();
        if (balance.signum() <= 0) {
            throw new BusinessRuleException("Aucun solde à verser à la plateforme actuellement.");
        }
        Long tenantId = TenantContext.get();
        String reference = generateRef("PFS");
        PlatformFeeSettlement settlement = settlementRepository.save(PlatformFeeSettlement.builder()
                .tenantId(tenantId)
                .reference(reference)
                .amount(balance)
                .channel(PaymentChannel.DJOMY)
                .status(PaymentStatus.INITIATED)
                .createdAt(LocalDateTime.now())
                .build());
        try {
            DjomyGatewayApi.GatewayResult gw = djomyGatewayApi.createGatewayPayment(
                    payerNumber, balance, reference, "Frais plateforme " + reference);
            settlement.setExternalTransactionId(gw.transactionId());
            settlement.setRedirectUrl(gw.redirectUrl());
            settlement.setStatus(PaymentStatus.PENDING);
            return settlementRepository.save(settlement);
        } catch (RuntimeException | JsonProcessingException e) {
            log.error("Échec initiation règlement frais plateforme tenant={} montant={} : {}",
                    tenantId, balance, e.getMessage());
            settlement.setStatus(PaymentStatus.FAILED);
            settlementRepository.save(settlement);
            throw e;
        }
    }

    @Override
    public PlatformFeeSettlement confirmViaDjomy(Long settlementId) throws JsonProcessingException {
        PlatformFeeSettlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new ResourceNotFoundException("PlatformFeeSettlement", settlementId));
        if (settlement.getStatus() == PaymentStatus.SUCCESSFUL) {
            return settlement; // idempotent
        }
        if (settlement.getExternalTransactionId() == null || settlement.getExternalTransactionId().isBlank()) {
            return settlement;
        }
        if (djomyGatewayApi.isConfirmedSuccess(settlement.getExternalTransactionId())) {
            settlement.setStatus(PaymentStatus.SUCCESSFUL);
            settlement.setConfirmedAt(LocalDateTime.now());
            log.info("Règlement frais plateforme confirmé via Djomy : id={} tenant={} montant={}",
                    settlementId, settlement.getTenantId(), settlement.getAmount());
            return settlementRepository.save(settlement);
        }
        return settlement;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlatformFeeSettlement> findByTenantId(Long tenantId) {
        return settlementRepository.findByTenantId(tenantId);
    }

    /** SUPER_ADMIN (aucun tenant courant, donc les sommes explicites "hors filtre" ci-dessous plutôt
     * que celles filtrées automatiquement) : solde de chaque école, y compris celles à 0. */
    @Override
    @Transactional(readOnly = true)
    public List<TenantBalance> balancesByTenant() {
        return tenantRepository.findAll().stream()
                .map(t -> {
                    BigDecimal collected = ledgerRepository.sumByTenantId(t.getId());
                    BigDecimal settled = settlementRepository.sumSucceededByTenantId(t.getId());
                    BigDecimal balance = collected.subtract(settled);
                    return new TenantBalance(t.getId(), t.getSchoolName(), collected, settled,
                            balance.signum() < 0 ? BigDecimal.ZERO : balance);
                })
                .sorted(Comparator.comparing(TenantBalance::balance).reversed())
                .toList();
    }

    private String generateRef(String prefix) {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String suffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return prefix + "-" + date + "-" + suffix;
    }
}
