package gn.lacaeducation.biometric.application.service;

import gn.lacaeducation.biometric.application.port.out.BiometricDeviceRepository;
import gn.lacaeducation.biometric.application.port.out.BiometricEnrollmentRepository;
import gn.lacaeducation.biometric.application.port.out.BiometricPunchConflictRepository;
import gn.lacaeducation.biometric.application.port.out.BiometricPunchRepository;
import gn.lacaeducation.biometric.application.service.BiometricService.PunchCommand;
import gn.lacaeducation.biometric.domain.*;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * §suivi-examen / SYNCHRONISATION_ET_CONFLITS.md §4.2 : un même sujet pointé sur deux appareils de
 * localisations différentes à quelques minutes d'intervalle est physiquement incompatible — ne jamais
 * résolu automatiquement, toujours routé vers une revue humaine, les pointages d'origine ne sont
 * jamais modifiés.
 */
@ExtendWith(MockitoExtension.class)
class BiometricPunchConflictTest {

    @Mock BiometricDeviceRepository deviceRepo;
    @Mock BiometricEnrollmentRepository enrollmentRepo;
    @Mock BiometricPunchRepository punchRepo;
    @Mock BiometricPunchConflictRepository conflictRepo;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks BiometricService service;

    private static final LocalDateTime WHEN = LocalDateTime.of(2026, 11, 6, 8, 0);

    private BiometricDevice deviceAt(Long id, String location) {
        return BiometricDevice.builder().id(id).tenantId(10L).name("Appareil " + id).location(location).active(true).build();
    }

    private BiometricEnrollment enrollment(Long deviceId) {
        return BiometricEnrollment.builder().id(5L).tenantId(10L).deviceId(deviceId)
                .subjectType(BiometricSubjectType.TEACHER).subjectId(42L)
                .externalUserId("EXT-42").status(EnrollmentStatus.ENROLLED).build();
    }

    private void stubEnrollmentAndSave() {
        when(enrollmentRepo.findByDeviceIdAndExternalUserId(any(), eq("EXT-42")))
                .thenAnswer(inv -> Optional.of(enrollment(inv.getArgument(0))));
        when(punchRepo.save(any())).thenAnswer(inv -> {
            BiometricPunch p = inv.getArgument(0);
            if (p.getId() == null) p.setId((long) (Math.random() * 100000) + 1000);
            return p;
        });
    }

    @Test
    void ingestPunch_sameSubjectDifferentDeviceDifferentLocationWithinWindow_flagsConflict() {
        stubEnrollmentAndSave();
        when(deviceRepo.findById(1L)).thenReturn(Optional.of(deviceAt(1L, "Centre A")));
        when(deviceRepo.findById(2L)).thenReturn(Optional.of(deviceAt(2L, "Centre B")));
        BiometricPunch existing = BiometricPunch.builder().id(1L).deviceId(1L).subjectType(BiometricSubjectType.TEACHER)
                .subjectId(42L).punchTime(WHEN).build();
        when(punchRepo.findByTenantIdAndSubjectTypeAndSubjectIdAndPunchTimeBetween(eq(10L), eq(BiometricSubjectType.TEACHER), eq(42L), any(), any()))
                .thenReturn(List.of(existing));

        service.ingestPunch(10L, 2L, new PunchCommand("EXT-42", WHEN.plusMinutes(3), PunchDirection.IN, null, null, null));

        ArgumentCaptor<BiometricPunchConflict> captor = ArgumentCaptor.forClass(BiometricPunchConflict.class);
        verify(conflictRepo, times(1)).save(captor.capture());
        BiometricPunchConflict conflict = captor.getValue();
        assertThat(conflict.getTenantId()).isEqualTo(10L);
        assertThat(conflict.getSubjectId()).isEqualTo(42L);
        assertThat(conflict.getPunchAId()).isEqualTo(1L);
        assertThat(conflict.getDeviceAId()).isEqualTo(1L);
        assertThat(conflict.getDeviceBId()).isEqualTo(2L);
        assertThat(conflict.getStatus()).isEqualTo(ConflictStatus.OPEN);
    }

    @Test
    void ingestPunch_sameDeviceTwice_neverConflict() {
        stubEnrollmentAndSave();
        when(deviceRepo.findById(1L)).thenReturn(Optional.of(deviceAt(1L, "Centre A")));
        BiometricPunch existing = BiometricPunch.builder().id(1L).deviceId(1L).subjectType(BiometricSubjectType.TEACHER)
                .subjectId(42L).punchTime(WHEN).build();
        when(punchRepo.findByTenantIdAndSubjectTypeAndSubjectIdAndPunchTimeBetween(eq(10L), eq(BiometricSubjectType.TEACHER), eq(42L), any(), any()))
                .thenReturn(List.of(existing));

        service.ingestPunch(10L, 1L, new PunchCommand("EXT-42", WHEN.plusMinutes(1), PunchDirection.OUT, null, null, null));

        verify(conflictRepo, never()).save(any());
    }

    @Test
    void ingestPunch_differentDevicesSameLocation_neverConflict() {
        stubEnrollmentAndSave();
        when(deviceRepo.findById(1L)).thenReturn(Optional.of(deviceAt(1L, "Centre A")));
        when(deviceRepo.findById(2L)).thenReturn(Optional.of(deviceAt(2L, "Centre A")));
        BiometricPunch existing = BiometricPunch.builder().id(1L).deviceId(1L).subjectType(BiometricSubjectType.TEACHER)
                .subjectId(42L).punchTime(WHEN).build();
        when(punchRepo.findByTenantIdAndSubjectTypeAndSubjectIdAndPunchTimeBetween(eq(10L), eq(BiometricSubjectType.TEACHER), eq(42L), any(), any()))
                .thenReturn(List.of(existing));

        service.ingestPunch(10L, 2L, new PunchCommand("EXT-42", WHEN.plusMinutes(2), PunchDirection.IN, null, null, null));

        verify(conflictRepo, never()).save(any());
    }

    @Test
    void ingestPunch_unknownLocationOnOneDevice_flagsConflictByPrudence() {
        stubEnrollmentAndSave();
        when(deviceRepo.findById(1L)).thenReturn(Optional.of(deviceAt(1L, null)));
        when(deviceRepo.findById(2L)).thenReturn(Optional.of(deviceAt(2L, "Centre B")));
        BiometricPunch existing = BiometricPunch.builder().id(1L).deviceId(1L).subjectType(BiometricSubjectType.TEACHER)
                .subjectId(42L).punchTime(WHEN).build();
        when(punchRepo.findByTenantIdAndSubjectTypeAndSubjectIdAndPunchTimeBetween(eq(10L), eq(BiometricSubjectType.TEACHER), eq(42L), any(), any()))
                .thenReturn(List.of(existing));

        service.ingestPunch(10L, 2L, new PunchCommand("EXT-42", WHEN.plusMinutes(1), PunchDirection.IN, null, null, null));

        verify(conflictRepo, times(1)).save(any());
    }

    @Test
    void reviewConflict_validDecision_recordsReviewerAndNote_doesNotTouchPunches() {
        BiometricPunchConflict open = BiometricPunchConflict.builder().id(7L).tenantId(10L)
                .status(ConflictStatus.OPEN).punchAId(1L).punchBId(2L).build();
        when(conflictRepo.findById(7L)).thenReturn(Optional.of(open));
        when(conflictRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BiometricPunchConflict result = service.reviewConflict(7L, ConflictStatus.DISMISSED, 99L, "Deux salles du même site, badge secondaire");

        assertThat(result.getStatus()).isEqualTo(ConflictStatus.DISMISSED);
        assertThat(result.getReviewedBy()).isEqualTo(99L);
        assertThat(result.getReviewNote()).contains("badge secondaire");
        assertThat(result.getReviewedAt()).isNotNull();
        verifyNoInteractions(punchRepo);
    }

    @Test
    void reviewConflict_decisionOpen_rejected() {
        assertThatThrownBy(() -> service.reviewConflict(7L, ConflictStatus.OPEN, 99L, null))
                .isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(conflictRepo);
    }

    @Test
    void reviewConflict_unknownId_throwsNotFound() {
        when(conflictRepo.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reviewConflict(404L, ConflictStatus.DISMISSED, 99L, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
