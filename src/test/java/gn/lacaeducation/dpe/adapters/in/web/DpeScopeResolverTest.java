package gn.lacaeducation.dpe.adapters.in.web;

import gn.lacaeducation.common.DpeScopeContext;
import gn.lacaeducation.common.DpeScopeProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DpeScopeResolverTest {

    @Mock DpeScopeProvider dpeScopeProvider;

    DpeScopeResolver resolver() {
        return new DpeScopeResolver(dpeScopeProvider);
    }

    private static Authentication auth(String... roles) {
        var authorities = java.util.Arrays.stream(roles).map(SimpleGrantedAuthority::new).toList();
        return new TestingAuthenticationToken("user", "n/a", authorities);
    }

    @AfterEach
    void clearContext() {
        DpeScopeContext.clear();
    }

    @Test
    void resolvePrefectureId_superAdminWithoutPrefectureId_throws() {
        assertThatThrownBy(() -> resolver().resolvePrefectureId(null, auth("ROLE_SUPER_ADMIN")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void resolvePrefectureId_superAdminWithPrefectureId_usesItDirectly() {
        Long result = resolver().resolvePrefectureId(5L, auth("ROLE_SUPER_ADMIN"));
        assertThat(result).isEqualTo(5L);
    }

    @Test
    void resolvePrefectureId_dpeAdminSinglePrefecture_resolvesAutomatically() {
        DpeScopeContext.set(Set.of(1L, 2L), Set.of(10L));

        Long result = resolver().resolvePrefectureId(null, auth("ROLE_DPE_ADMIN"));

        assertThat(result).isEqualTo(10L);
    }

    @Test
    void resolvePrefectureId_dpeAdminMultiplePrefectures_requiresExplicitParam() {
        DpeScopeContext.set(Set.of(1L, 2L), Set.of(10L, 20L));

        assertThatThrownBy(() -> resolver().resolvePrefectureId(null, auth("ROLE_DPE_ADMIN")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void resolvePrefectureId_dpeAdminRequestingUnassignedPrefecture_isDenied() {
        DpeScopeContext.set(Set.of(1L), Set.of(10L));

        assertThatThrownBy(() -> resolver().resolvePrefectureId(999L, auth("ROLE_DPE_ADMIN")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void resolvePrefectureId_dpeAdminWithoutAnyAssignment_returnsNull() {
        // DpeScopeContext jamais peuplé (aucune préfecture affectée) : pas d'exception, périmètre vide.
        Long result = resolver().resolvePrefectureId(null, auth("ROLE_DPE_ADMIN"));
        assertThat(result).isNull();
    }

    @Test
    void resolveTenantScope_superAdminWithoutPrefectureId_throws() {
        assertThatThrownBy(() -> resolver().resolveTenantScope(null, auth("ROLE_SUPER_ADMIN")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void resolveTenantScope_superAdminWithPrefectureId_delegatesToProvider() {
        when(dpeScopeProvider.tenantIdsForPrefecture(5L)).thenReturn(Set.of(100L, 200L));

        Set<Long> result = resolver().resolveTenantScope(5L, auth("ROLE_SUPER_ADMIN"));

        assertThat(result).containsExactlyInAnyOrder(100L, 200L);
    }

    @Test
    void resolveTenantScope_dpeAdmin_usesOwnContext_ignoringPrefectureIdParam() {
        DpeScopeContext.set(Set.of(100L), Set.of(10L));

        // Un DPE_ADMIN qui fournirait un prefectureId (ex. en manipulant l'URL) reste cantonné à son
        // propre périmètre : le paramètre n'est jamais consulté pour ce rôle.
        Set<Long> result = resolver().resolveTenantScope(999L, auth("ROLE_DPE_ADMIN"));

        assertThat(result).containsExactly(100L);
    }
}
