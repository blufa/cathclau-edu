package gn.lacaeducation.iam.application.port.out;

import gn.lacaeducation.iam.domain.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    User save(User user);
    Optional<User> findById(Long id);
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailAndTenantId(String email, Long tenantId);
    boolean existsByTenantIdAndEmail(Long tenantId, String email);
    List<User> findByTenantId(Long tenantId);
    void deleteById(Long id);
    boolean existsById(Long id);
    void assignRole(Long userId, Long roleId);
}
