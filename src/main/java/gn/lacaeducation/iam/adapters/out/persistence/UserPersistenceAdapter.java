package gn.lacaeducation.iam.adapters.out.persistence;

import gn.lacaeducation.iam.adapters.out.persistence.mapper.IamMapper;
import gn.lacaeducation.iam.application.port.out.UserRepository;
import gn.lacaeducation.iam.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class UserPersistenceAdapter implements UserRepository {

    private final UserJpaRepository jpaRepository;
    private final UserRoleJpaRepository userRoleJpaRepository;
    private final IamMapper mapper;

    @Override public User save(User u) { return mapper.toDomain(jpaRepository.save(mapper.toEntity(u))); }
    @Override public Optional<User> findById(Long id) { return jpaRepository.findById(id).map(mapper::toDomain); }
    @Override public Optional<User> findByEmail(String email) { return jpaRepository.findByEmail(email).map(mapper::toDomain); }
    @Override public Optional<User> findByUsername(String username) { return jpaRepository.findByUsername(username).map(mapper::toDomain); }
    @Override public Optional<User> findByPhone(String phone) { return jpaRepository.findFirstByPhone(phone).map(mapper::toDomain); }
    @Override public Optional<User> findByEmailAndTenantId(String email, Long tenantId) { return jpaRepository.findByEmailAndTenantId(email, tenantId).map(mapper::toDomain); }
    @Override public boolean existsByTenantIdAndEmail(Long tenantId, String email) { return jpaRepository.existsByTenantIdAndEmail(tenantId, email); }
    @Override public boolean existsByUsername(String username) { return jpaRepository.existsByUsername(username); }
    @Override public List<User> findByTenantId(Long tenantId) { return jpaRepository.findByTenantId(tenantId).stream().map(mapper::toDomain).toList(); }
    @Override public void deleteById(Long id) { jpaRepository.deleteById(id); }
    @Override public boolean existsById(Long id) { return jpaRepository.existsById(id); }
    @Override public void assignRole(Long userId, Long roleId) {
        UserRoleJpaEntity ur = new UserRoleJpaEntity();
        ur.setUserId(userId); ur.setRoleId(roleId);
        userRoleJpaRepository.save(ur);
    }
}
