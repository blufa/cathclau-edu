package gn.lacaeducation.accounting.application.service;

import gn.lacaeducation.accounting.application.port.out.ChartOfAccountRepository;
import gn.lacaeducation.accounting.application.port.out.JournalEntryRepository;
import gn.lacaeducation.accounting.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/** SYSCOHADA statements computed from VALIDATED journal entries (no data duplicated). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountingReportService {

    private final JournalEntryRepository entryRepo;
    private final ChartOfAccountRepository accountRepo;

    public record BalanceRow(Long accountId, String accountNumber, String label, BigDecimal debit, BigDecimal credit, BigDecimal balance) {}
    public record LedgerRow(LocalDate date, String pieceNumber, String label, BigDecimal debit, BigDecimal credit, BigDecimal runningBalance) {}
    public record IncomeStatement(BigDecimal charges, BigDecimal produits, BigDecimal resultatNet) {}
    public record BalanceSheet(BigDecimal actif, BigDecimal passif, BigDecimal resultatNet) {}

    public List<BalanceRow> balance(Long tenantId, Long fiscalYearId) {
        Map<Long, ChartOfAccount> accounts = accountsById(tenantId);
        Map<Long, BigDecimal[]> totals = new LinkedHashMap<>(); // [debit, credit]
        for (JournalEntry e : validatedEntries(tenantId, fiscalYearId)) {
            for (JournalEntryLine l : e.getLines()) {
                BigDecimal[] dc = totals.computeIfAbsent(l.getChartOfAccountId(), k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                dc[0] = dc[0].add(nz(l.getDebit()));
                dc[1] = dc[1].add(nz(l.getCredit()));
            }
        }
        List<BalanceRow> rows = new ArrayList<>();
        for (var entry : totals.entrySet()) {
            ChartOfAccount a = accounts.get(entry.getKey());
            BigDecimal d = entry.getValue()[0], c = entry.getValue()[1];
            rows.add(new BalanceRow(entry.getKey(), a != null ? a.getAccountNumber() : "?",
                    a != null ? a.getLabel() : "?", d, c, d.subtract(c)));
        }
        rows.sort(Comparator.comparing(BalanceRow::accountNumber));
        return rows;
    }

    public List<LedgerRow> generalLedger(Long tenantId, Long fiscalYearId, Long accountId) {
        List<LedgerRow> rows = new ArrayList<>();
        BigDecimal running = BigDecimal.ZERO;
        List<JournalEntry> entries = new ArrayList<>(validatedEntries(tenantId, fiscalYearId));
        entries.sort(Comparator.comparing(JournalEntry::getDate, Comparator.nullsLast(Comparator.naturalOrder())));
        for (JournalEntry e : entries) {
            for (JournalEntryLine l : e.getLines()) {
                if (!Objects.equals(l.getChartOfAccountId(), accountId)) continue;
                running = running.add(nz(l.getDebit())).subtract(nz(l.getCredit()));
                rows.add(new LedgerRow(e.getDate(), e.getPieceNumber(), l.getLabel() != null ? l.getLabel() : e.getLabel(),
                        nz(l.getDebit()), nz(l.getCredit()), running));
            }
        }
        return rows;
    }

    public IncomeStatement incomeStatement(Long tenantId, Long fiscalYearId) {
        Map<Long, ChartOfAccount> accounts = accountsById(tenantId);
        BigDecimal charges = BigDecimal.ZERO, produits = BigDecimal.ZERO;
        for (JournalEntry e : validatedEntries(tenantId, fiscalYearId)) {
            for (JournalEntryLine l : e.getLines()) {
                ChartOfAccount a = accounts.get(l.getChartOfAccountId());
                if (a == null || a.getType() == null) continue;
                if (a.getType() == AccountType.EXPENSE) charges = charges.add(nz(l.getDebit())).subtract(nz(l.getCredit()));
                else if (a.getType() == AccountType.REVENUE) produits = produits.add(nz(l.getCredit())).subtract(nz(l.getDebit()));
            }
        }
        return new IncomeStatement(charges, produits, produits.subtract(charges));
    }

    public BalanceSheet balanceSheet(Long tenantId, Long fiscalYearId) {
        Map<Long, ChartOfAccount> accounts = accountsById(tenantId);
        BigDecimal actif = BigDecimal.ZERO, passif = BigDecimal.ZERO;
        for (JournalEntry e : validatedEntries(tenantId, fiscalYearId)) {
            for (JournalEntryLine l : e.getLines()) {
                ChartOfAccount a = accounts.get(l.getChartOfAccountId());
                if (a == null || a.getType() == null) continue;
                if (a.getType() == AccountType.ASSET) actif = actif.add(nz(l.getDebit())).subtract(nz(l.getCredit()));
                else if (a.getType() == AccountType.LIABILITY) passif = passif.add(nz(l.getCredit())).subtract(nz(l.getDebit()));
            }
        }
        IncomeStatement is = incomeStatement(tenantId, fiscalYearId);
        return new BalanceSheet(actif, passif, is.resultatNet());
    }

    private List<JournalEntry> validatedEntries(Long tenantId, Long fiscalYearId) {
        return entries(tenantId, fiscalYearId).stream().filter(e -> e.getStatus() == JournalEntryStatus.VALIDATED).toList();
    }
    private List<JournalEntry> entries(Long tenantId, Long fiscalYearId) {
        return fiscalYearId != null ? entryRepo.findByTenantIdAndFiscalYearId(tenantId, fiscalYearId) : entryRepo.findByTenantId(tenantId);
    }
    private Map<Long, ChartOfAccount> accountsById(Long tenantId) {
        Map<Long, ChartOfAccount> m = new HashMap<>();
        for (ChartOfAccount a : accountRepo.findByTenantId(tenantId)) m.put(a.getId(), a);
        return m;
    }
    private static BigDecimal nz(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }
}
