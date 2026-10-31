package gn.lacaeducation.students.application.service;

import gn.lacaeducation.students.application.port.out.DuplicateDetectionPort;
import gn.lacaeducation.students.application.port.out.DuplicateDetectionPort.StudentFinancialRow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DuplicateDetectionServiceTest {

    @Mock DuplicateDetectionPort port;

    private DuplicateDetectionService service() {
        return new DuplicateDetectionService(port);
    }

    @Test
    void findDuplicateGroups_oneMemberWithFewerMonthlyInvoices_isMarkedSuspect() {
        // Reproduit le cas type : une fiche "vide" créée par erreur à côté de la vraie fiche facturée.
        when(port.findDuplicateCandidates(1L)).thenReturn(List.of(
                new StudentFinancialRow(10L, "Amadou", "Diallo", null, 5L, "CM2",
                        new BigDecimal("1800000"), new BigDecimal("1200000"), 9),
                new StudentFinancialRow(11L, "Amadou", "Diallo", null, 5L, "CM2",
                        BigDecimal.ZERO, BigDecimal.ZERO, 0)));

        List<DuplicateDetectionService.Group> groups = service().findDuplicateGroups(1L);

        assertThat(groups).hasSize(1);
        DuplicateDetectionService.Group g = groups.get(0);
        assertThat(g.members()).hasSize(2);
        assertThat(g.members()).filteredOn(c -> c.studentId().equals(10L)).extracting(DuplicateDetectionService.Candidate::suspect)
                .containsExactly(false);
        assertThat(g.members()).filteredOn(c -> c.studentId().equals(11L)).extracting(DuplicateDetectionService.Candidate::suspect)
                .containsExactly(true);
    }

    @Test
    void findDuplicateGroups_bothMembersFullyBilled_neitherMarkedSuspect() {
        // Homonymie réelle (ex. jumeaux) : chacun a son propre historique complet de factures —
        // jamais marqué suspect sur le seul nom (§retour comptable : "sans la même dans la même
        // classe").
        when(port.findDuplicateCandidates(1L)).thenReturn(List.of(
                new StudentFinancialRow(20L, "Fatoumata", "Camara", null, 6L, "6ème A",
                        new BigDecimal("1800000"), new BigDecimal("900000"), 9),
                new StudentFinancialRow(21L, "Fatoumata", "Camara", null, 6L, "6ème A",
                        new BigDecimal("1800000"), new BigDecimal("1800000"), 9)));

        List<DuplicateDetectionService.Group> groups = service().findDuplicateGroups(1L);

        assertThat(groups.get(0).members()).extracting(DuplicateDetectionService.Candidate::suspect)
                .containsExactly(false, false);
    }

    @Test
    void findDuplicateGroups_neitherHasAnyMonthlyInvoice_noneMarkedSuspectForLackOfSignal() {
        // Aucune mensualité facturée pour personne dans le groupe (ex. frais annuel unique, pas de
        // tranches) — aucune preuve financière à comparer, on ne marque jamais suspect sans preuve.
        when(port.findDuplicateCandidates(1L)).thenReturn(List.of(
                new StudentFinancialRow(30L, "Ibrahima", "Barry", null, 7L, "CP1",
                        BigDecimal.ZERO, BigDecimal.ZERO, 0),
                new StudentFinancialRow(31L, "Ibrahima", "Barry", null, 7L, "CP1",
                        BigDecimal.ZERO, BigDecimal.ZERO, 0)));

        List<DuplicateDetectionService.Group> groups = service().findDuplicateGroups(1L);

        assertThat(groups.get(0).members()).extracting(DuplicateDetectionService.Candidate::suspect)
                .containsExactly(false, false);
    }
}
