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

/** Même régression que RolePersistenceAdapterTest, côté permissions directes utilisateur. */
@ExtendWith(MockitoExtension.class)
class PermissionPersistenceAdapterTest {

    @Mock PermissionJpaRepository jpaRepository;
    @Mock RolePermissionJpaRepository rolePermissionJpaRepository;
    @Mock UserPermissionJpaRepository userPermissionJpaRepository;
    @Mock UserRevokedPermissionJpaRepository userRevokedPermissionJpaRepository;
    @Mock IamMapper mapper;

    PermissionPersistenceAdapter adapter() {
        return new PermissionPersistenceAdapter(jpaRepository, rolePermissionJpaRepository, userPermissionJpaRepository,
                userRevokedPermissionJpaRepository, mapper);
    }

    private UserPermissionJpaEntity existing(Long userId, Long permissionId) {
        UserPermissionJpaEntity up = new UserPermissionJpaEntity();
        up.setUserId(userId);
        up.setPermissionId(permissionId);
        return up;
    }

    private UserRevokedPermissionJpaEntity existingRevoked(Long userId, Long permissionId) {
        UserRevokedPermissionJpaEntity rev = new UserRevokedPermissionJpaEntity();
        rev.setUserId(userId);
        rev.setPermissionId(permissionId);
        return rev;
    }

    @Test
    @SuppressWarnings("unchecked")
    void assignDirectPermissions_neverReinsertsAnAlreadyGrantedPermission() {
        when(userPermissionJpaRepository.findByUserId(10L)).thenReturn(List.of(existing(10L, 7L)));

        adapter().assignDirectPermissions(10L, List.of(7L, 9L));

        ArgumentCaptor<UserPermissionJpaEntity> savedCaptor = ArgumentCaptor.forClass(UserPermissionJpaEntity.class);
        verify(userPermissionJpaRepository).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue().getPermissionId()).isEqualTo(9L);

        ArgumentCaptor<List<UserPermissionJpaEntity>> removedCaptor = ArgumentCaptor.forClass(List.class);
        verify(userPermissionJpaRepository).deleteAll(removedCaptor.capture());
        assertThat(removedCaptor.getValue()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void assignDirectPermissions_removesOnlyPermissionsNoLongerRequested() {
        when(userPermissionJpaRepository.findByUserId(10L)).thenReturn(List.of(existing(10L, 7L), existing(10L, 8L)));

        adapter().assignDirectPermissions(10L, List.of(7L));

        ArgumentCaptor<List<UserPermissionJpaEntity>> removedCaptor = ArgumentCaptor.forClass(List.class);
        verify(userPermissionJpaRepository).deleteAll(removedCaptor.capture());
        assertThat(removedCaptor.getValue()).extracting(UserPermissionJpaEntity::getPermissionId).containsExactly(8L);
        verify(userPermissionJpaRepository, never()).save(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void revokePermissions_sameDiffPattern_neverReinsertsAlreadyRevoked() {
        when(userRevokedPermissionJpaRepository.findByUserId(10L)).thenReturn(List.of(existingRevoked(10L, 7L)));

        adapter().revokePermissions(10L, List.of(7L, 9L));

        ArgumentCaptor<UserRevokedPermissionJpaEntity> savedCaptor = ArgumentCaptor.forClass(UserRevokedPermissionJpaEntity.class);
        verify(userRevokedPermissionJpaRepository).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue().getPermissionId()).isEqualTo(9L);

        ArgumentCaptor<List<UserRevokedPermissionJpaEntity>> removedCaptor = ArgumentCaptor.forClass(List.class);
        verify(userRevokedPermissionJpaRepository).deleteAll(removedCaptor.capture());
        assertThat(removedCaptor.getValue()).isEmpty();
    }
}
