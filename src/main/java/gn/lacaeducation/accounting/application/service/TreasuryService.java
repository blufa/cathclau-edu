package gn.lacaeducation.accounting.application.service;

import gn.lacaeducation.accounting.application.port.out.CashFlowEntryRepository;
import gn.lacaeducation.accounting.application.port.out.TreasuryAccountRepository;
import gn.lacaeducation.accounting.domain.*;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Level-1 (SIMPLE) accounting: cash boxes / bank accounts and their cash-flow movements.
 * Movements update the account's current balance; auto-fed movements are idempotent on (refType, refId).
 */
@Service
@Transactional
@RequiredArgsConstructor
public class TreasuryService {

    private final TreasuryAccountRepository accountRepo;
    private final CashFlowEntryRepository entryRepo;

    // ── Accounts ──────────────────────────────────────────────────────
    public TreasuryAccount saveAccount(TreasuryAccount a) {
        if (a.getCurrentBalance() == null) a.setCurrentBalance(a.getInitialBalance() != null ? a.getInitialBalance() : BigDecimal.ZERO);
        if (a.getActive() == null) a.setActive(true);
        return accountRepo.save(a);
    }

    @Transactional(readOnly = true)
    public List<TreasuryAccount> accounts(Long tenantId) { return accountRepo.findByTenantId(tenantId); }

    public TreasuryAccount adjustBalance(Long id, BigDecimal newBalance) {
        TreasuryAccount a = loadAccount(id);
        a.setCurrentBalance(newBalance);
        return accountRepo.save(a);
    }

    public void deleteAccount(Long id) { accountRepo.deleteById(id); }

    // ── Movements ─────────────────────────────────────────────────────
    public record MovementCommand(Long tenantId, Long accountId, CashFlowDirection direction, BigDecimal amount,
                                  LocalDate date, CashFlowCategory category, String label, String justificationUrl) {}

    public CashFlowEntry createMovement(MovementCommand c) {
        TreasuryAccount account = loadAccount(c.accountId());
        if (c.amount() == null || c.amount().signum() <= 0) throw new BusinessRuleException("Le montant doit être positif");
        CashFlowEntry e = entryRepo.save(CashFlowEntry.builder()
                .tenantId(c.tenantId()).accountId(c.accountId()).direction(c.direction()).amount(c.amount())
                .currency(account.getCurrency()).date(c.date() != null ? c.date() : LocalDate.now())
                .category(c.category() != null ? c.category() : CashFlowCategory.OTHER).label(c.label())
                .referenceType(CashFlowReferenceType.MANUAL).reconciled(false).build());
        applyDelta(account, c.direction(), c.amount());
        return e;
    }

    public CashFlowEntry updateMovement(Long id, MovementCommand c) {
        CashFlowEntry e = loadEntry(id);
        TreasuryAccount account = loadAccount(e.getAccountId());
        // revert previous, apply new
        applyDelta(account, opposite(e.getDirection()), e.getAmount());
        e.setDirection(c.direction()); e.setAmount(c.amount()); e.setCategory(c.category());
        e.setLabel(c.label()); if (c.date() != null) e.setDate(c.date());
        applyDelta(account, c.direction(), c.amount());
        return entryRepo.save(e);
    }

    public void deleteMovement(Long id) {
        CashFlowEntry e = loadEntry(id);
        TreasuryAccount account = loadAccount(e.getAccountId());
        applyDelta(account, opposite(e.getDirection()), e.getAmount());
        entryRepo.deleteById(id);
    }

    public CashFlowEntry reconcile(Long id) {
        CashFlowEntry e = loadEntry(id);
        e.setReconciled(true);
        return entryRepo.save(e);
    }

    @Transactional(readOnly = true)
    public List<CashFlowEntry> movements(Long tenantId) { return entryRepo.findByTenantId(tenantId); }

    @Transactional(readOnly = true)
    public List<CashFlowEntry> accountJournal(Long accountId) {
        return entryRepo.findByAccountId(accountId).stream()
                .sorted(Comparator.comparing(CashFlowEntry::getDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(CashFlowEntry::getId))
                .toList();
    }

    /** Auto-feed from domain events (PaiementReussi → IN, DepensePayee → OUT). Idempotent. */
    public void recordAuto(Long tenantId, CashFlowDirection direction, BigDecimal amount, CashFlowCategory category,
                           CashFlowReferenceType refType, Long refId, String label) {
        if (entryRepo.findByReference(refType, refId).isPresent()) return; // idempotent
        TreasuryAccount account = defaultAccount(tenantId);
        if (account == null) return; // no account configured yet
        entryRepo.save(CashFlowEntry.builder()
                .tenantId(tenantId).accountId(account.getId()).direction(direction).amount(amount)
                .currency(account.getCurrency()).date(LocalDate.now()).category(category).label(label)
                .referenceType(refType).referenceId(refId).reconciled(false).build());
        applyDelta(account, direction, amount);
    }

    private TreasuryAccount defaultAccount(Long tenantId) {
        List<TreasuryAccount> active = accountRepo.findByTenantId(tenantId).stream()
                .filter(a -> Boolean.TRUE.equals(a.getActive())).toList();
        return active.stream().filter(a -> a.getType() == TreasuryAccountType.BANK).findFirst()
                .orElseGet(() -> active.stream().findFirst().orElse(null));
    }

    private void applyDelta(TreasuryAccount account, CashFlowDirection direction, BigDecimal amount) {
        BigDecimal base = account.getCurrentBalance() != null ? account.getCurrentBalance() : BigDecimal.ZERO;
        account.setCurrentBalance(direction == CashFlowDirection.IN ? base.add(amount) : base.subtract(amount));
        accountRepo.save(account);
    }

    private CashFlowDirection opposite(CashFlowDirection d) { return d == CashFlowDirection.IN ? CashFlowDirection.OUT : CashFlowDirection.IN; }
    private TreasuryAccount loadAccount(Long id) { return accountRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Compte introuvable : " + id)); }
    private CashFlowEntry loadEntry(Long id) { return entryRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Mouvement introuvable : " + id)); }
}
