package gn.lacaeducation.biometric.adapters.in.web;

import gn.lacaeducation.biometric.application.service.BiometricService;
import gn.lacaeducation.biometric.domain.BiometricEnrollment;
import gn.lacaeducation.biometric.domain.BiometricSubjectType;
import gn.lacaeducation.biometric.domain.BiometricType;
import gn.lacaeducation.common.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Biometric Enrollment", description = "Enrôlement des personnes sur les appareils biométriques")
@RestController
@RequestMapping("/api/v1/biometric/enrollments")
@PreAuthorize("hasAuthority('biometrique:manage')")
@RequiredArgsConstructor
public class BiometricEnrollmentController {

    private final BiometricService service;

    public record EnrollRequest(Long deviceId, BiometricSubjectType subjectType, Long subjectId,
                                BiometricType biometricType, String externalUserId) {}

    @Operation(summary = "Lister les enrôlements (filtre appareil)")
    @GetMapping
    public List<BiometricEnrollment> list(@RequestParam(required = false) Long deviceId) {
        return service.enrollments(TenantContext.get(), deviceId);
    }

    @Operation(summary = "Enrôler une personne (mappe son User ID appareil)")
    @PostMapping
    public BiometricEnrollment enroll(@RequestBody EnrollRequest r) {
        return service.enroll(TenantContext.get(), r.deviceId(), r.subjectType(), r.subjectId(), r.biometricType(), r.externalUserId());
    }

    @Operation(summary = "Supprimer un enrôlement")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) { service.deleteEnrollment(id); return ResponseEntity.noContent().build(); }
}
