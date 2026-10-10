package gn.lacaeducation.iam.adapters.out.persistence;

import gn.lacaeducation.iam.UserPublicInfoProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class UserPublicInfoProviderAdapter implements UserPublicInfoProvider {

    private final UserJpaRepository jpa;

    @Override
    public Optional<UserPublicInfo> findUser(Long userId) {
        if (userId == null) return Optional.empty();
        return jpa.findById(userId).map(u -> new UserPublicInfo(u.getId(), u.getFirstName(), u.getLastName(), u.getPhone()));
    }
}
