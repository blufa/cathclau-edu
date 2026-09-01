package gn.lacaeducation.iam.adapters.out.persistence;

import gn.lacaeducation.iam.SchoolStaffDirectory;
import gn.lacaeducation.iam.domain.RoleCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
class SchoolStaffDirectoryAdapter implements SchoolStaffDirectory {

    private static final List<RoleCode> GRADE_CHANGE_ROLES =
            List.of(RoleCode.SCHOOL_ADMIN, RoleCode.RESPONSABLE_PEDAGOGIQUE);
    private static final List<RoleCode> SCHOOL_ADMIN_ROLE = List.of(RoleCode.SCHOOL_ADMIN);

    private final UserJpaRepository jpa;

    @Override
    public List<SchoolStaffContact> findGradeChangeRecipients(Long tenantId) {
        if (tenantId == null) return List.of();
        return jpa.findActiveByTenantIdAndRoleCodes(tenantId, GRADE_CHANGE_ROLES).stream()
                .map(u -> new SchoolStaffContact(u.getEmail(),
                        (safe(u.getFirstName()) + " " + safe(u.getLastName())).trim()))
                .toList();
    }

    @Override
    public List<SchoolAdminContact> findSchoolAdminContacts(Long tenantId) {
        if (tenantId == null) return List.of();
        return jpa.findActiveByTenantIdAndRoleCodes(tenantId, SCHOOL_ADMIN_ROLE).stream()
                .map(u -> new SchoolAdminContact(u.getPhone(),
                        (safe(u.getFirstName()) + " " + safe(u.getLastName())).trim()))
                .toList();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
