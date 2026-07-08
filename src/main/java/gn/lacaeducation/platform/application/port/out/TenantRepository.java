package gn.lacaeducation.platform.application.port.out;

import gn.lacaeducation.platform.domain.Tenant;

import java.util.List;
import java.util.Optional;

public interface TenantRepository {
    Tenant save(Tenant tenant);
    Optional<Tenant> findById(Long id);
    Optional<Tenant> findByEmail(String email);
    Optional<Tenant> findBySlug(String slug);
    boolean existsByEmail(String email);
    boolean existsBySlug(String slug);
    List<Tenant> findAll();
    void deleteById(Long id);
    boolean existsById(Long id);
}
