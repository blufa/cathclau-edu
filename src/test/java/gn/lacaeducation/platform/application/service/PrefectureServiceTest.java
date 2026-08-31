package gn.lacaeducation.platform.application.service;

import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.platform.application.port.in.PrefectureUseCase.PrefectureCommand;
import gn.lacaeducation.platform.application.port.out.PrefectureRepository;
import gn.lacaeducation.platform.application.port.out.TenantRepository;
import gn.lacaeducation.platform.domain.Prefecture;
import gn.lacaeducation.platform.domain.PrefectureStatus;
import gn.lacaeducation.platform.domain.PrefectureType;
import gn.lacaeducation.platform.domain.Tenant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrefectureServiceTest {

    @Mock PrefectureRepository repository;
    @Mock TenantRepository tenantRepository;

    PrefectureService service() {
        return new PrefectureService(repository, tenantRepository);
    }

    @Test
    void create_defaultsCountryAndActiveStatus() {
        when(repository.save(any(Prefecture.class))).thenAnswer(i -> i.getArgument(0));

        Prefecture p = service().create(new PrefectureCommand("Coyah", PrefectureType.DPE, "Kindia", null,
                null, null));

        assertThat(p.getPays()).isEqualTo("Guinée");
        assertThat(p.getStatut()).isEqualTo(PrefectureStatus.ACTIVE);
        assertThat(p.getNom()).isEqualTo("Coyah");
    }

    @Test
    void archive_thenReactivate_togglesStatus() {
        Prefecture existing = Prefecture.builder().id(1L).nom("Coyah").type(PrefectureType.DPE)
                .pays("Guinée").statut(PrefectureStatus.ACTIVE).build();
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any(Prefecture.class))).thenAnswer(i -> i.getArgument(0));

        Prefecture archived = service().archive(1L);
        assertThat(archived.getStatut()).isEqualTo(PrefectureStatus.ARCHIVEE);

        Prefecture reactivated = service().reactivate(1L);
        assertThat(reactivated.getStatut()).isEqualTo(PrefectureStatus.ACTIVE);
    }

    @Test
    void archive_unknownPrefecture_throwsNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().archive(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void assignSchool_setsTenantPrefectureId() {
        when(repository.findById(1L)).thenReturn(Optional.of(
                Prefecture.builder().id(1L).nom("Coyah").statut(PrefectureStatus.ACTIVE).build()));
        Tenant tenant = Tenant.builder().id(5L).schoolName("École A").build();
        when(tenantRepository.findById(5L)).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(i -> i.getArgument(0));

        Tenant saved = service().assignSchool(1L, 5L);

        assertThat(saved.getPrefectureId()).isEqualTo(1L);
    }

    @Test
    void assignSchool_unknownPrefecture_throwsNotFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().assignSchool(1L, 5L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void unassignSchool_clearsTenantPrefectureId() {
        Tenant tenant = Tenant.builder().id(5L).schoolName("École A").prefectureId(1L).build();
        when(tenantRepository.findById(5L)).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(i -> i.getArgument(0));

        Tenant saved = service().unassignSchool(5L);

        assertThat(saved.getPrefectureId()).isNull();
    }

    @Test
    void schoolsOf_returnsTenantsForThatPrefectureOnly() {
        when(repository.findById(1L)).thenReturn(Optional.of(
                Prefecture.builder().id(1L).nom("Coyah").statut(PrefectureStatus.ACTIVE).build()));
        when(tenantRepository.findByPrefectureIdIn(Set.of(1L))).thenReturn(
                List.of(Tenant.builder().id(5L).schoolName("École A").prefectureId(1L).build()));

        List<Tenant> schools = service().schoolsOf(1L);

        assertThat(schools).extracting(Tenant::getId).containsExactly(5L);
    }

    @Test
    void assignUser_delegatesToRepository() {
        when(repository.findById(1L)).thenReturn(Optional.of(
                Prefecture.builder().id(1L).nom("Coyah").statut(PrefectureStatus.ACTIVE).build()));

        service().assignUser(1L, 42L);

        verify(repository).assignUser(42L, 1L);
    }
}
