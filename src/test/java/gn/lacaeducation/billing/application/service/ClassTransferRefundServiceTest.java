package gn.lacaeducation.billing.application.service;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.academics.ClassPublicInfo;
import gn.lacaeducation.billing.application.port.in.ClassTransferRefundUseCase.PendingRefund;
import gn.lacaeducation.billing.application.port.out.ClassTransferRefundRepository;
import gn.lacaeducation.billing.domain.ClassTransferRefund;
import gn.lacaeducation.students.StudentDataProvider;
import gn.lacaeducation.students.StudentPublicInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * §retour comptable : "ajouter un écran pour voir/régulariser ces surplus" — surplus de crédit laissé
 * par un transfert de classe (parent qui avait déjà réglé plus que ce que coûte la nouvelle classe),
 * trouvés lors de l'analyse d'un export de production (10 élèves, 100 000 GNF jamais régularisés).
 */
@ExtendWith(MockitoExtension.class)
class ClassTransferRefundServiceTest {

    @Mock ClassTransferRefundRepository repo;
    @Mock StudentDataProvider studentDataProvider;
    @Mock AcademicsDataProvider academicsDataProvider;
    @InjectMocks ClassTransferRefundService service;

    @Test
    void findAllPending_resolvesStudentAndClassNames() {
        ClassTransferRefund refund = ClassTransferRefund.builder().id(1L).tenantId(10L).studentId(53L)
                .oldClassId(8L).newClassId(6L).amount(new BigDecimal("10000"))
                .reason("Transfert de classe (nouvelle classe moins chère)")
                .createdAt(LocalDateTime.of(2026, 10, 29, 11, 12)).build();
        when(repo.findPending()).thenReturn(List.of(refund));
        when(studentDataProvider.findStudent(53L)).thenReturn(Optional.of(
                new StudentPublicInfo(53L, 10L, "Ibrahima Sory", "CAMARA", null, null, "BD00001", null, null, null, null)));
        when(academicsDataProvider.findClass(8L)).thenReturn(Optional.of(
                new ClassPublicInfo(8L, "5ème Année", "5ème Année", "NORMALE", null, null, "PRIMAIRE")));
        when(academicsDataProvider.findClass(6L)).thenReturn(Optional.of(
                new ClassPublicInfo(6L, "3ème Année", "3ème Année", "NORMALE", null, null, "PRIMAIRE")));

        List<PendingRefund> result = service.findAllPending();

        assertThat(result).hasSize(1);
        PendingRefund p = result.get(0);
        assertThat(p.studentName()).isEqualTo("Ibrahima Sory CAMARA");
        assertThat(p.studentNumber()).isEqualTo("BD00001");
        assertThat(p.oldClassName()).isEqualTo("5ème Année");
        assertThat(p.newClassName()).isEqualTo("3ème Année");
        assertThat(p.amount()).isEqualByComparingTo("10000");
    }

    @Test
    void findAllPending_unknownStudent_fallsBackToPlaceholderName() {
        ClassTransferRefund refund = ClassTransferRefund.builder().id(2L).tenantId(10L).studentId(999L)
                .amount(new BigDecimal("5000")).createdAt(LocalDateTime.now()).build();
        when(repo.findPending()).thenReturn(List.of(refund));
        when(studentDataProvider.findStudent(999L)).thenReturn(Optional.empty());

        List<PendingRefund> result = service.findAllPending();

        assertThat(result.get(0).studentName()).isEqualTo("Élève #999");
        assertThat(result.get(0).oldClassName()).isNull();
        assertThat(result.get(0).newClassName()).isNull();
    }

    @Test
    void markRefunded_setsRefundedAt() {
        ClassTransferRefund refund = ClassTransferRefund.builder().id(1L).tenantId(10L).studentId(53L)
                .amount(new BigDecimal("10000")).createdAt(LocalDateTime.now()).build();
        when(repo.findById(1L)).thenReturn(Optional.of(refund));
        when(repo.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));

        ClassTransferRefund result = service.markRefunded(1L);

        assertThat(result.getRefundedAt()).isNotNull();
        assertThat(result.isPending()).isFalse();
    }
}
