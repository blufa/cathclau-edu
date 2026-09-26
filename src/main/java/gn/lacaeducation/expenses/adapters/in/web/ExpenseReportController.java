package gn.lacaeducation.expenses.adapters.in.web;

import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.expenses.application.service.MonthlyExpenseSheetService;
import gn.lacaeducation.expenses.domain.Expense;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Lecture transversale des dépenses, tous mois confondus — pour les tableaux de bord agrégés qui
 * n'ont pas de notion de fiche mensuelle (ex. dashboard fondateur). Écriture : voir
 * {@link MonthlyExpenseSheetController} (toujours rattachée à une fiche mensuelle précise). */
@Tag(name = "Expenses (report)", description = "Lecture agrégée des dépenses, tous mois confondus")
@RestController
@RequestMapping("/api/v1/expenses")
@RequiredArgsConstructor
class ExpenseReportController {

    private final MonthlyExpenseSheetService service;

    @Operation(summary = "Lister toutes les lignes de dépense du tenant courant, tous mois confondus")
    @GetMapping
    public List<Expense> list() { return service.findExpensesByTenant(TenantContext.get()); }
}
