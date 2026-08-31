package gn.lacaeducation.platform.adapters.out.persistence;

import gn.lacaeducation.platform.application.port.out.PrefectureRepository;
import gn.lacaeducation.platform.application.port.out.TenantRepository;
import gn.lacaeducation.platform.domain.Prefecture;
import gn.lacaeducation.platform.domain.PrefectureStatus;
import gn.lacaeducation.platform.domain.Tenant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DpeScopeProviderAdapterTest {

    @Mock PrefectureRepository prefectureRepository;
    @Mock TenantRepository tenantRepository;

    DpeScopeProviderAdapter adapter() {
        return new DpeScopeProviderAdapter(prefectureRepository, tenantRepository);
    }

    @Test
    void tenantIdsForUser_resolvesTenantsOfAssignedPrefectures() {
        when(prefectureRepository.findPrefectureIdsByUserId(42L)).thenReturn(List.of(1L, 2L));
        when(tenantRepository.findByPrefectureIdIn(List.of(1L, 2L))).thenReturn(List.of(
                Tenant.builder().id(10L).schoolName("École A").prefectureId(1L).build(),
                Tenant.builder().id(11L).schoolName("École B").prefectureId(2L).build()));

        Set<Long> tenantIds = adapter().tenantIdsForUser(42L);

        assertThat(tenantIds).containsExactlyInAnyOrder(10L, 11L);
    }

    @Test
    void tenantIdsForUser_returnsEmptySet_whenUserHasNoPrefectureAssignment() {
        when(prefectureRepository.findPrefectureIdsByUserId(42L)).thenReturn(List.of());

        Set<Long> tenantIds = adapter().tenantIdsForUser(42L);

        assertThat(tenantIds).isEmpty();
    }

    @Test
    void activePrefectureIds_excludesArchivedPrefectures() {
        when(prefectureRepository.findAll()).thenReturn(List.of(
                Prefecture.builder().id(1L).nom("Coyah").statut(PrefectureStatus.ACTIVE).build(),
                Prefecture.builder().id(2L).nom("Dubréka").statut(PrefectureStatus.ARCHIVEE).build()));

        Set<Long> ids = adapter().activePrefectureIds();

        assertThat(ids).containsExactly(1L);
    }
}
