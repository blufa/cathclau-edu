package gn.lacaeducation.expenses.application.service;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.expenses.application.port.out.AnnualBudgetRepository;
import gn.lacaeducation.expenses.application.port.out.BudgetLineRepository;
import gn.lacaeducation.expenses.domain.AnnualBudget;
import gn.lacaeducation.expenses.domain.BudgetLine;
import gn.lacaeducation.expenses.domain.BudgetLineType;
import gn.lacaeducation.expenses.domain.BudgetStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Budget annuel de l'école (Manuel de gestion financière §3) : un budget par année regroupant des
 * lignes de recettes et de dépenses, avec suivi prévu vs réalisé et approbation par le conseil.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class AnnualBudgetService {

    private final AnnualBudgetRepository budgetRepo;
    private final BudgetLineRepository lineRepo;

    public record BudgetCommand(Long tenantId, String academicYear, String label, String notes) {}
    public record LineCommand(BudgetLineType lineType, String category, BigDecimal plannedAmount,
                              BigDecimal actualAmount, String notes) {}
    public record BudgetSummary(BigDecimal plannedRevenue, BigDecimal actualRevenue,
                                BigDecimal plannedExpense, BigDecimal actualExpense,
                                BigDecimal plannedBalance, BigDecimal actualBalance) {}

    // ── Budget ──────────────────────────────────────────────────────────────────

    public AnnualBudget create(BudgetCommand c) {
        if (c.academicYear() == null || c.academicYear().isBlank()) throw new BusinessRuleException("L'année est requise");
        if (c.label() == null || c.label().isBlank()) throw new BusinessRuleException("Le libellé est requis");
        return budgetRepo.save(AnnualBudget.builder()
                .tenantId(c.tenantId()).academicYear(c.academicYear()).label(c.label()).notes(c.notes())
                .status(BudgetStatus.DRAFT).createdAt(LocalDateTime.now()).build());
    }

    public AnnualBudget update(Long id, BudgetCommand c) {
        AnnualBudget b = load(id);
        if (b.getStatus() != BudgetStatus.DRAFT && b.getStatus() != BudgetStatus.REJECTED) {
            throw new BusinessRuleException("Seul un budget en brouillon ou rejeté peut être modifié");
        }
        b.setAcademicYear(c.academicYear()); b.setLabel(c.label()); b.setNotes(c.notes());
        return budgetRepo.save(b);
    }

    /** Le comptable soumet son budget de production → SUBMITTED (en attente de validation direction). */
    public AnnualBudget submit(Long id) {
        AnnualBudget b = load(id);
        if (b.getStatus() != BudgetStatus.DRAFT && b.getStatus() != BudgetStatus.REJECTED) {
            throw new BusinessRuleException("Seul un budget en brouillon ou rejeté peut être soumis");
        }
        b.setStatus(BudgetStatus.SUBMITTED); b.setRejectionReason(null);
        return budgetRepo.save(b);
    }

    public AnnualBudget approve(Long id, Long userId) {
        AnnualBudget b = load(id);
        if (b.getStatus() != BudgetStatus.SUBMITTED) throw new BusinessRuleException("Seul un budget soumis peut être approuvé");
        b.setStatus(BudgetStatus.APPROVED); b.setApprovedBy(userId); b.setApprovedAt(LocalDate.now());
        return budgetRepo.save(b);
    }

    /** La direction rejette un budget soumis (avec motif) → REJECTED (le comptable peut corriger et resoumettre). */
    public AnnualBudget reject(Long id, Long userId, String reason) {
        AnnualBudget b = load(id);
        if (b.getStatus() != BudgetStatus.SUBMITTED) throw new BusinessRuleException("Seul un budget soumis peut être rejeté");
        b.setStatus(BudgetStatus.REJECTED); b.setApprovedBy(userId); b.setRejectionReason(reason);
        return budgetRepo.save(b);
    }

    public AnnualBudget close(Long id) {
        AnnualBudget b = load(id);
        b.setStatus(BudgetStatus.CLOSED);
        return budgetRepo.save(b);
    }

    public void delete(Long id) {
        lineRepo.deleteByBudgetId(id);
        budgetRepo.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<AnnualBudget> list(Long tenantId) { return budgetRepo.findByTenantId(tenantId); }

    @Transactional(readOnly = true)
    public AnnualBudget get(Long id) { return load(id); }

    // ── Lignes ──────────────────────────────────────────────────────────────────

    public BudgetLine addLine(Long budgetId, LineCommand c) {
        AnnualBudget b = load(budgetId);
        if (b.getStatus() == BudgetStatus.CLOSED) throw new BusinessRuleException("Budget clôturé : ajout de ligne interdit");
        return lineRepo.save(BudgetLine.builder()
                .tenantId(b.getTenantId()).budgetId(budgetId)
                .lineType(c.lineType() != null ? c.lineType() : BudgetLineType.EXPENSE)
                .category(c.category())
                .plannedAmount(nz(c.plannedAmount())).actualAmount(nz(c.actualAmount())).notes(c.notes())
                .build());
    }

    public BudgetLine updateLine(Long lineId, LineCommand c) {
        BudgetLine l = lineRepo.findById(lineId).orElseThrow(() -> new ResourceNotFoundException("BudgetLine", lineId));
        l.setLineType(c.lineType()); l.setCategory(c.category());
        l.setPlannedAmount(nz(c.plannedAmount())); l.setActualAmount(nz(c.actualAmount())); l.setNotes(c.notes());
        return lineRepo.save(l);
    }

    public void deleteLine(Long lineId) { lineRepo.deleteById(lineId); }

    @Transactional(readOnly = true)
    public List<BudgetLine> lines(Long budgetId) { return lineRepo.findByBudgetId(budgetId); }

    @Transactional(readOnly = true)
    public BudgetSummary summary(Long budgetId) {
        List<BudgetLine> lines = lineRepo.findByBudgetId(budgetId);
        BigDecimal pr = sum(lines, BudgetLineType.REVENUE, true);
        BigDecimal ar = sum(lines, BudgetLineType.REVENUE, false);
        BigDecimal pe = sum(lines, BudgetLineType.EXPENSE, true);
        BigDecimal ae = sum(lines, BudgetLineType.EXPENSE, false);
        return new BudgetSummary(pr, ar, pe, ae, pr.subtract(pe), ar.subtract(ae));
    }

    private BigDecimal sum(List<BudgetLine> lines, BudgetLineType type, boolean planned) {
        return lines.stream().filter(l -> l.getLineType() == type)
                .map(l -> nz(planned ? l.getPlannedAmount() : l.getActualAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal nz(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }
    private AnnualBudget load(Long id) { return budgetRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("AnnualBudget", id)); }
}
