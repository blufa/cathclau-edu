package gn.lacaeducation.communication.application.listener;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.common.TenantInfoProvider;
import gn.lacaeducation.common.TenantPublicInfo;
import gn.lacaeducation.communication.application.port.in.CommunicationUseCase;
import gn.lacaeducation.communication.application.port.in.CommunicationUseCase.SendCommunicationCommand;
import gn.lacaeducation.communication.application.port.out.MessageTemplateRepository;
import gn.lacaeducation.communication.domain.CommunicationChannel;
import gn.lacaeducation.communication.domain.RecipientType;
import gn.lacaeducation.iam.SchoolStaffDirectory;
import gn.lacaeducation.iam.SchoolStaffDirectory.SchoolAdminContact;
import gn.lacaeducation.payments.PaymentDataProvider;
import gn.lacaeducation.payments.PaymentPublicInfo;
import gn.lacaeducation.payments.application.event.PaymentSucceededEvent;
import gn.lacaeducation.students.ParentPublicInfo;
import gn.lacaeducation.students.StudentDataProvider;
import gn.lacaeducation.students.StudentPublicInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommunicationEventListenerTest {

    @Mock CommunicationUseCase communicationUseCase;
    @Mock MessageTemplateRepository templateRepository;
    @Mock StudentDataProvider studentDataProvider;
    @Mock PaymentDataProvider paymentDataProvider;
    @Mock AcademicsDataProvider academicsDataProvider;
    @Mock TenantInfoProvider tenantInfoProvider;
    @Mock SchoolStaffDirectory schoolStaffDirectory;

    private CommunicationEventListener listener() {
        return new CommunicationEventListener(communicationUseCase, templateRepository, studentDataProvider,
                paymentDataProvider, academicsDataProvider, tenantInfoProvider, schoolStaffDirectory);
    }

    private final StudentPublicInfo student = new StudentPublicInfo(
            7L, 10L, "Awa", "Diallo", null, "620000000", "MAT-1", null, null, null);

    @Test
    void onPaymentSucceeded_notifiesParentAndSchoolAdmin() {
        when(studentDataProvider.findStudent(7L)).thenReturn(Optional.of(student));
        when(studentDataProvider.hasPlatformAccessOptIn(7L)).thenReturn(true);
        when(paymentDataProvider.findPayment(1L))
                .thenReturn(Optional.of(new PaymentPublicInfo(1L, "PAY-1", new BigDecimal("50000"), "CASH", null)));
        when(studentDataProvider.findParentsByStudent(7L))
                .thenReturn(List.of(new ParentPublicInfo(1L, "Mariama", "Bah", null, "621000000")));
        when(schoolStaffDirectory.findSchoolAdminContacts(10L))
                .thenReturn(List.of(new SchoolAdminContact("622000000", "Directeur École")));
        when(tenantInfoProvider.findTenantInfo(10L)).thenReturn(Optional.of(
                new TenantPublicInfo(10L, "École Test", null, null, null, null, null, null, null, null, null, null, null)));
        when(templateRepository.findByTenantIdAndReason(any(), any())).thenReturn(Optional.empty());

        listener().onPaymentSucceeded(new PaymentSucceededEvent(10L, 7L, 1L, null, new BigDecimal("50000")));

        ArgumentCaptor<SendCommunicationCommand> captor = ArgumentCaptor.forClass(SendCommunicationCommand.class);
        verify(communicationUseCase, times(2)).send(captor.capture());

        SendCommunicationCommand parentCmd = captor.getAllValues().stream()
                .filter(c -> c.recipientType() == RecipientType.PARENT).findFirst().orElseThrow();
        assertThat(parentCmd.recipients()).extracting("destination").containsExactly("621000000");

        SendCommunicationCommand founderCmd = captor.getAllValues().stream()
                .filter(c -> c.recipientType() == RecipientType.GROUP).findFirst().orElseThrow();
        assertThat(founderCmd.channel()).isEqualTo(CommunicationChannel.SMS);
        assertThat(founderCmd.recipients()).extracting("destination").containsExactly("622000000");
        assertThat(founderCmd.content()).contains("Awa Diallo").contains("50000");
    }

    @Test
    void onPaymentSucceeded_noSchoolAdminPhone_skipsFounderAlertWithoutError() {
        when(studentDataProvider.findStudent(7L)).thenReturn(Optional.of(student));
        when(studentDataProvider.hasPlatformAccessOptIn(7L)).thenReturn(true);
        when(paymentDataProvider.findPayment(1L))
                .thenReturn(Optional.of(new PaymentPublicInfo(1L, "PAY-1", new BigDecimal("50000"), "CASH", null)));
        when(studentDataProvider.findParentsByStudent(7L)).thenReturn(List.of());
        when(schoolStaffDirectory.findSchoolAdminContacts(10L)).thenReturn(List.of());
        when(tenantInfoProvider.findTenantInfo(10L)).thenReturn(Optional.empty());
        when(templateRepository.findByTenantIdAndReason(any(), any())).thenReturn(Optional.empty());

        listener().onPaymentSucceeded(new PaymentSucceededEvent(10L, 7L, 1L, null, new BigDecimal("50000")));

        verify(communicationUseCase, times(0)).send(any());
    }
}
