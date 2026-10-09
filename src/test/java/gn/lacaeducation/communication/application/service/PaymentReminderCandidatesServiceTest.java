package gn.lacaeducation.communication.application.service;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.academics.ClassPublicInfo;
import gn.lacaeducation.communication.application.port.out.PaymentReminderDataPort;
import gn.lacaeducation.communication.application.port.out.PaymentReminderDataPort.ClassFeeLine;
import gn.lacaeducation.students.ParentPublicInfo;
import gn.lacaeducation.students.StudentDataProvider;
import gn.lacaeducation.students.StudentPublicInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentReminderCandidatesServiceTest {

    @Mock PaymentReminderDataPort dataPort;
    @Mock StudentDataProvider studentDataProvider;
    @Mock AcademicsDataProvider academicsDataProvider;
    @InjectMocks PaymentReminderCandidatesService service;

    private static StudentPublicInfo student(long id, String first, String last) {
        return new StudentPublicInfo(id, 1L, first, last, null, null, "MAT-" + id, null, null, "MALE");
    }

    @Test
    void listByClass_classifiesOverdueUpcomingAndUpToDate_withTranchesPaid() {
        when(academicsDataProvider.findClass(3L)).thenReturn(Optional.of(
                new ClassPublicInfo(3L, "CM2 A", "CM2", "NORMALE", null, null, "PRIMAIRE")));
        when(dataPort.countMonthlyTranches(1L, 3L)).thenReturn(3);
        when(studentDataProvider.findStudentsByClass(3L)).thenReturn(List.of(
                student(7L, "Mamadou", "Camara"),   // en retard
                student(8L, "Fatou", "Sylla"),       // échéance proche
                student(9L, "Ibrahima", "Diallo")    // à jour, 2 tranches payées sur 3
        ));

        // Élève 7 : tranche 1 PAID, tranche 2 UNPAID en retard (due hier).
        // Élève 8 : tranche 1 PAID, tranche 2 UNPAID dans 3 jours.
        // Élève 9 : tranches 1 et 2 PAID, tranche 3 pas encore facturée (aucune ligne) -> à jour.
        when(dataPort.findClassMonthlyFeeLines(1L, 3L)).thenReturn(List.of(
                new ClassFeeLine(7L, 100L, BigDecimal.ZERO, "PAID", LocalDate.now().minusMonths(1)),
                new ClassFeeLine(7L, 101L, new BigDecimal("50000"), "UNPAID", LocalDate.now().minusDays(5)),
                new ClassFeeLine(8L, 100L, BigDecimal.ZERO, "PAID", LocalDate.now().minusMonths(1)),
                new ClassFeeLine(8L, 101L, new BigDecimal("50000"), "UNPAID", LocalDate.now().plusDays(3)),
                new ClassFeeLine(9L, 100L, BigDecimal.ZERO, "PAID", LocalDate.now().minusMonths(2)),
                new ClassFeeLine(9L, 101L, BigDecimal.ZERO, "PAID", LocalDate.now().minusMonths(1))
        ));

        when(studentDataProvider.hasPlatformAccessOptIn(7L)).thenReturn(true);
        when(studentDataProvider.hasPlatformAccessOptIn(8L)).thenReturn(true);
        when(studentDataProvider.hasPlatformAccessOptIn(9L)).thenReturn(true);
        when(studentDataProvider.findParentsByStudent(7L)).thenReturn(List.of(new ParentPublicInfo(70L, "P7", "N7", null, "623000007")));
        when(studentDataProvider.findParentsByStudent(8L)).thenReturn(List.of(new ParentPublicInfo(80L, "P8", "N8", null, "623000008")));
        when(studentDataProvider.findParentsByStudent(9L)).thenReturn(List.of(new ParentPublicInfo(90L, "P9", "N9", null, "623000009")));

        List<PaymentReminderCandidatesService.Candidate> result = service.listByClass(1L, 3L);

        assertThat(result).hasSize(3);
        assertThat(result).allMatch(c -> "CM2 A".equals(c.className()));
        assertThat(result).allMatch(c -> c.totalTranches() == 3);

        var s7 = result.stream().filter(c -> c.studentId() == 7L).findFirst().orElseThrow();
        assertThat(s7.status()).isEqualTo(PaymentReminderCandidatesService.Status.OVERDUE);
        assertThat(s7.tranchesPaid()).isEqualTo(1);
        assertThat(s7.daysLate()).isEqualTo(5);

        var s8 = result.stream().filter(c -> c.studentId() == 8L).findFirst().orElseThrow();
        assertThat(s8.status()).isEqualTo(PaymentReminderCandidatesService.Status.UPCOMING);
        assertThat(s8.tranchesPaid()).isEqualTo(1);
        assertThat(s8.daysLate()).isEqualTo(-3);

        var s9 = result.stream().filter(c -> c.studentId() == 9L).findFirst().orElseThrow();
        assertThat(s9.status()).isEqualTo(PaymentReminderCandidatesService.Status.UP_TO_DATE);
        assertThat(s9.tranchesPaid()).isEqualTo(2);
        assertThat(s9.amountDue()).isEqualByComparingTo(BigDecimal.ZERO);

        // Classés : en retard d'abord, puis échéance proche, puis à jour.
        assertThat(result).extracting(PaymentReminderCandidatesService.Candidate::studentId)
                .containsExactly(7L, 8L, 9L);
    }

    @Test
    void listByClass_studentWithoutPlatformAccess_hasNoParentContacts() {
        when(academicsDataProvider.findClass(3L)).thenReturn(Optional.empty());
        when(dataPort.countMonthlyTranches(1L, 3L)).thenReturn(0);
        when(dataPort.findClassMonthlyFeeLines(1L, 3L)).thenReturn(List.of());
        when(studentDataProvider.findStudentsByClass(3L)).thenReturn(List.of(student(7L, "Mamadou", "Camara")));
        when(studentDataProvider.hasPlatformAccessOptIn(7L)).thenReturn(false);

        List<PaymentReminderCandidatesService.Candidate> result = service.listByClass(1L, 3L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).parents()).isEmpty();
        assertThat(result.get(0).status()).isEqualTo(PaymentReminderCandidatesService.Status.UP_TO_DATE);
    }
}
