package gn.lacaeducation.biometric.application.service;

import gn.lacaeducation.biometric.application.port.out.BiometricDeviceRepository;
import gn.lacaeducation.biometric.application.port.out.BiometricEnrollmentRepository;
import gn.lacaeducation.biometric.application.port.out.BiometricPunchConflictRepository;
import gn.lacaeducation.biometric.application.port.out.BiometricPunchRepository;
import gn.lacaeducation.biometric.application.service.BiometricService.PunchCommand;
import gn.lacaeducation.biometric.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * §suivi-examen (pointage offline) : un appareil/agent qui enregistre un pointage hors ligne peut le
 * renvoyer après une coupure réseau (retry) — l'ingestion doit rester idempotente par syncId, sans
 * jamais dupliquer le pointage ni republier l'événement {@link gn.lacaeducation.biometric.application.event.BiometricPunchRecorded}.
 */
@ExtendWith(MockitoExtension.class)
class BiometricServiceTest {

    @Mock BiometricDeviceRepository deviceRepo;
    @Mock BiometricEnrollmentRepository enrollmentRepo;
    @Mock BiometricPunchRepository punchRepo;
    @Mock BiometricPunchConflictRepository conflictRepo;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks BiometricService service;

    private BiometricDevice device() {
        return BiometricDevice.builder().id(1L).tenantId(10L).name("Tablette centre A").active(true).build();
    }

    private BiometricEnrollment enrollment() {
        return BiometricEnrollment.builder().id(5L).tenantId(10L).deviceId(1L)
                .subjectType(BiometricSubjectType.TEACHER).subjectId(42L)
                .externalUserId("EXT-42").status(EnrollmentStatus.ENROLLED).build();
    }

    @Test
    void ingestPunch_withSyncId_firstTime_createsAndPublishesEvent() {
        when(deviceRepo.findById(1L)).thenReturn(Optional.of(device()));
        when(punchRepo.findByDeviceIdAndSyncId(1L, "SYNC-1")).thenReturn(Optional.empty());
        when(enrollmentRepo.findByDeviceIdAndExternalUserId(1L, "EXT-42")).thenReturn(Optional.of(enrollment()));
        when(punchRepo.save(any())).thenAnswer(inv -> {
            BiometricPunch p = inv.getArgument(0);
            p.setId(99L);
            return p;
        });
        when(punchRepo.findByTenantIdAndSubjectTypeAndSubjectIdAndPunchTimeBetween(any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        BiometricPunch result = service.ingestPunch(10L, 1L,
                new PunchCommand("EXT-42", LocalDateTime.of(2026, 11, 6, 8, 0), PunchDirection.IN, null, null, "SYNC-1"));

        assertThat(result.getId()).isEqualTo(99L);
        assertThat(result.getSyncId()).isEqualTo("SYNC-1");
        verify(punchRepo, times(1)).save(any());
        verify(eventPublisher, times(1)).publishEvent(any(gn.lacaeducation.biometric.application.event.BiometricPunchRecorded.class));
        verify(conflictRepo, never()).save(any());
    }

    @Test
    void ingestPunch_sameSyncIdRetried_returnsExistingPunch_withoutDuplicatingOrRepublishing() {
        when(deviceRepo.findById(1L)).thenReturn(Optional.of(device()));
        BiometricPunch alreadySynced = BiometricPunch.builder().id(99L).tenantId(10L).deviceId(1L)
                .syncId("SYNC-1").externalUserId("EXT-42").punchTime(LocalDateTime.of(2026, 11, 6, 8, 0))
                .direction(PunchDirection.IN).build();
        when(punchRepo.findByDeviceIdAndSyncId(1L, "SYNC-1")).thenReturn(Optional.of(alreadySynced));

        BiometricPunch result = service.ingestPunch(10L, 1L,
                new PunchCommand("EXT-42", LocalDateTime.of(2026, 11, 6, 8, 0), PunchDirection.IN, null, null, "SYNC-1"));

        assertThat(result).isEqualTo(alreadySynced);
        verify(punchRepo, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(gn.lacaeducation.biometric.application.event.BiometricPunchRecorded.class));
        verify(enrollmentRepo, never()).findByDeviceIdAndExternalUserId(any(), any());
    }

    @Test
    void ingestPunch_withoutSyncId_legacyAgent_alwaysCreatesNewPunch() {
        when(deviceRepo.findById(1L)).thenReturn(Optional.of(device()));
        when(enrollmentRepo.findByDeviceIdAndExternalUserId(1L, "EXT-42")).thenReturn(Optional.empty());
        when(punchRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.ingestPunch(10L, 1L, new PunchCommand("EXT-42", null, null, null, null, null));
        service.ingestPunch(10L, 1L, new PunchCommand("EXT-42", null, null, null, null, null));

        verify(punchRepo, times(2)).save(any());
        verify(punchRepo, never()).findByDeviceIdAndSyncId(any(), any());
    }

    @Test
    void ingestBatch_mixOfNewAndAlreadySyncedPunches_onlyPersistsNewOnes() {
        when(deviceRepo.findById(1L)).thenReturn(Optional.of(device()));
        BiometricPunch alreadySynced = BiometricPunch.builder().id(1L).deviceId(1L).syncId("SYNC-OLD").build();
        when(punchRepo.findByDeviceIdAndSyncId(1L, "SYNC-OLD")).thenReturn(Optional.of(alreadySynced));
        when(punchRepo.findByDeviceIdAndSyncId(1L, "SYNC-NEW")).thenReturn(Optional.empty());
        when(enrollmentRepo.findByDeviceIdAndExternalUserId(1L, "EXT-42")).thenReturn(Optional.empty());
        when(punchRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.ingestBatch(10L, 1L, java.util.List.of(
                new PunchCommand("EXT-42", null, PunchDirection.IN, null, null, "SYNC-OLD"),
                new PunchCommand("EXT-42", null, PunchDirection.OUT, null, null, "SYNC-NEW")));

        verify(punchRepo, times(1)).save(any());
    }
}
