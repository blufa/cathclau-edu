package gn.lacaeducation.audit.adapters.out;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.academics.ClassPublicInfo;
import gn.lacaeducation.audit.adapters.in.web.dto.AuditLogResponse;
import gn.lacaeducation.audit.domain.AuditLog;
import gn.lacaeducation.billing.InvoiceDataProvider;
import gn.lacaeducation.billing.InvoicePublicInfo;
import gn.lacaeducation.iam.UserPublicInfoProvider;
import gn.lacaeducation.payments.PaymentDataProvider;
import gn.lacaeducation.payments.PaymentPublicInfo;
import gn.lacaeducation.students.StudentDataProvider;
import gn.lacaeducation.students.StudentPublicInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditResourceNameResolverTest {

    @Mock UserPublicInfoProvider userProvider;
    @Mock StudentDataProvider studentProvider;
    @Mock InvoiceDataProvider invoiceDataProvider;
    @Mock PaymentDataProvider paymentDataProvider;
    @Mock AcademicsDataProvider academicsDataProvider;
    @InjectMocks AuditResourceNameResolver resolver;

    private AuditLog log(String action, String ressource, Long ressourceId, String details) {
        return AuditLog.builder().id(1L).tenantId(1L).utilisateurId(45L)
                .action(action).ressource(ressource).ressourceId(ressourceId).details(details).build();
    }

    @Test
    void toResponse_resolvesActorName() {
        AuditLog l = log("LOGIN", "User", 45L, null);
        when(userProvider.findUser(45L)).thenReturn(Optional.of(new UserPublicInfoProvider.UserPublicInfo(45L, "Fatou", "Diallo", null)));

        AuditLogResponse r = resolver.toResponse(l);

        assertThat(r.utilisateurNom()).isEqualTo("Fatou Diallo");
        assertThat(r.ressourceNom()).isEqualTo("Fatou Diallo");
    }

    @Test
    void toResponse_resolvesInvoiceReference() {
        when(invoiceDataProvider.findInvoice(60L)).thenReturn(Optional.of(
                new InvoicePublicInfo(60L, "EC-INS-2026-501088", null, null, null)));

        AuditLogResponse r = resolver.toResponse(log("INVOICE_GENERATED", "Invoice", 60L, "studentId=7"));

        assertThat(r.ressourceNom()).isEqualTo("EC-INS-2026-501088");
    }

    @Test
    void toResponse_resolvesInstallmentLabel() {
        when(invoiceDataProvider.findInstallmentLabel(412L)).thenReturn(Optional.of("Octobre 2026"));

        AuditLogResponse r = resolver.toResponse(log("INSTALLMENT_CREATED", "Installment", 412L, null));

        assertThat(r.ressourceNom()).isEqualTo("Octobre 2026");
    }

    @Test
    void toResponse_resolvesEnrollmentViaStudentIdInDetails() {
        when(studentProvider.findStudent(7L)).thenReturn(Optional.of(
                new StudentPublicInfo(7L, 1L, "Mamadou", "Barry", null, null, "MAT-7", null, null, null, null)));

        AuditLogResponse r = resolver.toResponse(log("ENROLLMENT_CREATED", "Enrollment", 14L,
                "studentId=7;classId=3;type=NEW"));

        assertThat(r.ressourceNom()).isEqualTo("Mamadou Barry");
    }

    @Test
    void toResponse_resolvesPaymentReference() {
        when(paymentDataProvider.findPayment(200L)).thenReturn(Optional.of(
                new PaymentPublicInfo(200L, "PAY-GRP-7-ABC", null, "CASH", null, null)));

        AuditLogResponse r = resolver.toResponse(log("PAYMENT_SUCCEEDED", "Payment", 200L, null));

        assertThat(r.ressourceNom()).isEqualTo("PAY-GRP-7-ABC");
    }

    @Test
    void toResponse_resolvesClassLabel() {
        when(academicsDataProvider.findClass(3L)).thenReturn(Optional.of(
                new ClassPublicInfo(3L, "8ème A", "8ème", null, null, null, "SECONDAIRE")));

        AuditLogResponse r = resolver.toResponse(log("CLASS_CAPACITY_OVERRIDDEN", "Classe", 3L, null));

        assertThat(r.ressourceNom()).isEqualTo("8ème A");
    }

    @Test
    void toResponse_unresolvedResourceType_returnsNullNeverThrows() {
        AuditLogResponse r = resolver.toResponse(log("CASHIER_SESSION_CLOSED", "CashierSession", 9L, null));

        assertThat(r.ressourceNom()).isNull();
    }

    @Test
    void toResponse_nullActor_neverResolvesName() {
        AuditLog l = AuditLog.builder().id(1L).tenantId(1L).utilisateurId(null)
                .action("TENANT_CREATED").ressource("Tenant").ressourceId(1L).details("schoolName=X").build();

        AuditLogResponse r = resolver.toResponse(l);

        assertThat(r.utilisateurNom()).isNull();
    }
}
