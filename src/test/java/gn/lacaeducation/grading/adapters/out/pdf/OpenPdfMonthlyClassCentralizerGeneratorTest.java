package gn.lacaeducation.grading.adapters.out.pdf;

import gn.lacaeducation.common.TenantInfoProvider;
import gn.lacaeducation.common.TenantPublicInfo;
import gn.lacaeducation.grading.application.port.out.ClassCentralizerPdfGenerator.SubjectColumn;
import gn.lacaeducation.grading.application.port.out.MonthlyClassCentralizerPdfGenerator;
import gn.lacaeducation.grading.application.port.out.MonthlyClassCentralizerPdfGenerator.MonthlyCentralizer;
import gn.lacaeducation.grading.application.port.out.MonthlyClassCentralizerPdfGenerator.MonthlyStudentBlock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Test de fumée (même raisonnement que {@code OpenPdfMonthlyReportCardGeneratorTest}) : vérifie que
 * la génération ne plante jamais, y compris sans aucun bulletin pour la classe/le mois (table vide)
 * et sans tenant connu (en-tête dégradé).
 */
@ExtendWith(MockitoExtension.class)
class OpenPdfMonthlyClassCentralizerGeneratorTest {

    @Mock TenantInfoProvider tenantInfoProvider;

    private MonthlyClassCentralizerPdfGenerator generator() {
        return new OpenPdfMonthlyClassCentralizerGenerator(tenantInfoProvider);
    }

    @Test
    void generate_noRows_stillProducesValidPdf() {
        MonthlyCentralizer data = new MonthlyCentralizer(10L, "6ème A", "Collège", "2026-2027",
                "Novembre 2026", List.of(new SubjectColumn(1L, "Mathématiques", BigDecimal.valueOf(3))), List.of());

        byte[] pdf = generator().generate(data);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
    }

    @Test
    void generate_withRowsAndTenantBranding_stillProducesValidPdf() {
        when(tenantInfoProvider.findTenantInfo(10L)).thenReturn(Optional.of(
                new TenantPublicInfo(10L, "École Test", null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null, null, null, null, null, null, false)));

        List<SubjectColumn> subjects = List.of(
                new SubjectColumn(1L, "Mathématiques", BigDecimal.valueOf(3)),
                new SubjectColumn(2L, "Français", BigDecimal.valueOf(4)));
        List<MonthlyStudentBlock> rows = List.of(
                new MonthlyStudentBlock(10L, "MAT-001", "BAH Awa", "Collège",
                        Map.of(1L, BigDecimal.valueOf(16), 2L, BigDecimal.valueOf(14)),
                        BigDecimal.valueOf(14.85), 1),
                new MonthlyStudentBlock(20L, "MAT-002", "CAMARA Moussa", "Collège",
                        Map.of(1L, BigDecimal.valueOf(10)),
                        BigDecimal.valueOf(10.00), 2));
        MonthlyCentralizer data = new MonthlyCentralizer(10L, "6ème A", "Collège", "2026-2027",
                "Novembre 2026", subjects, rows);

        byte[] pdf = generator().generate(data);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
    }
}
