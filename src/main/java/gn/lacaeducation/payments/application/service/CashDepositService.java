package gn.lacaeducation.payments.application.service;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.payments.application.event.CashDepositCreatedEvent;
import gn.lacaeducation.payments.application.event.CashDepositDecidedEvent;
import gn.lacaeducation.payments.application.port.in.CashDepositUseCase;
import gn.lacaeducation.payments.application.port.out.CashDepositRepository;
import gn.lacaeducation.payments.domain.CashDeposit;
import gn.lacaeducation.payments.domain.CashDepositStatus;
import gn.lacaeducation.payments.domain.PaymentChannel;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Dépôt de fonds comptable → fondateur, avec validation explicite et SMS bidirectionnel (§5 cahier des
 * charges). Jamais considéré comme comptabilisé avant la validation du fondateur — {@code create} ne
 * fait que poser une demande PENDING ; seul {@code validate} acte le dépôt. Un échec d'envoi SMS
 * (découplé via événements, voir {@code CommunicationEventListener}) n'affecte jamais le statut
 * financier du dépôt lui-même.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class CashDepositService implements CashDepositUseCase {

    private final CashDepositRepository repo;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public CashDeposit create(CreateCashDepositCommand cmd) {
        if (cmd.idempotencyKey() != null && !cmd.idempotencyKey().isBlank()) {
            var existing = repo.findByTenantIdAndIdempotencyKey(cmd.tenantId(), cmd.idempotencyKey());
            if (existing.isPresent()) return existing.get();
        }
        if (cmd.amount() == null || cmd.amount().signum() <= 0) {
            throw new BusinessRuleException("Le montant du dépôt doit être positif.");
        }
        if (cmd.method() == null || !cmd.method().isDeskManual()) {
            throw new BusinessRuleException("Moyen de dépôt invalide — attendu : espèces, Orange Money ou virement bancaire.");
        }
        CashDeposit saved = repo.save(CashDeposit.builder()
                .tenantId(cmd.tenantId()).accountantId(cmd.accountantId())
                .amount(cmd.amount()).currency(cmd.currency() != null ? cmd.currency() : "GNF")
                .method(cmd.method()).destinationAccount(cmd.destinationAccount())
                .depositedAt(cmd.depositedAt() != null ? cmd.depositedAt() : LocalDateTime.now())
                .transactionReference(cmd.transactionReference()).comment(cmd.comment())
                .status(CashDepositStatus.PENDING)
                .idempotencyKey(cmd.idempotencyKey())
                .createdAt(LocalDateTime.now())
                .build());
        eventPublisher.publishEvent(new CashDepositCreatedEvent(saved.getTenantId(), saved.getId(),
                saved.getAccountantId(), saved.getAmount(), saved.getCurrency(), saved.getMethod().name(),
                saved.getDepositedAt(), saved.getTransactionReference()));
        return saved;
    }

    @Override @Transactional(readOnly = true)
    public CashDeposit findById(Long id) { return load(id); }

    @Override @Transactional(readOnly = true)
    public List<CashDeposit> search(Long tenantId, Long accountantId, CashDepositStatus status,
                                     PaymentChannel method, LocalDateTime from, LocalDateTime to) {
        return repo.search(tenantId, accountantId, status, method, from, to);
    }

    @Override
    public CashDeposit validate(Long id, Long founderUserId) {
        return decide(id, founderUserId, CashDepositStatus.VALIDATED, null);
    }

    @Override
    public CashDeposit reject(Long id, Long founderUserId, String reason) {
        return decide(id, founderUserId, CashDepositStatus.REJECTED, reason);
    }

    /** Verrou pessimiste avant lecture du statut (§5.3) : deux validations/rejets simultanés sur le
     * même dépôt ne peuvent plus l'un et l'autre réussir — le second trouve déjà un statut décidé. */
    private CashDeposit decide(Long id, Long founderUserId, CashDepositStatus newStatus, String reason) {
        CashDeposit locked = repo.lockById(id)
                .filter(d -> sameTenant(d))
                .orElseThrow(() -> new ResourceNotFoundException("CashDeposit", id));
        if (!locked.isPending()) {
            throw new BusinessRuleException("Ce dépôt a déjà été " +
                    (locked.getStatus() == CashDepositStatus.VALIDATED ? "validé" : "rejeté") + ".");
        }
        locked.setStatus(newStatus);
        locked.setValidatedBy(founderUserId);
        locked.setValidatedAt(LocalDateTime.now());
        if (newStatus == CashDepositStatus.REJECTED) locked.setRejectionReason(reason);
        CashDeposit saved = repo.save(locked);
        eventPublisher.publishEvent(new CashDepositDecidedEvent(saved.getTenantId(), saved.getId(),
                saved.getAccountantId(), founderUserId, newStatus.name(), saved.getAmount(), saved.getCurrency(),
                saved.getMethod().name(), saved.getRejectionReason()));
        return saved;
    }

    /** Le filtre Hibernate multi-tenant ne s'applique pas à un find-by-PK (même précaution que
     * {@code CashierSessionService#load}) : revérifié explicitement pour empêcher un fondateur d'une
     * autre école de valider/consulter un dépôt en devinant son id. */
    private boolean sameTenant(CashDeposit d) {
        Long ctx = TenantContext.get();
        return ctx == null || ctx.equals(d.getTenantId());
    }

    private CashDeposit load(Long id) {
        return repo.findById(id).filter(this::sameTenant)
                .orElseThrow(() -> new ResourceNotFoundException("CashDeposit", id));
    }
}
