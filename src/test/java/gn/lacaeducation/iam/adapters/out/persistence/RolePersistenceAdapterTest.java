package gn.lacaeducation.iam.adapters.out.persistence;

import gn.lacaeducation.iam.adapters.out.persistence.mapper.IamMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Régression : re-sauvegarder les permissions d'un rôle en ajoutant de nouvelles permissions à des
 * permissions déjà accordées ne doit jamais tenter de réinsérer une paire (roleId, permissionId) déjà
 * en base — sinon violation de la contrainte unique "role_permissions_role_id_permission_id_key"
 * (Hibernate exécute les INSERT avant les DELETE au flush, donc un delete-then-insert naïf sur la même
 * paire échoue même si le DELETE est bien appelé en premier dans le code).
 */
@ExtendWith(MockitoExtension.class)
class RolePersistenceAdapterTest {

    @Mock RoleJpaRepository jpaRepository;
    @Mock RolePermissionJpaRepository rolePermissionJpaRepository;
    @Mock UserRoleJpaRepository userRoleJpaRepository;
    @Mock IamMapper mapper;

    RolePersistenceAdapter adapter() {
        return new RolePersistenceAdapter(jpaRepository, rolePermissionJpaRepository, userRoleJpaRepository, mapper);
    }

    private RolePermissionJpaEntity existing(Long roleId, Long permissionId) {
        RolePermissionJpaEntity rp = new RolePermissionJpaEntity();
        rp.setRoleId(roleId);
        rp.setPermissionId(permissionId);
        return rp;
    }

    @Test
    @SuppressWarnings("unchecked")
    void assignPermissions_neverReinsertsAnAlreadyGrantedPermission() {
        // Le rôle a déjà la permission 7 ; l'utilisateur coche en plus la permission 9 et sauvegarde.
        when(rolePermissionJpaRepository.findByRoleId(4L)).thenReturn(List.of(existing(4L, 7L)));

        adapter().assignPermissions(4L, List.of(7L, 9L));

        // La permission déjà présente (7) ne doit jamais être re-sauvegardée...
        ArgumentCaptor<RolePermissionJpaEntity> savedCaptor = ArgumentCaptor.forClass(RolePermissionJpaEntity.class);
        verify(rolePermissionJpaRepository).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue().getPermissionId()).isEqualTo(9L);

        // ...et rien n'est supprimé puisque la permission existante (7) reste demandée.
        ArgumentCaptor<List<RolePermissionJpaEntity>> removedCaptor = ArgumentCaptor.forClass(List.class);
        verify(rolePermissionJpaRepository).deleteAll(removedCaptor.capture());
        assertThat(removedCaptor.getValue()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void assignPermissions_removesOnlyPermissionsNoLongerRequested() {
        when(rolePermissionJpaRepository.findByRoleId(4L)).thenReturn(List.of(existing(4L, 7L), existing(4L, 8L)));

        adapter().assignPermissions(4L, List.of(7L));

        ArgumentCaptor<List<RolePermissionJpaEntity>> removedCaptor = ArgumentCaptor.forClass(List.class);
        verify(rolePermissionJpaRepository).deleteAll(removedCaptor.capture());
        assertThat(removedCaptor.getValue()).extracting(RolePermissionJpaEntity::getPermissionId).containsExactly(8L);
        verify(rolePermissionJpaRepository, never()).save(any());
    }
}
