package gn.lacaeducation.platform.application.port.out;

import gn.lacaeducation.platform.domain.Tenant;

import java.util.List;
import java.util.Optional;

public interface TenantRepository {
    Tenant save(Tenant tenant);
    Optional<Tenant> findById(Long id);
    Optional<Tenant> findByEmail(String email);
    List<Tenant> findAll();
    void deleteById(Long id);
    boolean existsById(Long id);
}
