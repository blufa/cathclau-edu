package gn.lacaeducation.grading.adapters.out.pdf;

import gn.lacaeducation.common.TenantInfoProvider;
import gn.lacaeducation.common.TenantPublicInfo;
import gn.lacaeducation.grading.application.port.out.MonthlyReportCardPdfGenerator;
import gn.lacaeducation.grading.application.port.out.MonthlyReportCardPdfGenerator.Line;
import gn.lacaeducation.grading.application.port.out.MonthlyReportCardPdfGenerator.MonthlyReportCardDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Test de fumée (même raisonnement que {@code OpenPdfPaymentStatusListGeneratorTest}) — vérifie
 * surtout que la génération ne plante jamais, y compris avec beaucoup de matières (hauteur du
 * diagramme calculée dynamiquement, §retour utilisateur : "il faut le contenir sur une seule page")
 * et sans aucune matière (diagramme ignoré).
 */
@ExtendWith(MockitoExtension.class)
class OpenPdfMonthlyReportCardGeneratorTest {

    @Mock TenantInfoProvider tenantInfoProvider;

    private MonthlyReportCardPdfGenerator generator() {
        return new OpenPdfMonthlyReportCardGenerator(tenantInfoProvider);
    }

    @Test
    void generate_noSubjects_stillProducesValidPdf() {
        MonthlyReportCardDocument d = new MonthlyReportCardDocument(10L, "Amadou Diallo", "MAT-001",
                "CM2", "Octobre 2026", null, null, null, List.of(), null, null);

        byte[] pdf = generator().generate(d);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        assertThat(pageCount(pdf)).isEqualTo(1);
    }

    @Test
    void generate_manySubjectsWithAppreciationAndComment_stillProducesValidPdf() {
        when(tenantInfoProvider.findTenantInfo(10L)).thenReturn(Optional.of(
                new TenantPublicInfo(10L, "École Test", null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null, null, null, null, null, null, false)));

        // 14 matières : au-delà de la plupart des curricula réels (secondaire guinéen), choisi
        // volontairement haut pour prouver que le document tient sur une seule page même dans ce cas.
        List<Line> lines = new ArrayList<>();
        for (int i = 0; i < 14; i++) {
            lines.add(new Line("Matière " + i, new BigDecimal("30"), new BigDecimal("3"), "Prof " + i));
        }
        MonthlyReportCardDocument d = new MonthlyReportCardDocument(10L, "Fatoumata Camara", "MAT-002",
                "9ème", "Novembre 2026", new BigDecimal("12.50"), 3, 28, lines,
                "Bon niveau général.", "Attention à la discipline en classe.");

        byte[] pdf = generator().generate(d);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        // §retour utilisateur : "il faut le contenir sur une seule page" — même avec 14 matières,
        // la hauteur du diagramme en bâtonnets se réduit dynamiquement pour ne jamais déborder.
        assertThat(pageCount(pdf)).isEqualTo(1);
    }

    private static int pageCount(byte[] pdf) {
        try {
            com.lowagie.text.pdf.PdfReader reader = new com.lowagie.text.pdf.PdfReader(pdf);
            int n = reader.getNumberOfPages();
            reader.close();
            return n;
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }
}
