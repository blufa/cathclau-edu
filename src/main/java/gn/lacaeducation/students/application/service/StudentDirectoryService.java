package gn.lacaeducation.students.application.service;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.students.application.port.out.StudentDirectoryPort;
import gn.lacaeducation.students.domain.StudentDirectoryEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Cross-school student directory (features §E): lets a school verify a transferred student's previous
 * school and last class/promotion decision. Requires an exact matricule or a name of ≥ 3 chars to
 * prevent broad scraping, returns only minimal fields, and audits every lookup.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StudentDirectoryService {

    private final StudentDirectoryPort directory;

    public List<StudentDirectoryEntry> search(String matricule, String name) {
        String m = matricule == null ? "" : matricule.trim();
        String q = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
        if (m.isEmpty() && q.length() < 3) {
            throw new BusinessRuleException("Fournissez un matricule exact ou un nom d'au moins 3 caractères");
        }
        // Audit: cross-tenant access is sensitive — record who searched what.
        log.info("Cross-school directory lookup by tenant={} matricule='{}' name='{}'", TenantContext.get(), m, q);
        return directory.search(m, q);
    }

    /** Suggestions de noms d'écoles de la plateforme (école d'origine d'un élève transféré). */
    public List<String> searchSchools(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (q.length() < 2) return List.of();
        return directory.searchSchools(q);
    }
}
