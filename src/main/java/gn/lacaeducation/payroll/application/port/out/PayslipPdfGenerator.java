package gn.lacaeducation.payroll.application.port.out;

import gn.lacaeducation.payroll.domain.Payslip;
import gn.lacaeducation.payroll.domain.PayslipLine;

import java.util.List;

/** Génère le PDF (A5 stylé, aux couleurs de l'école) d'une fiche de paie. */
public interface PayslipPdfGenerator {
    /**
     * Heures du mois pour un enseignant : dues (projection de l'emploi du temps), réalisées (cahier
     * de textes, séances marquées « Réalisée ») et non réalisées (dues − réalisées, jamais négatif).
     * lateMinutes/earlyMinutes = retard cumulé à l'entrée / départ anticipé cumulé à la sortie des
     * séances pointées. effectiveMinutes = réalisées − retard − départ anticipé (temps réellement en
     * classe, la base du calcul de la paie horaire — distinct de missingMinutes, l'écart avec le
     * théorique). {@code null} pour le personnel non-enseignant ou si la période n'est pas connue.
     */
    record TeacherHours(int dueMinutes, int realizedMinutes, int missingMinutes, int lateMinutes,
                        int earlyMinutes, int effectiveMinutes) {}

    byte[] generate(Payslip payslip, List<PayslipLine> lines, TeacherHours hours);
}
