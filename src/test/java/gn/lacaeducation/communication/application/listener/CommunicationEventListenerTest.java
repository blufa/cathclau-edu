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
import gn.lacaeducation.iam.UserPublicInfoProvider;
import gn.lacaeducation.iam.UserPublicInfoProvider.UserPublicInfo;
import gn.lacaeducation.billing.application.event.InvoicePaymentAppliedEvent;
import gn.lacaeducation.communication.domain.CommunicationReason;
import gn.lacaeducation.payments.PayerDirectory;
import gn.lacaeducation.payments.PaymentDataProvider;
import gn.lacaeducation.payments.PaymentPublicInfo;
import gn.lacaeducation.payments.application.event.CashierSessionClosedEvent;
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
    @Mock PayerDirectory payerDirectory;
    @Mock AcademicsDataProvider academicsDataProvider;
    @Mock TenantInfoProvider tenantInfoProvider;
    @Mock SchoolStaffDirectory schoolStaffDirectory;
    @Mock UserPublicInfoProvider userPublicInfoProvider;

    private CommunicationEventListener listener() {
        return new CommunicationEventListener(communicationUseCase, templateRepository, studentDataProvider,
                paymentDataProvider, payerDirectory, academicsDataProvider, tenantInfoProvider, schoolStaffDirectory,
                userPublicInfoProvider);
    }

    private final StudentPublicInfo student = new StudentPublicInfo(
            7L, 10L, "Awa", "Diallo", null, "620000000", "MAT-1", null, null, null);

    // ── onStudentExit (§retour fondateurs : SMS de sortie avec durée passée à l'école) ──────────────

    @Test
    void onStudentExit_notifiesParent_withDuration_noLoginLink() {
        when(studentDataProvider.findStudent(7L)).thenReturn(Optional.of(student));
        when(studentDataProvider.hasPlatformAccessOptIn(7L)).thenReturn(true);
        when(academicsDataProvider.findClass(3L)).thenReturn(Optional.of(
                new gn.lacaeducation.academics.ClassPublicInfo(3L, "6ème A", "Collège", "NORMALE", null, null, null)));
        when(studentDataProvider.findParentsByStudent(7L))
                .thenReturn(List.of(new ParentPublicInfo(1L, "Mariama", "Bah", null, "621000000")));
        when(templateRepository.findByTenantIdAndReason(any(), any())).thenReturn(Optional.empty());

        listener().onStudentExit(new gn.lacaeducation.attendance.application.event.StudentExitEvent(
                10L, 7L, 3L, java.time.LocalDate.of(2026, 10, 5),
                java.time.LocalTime.of(8, 5), java.time.LocalTime.of(16, 0), 475));

        ArgumentCaptor<SendCommunicationCommand> captor = ArgumentCaptor.forClass(SendCommunicationCommand.class);
        verify(communicationUseCase).send(captor.capture());
        SendCommunicationCommand cmd = captor.getValue();
        assertThat(cmd.channel()).isEqualTo(CommunicationChannel.SMS);
        assertThat(cmd.content()).contains("Awa Diallo").contains("7h55").doesNotContain("http");
    }

    @Test
    void onPaymentSucceeded_notifiesParentAndSchoolAdmin() {
        when(studentDataProvider.findStudent(7L)).thenReturn(Optional.of(student));
        when(studentDataProvider.hasPlatformAccessOptIn(7L)).thenReturn(true);
        when(paymentDataProvider.findPayment(1L))
                .thenReturn(Optional.of(new PaymentPublicInfo(1L, "PAY-1", new BigDecimal("50000"), "CASH", null, "Octobre, Novembre")));
        when(studentDataProvider.findParentsByStudent(7L))
                .thenReturn(List.of(new ParentPublicInfo(1L, "Mariama", "Bah", null, "621000000")));
        when(schoolStaffDirectory.findSchoolAdminContacts(10L))
                .thenReturn(List.of(new SchoolAdminContact("622000000", "Directeur École")));
        when(tenantInfoProvider.findTenantInfo(10L)).thenReturn(Optional.of(
                new TenantPublicInfo(10L, "École Test", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null)));
        when(templateRepository.findByTenantIdAndReason(any(), any())).thenReturn(Optional.empty());

        listener().onPaymentSucceeded(new PaymentSucceededEvent(10L, 7L, 1L, null, new BigDecimal("50000"), null));

        ArgumentCaptor<SendCommunicationCommand> captor = ArgumentCaptor.forClass(SendCommunicationCommand.class);
        verify(communicationUseCase, times(2)).send(captor.capture());

        SendCommunicationCommand parentCmd = captor.getAllValues().stream()
                .filter(c -> c.recipientType() == RecipientType.PARENT).findFirst().orElseThrow();
        assertThat(parentCmd.recipients()).extracting("destination").containsExactly("621000000");
        // Contenu bref (§retour comptable) : juste le montant payé et le reste à payer, sans détailler
        // les mois couverts par ce règlement groupé.
        assertThat(parentCmd.content()).contains("Awa Diallo").contains("50000").contains("Aucun reste à payer.");

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
                .thenReturn(Optional.of(new PaymentPublicInfo(1L, "PAY-1", new BigDecimal("50000"), "CASH", null, null)));
        when(studentDataProvider.findParentsByStudent(7L)).thenReturn(List.of());
        when(schoolStaffDirectory.findSchoolAdminContacts(10L)).thenReturn(List.of());
        when(tenantInfoProvider.findTenantInfo(10L)).thenReturn(Optional.empty());
        when(templateRepository.findByTenantIdAndReason(any(), any())).thenReturn(Optional.empty());

        listener().onPaymentSucceeded(new PaymentSucceededEvent(10L, 7L, 1L, null, new BigDecimal("50000"), null));

        verify(communicationUseCase, times(0)).send(any());
    }

    /**
     * Un encaissement guichet (canal CASH) doit aussi alerter le comptable/caissier qui a
     * physiquement encaissé, pas seulement le(s) compte(s) SCHOOL_ADMIN — son numéro peut être
     * différent (§retour comptable, point 2).
     */
    @Test
    void onPaymentSucceeded_alsoNotifiesCollectorWhenDifferentFromSchoolAdmin() {
        when(studentDataProvider.findStudent(7L)).thenReturn(Optional.of(student));
        when(studentDataProvider.hasPlatformAccessOptIn(7L)).thenReturn(true);
        when(paymentDataProvider.findPayment(1L))
                .thenReturn(Optional.of(new PaymentPublicInfo(1L, "PAY-1", new BigDecimal("50000"), "CASH", null, null)));
        when(studentDataProvider.findParentsByStudent(7L))
                .thenReturn(List.of(new ParentPublicInfo(1L, "Mariama", "Bah", null, "621000000")));
        when(schoolStaffDirectory.findSchoolAdminContacts(10L))
                .thenReturn(List.of(new SchoolAdminContact("622000000", "Directeur École")));
        when(userPublicInfoProvider.findUser(99L))
                .thenReturn(Optional.of(new UserPublicInfo(99L, "Fatoumata", "Camara", "628000000")));
        when(tenantInfoProvider.findTenantInfo(10L)).thenReturn(Optional.of(
                new TenantPublicInfo(10L, "École Test", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null)));
        when(templateRepository.findByTenantIdAndReason(any(), any())).thenReturn(Optional.empty());

        listener().onPaymentSucceeded(new PaymentSucceededEvent(10L, 7L, 1L, null, new BigDecimal("50000"), 99L));

        ArgumentCaptor<SendCommunicationCommand> captor = ArgumentCaptor.forClass(SendCommunicationCommand.class);
        verify(communicationUseCase, times(2)).send(captor.capture());

        SendCommunicationCommand founderCmd = captor.getAllValues().stream()
                .filter(c -> c.recipientType() == RecipientType.GROUP).findFirst().orElseThrow();
        assertThat(founderCmd.recipients()).extracting("destination")
                .containsExactlyInAnyOrder("622000000", "628000000");
    }

    // ── onCashierSessionClosed (§retour fondateurs : alerte SMS sur écart de caisse, jamais au
    // comptable concerné lui-même) ──────────────────────────────────────────────────────────────────

    @Test
    void onCashierSessionClosed_negativeDiscrepancy_alertsSchoolAdminOnly() {
        when(schoolStaffDirectory.findSchoolAdminContacts(10L))
                .thenReturn(List.of(new SchoolAdminContact("622000000", "Directeur École")));
        when(userPublicInfoProvider.findUser(99L))
                .thenReturn(Optional.of(new UserPublicInfo(99L, "Fatoumata", "Camara", "628000000")));
        when(tenantInfoProvider.findTenantInfo(10L)).thenReturn(Optional.of(
                new TenantPublicInfo(10L, "École Test", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null)));

        listener().onCashierSessionClosed(new CashierSessionClosedEvent(10L, 99L, 5L,
                new BigDecimal("50000"), new BigDecimal("30000"), new BigDecimal("-20000")));

        ArgumentCaptor<SendCommunicationCommand> captor = ArgumentCaptor.forClass(SendCommunicationCommand.class);
        verify(communicationUseCase).send(captor.capture());
        SendCommunicationCommand cmd = captor.getValue();
        assertThat(cmd.channel()).isEqualTo(CommunicationChannel.SMS);
        assertThat(cmd.reason()).isEqualTo(CommunicationReason.SECURITY_ALERT);
        // Le comptable concerné (99L, tél. 628000000) ne doit JAMAIS recevoir cette alerte, à la
        // différence de notifyFounder utilisé pour un encaissement normal.
        assertThat(cmd.recipients()).extracting("destination").containsExactly("622000000");
        assertThat(cmd.content()).contains("NÉGATIF").contains("20000").contains("Fatoumata Camara");
    }

    @Test
    void onCashierSessionClosed_zeroDiscrepancy_sendsNothing() {
        listener().onCashierSessionClosed(new CashierSessionClosedEvent(10L, 99L, 5L,
                new BigDecimal("50000"), new BigDecimal("50000"), BigDecimal.ZERO));

        verify(communicationUseCase, times(0)).send(any());
    }

    @Test
    void onCashierSessionClosed_noSchoolAdminPhone_skipsWithoutError() {
        when(schoolStaffDirectory.findSchoolAdminContacts(10L)).thenReturn(List.of());

        listener().onCashierSessionClosed(new CashierSessionClosedEvent(10L, 99L, 5L,
                new BigDecimal("50000"), new BigDecimal("60000"), new BigDecimal("10000")));

        verify(communicationUseCase, times(0)).send(any());
    }

    // ── onCashCollectedWithoutOpenSession (§retour fondateurs, détecteur B2) ────────────────────────

    @Test
    void onCashCollectedWithoutOpenSession_alertsSchoolAdminOnly() {
        when(schoolStaffDirectory.findSchoolAdminContacts(10L))
                .thenReturn(List.of(new SchoolAdminContact("622000000", "Directeur École")));
        when(userPublicInfoProvider.findUser(99L))
                .thenReturn(Optional.of(new UserPublicInfo(99L, "Fatoumata", "Camara", "628000000")));
        when(tenantInfoProvider.findTenantInfo(10L)).thenReturn(Optional.of(
                new TenantPublicInfo(10L, "École Test", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null)));

        listener().onCashCollectedWithoutOpenSession(
                new gn.lacaeducation.payments.application.event.CashCollectedWithoutOpenSessionEvent(
                        10L, 99L, 1L, new BigDecimal("50000")));

        ArgumentCaptor<SendCommunicationCommand> captor = ArgumentCaptor.forClass(SendCommunicationCommand.class);
        verify(communicationUseCase).send(captor.capture());
        SendCommunicationCommand cmd = captor.getValue();
        assertThat(cmd.reason()).isEqualTo(CommunicationReason.SECURITY_ALERT);
        assertThat(cmd.recipients()).extracting("destination").containsExactly("622000000");
        assertThat(cmd.content()).contains("Fatoumata Camara").contains("50000").contains("SANS avoir ouvert");
    }

    // ── onUndeclaredAttendance (§retour fondateurs, détecteur B1) ───────────────────────────────────

    @Test
    void onUndeclaredAttendance_alertsSchoolAdminOnly() {
        when(studentDataProvider.findStudent(7L)).thenReturn(Optional.of(student));
        when(schoolStaffDirectory.findSchoolAdminContacts(10L))
                .thenReturn(List.of(new SchoolAdminContact("622000000", "Directeur École")));
        when(tenantInfoProvider.findTenantInfo(10L)).thenReturn(Optional.of(
                new TenantPublicInfo(10L, "École Test", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null)));

        listener().onUndeclaredAttendance(new gn.lacaeducation.attendance.application.event.UndeclaredAttendanceEvent(
                10L, 7L, java.time.LocalDate.of(2026, 10, 4)));

        ArgumentCaptor<SendCommunicationCommand> captor = ArgumentCaptor.forClass(SendCommunicationCommand.class);
        verify(communicationUseCase).send(captor.capture());
        SendCommunicationCommand cmd = captor.getValue();
        assertThat(cmd.channel()).isEqualTo(CommunicationChannel.SMS);
        assertThat(cmd.reason()).isEqualTo(CommunicationReason.SECURITY_ALERT);
        assertThat(cmd.recipients()).extracting("destination").containsExactly("622000000");
        assertThat(cmd.content()).contains("Awa Diallo").contains("2026-10-04").contains("SANS inscription déclarée");
    }

    @Test
    void onUndeclaredAttendance_noSchoolAdminPhone_skipsWithoutError() {
        when(studentDataProvider.findStudent(7L)).thenReturn(Optional.of(student));
        when(schoolStaffDirectory.findSchoolAdminContacts(10L)).thenReturn(List.of());

        listener().onUndeclaredAttendance(new gn.lacaeducation.attendance.application.event.UndeclaredAttendanceEvent(
                10L, 7L, java.time.LocalDate.of(2026, 10, 4)));

        verify(communicationUseCase, times(0)).send(any());
    }

    // ── onAccountantAnomalyDetected (§retour fondateurs, détecteur B3) ──────────────────────────────

    @Test
    void onAccountantAnomalyDetected_alertsSchoolAdminOnly() {
        when(schoolStaffDirectory.findSchoolAdminContacts(10L))
                .thenReturn(List.of(new SchoolAdminContact("622000000", "Directeur École")));
        when(userPublicInfoProvider.findUser(99L))
                .thenReturn(Optional.of(new UserPublicInfo(99L, "Fatoumata", "Camara", "628000000")));
        when(tenantInfoProvider.findTenantInfo(10L)).thenReturn(Optional.of(
                new TenantPublicInfo(10L, "École Test", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null)));

        listener().onAccountantAnomalyDetected(new gn.lacaeducation.audit.application.event.AccountantAnomalyDetectedEvent(
                10L, 99L, "CASH_DISCREPANCY", new BigDecimal("900000"), new BigDecimal("7500")));

        ArgumentCaptor<SendCommunicationCommand> captor = ArgumentCaptor.forClass(SendCommunicationCommand.class);
        verify(communicationUseCase).send(captor.capture());
        SendCommunicationCommand cmd = captor.getValue();
        assertThat(cmd.channel()).isEqualTo(CommunicationChannel.SMS);
        assertThat(cmd.reason()).isEqualTo(CommunicationReason.SECURITY_ALERT);
        assertThat(cmd.recipients()).extracting("destination").containsExactly("622000000");
        assertThat(cmd.content()).contains("Fatoumata Camara").contains("900000").contains("7500");
    }

    @Test
    void onAccountantAnomalyDetected_noSchoolAdminPhone_skipsWithoutError() {
        when(schoolStaffDirectory.findSchoolAdminContacts(10L)).thenReturn(List.of());

        listener().onAccountantAnomalyDetected(new gn.lacaeducation.audit.application.event.AccountantAnomalyDetectedEvent(
                10L, 99L, "EXEMPTION_VOLUME", new BigDecimal("20"), new BigDecimal("2.5")));

        verify(communicationUseCase, times(0)).send(any());
    }

    // ── onInvoicePaymentApplied (§retour comptable : contenu bref — montant payé + reste à payer
    // total, toutes factures confondues, pas seulement le solde de la facture réglée) ───────────────

    @Test
    void onInvoicePaymentApplied_isBrief_andTotalsOtherUnpaidInvoices() {
        when(studentDataProvider.findStudent(7L)).thenReturn(Optional.of(student));
        when(studentDataProvider.hasPlatformAccessOptIn(7L)).thenReturn(true);
        when(studentDataProvider.findParentsByStudent(7L))
                .thenReturn(List.of(new ParentPublicInfo(1L, "Mariama", "Bah", null, "621000000")));
        when(tenantInfoProvider.findTenantInfo(10L)).thenReturn(Optional.of(
                new TenantPublicInfo(10L, "École Test", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null)));
        when(templateRepository.findByTenantIdAndReason(any(), any())).thenReturn(Optional.empty());
        when(schoolStaffDirectory.findSchoolAdminContacts(10L)).thenReturn(List.of());
        when(payerDirectory.invoiceFeeInfo(50L)).thenReturn(Optional.of(
                new PayerDirectory.InvoiceFeeInfo("Mensualité", "Novembre 2026", null, null, null)));
        when(payerDirectory.className(7L)).thenReturn(Optional.of("7ème A"));
        when(payerDirectory.unpaidInvoices(7L)).thenReturn(List.of(
                new PayerDirectory.UnpaidInvoiceInfo("Cantine", new BigDecimal("20000"))));

        listener().onInvoicePaymentApplied(new InvoicePaymentAppliedEvent(
                10L, 7L, 50L, "FAC-1", new BigDecimal("50000"), BigDecimal.ZERO, null));

        ArgumentCaptor<SendCommunicationCommand> captor = ArgumentCaptor.forClass(SendCommunicationCommand.class);
        verify(communicationUseCase).send(captor.capture());
        String content = captor.getValue().content();
        assertThat(content).contains("Awa Diallo").contains("50000");
        // La mensualité réglée n'efface pas un autre frais encore dû (cantine) du message — le solde
        // affiché est le TOTAL toutes factures confondues, pas le détail facture par facture.
        assertThat(content).contains("Reste à payer : 20000 GNF.");
    }

    @Test
    void onInvoicePaymentApplied_noOtherUnpaidInvoices_saysNoneRemaining() {
        when(studentDataProvider.findStudent(7L)).thenReturn(Optional.of(student));
        when(studentDataProvider.hasPlatformAccessOptIn(7L)).thenReturn(true);
        when(studentDataProvider.findParentsByStudent(7L))
                .thenReturn(List.of(new ParentPublicInfo(1L, "Mariama", "Bah", null, "621000000")));
        when(tenantInfoProvider.findTenantInfo(10L)).thenReturn(Optional.of(
                new TenantPublicInfo(10L, "École Test", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null)));
        when(templateRepository.findByTenantIdAndReason(any(), any())).thenReturn(Optional.empty());
        when(schoolStaffDirectory.findSchoolAdminContacts(10L)).thenReturn(List.of());
        when(payerDirectory.invoiceFeeInfo(50L)).thenReturn(Optional.empty());
        when(payerDirectory.className(7L)).thenReturn(Optional.empty());
        when(payerDirectory.unpaidInvoices(7L)).thenReturn(List.of());

        listener().onInvoicePaymentApplied(new InvoicePaymentAppliedEvent(
                10L, 7L, 50L, "FAC-1", new BigDecimal("50000"), BigDecimal.ZERO, null));

        ArgumentCaptor<SendCommunicationCommand> captor = ArgumentCaptor.forClass(SendCommunicationCommand.class);
        verify(communicationUseCase).send(captor.capture());
        assertThat(captor.getValue().content()).contains("Aucun reste à payer.");
    }
}
