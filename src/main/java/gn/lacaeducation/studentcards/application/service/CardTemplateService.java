package gn.lacaeducation.studentcards.application.service;

import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.studentcards.application.port.out.CardTemplateRepository;
import gn.lacaeducation.studentcards.domain.CardTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** CRUD for student-card templates; a single default template per tenant. */
@Service
@Transactional
@RequiredArgsConstructor
public class CardTemplateService {

    private final CardTemplateRepository repo;

    public CardTemplate save(CardTemplate t) {
        if (t.getActive() == null) t.setActive(true);
        if (t.getIsDefault() == null) t.setIsDefault(false);
        return repo.save(t);
    }

    public CardTemplate update(Long id, CardTemplate patch) {
        CardTemplate t = load(id);
        t.setName(patch.getName()); t.setLogoUrl(patch.getLogoUrl());
        t.setPrimaryColor(patch.getPrimaryColor()); t.setSecondaryColor(patch.getSecondaryColor());
        t.setBackgroundUrl(patch.getBackgroundUrl()); t.setOrientation(patch.getOrientation());
        t.setRectoConfig(patch.getRectoConfig()); t.setVersoConfig(patch.getVersoConfig());
        if (patch.getActive() != null) t.setActive(patch.getActive());
        return repo.save(t);
    }

    @Transactional(readOnly = true)
    public List<CardTemplate> findByTenant(Long tenantId) { return repo.findByTenantId(tenantId); }

    /** Sets the given template as the tenant's only default. */
    public CardTemplate setDefault(Long tenantId, Long id) {
        CardTemplate target = load(id);
        for (CardTemplate t : repo.findByTenantId(tenantId)) {
            if (Boolean.TRUE.equals(t.getIsDefault()) && !t.getId().equals(id)) {
                t.setIsDefault(false);
                repo.save(t);
            }
        }
        target.setIsDefault(true);
        return repo.save(target);
    }

    public void delete(Long id) { repo.deleteById(id); }

    private CardTemplate load(Long id) { return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Modèle introuvable : " + id)); }
}
