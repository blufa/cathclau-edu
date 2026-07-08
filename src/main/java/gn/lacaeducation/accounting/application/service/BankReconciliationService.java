package gn.lacaeducation.accounting.application.service;

import gn.lacaeducation.accounting.application.port.out.BankReconciliationRepository;
import gn.lacaeducation.accounting.application.port.out.ReconciliationItemRepository;
import gn.lacaeducation.accounting.application.port.out.TreasuryAccountRepository;
import gn.lacaeducation.accounting.domain.BankReconciliation;
import gn.lacaeducation.accounting.domain.ReconciliationItem;
import gn.lacaeducation.accounting.domain.ReconciliationItemType;
import gn.lacaeducation.accounting.domain.ReconciliationStatus;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Rapprochement bancaire (Manuel §8) : confronte le solde du relevé au solde du livre (compte de
 * trésorerie) en tenant compte des éléments en suspens. Réussi quand l'écart ajusté est nul.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class BankReconciliationService {

    private final BankReconciliationRepository reconRepo;
    private final ReconciliationItemRepository itemRepo;
    private final TreasuryAccountRepository accountRepo;

    public record ReconciliationCommand(Long tenantId, Long treasuryAccountId, LocalDate statementDate,
                                        BigDecimal statementBalance, String notes) {}
    public record ItemCommand(ReconciliationItemType itemType, String description, BigDecimal amount) {}
    public record ReconciliationSummary(BigDecimal statementBalance, BigDecimal bookBalance,
                                        BigDecimal adjustedBank, BigDecimal adjustedBook,
                                        BigDecimal difference, boolean balanced) {}

    public BankReconciliation create(ReconciliationCommand c) {
        BigDecimal book = accountRepo.findById(c.treasuryAccountId())
                .map(a -> a.getCurrentBalance() != null ? a.getCurrentBalance() : BigDecimal.ZERO)
                .orElseThrow(() -> new ResourceNotFoundException("TreasuryAccount", c.treasuryAccountId()));
        return reconRepo.save(BankReconciliation.builder()
                .tenantId(c.tenantId()).treasuryAccountId(c.treasuryAccountId())
                .statementDate(c.statementDate() != null ? c.statementDate() : LocalDate.now())
                .statementBalance(nz(c.statementBalance())).bookBalance(book)
                .status(ReconciliationStatus.DRAFT).notes(c.notes()).createdAt(LocalDateTime.now()).build());
    }

    public BankReconciliation complete(Long id, Long userId) {
        BankReconciliation r = load(id);
        ReconciliationSummary s = summary(id);
        if (!s.balanced()) throw new BusinessRuleException("Rapprochement déséquilibré : écart de " + s.difference());
        r.setStatus(ReconciliationStatus.COMPLETED); r.setReconciledBy(userId);
        return reconRepo.save(r);
    }

    public BankReconciliation reopen(Long id) {
        BankReconciliation r = load(id);
        r.setStatus(ReconciliationStatus.DRAFT);
        return reconRepo.save(r);
    }

    public void delete(Long id) {
        itemRepo.deleteByReconciliationId(id);
        reconRepo.deleteById(id);
    }

    public ReconciliationItem addItem(Long reconciliationId, ItemCommand c) {
        BankReconciliation r = load(reconciliationId);
        if (r.getStatus() == ReconciliationStatus.COMPLETED) throw new BusinessRuleException("Rapprochement clôturé");
        return itemRepo.save(ReconciliationItem.builder()
                .tenantId(r.getTenantId()).reconciliationId(reconciliationId)
                .itemType(c.itemType()).description(c.description()).amount(nz(c.amount())).build());
    }

    public void deleteItem(Long itemId) { itemRepo.deleteById(itemId); }

    @Transactional(readOnly = true)
    public ReconciliationSummary summary(Long reconciliationId) {
        BankReconciliation r = load(reconciliationId);
        List<ReconciliationItem> items = itemRepo.findByReconciliationId(reconciliationId);
        BigDecimal deposits = sum(items, ReconciliationItemType.DEPOSIT_IN_TRANSIT);
        BigDecimal cheques = sum(items, ReconciliationItemType.OUTSTANDING_CHEQUE);
        BigDecimal interest = sum(items, ReconciliationItemType.BANK_INTEREST);
        BigDecimal charges = sum(items, ReconciliationItemType.BANK_CHARGE);
        BigDecimal other = sum(items, ReconciliationItemType.OTHER).add(sum(items, ReconciliationItemType.ERROR));
        BigDecimal adjustedBank = nz(r.getStatementBalance()).add(deposits).subtract(cheques);
        BigDecimal adjustedBook = nz(r.getBookBalance()).add(interest).subtract(charges).add(other);
        BigDecimal diff = adjustedBank.subtract(adjustedBook);
        return new ReconciliationSummary(nz(r.getStatementBalance()), nz(r.getBookBalance()),
                adjustedBank, adjustedBook, diff, diff.signum() == 0);
    }

    @Transactional(readOnly = true)
    public List<BankReconciliation> list(Long tenantId) { return reconRepo.findByTenantId(tenantId); }

    @Transactional(readOnly = true)
    public BankReconciliation get(Long id) { return load(id); }

    @Transactional(readOnly = true)
    public List<ReconciliationItem> items(Long reconciliationId) { return itemRepo.findByReconciliationId(reconciliationId); }

    private BigDecimal sum(List<ReconciliationItem> items, ReconciliationItemType type) {
        return items.stream().filter(i -> i.getItemType() == type)
                .map(i -> nz(i.getAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal nz(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }
    private BankReconciliation load(Long id) { return reconRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("BankReconciliation", id)); }
}
