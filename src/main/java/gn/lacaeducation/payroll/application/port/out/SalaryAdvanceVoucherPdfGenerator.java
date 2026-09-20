package gn.lacaeducation.payroll.application.port.out;

import gn.lacaeducation.payroll.domain.SalaryAdvance;

/** Génère le PDF (A5 sobre, aux couleurs de l'école) du bon d'avance sur salaire remis au salarié. */
public interface SalaryAdvanceVoucherPdfGenerator {
    byte[] generate(SalaryAdvance advance);
}
