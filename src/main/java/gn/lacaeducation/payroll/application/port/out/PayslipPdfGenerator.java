package gn.lacaeducation.payroll.application.port.out;

import gn.lacaeducation.payroll.domain.Payslip;
import gn.lacaeducation.payroll.domain.PayslipLine;

import java.util.List;

/** Génère le PDF (A5 stylé, aux couleurs de l'école) d'une fiche de paie. */
public interface PayslipPdfGenerator {
    byte[] generate(Payslip payslip, List<PayslipLine> lines);
}
