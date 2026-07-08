package gn.lacaeducation.accounting.application.service;

import gn.lacaeducation.accounting.application.port.out.ChartOfAccountRepository;
import gn.lacaeducation.accounting.application.port.out.JournalEntryRepository;
import gn.lacaeducation.accounting.application.port.out.JournalRepository;
import gn.lacaeducation.accounting.domain.*;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Double-entry (SYSCOHADA) bookkeeping: chart of accounts, journals and balanced journal entries.
 * Each entry must satisfy total debit = total credit (422 otherwise); a VALIDATED entry is immutable.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class DoubleEntryService {

    private final ChartOfAccountRepository accountRepo;
    private final JournalRepository journalRepo;
    private final JournalEntryRepository entryRepo;

    // ── Chart of accounts ─────────────────────────────────────────────
    public ChartOfAccount saveAccount(ChartOfAccount a) {
        if (a.getActive() == null) a.setActive(true);
        if (a.getLettrable() == null) a.setLettrable(false);
        return accountRepo.save(a);
    }
    @Transactional(readOnly = true) public List<ChartOfAccount> accounts(Long tenantId) { return accountRepo.findByTenantId(tenantId); }
    public ChartOfAccount updateAccount(Long id, ChartOfAccount patch) {
        ChartOfAccount a = accountRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Compte introuvable : " + id));
        a.setAccountNumber(patch.getAccountNumber()); a.setLabel(patch.getLabel());
        a.setAccountClass(patch.getAccountClass()); a.setType(patch.getType()); a.setParentId(patch.getParentId());
        if (patch.getLettrable() != null) a.setLettrable(patch.getLettrable());
        if (patch.getActive() != null) a.setActive(patch.getActive());
        return accountRepo.save(a);
    }
    public void deleteAccount(Long id) { accountRepo.deleteById(id); }

    // ── Journals ──────────────────────────────────────────────────────
    public Journal saveJournal(Journal j) { return journalRepo.save(j); }
    @Transactional(readOnly = true) public List<Journal> journals(Long tenantId) { return journalRepo.findByTenantId(tenantId); }
    public void deleteJournal(Long id) { journalRepo.deleteById(id); }

    // ── Journal entries ───────────────────────────────────────────────
    public JournalEntry createEntry(JournalEntry e) {
        validateLines(e.getLines());
        BigDecimal debit = sum(e.getLines(), true);
        BigDecimal credit = sum(e.getLines(), false);
        if (debit.compareTo(credit) != 0) {
            throw new BusinessRuleException("Écriture déséquilibrée : débit " + debit + " ≠ crédit " + credit);
        }
        e.setTotalDebit(debit);
        e.setTotalCredit(credit);
        e.setStatus(JournalEntryStatus.DRAFT);
        if (e.getDate() == null) e.setDate(LocalDate.now());
        e.setPieceNumber(nextPieceNumber(e.getJournalId(), e.getFiscalYearId()));
        return entryRepo.save(e);
    }

    public JournalEntry updateEntry(Long id, JournalEntry patch) {
        JournalEntry e = load(id);
        if (e.getStatus() != JournalEntryStatus.DRAFT) throw new BusinessRuleException("Seule une écriture en brouillon est modifiable");
        validateLines(patch.getLines());
        BigDecimal debit = sum(patch.getLines(), true);
        BigDecimal credit = sum(patch.getLines(), false);
        if (debit.compareTo(credit) != 0) throw new BusinessRuleException("Écriture déséquilibrée");
        e.setDate(patch.getDate()); e.setLabel(patch.getLabel());
        e.setLines(patch.getLines()); e.setTotalDebit(debit); e.setTotalCredit(credit);
        return entryRepo.save(e);
    }

    public JournalEntry validate(Long id) {
        JournalEntry e = load(id);
        if (e.getStatus() != JournalEntryStatus.DRAFT) throw new BusinessRuleException("Transition invalide");
        e.setStatus(JournalEntryStatus.VALIDATED);
        return entryRepo.save(e);
    }

    public void deleteEntry(Long id) {
        JournalEntry e = load(id);
        if (e.getStatus() != JournalEntryStatus.DRAFT) throw new BusinessRuleException("Seule une écriture en brouillon est supprimable");
        entryRepo.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<JournalEntry> entries(Long tenantId, Long fiscalYearId) {
        return fiscalYearId != null ? entryRepo.findByTenantIdAndFiscalYearId(tenantId, fiscalYearId) : entryRepo.findByTenantId(tenantId);
    }

    private void validateLines(List<JournalEntryLine> lines) {
        if (lines == null || lines.isEmpty()) throw new BusinessRuleException("Au moins deux lignes sont requises");
        for (JournalEntryLine l : lines) {
            boolean hasDebit = l.getDebit() != null && l.getDebit().signum() > 0;
            boolean hasCredit = l.getCredit() != null && l.getCredit().signum() > 0;
            if (hasDebit == hasCredit) throw new BusinessRuleException("Chaque ligne doit avoir soit un débit, soit un crédit");
            if (l.getChartOfAccountId() == null) throw new BusinessRuleException("Compte requis sur chaque ligne");
        }
    }

    private BigDecimal sum(List<JournalEntryLine> lines, boolean debit) {
        return lines.stream()
                .map(l -> (debit ? l.getDebit() : l.getCredit()) != null ? (debit ? l.getDebit() : l.getCredit()) : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String nextPieceNumber(Long journalId, Long fiscalYearId) {
        long seq = entryRepo.countByJournalIdAndFiscalYearId(journalId, fiscalYearId) + 1;
        String code = journalRepo.findById(journalId).map(Journal::getCode).orElse("OD");
        return String.format("%s-%05d", code, seq);
    }

    private JournalEntry load(Long id) { return entryRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Écriture introuvable : " + id)); }
}
