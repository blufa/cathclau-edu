package gn.lacaeducation.payroll.application.port.out;

import gn.lacaeducation.payroll.domain.SalaryAdvance;

/** Génère le PDF (A5 sobre, aux couleurs de l'école) du bon d'avance sur salaire remis au salarié. */
public interface SalaryAdvanceVoucherPdfGenerator {
    /** @param accountantName nom affiché sous "Signature et Cachet de l'Établissement" — l'agent ayant
     *                        accordé l'avance ({@code advance.getGrantedByUserId()} résolu en nom),
     *                        {@code null} si non résolu. */
    byte[] generate(SalaryAdvance advance, String accountantName);
}
