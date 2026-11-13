package gn.lacaeducation.students.adapters.out.pdf;

import gn.lacaeducation.common.TenantInfoProvider;
import gn.lacaeducation.common.TenantPublicInfo;
import gn.lacaeducation.students.application.port.out.DuplicateStudentsPdfGenerator;
import gn.lacaeducation.students.application.service.DuplicateDetectionService.Candidate;
import gn.lacaeducation.students.application.service.DuplicateDetectionService.Group;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Test de fumée (même raisonnement que {@code OpenPdfPaymentStatusListGeneratorTest}) — pas
 * d'assertion sur le contenu textuel du PDF, seulement que le document se génère sans exception,
 * y compris avec le prénom/téléphone du parent null (§retour comptable : "il faut mettre le
 * prénom, nom, numéro du parent" sur la fiche imprimée).
 */
@ExtendWith(MockitoExtension.class)
class OpenPdfDuplicateStudentsListGeneratorTest {

    @Mock TenantInfoProvider tenantInfoProvider;

    private DuplicateStudentsPdfGenerator generator() {
        return new OpenPdfDuplicateStudentsListGenerator(tenantInfoProvider);
    }

    @Test
    void generate_emptyList_stillProducesValidPdf() {
        byte[] pdf = generator().generate(10L, List.of());
        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
    }

    @Test
    void generate_groupWithParentDetails_producesValidPdf() {
        when(tenantInfoProvider.findTenantInfo(10L)).thenReturn(Optional.of(
                new TenantPublicInfo(10L, "École Test", null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null, null, null, null, null, null, false)));

        Group group = new Group("Amadou", "Diallo", "Diallo", List.of(
                new Candidate(10L, "Amadou", "Diallo", null, 5L, "CM2", "Mamadou", "+224600000000",
                        new BigDecimal("1800000"), new BigDecimal("1200000"), 9, false),
                // Téléphone/prénom parent null (donnée manquante) : ne doit jamais faire échouer la génération.
                new Candidate(11L, "Amadou", "Diallo", null, 5L, "CM2", null, null,
                        BigDecimal.ZERO, BigDecimal.ZERO, 0, true)));

        byte[] pdf = generator().generate(10L, List.of(group));

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
    }
}
