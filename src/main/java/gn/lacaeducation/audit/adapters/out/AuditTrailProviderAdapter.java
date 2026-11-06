package gn.lacaeducation.audit.adapters.out;

import gn.lacaeducation.audit.AuditTrailProvider;
import gn.lacaeducation.audit.application.port.in.AuditLogUseCase;
import gn.lacaeducation.audit.domain.AuditLog;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
class AuditTrailProviderAdapter implements AuditTrailProvider {

    private static final String CLASS_CHANGED_ACTION = "ENROLLMENT_CLASS_CHANGED";

    private final AuditLogUseCase auditLogUseCase;

    @Override
    @Transactional(readOnly = true)
    public List<ClassTransferAudit> findClassTransfersBetween(LocalDateTime from, LocalDateTime to) {
        return auditLogUseCase.searchForCurrentTenant(CLASS_CHANGED_ACTION, null, from, to, Pageable.unpaged())
                .stream()
                .map(this::toClassTransferAudit)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    /** Parse "studentId=X;oldClassId=Y;newClassId=Z" posé par
     * AuditEventListener#onEnrollmentClassChanged — {@code null} si absent/mal formé (ligne d'audit
     * antérieure à ce correctif, ou format inattendu). */
    private ClassTransferAudit toClassTransferAudit(AuditLog log) {
        if (log.getDetails() == null) return null;
        Map<String, String> fields = new HashMap<>();
        for (String part : log.getDetails().split(";")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2) fields.put(kv[0], kv[1]);
        }
        Long studentId = parseLongOrNull(fields.get("studentId"));
        if (studentId == null) return null;
        return new ClassTransferAudit(studentId, parseLongOrNull(fields.get("oldClassId")),
                parseLongOrNull(fields.get("newClassId")), log.getCreatedAt());
    }

    private Long parseLongOrNull(String s) {
        if (s == null) return null;
        try { return Long.valueOf(s); } catch (NumberFormatException e) { return null; }
    }
}
