package gn.lacaeducation.iam.adapters.out.persistence;

import gn.lacaeducation.iam.UserPublicInfoProvider;
import gn.lacaeducation.iam.domain.RoleCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
class UserPublicInfoProviderAdapter implements UserPublicInfoProvider {

    private final UserJpaRepository jpa;

    @Override
    public Optional<UserPublicInfo> findUser(Long userId) {
        if (userId == null) return Optional.empty();
        return jpa.findById(userId).map(u -> new UserPublicInfo(u.getId(), u.getFirstName(), u.getLastName(), u.getPhone()));
    }

    @Override
    public List<Long> findUserIdsByRoles(Long tenantId, Set<String> roleCodes) {
        if (tenantId == null || roleCodes == null || roleCodes.isEmpty()) return List.of();
        List<RoleCode> codes = roleCodes.stream().map(RoleCode::valueOf).toList();
        return jpa.findActiveByTenantIdAndRoleCodesAnyContact(tenantId, codes).stream()
                .map(UserJpaEntity::getId).distinct().toList();
    }
}
