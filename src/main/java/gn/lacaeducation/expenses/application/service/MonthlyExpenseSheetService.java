package gn.lacaeducation.expenses.application.service;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.expenses.application.event.ExpensePaidEvent;
import gn.lacaeducation.expenses.application.port.out.ExpenseRepository;
import gn.lacaeducation.expenses.application.port.out.MonthlyExpenseSheetRepository;
import gn.lacaeducation.expenses.application.port.out.RevenueLineRepository;
import gn.lacaeducation.expenses.domain.Expense;
import gn.lacaeducation.expenses.domain.FinancingMode;
import gn.lacaeducation.expenses.domain.MonthlyExpenseSheet;
import gn.lacaeducation.expenses.domain.RevenueLine;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Fiches de dépense mensuelles (§retour comptable, point 31) : remplace le budget annuel prévisionnel
 * par une saisie directe des dépenses/recettes réellement engagées, avec clôture qui verrouille le
 * mois. Aucun circuit d'approbation — chaque ligne ajoutée est immédiatement considérée comme dépensée
 * et alimente la trésorerie (voir {@link #addExpense}).
 */
@Service
@Transactional
@RequiredArgsConstructor
public class MonthlyExpenseSheetService {

    private final MonthlyExpenseSheetRepository sheetRepo;
    private final ExpenseRepository expenseRepo;
    private final RevenueLineRepository revenueRepo;
    private final ApplicationEventPublisher eventPublisher;

    public record SheetCommand(Long tenantId, String label, String academicYear,
                               FinancingMode financingMode, BigDecimal financingAmount) {}
    public record ExpenseLineCommand(String label, BigDecimal amount, boolean necessary, LocalDate expenseDate) {}
    public record RevenueLineCommand(String label, BigDecimal amount, LocalDate revenueDate) {}
    public record SheetDetail(MonthlyExpenseSheet sheet, List<Expense> expenses, List<RevenueLine> revenues,
                              BigDecimal totalExpenses, BigDecimal totalRevenues) {}

    // ── Fiches ──────────────────────────────────────────────────────────────────

    public MonthlyExpenseSheet create(SheetCommand c, Long createdBy) {
        if (c.label() == null || c.label().isBlank()) throw new BusinessRuleException("Le libellé est requis");
        if (c.academicYear() == null || c.academicYear().isBlank()) throw new BusinessRuleException("L'année scolaire est requise");
        return sheetRepo.save(MonthlyExpenseSheet.builder()
                .tenantId(c.tenantId()).label(c.label()).academicYear(c.academicYear())
                .financingMode(c.financingMode() != null ? c.financingMode() : FinancingMode.NONE)
                .financingAmount(c.financingAmount())
                .createdBy(createdBy).createdAt(LocalDateTime.now()).build());
    }

    public MonthlyExpenseSheet close(Long id) {
        MonthlyExpenseSheet s = load(id);
        if (s.isClosed()) throw new BusinessRuleException("Cette fiche est déjà clôturée");
        s.setClosedAt(LocalDateTime.now());
        return sheetRepo.save(s);
    }

    @Transactional(readOnly = true)
    public List<MonthlyExpenseSheet> list(Long tenantId) { return sheetRepo.findByTenantId(tenantId); }

    /** Toutes les lignes de dépense du tenant, tous mois confondus — pour les tableaux de bord
     * agrégés (dashboard fondateur), qui totalisent par mois sans notion de fiche mensuelle. */
    @Transactional(readOnly = true)
    public List<Expense> findExpensesByTenant(Long tenantId) { return expenseRepo.findByTenantId(tenantId); }

    @Transactional(readOnly = true)
    public SheetDetail detail(Long id) {
        MonthlyExpenseSheet s = load(id);
        List<Expense> expenses = expenseRepo.findBySheetId(id);
        List<RevenueLine> revenues = revenueRepo.findBySheetId(id);
        BigDecimal totalExpenses = expenses.stream().map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRevenues = revenues.stream().map(RevenueLine::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new SheetDetail(s, expenses, revenues, totalExpenses, totalRevenues);
    }

    // ── Lignes de dépense ───────────────────────────────────────────────────────

    /** Chaque ligne ajoutée correspond à un montant déjà dépensé (pas de circuit d'approbation
     * séparé) — publie immédiatement l'événement qui alimente la trésorerie. */
    public Expense addExpense(Long sheetId, ExpenseLineCommand c) {
        MonthlyExpenseSheet s = requireOpen(sheetId);
        if (c.label() == null || c.label().isBlank()) throw new BusinessRuleException("Le libellé est requis");
        if (c.amount() == null || c.amount().signum() <= 0) throw new BusinessRuleException("Le montant doit être positif");
        Expense saved = expenseRepo.save(Expense.builder()
                .tenantId(s.getTenantId()).sheetId(sheetId).label(c.label()).amount(c.amount())
                .necessary(c.necessary()).expenseDate(c.expenseDate() != null ? c.expenseDate() : LocalDate.now())
                .createdAt(LocalDateTime.now()).build());
        eventPublisher.publishEvent(new ExpensePaidEvent(saved.getTenantId(), saved.getId(), saved.getAmount()));
        return saved;
    }

    public Expense updateExpense(Long lineId, ExpenseLineCommand c) {
        Expense e = expenseRepo.findById(lineId).orElseThrow(() -> new ResourceNotFoundException("Ligne de dépense introuvable : " + lineId));
        requireOpen(e.getSheetId());
        e.setLabel(c.label()); e.setAmount(c.amount()); e.setNecessary(c.necessary());
        if (c.expenseDate() != null) e.setExpenseDate(c.expenseDate());
        return expenseRepo.save(e);
    }

    public void deleteExpense(Long lineId) {
        Expense e = expenseRepo.findById(lineId).orElseThrow(() -> new ResourceNotFoundException("Ligne de dépense introuvable : " + lineId));
        requireOpen(e.getSheetId());
        expenseRepo.deleteById(lineId);
    }

    // ── Lignes de recette ───────────────────────────────────────────────────────

    public RevenueLine addRevenue(Long sheetId, RevenueLineCommand c) {
        MonthlyExpenseSheet s = requireOpen(sheetId);
        if (c.label() == null || c.label().isBlank()) throw new BusinessRuleException("Le libellé est requis");
        if (c.amount() == null || c.amount().signum() <= 0) throw new BusinessRuleException("Le montant doit être positif");
        return revenueRepo.save(RevenueLine.builder()
                .tenantId(s.getTenantId()).sheetId(sheetId).label(c.label()).amount(c.amount())
                .revenueDate(c.revenueDate() != null ? c.revenueDate() : LocalDate.now())
                .createdAt(LocalDateTime.now()).build());
    }

    public RevenueLine updateRevenue(Long lineId, RevenueLineCommand c) {
        RevenueLine r = revenueRepo.findById(lineId).orElseThrow(() -> new ResourceNotFoundException("Ligne de recette introuvable : " + lineId));
        requireOpen(r.getSheetId());
        r.setLabel(c.label()); r.setAmount(c.amount());
        if (c.revenueDate() != null) r.setRevenueDate(c.revenueDate());
        return revenueRepo.save(r);
    }

    public void deleteRevenue(Long lineId) {
        RevenueLine r = revenueRepo.findById(lineId).orElseThrow(() -> new ResourceNotFoundException("Ligne de recette introuvable : " + lineId));
        requireOpen(r.getSheetId());
        revenueRepo.deleteById(lineId);
    }

    private MonthlyExpenseSheet requireOpen(Long sheetId) {
        MonthlyExpenseSheet s = load(sheetId);
        if (s.isClosed()) throw new BusinessRuleException("Cette fiche est clôturée : ajout/modification impossible");
        return s;
    }

    private MonthlyExpenseSheet load(Long id) {
        return sheetRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Fiche de dépense introuvable : " + id));
    }
}
