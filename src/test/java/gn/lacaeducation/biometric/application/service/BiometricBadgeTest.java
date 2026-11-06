package gn.lacaeducation.biometric.application.service;

import gn.lacaeducation.biometric.application.port.out.BiometricDeviceRepository;
import gn.lacaeducation.biometric.application.port.out.BiometricEnrollmentRepository;
import gn.lacaeducation.biometric.application.port.out.BiometricPunchConflictRepository;
import gn.lacaeducation.biometric.application.port.out.BiometricPunchRepository;
import gn.lacaeducation.biometric.application.service.BiometricService.BadgeResult;
import gn.lacaeducation.biometric.application.service.BiometricService.ResolvedBadge;
import gn.lacaeducation.biometric.domain.*;
import gn.lacaeducation.common.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * §suivi-examen (BADGES_QR_NFC.md) : l'ancien format « BADGE|tenant|type|id » était lisible et
 * devinable — n'importe qui pouvait construire un QR valide pour un identifiant arbitraire sans avoir
 * jamais vu le vrai badge. Le QR encode désormais un jeton opaque aléatoire, résolu uniquement côté
 * serveur.
 */
@ExtendWith(MockitoExtension.class)
class BiometricBadgeTest {

    @Mock BiometricDeviceRepository deviceRepo;
    @Mock BiometricEnrollmentRepository enrollmentRepo;
    @Mock BiometricPunchRepository punchRepo;
    @Mock BiometricPunchConflictRepository conflictRepo;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks BiometricService service;

    private BiometricDevice badgeDevice() {
        return BiometricDevice.builder().id(1L).tenantId(10L).name("Badge QR").active(true).build();
    }

    @Test
    void generateBadge_payloadIsOpaqueToken_neverExposesTenantTypeOrSubjectId() {
        when(deviceRepo.findByTenantId(10L)).thenReturn(List.of(badgeDevice()));
        when(deviceRepo.findById(1L)).thenReturn(Optional.of(badgeDevice()));
        when(enrollmentRepo.findByDeviceIdAndExternalUserId(1L, "BADGE-STUDENT-42")).thenReturn(Optional.empty());
        when(enrollmentRepo.save(any())).thenAnswer(inv -> {
            BiometricEnrollment e = inv.getArgument(0);
            if (e.getId() == null) e.setId(5L);
            return e;
        });

        BadgeResult result = service.generateBadge(10L, BiometricSubjectType.STUDENT, 42L);

        assertThat(result.qrPayload()).startsWith("BADGE2|");
        String token = result.qrPayload().substring("BADGE2|".length());
        assertThat(token).doesNotContain("10", "STUDENT", "42"); // aucune fuite du triplet d'origine
        assertThat(token).hasSize(32); // UUID sans tirets
    }

    @Test
    void generateBadge_calledTwice_rotatesToken_revokingThePreviousOne() {
        when(deviceRepo.findByTenantId(10L)).thenReturn(List.of(badgeDevice()));
        when(deviceRepo.findById(1L)).thenReturn(Optional.of(badgeDevice()));
        BiometricEnrollment existing = BiometricEnrollment.builder().id(5L).tenantId(10L).deviceId(1L)
                .subjectType(BiometricSubjectType.STUDENT).subjectId(42L).externalUserId("BADGE-STUDENT-42")
                .status(EnrollmentStatus.ENROLLED).badgeToken("OLDTOKEN").build();
        when(enrollmentRepo.findByDeviceIdAndExternalUserId(1L, "BADGE-STUDENT-42")).thenReturn(Optional.of(existing));
        ArgumentCaptor<BiometricEnrollment> captor = ArgumentCaptor.forClass(BiometricEnrollment.class);
        when(enrollmentRepo.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        BadgeResult result = service.generateBadge(10L, BiometricSubjectType.STUDENT, 42L);

        String newToken = result.qrPayload().substring("BADGE2|".length());
        assertThat(newToken).isNotEqualTo("OLDTOKEN");
        assertThat(captor.getValue().getBadgeToken()).isEqualTo(newToken);
    }

    @Test
    void resolveBadge_knownToken_returnsSubject() {
        BiometricEnrollment e = BiometricEnrollment.builder().id(5L).tenantId(10L)
                .subjectType(BiometricSubjectType.TEACHER).subjectId(7L)
                .status(EnrollmentStatus.ENROLLED).badgeToken("ABC123").build();
        when(enrollmentRepo.findByBadgeToken("ABC123")).thenReturn(Optional.of(e));

        ResolvedBadge resolved = service.resolveBadge("ABC123");

        assertThat(resolved.subjectType()).isEqualTo(BiometricSubjectType.TEACHER);
        assertThat(resolved.subjectId()).isEqualTo(7L);
    }

    @Test
    void resolveBadge_unknownToken_rejected_neverLeaksWhetherIdExists() {
        when(enrollmentRepo.findByBadgeToken("GUESS")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolveBadge("GUESS")).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void resolveBadge_blankToken_rejectedWithoutRepositoryCall() {
        assertThatThrownBy(() -> service.resolveBadge(" ")).isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(enrollmentRepo);
    }
}
