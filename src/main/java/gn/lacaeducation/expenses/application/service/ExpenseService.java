package gn.lacaeducation.expenses.application.service;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.expenses.application.event.ExpensePaidEvent;
import gn.lacaeducation.expenses.application.port.out.ExpenseRepository;
import gn.lacaeducation.expenses.domain.Expense;
import gn.lacaeducation.expenses.domain.ExpensePaymentMode;
import gn.lacaeducation.expenses.domain.ExpenseStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Expense workflow (features/SCHOOL_back_complements.md §13): DRAFT → SUBMITTED → APPROVED/REJECTED → PAID.
 * Illegal transitions are rejected as 422; edits/deletes are only allowed while DRAFT.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository repo;
    private final ApplicationEventPublisher eventPublisher;

    public record CreateExpenseCommand(Long tenantId, Long categoryId, Long supplierId, String label,
                                       BigDecimal amount, String currency, LocalDate expenseDate,
                                       ExpensePaymentMode paymentMode, String reference, String justificationUrl,
                                       Long budgetLineId) {}

    public Expense create(CreateExpenseCommand c) {
        if (c.label() == null || c.label().isBlank()) throw new BusinessRuleException("Le libellé est requis");
        if (c.amount() == null || c.amount().signum() <= 0) throw new BusinessRuleException("Le montant doit être positif");
        return repo.save(Expense.builder()
                .tenantId(c.tenantId()).categoryId(c.categoryId()).supplierId(c.supplierId())
                .budgetLineId(c.budgetLineId())
                .label(c.label()).amount(c.amount())
                .currency(c.currency() != null && !c.currency().isBlank() ? c.currency() : "GNF")
                .expenseDate(c.expenseDate() != null ? c.expenseDate() : LocalDate.now())
                .paymentMode(c.paymentMode()).reference(c.reference()).justificationUrl(c.justificationUrl())
                .status(ExpenseStatus.DRAFT).createdAt(LocalDateTime.now()).build());
    }

    public Expense update(Long id, CreateExpenseCommand c) {
        Expense e = load(id);
        if (e.getStatus() != ExpenseStatus.DRAFT) throw new BusinessRuleException("Seule une dépense en brouillon peut être modifiée");
        e.setCategoryId(c.categoryId()); e.setSupplierId(c.supplierId()); e.setLabel(c.label());
        e.setBudgetLineId(c.budgetLineId());
        e.setAmount(c.amount()); if (c.currency() != null) e.setCurrency(c.currency());
        e.setExpenseDate(c.expenseDate()); e.setPaymentMode(c.paymentMode());
        e.setReference(c.reference()); e.setJustificationUrl(c.justificationUrl());
        return repo.save(e);
    }

    public Expense submit(Long id) { return transition(id, ExpenseStatus.DRAFT, ExpenseStatus.SUBMITTED, e -> e.setSubmittedAt(LocalDateTime.now())); }

    public Expense approve(Long id, Long decidedBy) {
        return transition(id, ExpenseStatus.SUBMITTED, ExpenseStatus.APPROVED, e -> { e.setApprovedAt(LocalDateTime.now()); e.setDecidedBy(decidedBy); });
    }

    public Expense reject(Long id, Long decidedBy, String reason) {
        return transition(id, ExpenseStatus.SUBMITTED, ExpenseStatus.REJECTED, e -> { e.setRejectionReason(reason); e.setDecidedBy(decidedBy); });
    }

    public Expense pay(Long id) {
        // Le justificatif reste possible à joindre (voir création/édition) mais n'est plus exigé pour
        // payer — simplification demandée : ne pas bloquer le paiement faute de pièce jointe.
        Expense paid = transition(id, ExpenseStatus.APPROVED, ExpenseStatus.PAID, e -> e.setPaidAt(LocalDateTime.now()));
        // Accounting records an OUT cash-flow / entry from this event ; AnnualBudgetService increments
        // the linked BudgetLine's actualAmount when budgetLineId is set.
        eventPublisher.publishEvent(new ExpensePaidEvent(paid.getTenantId(), paid.getId(), paid.getCategoryId(), paid.getBudgetLineId(), paid.getAmount()));
        return paid;
    }

    public void delete(Long id) {
        Expense e = load(id);
        if (e.getStatus() != ExpenseStatus.DRAFT) throw new BusinessRuleException("Seule une dépense en brouillon peut être supprimée");
        repo.deleteById(id);
    }

    @Transactional(readOnly = true) public Expense findById(Long id) { return load(id); }
    @Transactional(readOnly = true) public List<Expense> findByTenant(Long t) { return repo.findByTenantId(t); }
    @Transactional(readOnly = true) public List<Expense> findByStatus(Long t, ExpenseStatus s) { return repo.findByTenantIdAndStatus(t, s); }

    private Expense transition(Long id, ExpenseStatus from, ExpenseStatus to, java.util.function.Consumer<Expense> mutate) {
        Expense e = load(id);
        if (e.getStatus() != from) {
            throw new BusinessRuleException("Transition invalide : " + e.getStatus() + " → " + to);
        }
        mutate.accept(e);
        e.setStatus(to);
        return repo.save(e);
    }

    private Expense load(Long id) { return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Dépense introuvable : " + id)); }
}
