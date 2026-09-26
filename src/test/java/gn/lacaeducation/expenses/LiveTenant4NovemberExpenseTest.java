package gn.lacaeducation.expenses;

import gn.lacaeducation.expenses.application.service.MonthlyExpenseSheetService;
import gn.lacaeducation.expenses.application.service.MonthlyExpenseSheetService.ExpenseLineCommand;
import gn.lacaeducation.expenses.application.service.MonthlyExpenseSheetService.SheetCommand;
import gn.lacaeducation.expenses.domain.Expense;
import gn.lacaeducation.expenses.domain.FinancingMode;
import gn.lacaeducation.expenses.domain.MonthlyExpenseSheet;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Test réel (données live, tenant 4 "Groupe Scolaire Privé Notre Mère") pour vérifier de bout en
 * bout la fiche de dépense mensuelle de Novembre 2026 sur le dashboard Vision Fondateur — voir
 * demande utilisateur du 2026-09-25 : créer une dépense de Novembre, ajouter des lignes, puis
 * constater dans l'UI que "Dépenses totales" grimpe (cumulatif, ne se réinitialise jamais) alors que
 * "Bénéfice du mois" n'est affecté que pour le mois de Novembre. Conservé volontairement (fiche NON
 * clôturée) pour revue par l'utilisateur dans l'UI — ne pas supprimer, mais {@code @Disabled} :
 * il se connecte à la vraie base locale `ecole_local`, absente sur tout runner CI/pipeline (ce qui
 * faisait échouer le build), et rejouer ce test recréerait une nouvelle fiche "Dépense Novembre" en
 * double à chaque exécution. À réactiver ponctuellement à la main (retirer l'annotation) pour un
 * nouveau test manuel similaire, jamais laisser actif en continu.
 */
@SpringBootTest
@Disabled("Test manuel contre la base réelle ecole_local — ne doit jamais tourner en CI")
class LiveTenant4NovemberExpenseTest {

    @Autowired MonthlyExpenseSheetService service;

    @Test
    void createNovemberExpenseSheetWithLines() {
        MonthlyExpenseSheet sheet = service.create(
                new SheetCommand(4L, "Dépense Novembre", "2026-2027", FinancingMode.NONE, null), 45L);
        System.out.println("Fiche créée id=" + sheet.getId());

        Expense e1 = service.addExpense(sheet.getId(), new ExpenseLineCommand(
                "Fournitures scolaires", new BigDecimal("150000"), true, LocalDate.of(2026, 11, 5)));
        Expense e2 = service.addExpense(sheet.getId(), new ExpenseLineCommand(
                "Réparation électricité", new BigDecimal("80000"), true, LocalDate.of(2026, 11, 12)));
        Expense e3 = service.addExpense(sheet.getId(), new ExpenseLineCommand(
                "Carburant groupe électrogène", new BigDecimal("45000"), false, LocalDate.of(2026, 11, 20)));

        System.out.println("Ligne 1 id=" + e1.getId() + " montant=" + e1.getAmount());
        System.out.println("Ligne 2 id=" + e2.getId() + " montant=" + e2.getAmount());
        System.out.println("Ligne 3 id=" + e3.getId() + " montant=" + e3.getAmount());
        System.out.println("Total lignes Novembre = 275000");
    }
}
