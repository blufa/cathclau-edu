package gn.lacaeducation.iam.adapters.out.persistence;

import gn.lacaeducation.iam.UserPublicInfoProvider;
import gn.lacaeducation.iam.application.port.out.RoleRepository;
import gn.lacaeducation.iam.domain.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
class UserPublicInfoProviderAdapter implements UserPublicInfoProvider {

    private final UserJpaRepository jpa;
    private final RoleRepository roleRepository;

    @Override
    public Optional<UserPublicInfo> findUser(Long userId) {
        if (userId == null) return Optional.empty();
        return jpa.findById(userId).map(u -> new UserPublicInfo(u.getId(), u.getFirstName(), u.getLastName(), u.getPhone()));
    }

    @Override
    public boolean hasAnyRole(Long userId, Set<String> roleCodes) {
        if (userId == null) return false;
        return roleRepository.findUserRoles(userId).stream()
                .map(Role::getCode)
                .filter(java.util.Objects::nonNull)
                .anyMatch(code -> roleCodes.contains(code.name()));
    }
}
