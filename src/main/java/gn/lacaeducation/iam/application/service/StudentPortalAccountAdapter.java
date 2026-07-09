package gn.lacaeducation.iam.application.service;

import gn.lacaeducation.iam.application.port.out.PasswordHasher;
import gn.lacaeducation.iam.application.port.out.RoleRepository;
import gn.lacaeducation.iam.application.port.out.UserRepository;
import gn.lacaeducation.iam.domain.Role;
import gn.lacaeducation.iam.domain.RoleCode;
import gn.lacaeducation.iam.domain.User;
import gn.lacaeducation.students.PortalAccountProvisioner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Implémentation iam du port {@link PortalAccountProvisioner} : crée un compte élève (rôle STUDENT)
 * connectable par son <b>matricule</b> (username), sans email. Le login existant accepte déjà
 * « username OU email », donc rien à changer côté authentification.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StudentPortalAccountAdapter implements PortalAccountProvisioner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordHasher passwordHasher;

    @Override
    @Transactional
    public Long createStudentLogin(Long tenantId, String firstName, String lastName, String username, String rawPassword) {
        String finalUsername = uniqueUsername(username);
        User user = User.builder()
                .tenantId(tenantId)
                .firstName(firstName)
                .lastName(lastName)
                .username(finalUsername)
                .email(null) // pas d'email : connexion par matricule
                .passwordHash(passwordHasher.hash(rawPassword))
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
        User saved = userRepository.save(user);

        Role role = roleRepository.findByCode(RoleCode.STUDENT)
                .orElseThrow(() -> new IllegalStateException("Rôle STUDENT non seedé"));
        userRepository.assignRole(saved.getId(), role.getId());
        log.info("Compte portail élève créé (username={}, id={})", finalUsername, saved.getId());
        return saved.getId();
    }

    /** Le matricule est unique par tenant ; on garantit l'unicité globale du username par un suffixe si besoin. */
    private String uniqueUsername(String base) {
        String candidate = base;
        int i = 1;
        while (userRepository.existsByUsername(candidate)) {
            candidate = base + i++;
        }
        return candidate;
    }
}
