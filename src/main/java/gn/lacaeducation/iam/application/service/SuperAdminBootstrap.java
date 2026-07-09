package gn.lacaeducation.iam.application.service;

import gn.lacaeducation.iam.application.port.out.PasswordHasher;
import gn.lacaeducation.iam.application.port.out.RoleRepository;
import gn.lacaeducation.iam.application.port.out.UserRepository;
import gn.lacaeducation.iam.domain.Role;
import gn.lacaeducation.iam.domain.RoleCode;
import gn.lacaeducation.iam.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Crée le compte SUPER_ADMIN plateforme au démarrage à partir de variables d'environnement, quand il
 * n'existe pas encore. Remplace le seed de test (dev uniquement) pour la production : le mot de passe
 * n'est jamais en base ni en clair dans le code — il vient de {@code BOOTSTRAP_SUPERADMIN_PASSWORD} et
 * est haché par l'application. Idempotent : ne fait rien si l'email existe déjà ou si les variables
 * sont absentes.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SuperAdminBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordHasher passwordHasher;

    @Value("${BOOTSTRAP_SUPERADMIN_EMAIL:}")
    private String email;

    @Value("${BOOTSTRAP_SUPERADMIN_PASSWORD:}")
    private String password;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return; // pas de bootstrap demandé
        }
        if (userRepository.findByEmail(email).isPresent()) {
            log.info("SUPER_ADMIN {} déjà présent — bootstrap ignoré", email);
            return;
        }
        Role role = roleRepository.findByCode(RoleCode.SUPER_ADMIN)
                .orElseThrow(() -> new IllegalStateException("Rôle SUPER_ADMIN non seedé (V3)"));
        User admin = User.builder()
                .tenantId(null) // le SUPER_ADMIN n'appartient à aucun tenant
                .firstName("Super")
                .lastName("Admin")
                .username(UsernameGenerator.generate("Super", "Admin", userRepository::existsByUsername))
                .email(email)
                .passwordHash(passwordHasher.hash(password))
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
        User saved = userRepository.save(admin);
        userRepository.assignRole(saved.getId(), role.getId());
        log.info("SUPER_ADMIN {} créé via bootstrap (id={})", email, saved.getId());
    }
}
