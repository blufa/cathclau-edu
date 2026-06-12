package gn.lacaeducation.iam.application.service;

import gn.lacaeducation.iam.application.port.out.PasswordHasher;
import gn.lacaeducation.iam.application.port.out.RefreshTokenRepository;
import gn.lacaeducation.iam.application.port.out.RoleRepository;
import gn.lacaeducation.iam.application.port.out.UserRepository;
import gn.lacaeducation.iam.domain.Role;
import gn.lacaeducation.iam.domain.RoleCode;
import gn.lacaeducation.iam.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Reacts to platform tenant lifecycle events: provisions the initial SCHOOL_ADMIN account on tenant
 * creation and invalidates active sessions when a tenant/subscription is suspended. Default roles and
 * permissions are seeded globally by Flyway (V3/V16), so provisioning only creates the admin user and
 * binds it to the SCHOOL_ADMIN role.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TenantProvisioningService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordHasher passwordHasher;

    /** Creates the tenant's ADMIN_ECOLE (SCHOOL_ADMIN) user. Idempotent on (tenant, email). */
    public void provisionTenantAdmin(Long tenantId, String schoolName, String adminEmail, String director) {
        if (adminEmail == null || adminEmail.isBlank()) {
            log.warn("Cannot provision admin for tenant {}: no admin email provided", tenantId);
            return;
        }
        if (userRepository.existsByTenantIdAndEmail(tenantId, adminEmail)) {
            log.info("Admin user already exists for tenant {} ({})", tenantId, adminEmail);
            return;
        }

        String[] name = splitName(director, schoolName);
        // Random initial password: the admin activates the account via the password-reset flow.
        User admin = User.builder()
                .tenantId(tenantId)
                .firstName(name[0])
                .lastName(name[1])
                .email(adminEmail)
                .passwordHash(passwordHasher.hash(UUID.randomUUID().toString()))
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
        User saved = userRepository.save(admin);

        Role schoolAdmin = roleRepository.findByCode(RoleCode.SCHOOL_ADMIN)
                .orElseThrow(() -> new IllegalStateException("SCHOOL_ADMIN role not seeded"));
        userRepository.assignRole(saved.getId(), schoolAdmin.getId());
        log.info("Provisioned SCHOOL_ADMIN user {} for tenant {}", saved.getId(), tenantId);
    }

    /** Revokes every refresh token of a tenant's users, forcing re-authentication. */
    public void revokeTenantSessions(Long tenantId) {
        if (tenantId == null) return;
        userRepository.findByTenantId(tenantId)
                .forEach(u -> refreshTokenRepository.revokeAllByUserId(u.getId()));
        log.info("Revoked active sessions for tenant {}", tenantId);
    }

    private String[] splitName(String director, String schoolName) {
        if (director != null && !director.isBlank()) {
            String[] parts = director.trim().split("\\s+", 2);
            return parts.length == 2 ? new String[]{parts[0], parts[1]} : new String[]{"Admin", parts[0]};
        }
        return new String[]{"Admin", schoolName != null && !schoolName.isBlank() ? schoolName : "School"};
    }
}
