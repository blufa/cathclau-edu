package gn.lacaeducation.communication.adapters.out.pdf;

import gn.lacaeducation.common.TenantInfoProvider;
import gn.lacaeducation.common.TenantPublicInfo;
import gn.lacaeducation.communication.application.port.out.PaymentStatusListPdfGenerator;
import gn.lacaeducation.communication.application.service.PaymentReminderCandidatesService.Candidate;
import gn.lacaeducation.communication.application.service.PaymentReminderCandidatesService.Status;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Test de fumée (même raisonnement que {@code OpenPdfReceiptGeneratorTest}) — pas d'assertion sur le
 * contenu textuel du PDF (aucune bibliothèque d'extraction de texte dans le projet), seulement que le
 * document se génère sans exception avec une liste vide, 1 élève, et plusieurs statuts mélangés.
 */
@ExtendWith(MockitoExtension.class)
class OpenPdfPaymentStatusListGeneratorTest {

    @Mock TenantInfoProvider tenantInfoProvider;

    private PaymentStatusListPdfGenerator generator() {
        return new OpenPdfPaymentStatusListGenerator(tenantInfoProvider);
    }

    @Test
    void generate_emptyList_stillProducesValidPdf() {
        byte[] pdf = generator().generate(10L, List.of());
        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
    }

    @Test
    void generate_mixedStatuses_producesValidPdf() {
        when(tenantInfoProvider.findTenantInfo(10L)).thenReturn(Optional.of(
                new TenantPublicInfo(10L, "École Test", null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null, null, null, null, null, null, false)));

        List<Candidate> candidates = List.of(
                new Candidate(7L, "Mamadou Camara", 3L, "CM2 A", Status.OVERDUE,
                        new BigDecimal("50000"), LocalDate.now().minusDays(5), 5, 1, 3, List.of()),
                new Candidate(8L, "Fatou Sylla", 3L, "CM2 A", Status.UPCOMING,
                        new BigDecimal("50000"), LocalDate.now().plusDays(3), -3, 1, 3, List.of()),
                new Candidate(9L, "Ibrahima Diallo", 4L, "6ème A", Status.UP_TO_DATE,
                        BigDecimal.ZERO, null, 0, 2, 2, List.of())
        );

        byte[] pdf = generator().generate(10L, candidates);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
    }
}
