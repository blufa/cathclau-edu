package gn.lacaeducation.portal.adapters.in.web;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.academics.ClassPublicInfo;
import gn.lacaeducation.billing.StudentBillingProvider;
import gn.lacaeducation.grading.StudentReportProvider;
import gn.lacaeducation.students.ParentAccessProvider;
import gn.lacaeducation.students.StudentDataProvider;
import gn.lacaeducation.students.StudentPublicInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParentDashboardControllerTest {

    @Mock ParentAccessProvider parentAccessProvider;
    @Mock StudentDataProvider studentDataProvider;
    @Mock AcademicsDataProvider academicsDataProvider;
    @Mock StudentReportProvider studentReportProvider;
    @Mock StudentBillingProvider studentBillingProvider;
    @Mock Jwt jwt;
    @InjectMocks ParentDashboardController controller;

    @Test
    void dashboard_aggregatesChildrenClassAverageAndBalance() {
        when(jwt.getSubject()).thenReturn("7");
        when(parentAccessProvider.findChildStudentIds(7L)).thenReturn(List.of(10L));
        when(studentDataProvider.findStudent(10L)).thenReturn(Optional.of(new StudentPublicInfo(
                10L, 1L, "Mariama", "Bah", null, null, "M-1", null, null, "F")));
        when(studentDataProvider.findCurrentClassId(10L)).thenReturn(Optional.of(5L));
        when(academicsDataProvider.findClass(5L)).thenReturn(Optional.of(new ClassPublicInfo(5L, "6ème A", "6ème", "NORMALE", null, null, null)));
        when(studentReportProvider.latestPublished(10L)).thenReturn(Optional.of(
                new StudentReportProvider.ReportInfo(new BigDecimal("14.50"), 3, 40, "Bien", "ADMIS")));
        when(studentBillingProvider.latestInvoice(10L)).thenReturn(Optional.of(
                new StudentBillingProvider.InvoiceInfo(10L, "FAC-1", new BigDecimal("100"),
                        new BigDecimal("60"), new BigDecimal("40"), "PARTIELLE", "T1")));

        ParentDashboardController.ParentDashboard result = controller.dashboard(jwt);

        assertThat(result.childrenCount()).isEqualTo(1);
        ParentDashboardController.ChildSummary child = result.children().get(0);
        assertThat(child.firstName()).isEqualTo("Mariama");
        assertThat(child.className()).isEqualTo("6ème A");
        assertThat(child.overallAverage()).isEqualByComparingTo("14.50");
        assertThat(child.rank()).isEqualTo(3);
        assertThat(child.feesRemaining()).isEqualByComparingTo("40");
    }

    @Test
    void dashboard_noChildren_returnsEmpty() {
        when(jwt.getSubject()).thenReturn("7");
        when(parentAccessProvider.findChildStudentIds(7L)).thenReturn(List.of());

        ParentDashboardController.ParentDashboard result = controller.dashboard(jwt);

        assertThat(result.childrenCount()).isZero();
        assertThat(result.children()).isEmpty();
    }
}
