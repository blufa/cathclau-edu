package gn.lacaeducation.academics.application.port.out;

import gn.lacaeducation.academics.domain.AnneeScolaire;

import java.util.List;
import java.util.Optional;

public interface AnneeScolaireRepository {
    AnneeScolaire save(AnneeScolaire annee);
    Optional<AnneeScolaire> findById(Long id);
    Optional<AnneeScolaire> findCurrent(Long tenantId);
    List<AnneeScolaire> findByTenantId(Long tenantId);
    void removeCurrent(Long tenantId);
}
