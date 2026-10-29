package gn.lacaeducation.iam.adapters.out.persistence;

import gn.lacaeducation.iam.domain.RoleCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPublicInfoProviderAdapterTest {

    @Mock UserJpaRepository jpa;

    UserPublicInfoProviderAdapter adapter() {
        return new UserPublicInfoProviderAdapter(jpa);
    }

    private UserJpaEntity user(Long id) {
        UserJpaEntity u = new UserJpaEntity();
        u.setId(id);
        return u;
    }

    /** §retour comptable : un utilisateur cumulant SCHOOL_ADMIN et ACCOUNTANT ne doit être notifié
     * qu'une seule fois, pas une fois par rôle demandé. */
    @Test
    void findUserIdsByRoles_dedupesUsersWithMultipleMatchingRoles() {
        when(jpa.findActiveByTenantIdAndRoleCodesAnyContact(eq(10L), anyCollection()))
                .thenReturn(List.of(user(5L), user(5L), user(6L)));

        List<Long> ids = adapter().findUserIdsByRoles(10L, Set.of("SCHOOL_ADMIN", "ACCOUNTANT"));

        assertThat(ids).containsExactlyInAnyOrder(5L, 6L);
    }

    @Test
    void findUserIdsByRoles_nullTenantOrEmptyRoles_returnsEmpty() {
        assertThat(adapter().findUserIdsByRoles(null, Set.of("ACCOUNTANT"))).isEmpty();
        assertThat(adapter().findUserIdsByRoles(10L, Set.of())).isEmpty();
    }

    @Test
    void findUserIdsByRoles_passesParsedRoleCodesToRepository() {
        when(jpa.findActiveByTenantIdAndRoleCodesAnyContact(eq(10L), anyCollection())).thenReturn(List.of());

        adapter().findUserIdsByRoles(10L, Set.of("ACCOUNTANT"));

        org.mockito.Mockito.verify(jpa).findActiveByTenantIdAndRoleCodesAnyContact(eq(10L), eq(List.of(RoleCode.ACCOUNTANT)));
    }

    // ── findUsersByTenant (§retour comptable : filtre "par utilisateur" du journal d'audit, ne doit
    // pas exiger user:read) ──────────────────────────────────────────────────────────────────────────

    @Test
    void findUsersByTenant_mapsAllUsersOfTenant() {
        UserJpaEntity u1 = user(5L); u1.setFirstName("Awa"); u1.setLastName("Bah");
        UserJpaEntity u2 = user(6L); u2.setFirstName("Moussa"); u2.setLastName("Camara");
        when(jpa.findByTenantId(10L)).thenReturn(List.of(u1, u2));

        List<gn.lacaeducation.iam.UserPublicInfoProvider.UserPublicInfo> result = adapter().findUsersByTenant(10L);

        assertThat(result).extracting(gn.lacaeducation.iam.UserPublicInfoProvider.UserPublicInfo::id)
                .containsExactlyInAnyOrder(5L, 6L);
    }

    @Test
    void findUsersByTenant_nullTenant_returnsEmpty() {
        assertThat(adapter().findUsersByTenant(null)).isEmpty();
    }
}
