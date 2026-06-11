package gn.lacaeducation.iam.adapters.out.persistence;

import gn.lacaeducation.iam.adapters.out.persistence.mapper.IamMapper;
import gn.lacaeducation.iam.application.port.out.RoleRepository;
import gn.lacaeducation.iam.domain.Role;
import gn.lacaeducation.iam.domain.RoleCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class RolePersistenceAdapter implements RoleRepository {

    private final RoleJpaRepository jpaRepository;
    private final IamMapper mapper;

    @Override
    public Optional<Role> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Role> findByCode(RoleCode code) {
        return jpaRepository.findByCode(code).map(mapper::toDomain);
    }

    @Override
    public List<Role> findUserRoles(Long userId) {
        return jpaRepository.findByUserId(userId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
