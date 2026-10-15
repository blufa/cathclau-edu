package gn.lacaeducation.communication.application.service;

import gn.lacaeducation.communication.application.event.UpcomingPaymentDeadlinesDigestEvent;
import gn.lacaeducation.communication.application.service.PaymentReminderCandidatesService.Candidate;
import gn.lacaeducation.communication.application.service.PaymentReminderCandidatesService.Status;
import gn.lacaeducation.iam.UserPublicInfoProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpcomingDeadlineNotifyProcessorTest {

    @Mock PaymentReminderCandidatesService candidatesService;
    @Mock UserPublicInfoProvider userPublicInfoProvider;
    @Mock ApplicationEventPublisher eventPublisher;

    UpcomingDeadlineNotifyProcessor processor() {
        return new UpcomingDeadlineNotifyProcessor(candidatesService, userPublicInfoProvider, eventPublisher);
    }

    private Candidate candidate(Long studentId, Status status) {
        return new Candidate(studentId, "Élève " + studentId, 3L, "CM2 A", status,
                BigDecimal.ZERO, null, 0, 1, 3, List.of());
    }

    @Test
    void upcomingCandidatesExist_publishesDigestToResolvedStaff() {
        when(candidatesService.listByTenant(1L)).thenReturn(List.of(
                candidate(7L, Status.UPCOMING), candidate(8L, Status.UPCOMING), candidate(9L, Status.OVERDUE)));
        when(userPublicInfoProvider.findUserIdsByRoles(eq(1L), anySet())).thenReturn(List.of(100L, 101L));

        processor().notifyForTenant(1L);

        ArgumentCaptor<UpcomingPaymentDeadlinesDigestEvent> captor = ArgumentCaptor.forClass(UpcomingPaymentDeadlinesDigestEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().tenantId()).isEqualTo(1L);
        assertThat(captor.getValue().studentCount()).isEqualTo(2); // seul UPCOMING compte, pas OVERDUE
        assertThat(captor.getValue().userIds()).containsExactlyInAnyOrder(100L, 101L);
    }

    @Test
    void resolvesOnlyAccountingAndLeadershipRoles() {
        when(candidatesService.listByTenant(1L)).thenReturn(List.of(candidate(7L, Status.UPCOMING)));
        when(userPublicInfoProvider.findUserIdsByRoles(eq(1L), anySet())).thenReturn(List.of(100L));

        processor().notifyForTenant(1L);

        ArgumentCaptor<Set<String>> rolesCaptor = ArgumentCaptor.forClass(Set.class);
        verify(userPublicInfoProvider).findUserIdsByRoles(eq(1L), rolesCaptor.capture());
        assertThat(rolesCaptor.getValue()).containsExactlyInAnyOrder("SCHOOL_ADMIN", "DIRECTOR", "ACCOUNTANT");
    }

    @Test
    void noUpcomingCandidates_publishesNothing() {
        when(candidatesService.listByTenant(1L)).thenReturn(List.of(candidate(7L, Status.UP_TO_DATE)));

        processor().notifyForTenant(1L);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void upcomingCandidatesExistButNoStaffResolved_publishesNothing() {
        when(candidatesService.listByTenant(1L)).thenReturn(List.of(candidate(7L, Status.UPCOMING)));
        when(userPublicInfoProvider.findUserIdsByRoles(eq(1L), anySet())).thenReturn(List.of());

        processor().notifyForTenant(1L);

        verify(eventPublisher, never()).publishEvent(any());
    }
}
