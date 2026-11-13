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
                new StudentFinancialRow(10L, "Amadou", "Diallo", null, 5L, "CM2", "Diallo", null, null,
                        new BigDecimal("1800000"), new BigDecimal("1200000"), 9),
                new StudentFinancialRow(11L, "Amadou", "Diallo", null, 5L, "CM2", "Diallo", null, null,
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
                new StudentFinancialRow(20L, "Fatoumata", "Camara", null, 6L, "6ème A", "Camara", null, null,
                        new BigDecimal("1800000"), new BigDecimal("900000"), 9),
                new StudentFinancialRow(21L, "Fatoumata", "Camara", null, 6L, "6ème A", "Camara", null, null,
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
                new StudentFinancialRow(30L, "Ibrahima", "Barry", null, 7L, "CP1", "Barry", null, null,
                        BigDecimal.ZERO, BigDecimal.ZERO, 0),
                new StudentFinancialRow(31L, "Ibrahima", "Barry", null, 7L, "CP1", "Barry", null, null,
                        BigDecimal.ZERO, BigDecimal.ZERO, 0)));

        List<DuplicateDetectionService.Group> groups = service().findDuplicateGroups(1L);

        assertThat(groups.get(0).members()).extracting(DuplicateDetectionService.Candidate::suspect)
                .containsExactly(false, false);
    }

    @Test
    void findDuplicateGroups_groupsAcrossDifferentClasses_whenSameParentLastName() {
        // Requête élargie (§retour comptable) : plus limité à la même classe, tant que le nom de
        // famille d'un parent correspond aussi.
        when(port.findDuplicateCandidates(1L)).thenReturn(List.of(
                new StudentFinancialRow(40L, "Mamadou", "Bah", null, 5L, "CM2", "Bah", null, null,
                        BigDecimal.ZERO, BigDecimal.ZERO, 0),
                new StudentFinancialRow(41L, "Mamadou", "Bah", null, 6L, "6ème A", "Bah", null, null,
                        BigDecimal.ZERO, BigDecimal.ZERO, 0)));

        List<DuplicateDetectionService.Group> groups = service().findDuplicateGroups(1L);

        assertThat(groups).hasSize(1);
        assertThat(groups.get(0).members()).hasSize(2);
    }

    @Test
    void findDuplicateGroups_neverGroups_whenParentLastNameDiffers() {
        // Même prénom/nom élève mais parents de noms différents : jamais un doublon (le nouveau
        // critère exige aussi le même nom de famille parent).
        when(port.findDuplicateCandidates(1L)).thenReturn(List.of(
                new StudentFinancialRow(50L, "Mariam", "Diallo", null, 5L, "CM2", "Diallo", null, null,
                        BigDecimal.ZERO, BigDecimal.ZERO, 0),
                new StudentFinancialRow(51L, "Mariam", "Diallo", null, 5L, "CM2", "Keita", null, null,
                        BigDecimal.ZERO, BigDecimal.ZERO, 0)));

        List<DuplicateDetectionService.Group> groups = service().findDuplicateGroups(1L);

        assertThat(groups).hasSize(2);
        assertThat(groups).allSatisfy(g -> assertThat(g.members()).hasSize(1));
    }
}
