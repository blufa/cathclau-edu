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
        when(jpa.findActiveByTenantIdAndRoleCodes(eq(10L), anyCollection()))
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
        when(jpa.findActiveByTenantIdAndRoleCodes(eq(10L), anyCollection())).thenReturn(List.of());

        adapter().findUserIdsByRoles(10L, Set.of("ACCOUNTANT"));

        org.mockito.Mockito.verify(jpa).findActiveByTenantIdAndRoleCodes(eq(10L), eq(List.of(RoleCode.ACCOUNTANT)));
    }
}
