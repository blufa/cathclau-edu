package gn.lacaeducation.biometric.application.service;

import gn.lacaeducation.biometric.application.port.out.BiometricDeviceRepository;
import gn.lacaeducation.biometric.application.port.out.BiometricEnrollmentRepository;
import gn.lacaeducation.biometric.application.port.out.BiometricPunchConflictRepository;
import gn.lacaeducation.biometric.application.port.out.BiometricPunchRepository;
import gn.lacaeducation.biometric.application.service.BiometricService.ResolvedBadge;
import gn.lacaeducation.biometric.domain.*;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * §suivi-examen (BADGES_QR_NFC.md) : un badge peut porter QR et NFC simultanément, tous deux associés
 * à la même personne — l'UID NFC vient de la carte physique réellement présentée (pas généré par le
 * serveur, contrairement au jeton QR), d'où un contrôle de doublon explicite plutôt qu'une rotation.
 */
@ExtendWith(MockitoExtension.class)
class BiometricNfcTest {

    @Mock BiometricDeviceRepository deviceRepo;
    @Mock BiometricEnrollmentRepository enrollmentRepo;
    @Mock BiometricPunchRepository punchRepo;
    @Mock BiometricPunchConflictRepository conflictRepo;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks BiometricService service;

    private BiometricEnrollment enrollment(Long id) {
        return BiometricEnrollment.builder().id(id).tenantId(10L)
                .subjectType(BiometricSubjectType.TEACHER).subjectId(42L)
                .status(EnrollmentStatus.ENROLLED).badgeToken("QRTOKEN").build();
    }

    @Test
    void associateNfcUid_newUid_savesTrimmedValue() {
        when(enrollmentRepo.findById(5L)).thenReturn(Optional.of(enrollment(5L)));
        when(enrollmentRepo.findByNfcUid("ABCD1234")).thenReturn(Optional.empty());
        when(enrollmentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BiometricEnrollment result = service.associateNfcUid(5L, "  ABCD1234  ");

        assertThat(result.getNfcUid()).isEqualTo("ABCD1234");
        assertThat(result.getBadgeToken()).isEqualTo("QRTOKEN"); // le QR n'est jamais touché par l'ajout du NFC
    }

    @Test
    void associateNfcUid_blankValue_clearsExistingUid() {
        BiometricEnrollment e = enrollment(5L);
        e.setNfcUid("OLDUID");
        when(enrollmentRepo.findById(5L)).thenReturn(Optional.of(e));
        when(enrollmentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BiometricEnrollment result = service.associateNfcUid(5L, "  ");

        assertThat(result.getNfcUid()).isNull();
        verify(enrollmentRepo, never()).findByNfcUid(any());
    }

    @Test
    void associateNfcUid_alreadyUsedByAnotherEnrollment_rejected() {
        when(enrollmentRepo.findById(5L)).thenReturn(Optional.of(enrollment(5L)));
        when(enrollmentRepo.findByNfcUid("ABCD1234")).thenReturn(Optional.of(enrollment(9L)));

        assertThatThrownBy(() -> service.associateNfcUid(5L, "ABCD1234"))
                .isInstanceOf(BusinessRuleException.class);
        verify(enrollmentRepo, never()).save(any());
    }

    @Test
    void associateNfcUid_reassigningSameUidToSameEnrollment_allowed() {
        BiometricEnrollment e = enrollment(5L);
        e.setNfcUid("ABCD1234");
        when(enrollmentRepo.findById(5L)).thenReturn(Optional.of(e));
        when(enrollmentRepo.findByNfcUid("ABCD1234")).thenReturn(Optional.of(e));
        when(enrollmentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BiometricEnrollment result = service.associateNfcUid(5L, "ABCD1234");

        assertThat(result.getNfcUid()).isEqualTo("ABCD1234");
    }

    @Test
    void associateNfcUid_unknownEnrollment_throwsNotFound() {
        when(enrollmentRepo.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.associateNfcUid(404L, "ABCD1234"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void resolveBadgeByNfc_knownUid_returnsSubject() {
        BiometricEnrollment e = enrollment(5L);
        e.setNfcUid("ABCD1234");
        when(enrollmentRepo.findByNfcUid("ABCD1234")).thenReturn(Optional.of(e));

        ResolvedBadge resolved = service.resolveBadgeByNfc("ABCD1234");

        assertThat(resolved.subjectType()).isEqualTo(BiometricSubjectType.TEACHER);
        assertThat(resolved.subjectId()).isEqualTo(42L);
    }

    @Test
    void resolveBadgeByNfc_unknownUid_rejected() {
        when(enrollmentRepo.findByNfcUid("GUESS")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolveBadgeByNfc("GUESS")).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void resolveBadgeByNfc_blankUid_rejectedWithoutRepositoryCall() {
        assertThatThrownBy(() -> service.resolveBadgeByNfc(" ")).isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(enrollmentRepo);
    }
}
