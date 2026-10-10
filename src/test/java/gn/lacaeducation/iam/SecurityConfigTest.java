package gn.lacaeducation.iam;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SUPER_ADMIN est censé "tout pouvoir" en impersonant une école (ALL_ROLES), mais PARENT/STUDENT sont
 * délibérément exclus de ce grant : plusieurs endpoints d'administration les EXCLUENT explicitement
 * ({@code !hasRole('PARENT')}, ex. liste des élèves) pour les réserver au personnel — un SUPER_ADMIN
 * qui "possède" aussi ces rôles se faisait donc bloquer par ses propres droits (constaté en vrai : 403
 * sur la liste des élèves pour le SUPER_ADMIN).
 */
class SecurityConfigTest {

    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractAuthorities(List<String> roles, List<String> permissions) {
        try {
            Jwt jwt = Jwt.withTokenValue("token")
                    .header("alg", "HS256")
                    .claim("sub", "1")
                    .claim("roles", roles)
                    .claim("permissions", permissions)
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();
            Method m = SecurityConfig.class.getDeclaredMethod("extractAuthorities", Jwt.class);
            m.setAccessible(true);
            return (Collection<GrantedAuthority>) m.invoke(null, jwt);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void superAdmin_isNotGrantedParentOrStudentRole() {
        Collection<GrantedAuthority> authorities = extractAuthorities(List.of("SUPER_ADMIN"), List.of("eleve:read"));
        List<String> names = authorities.stream().map(GrantedAuthority::getAuthority).toList();

        assertThat(names).doesNotContain("ROLE_PARENT", "ROLE_STUDENT");
        assertThat(names).contains("ROLE_SUPER_ADMIN", "ROLE_SCHOOL_ADMIN", "ROLE_DIRECTOR",
                "ROLE_ACCOUNTANT", "ROLE_SECRETARY", "ROLE_TEACHER");
    }

    @Test
    void ordinaryRole_keepsOnlyItsOwnRoles() {
        Collection<GrantedAuthority> authorities = extractAuthorities(List.of("PARENT"), List.of());
        List<String> names = authorities.stream().map(GrantedAuthority::getAuthority).toList();

        assertThat(names).containsExactly("ROLE_PARENT");
    }
}
