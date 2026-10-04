package gn.lacaeducation.payments.application.service;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.payments.application.event.CashierSessionClosedEvent;
import gn.lacaeducation.payments.application.port.out.CashierSessionRepository;
import gn.lacaeducation.payments.application.port.out.PaymentRepository;
import gn.lacaeducation.payments.domain.CashierSession;
import gn.lacaeducation.payments.domain.CashierSessionStatus;
import gn.lacaeducation.payments.domain.Payment;
import gn.lacaeducation.payments.domain.PaymentChannel;
import gn.lacaeducation.payments.domain.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Rapprochement de caisse (§angles morts comptables) : un comptable ouvre sa caisse au début du
 * service, encaisse des paiements CASH au guichet, puis la clôture en indiquant le comptant physique
 * compté — le système calcule l'écart avec ce qu'il a lui-même enregistré. Même logique que
 * {@code accounting.BankReconciliationService} (solde théorique vs réel), mais sans ligne d'éléments en
 * suspens : la comparaison est directe.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class CashierSessionService {

    private final CashierSessionRepository repo;
    private final PaymentRepository paymentRepo;
    private final ApplicationEventPublisher eventPublisher;

    public CashierSession open(Long tenantId, Long cashierId) {
        if (repo.findOpenByCashierId(cashierId).isPresent()) {
            throw new BusinessRuleException("Une session de caisse est déjà ouverte pour cet utilisateur.");
        }
        return repo.save(CashierSession.builder()
                .tenantId(tenantId).cashierId(cashierId)
                .openedAt(LocalDateTime.now()).status(CashierSessionStatus.OPEN)
                .createdAt(LocalDateTime.now()).build());
    }

    public CashierSession close(Long id, BigDecimal physicalCount, String notes) {
        CashierSession session = load(id);
        if (session.getStatus() != CashierSessionStatus.OPEN) {
            throw new BusinessRuleException("Cette session de caisse est déjà clôturée.");
        }
        LocalDateTime closedAt = LocalDateTime.now();
        BigDecimal systemTotal = computeSystemTotal(session.getCashierId(), session.getOpenedAt(), closedAt);
        BigDecimal count = physicalCount != null ? physicalCount : BigDecimal.ZERO;
        session.setClosedAt(closedAt);
        session.setSystemTotal(systemTotal);
        session.setPhysicalCount(count);
        session.setDiscrepancy(count.subtract(systemTotal));
        session.setNotes(notes);
        session.setStatus(CashierSessionStatus.CLOSED);
        CashierSession saved = repo.save(session);
        // §retour fondateurs (contrôle anti-fraude) : toujours publié, même écart nul — l'absence de
        // trace serait elle-même suspecte. L'alerte SMS au fondateur (communication) ne se déclenche
        // que si discrepancy != 0, voir le listener consommateur.
        eventPublisher.publishEvent(new CashierSessionClosedEvent(saved.getTenantId(), saved.getCashierId(),
                saved.getId(), saved.getSystemTotal(), saved.getPhysicalCount(), saved.getDiscrepancy()));
        return saved;
    }

    /** Total CASH réussi encaissé par ce comptable depuis l'ouverture, à afficher en temps réel pendant
     * qu'une session reste ouverte (sans la clôturer). */
    @Transactional(readOnly = true)
    public BigDecimal runningTotal(CashierSession session) {
        return computeSystemTotal(session.getCashierId(), session.getOpenedAt(), LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public java.util.Optional<CashierSession> currentOpen(Long cashierId) {
        return repo.findOpenByCashierId(cashierId);
    }

    @Transactional(readOnly = true)
    public CashierSession get(Long id) { return load(id); }

    @Transactional(readOnly = true)
    public List<CashierSession> history(Long tenantId, Long cashierId, CashierSessionStatus status) {
        return repo.findByTenantId(tenantId, cashierId, status);
    }

    private BigDecimal computeSystemTotal(Long cashierId, LocalDateTime from, LocalDateTime to) {
        List<Payment> payments = paymentRepo.findByCollectedByUserIdAndPaidAtBetween(cashierId, from, to);
        return payments.stream()
                .filter(p -> p.getChannel() == PaymentChannel.CASH && p.getStatus() == PaymentStatus.SUCCEEDED)
                .map(Payment::getAmount).filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Le filtre Hibernate multi-tenant ne s'applique pas à un find-by-PK (mêmes précautions que
     * {@code UserController.findById}) : on revérifie explicitement le tenant pour empêcher un
     * utilisateur d'un autre établissement de consulter/clôturer une session en devinant son id. */
    private CashierSession load(Long id) {
        Long ctx = TenantContext.get();
        return repo.findById(id)
                .filter(s -> ctx == null || ctx.equals(s.getTenantId()))
                .orElseThrow(() -> new ResourceNotFoundException("CashierSession", id));
    }
}
