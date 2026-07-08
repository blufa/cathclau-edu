package gn.lacaeducation.portal.adapters.in.web;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.academics.PeriodPublicInfo;
import gn.lacaeducation.grading.StudentReportProvider;
import gn.lacaeducation.payments.StudentReceiptProvider;
import gn.lacaeducation.students.ParentAccessProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParentDocumentsControllerTest {

    @Mock ParentAccessProvider parentAccessProvider;
    @Mock StudentReportProvider studentReportProvider;
    @Mock StudentReceiptProvider studentReceiptProvider;
    @Mock AcademicsDataProvider academicsDataProvider;
    @Mock Jwt jwt;
    @InjectMocks ParentDocumentsController controller;

    @Test
    void documents_aggregatesPublishedReportCardsAndReceipts() {
        when(jwt.getClaimAsStringList("roles")).thenReturn(List.of("PARENT"));
        when(jwt.getSubject()).thenReturn("7");
        when(parentAccessProvider.isParentOfStudent(7L, 10L)).thenReturn(true);
        when(studentReportProvider.listPublished(10L)).thenReturn(List.of(
                new StudentReportProvider.PublishedReportInfo(55L, 2L, new BigDecimal("14"), "Bien")));
        when(academicsDataProvider.findPeriod(2L)).thenReturn(
                java.util.Optional.of(new PeriodPublicInfo(2L, "Trimestre 1", "TRIMESTRE")));
        when(studentReceiptProvider.listReceipts(10L)).thenReturn(List.of(
                new StudentReceiptProvider.ReceiptInfo(88L, "EC-REC-1", new BigDecimal("100"), LocalDateTime.now())));

        List<ParentDocumentsController.DocumentItem> docs = controller.documents(10L, jwt);

        assertThat(docs).hasSize(2);
        assertThat(docs.get(0).type()).isEqualTo("BULLETIN");
        assertThat(docs.get(0).downloadUrl()).isEqualTo("/api/v1/grading/report-cards/55/pdf/download");
        assertThat(docs.get(0).label()).contains("Trimestre 1");
        assertThat(docs.get(1).type()).isEqualTo("RECU");
        assertThat(docs.get(1).downloadUrl()).isEqualTo("/api/v1/parent/receipts/88/pdf/download");
    }

    @Test
    void documents_unlinkedChild_throws() {
        when(jwt.getClaimAsStringList("roles")).thenReturn(List.of("PARENT"));
        when(jwt.getSubject()).thenReturn("7");
        when(parentAccessProvider.isParentOfStudent(7L, 99L)).thenReturn(false);

        assertThatThrownBy(() -> controller.documents(99L, jwt)).isInstanceOf(AccessDeniedException.class);
    }
}
