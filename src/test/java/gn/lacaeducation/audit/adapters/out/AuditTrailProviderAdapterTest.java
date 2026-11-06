package gn.lacaeducation.audit.adapters.out;

import gn.lacaeducation.audit.AuditTrailProvider.ClassTransferAudit;
import gn.lacaeducation.audit.application.port.in.AuditLogUseCase;
import gn.lacaeducation.audit.domain.AuditLog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * §retour comptable : "je veux savoir les élèves transférés par jour" — jusqu'ici aucune trace datée
 * d'un transfert de classe n'existait nulle part (voir AuditEventListener#onEnrollmentClassChanged,
 * qui pose les lignes "studentId=X;oldClassId=Y;newClassId=Z" que cet adaptateur reparse.
 */
@ExtendWith(MockitoExtension.class)
class AuditTrailProviderAdapterTest {

    @Mock AuditLogUseCase auditLogUseCase;
    @InjectMocks AuditTrailProviderAdapter adapter;

    private AuditLog log(String details, LocalDateTime createdAt) {
        return AuditLog.builder().action("ENROLLMENT_CLASS_CHANGED").details(details).createdAt(createdAt).build();
    }

    @Test
    void findClassTransfersBetween_parsesWellFormedDetails() {
        LocalDateTime from = LocalDateTime.of(2026, 11, 2, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 11, 3, 0, 0);
        LocalDateTime occurredAt = LocalDateTime.of(2026, 11, 2, 14, 30);
        when(auditLogUseCase.searchForCurrentTenant(eq("ENROLLMENT_CLASS_CHANGED"), eq(null), eq(from), eq(to), any()))
                .thenReturn(new PageImpl<>(List.of(
                        log("studentId=53;oldClassId=8;newClassId=6", occurredAt))));

        List<ClassTransferAudit> result = adapter.findClassTransfersBetween(from, to);

        assertThat(result).hasSize(1);
        ClassTransferAudit t = result.get(0);
        assertThat(t.studentId()).isEqualTo(53L);
        assertThat(t.oldClassId()).isEqualTo(8L);
        assertThat(t.newClassId()).isEqualTo(6L);
        assertThat(t.occurredAt()).isEqualTo(occurredAt);
    }

    @Test
    void findClassTransfersBetween_missingDetails_skipsLineInsteadOfThrowing() {
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now();
        when(auditLogUseCase.searchForCurrentTenant(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(log(null, LocalDateTime.now()))));

        List<ClassTransferAudit> result = adapter.findClassTransfersBetween(from, to);

        assertThat(result).isEmpty();
    }

    @Test
    void findClassTransfersBetween_malformedDetails_withoutStudentId_skipsLine() {
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now();
        when(auditLogUseCase.searchForCurrentTenant(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(log("oldClassId=8;newClassId=6", LocalDateTime.now()))));

        List<ClassTransferAudit> result = adapter.findClassTransfersBetween(from, to);

        assertThat(result).isEmpty();
    }

    @Test
    void findClassTransfersBetween_noResults_returnsEmptyList() {
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now();
        when(auditLogUseCase.searchForCurrentTenant(any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        List<ClassTransferAudit> result = adapter.findClassTransfersBetween(from, to);

        assertThat(result).isEmpty();
    }
}
